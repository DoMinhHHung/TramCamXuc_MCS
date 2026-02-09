package iuh.fit.se.servicetranscode.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodeService {
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffprobe.path}")
    private String ffprobePath;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("transcode-worker-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
    );

    private static final Map<String, Integer> QUALITY_BITRATES = new LinkedHashMap<>() {{
        put("128kbps", 128000);
        put("256kbps", 256000);
        put("320kbps", 320000);
    }};

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "mp3", "mp4", "m4a", "wav", "flac", "aac", "ogg", "wma", "webm", "mkv", "avi", "mov"
    );

    private static final long TRANSCODE_TIMEOUT_MINUTES = 15;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public TranscodeResult processAudio(UUID songId, String rawObjectKey) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Starting transcode for songId: {}, objectKey: {}", songId, rawObjectKey);

        Path tempDir = Files.createTempDirectory("transcode_" + songId);

        try {
            File inputFile = downloadFromMinio(rawObjectKey, tempDir);
            File audioFile = prepareAudioFile(inputFile, tempDir);
            int duration = probeDuration(audioFile);

            String s3PathPrefix = "hls/" + songId + "/";

            List<CompletableFuture<String>> futures = QUALITY_BITRATES.entrySet()
                    .stream()
                    .map(entry -> CompletableFuture.supplyAsync(() ->
                                    transcodeQualityWithRetry(songId, audioFile, tempDir, s3PathPrefix,
                                            entry.getKey(), entry.getValue()),
                            executorService
                    ))
                    .collect(Collectors.toList());

            List<String> qualityPlaylists = futures.stream()
                    .map(f -> {
                        try {
                            return f.get(TRANSCODE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                        } catch (TimeoutException e) {
                            log.error("Transcode timeout for songId: {}", songId);
                            throw new RuntimeException("Transcode timeout", e);
                        } catch (Exception e) {
                            log.error("Transcode failed for songId: {}", songId, e);
                            throw new RuntimeException("Transcode failed", e);
                        }
                    })
                    .collect(Collectors.toList());

            String masterPlaylistKey = uploadMasterPlaylist(s3PathPrefix, qualityPlaylists);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Completed transcode for songId: {} in {}ms", songId, totalTime);

            return new TranscodeResult(masterPlaylistKey, duration, qualityPlaylists);

        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    private File downloadFromMinio(String rawObjectKey, Path tempDir) throws Exception {
        String extension = getFileExtension(rawObjectKey);
        File inputFile = tempDir.resolve("input." + extension).toFile();

        log.debug("Downloading file from MinIO: {}", rawObjectKey);
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(rawObjectKey)
                .build())) {
            Files.copy(stream, inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return inputFile;
    }

    private File prepareAudioFile(File inputFile, Path tempDir) throws Exception {
        String extension = getFileExtension(inputFile.getName());

        if (isVideoFormat(extension)) {
            log.info("Detected video format, extracting audio: {}", inputFile.getName());
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);
            return extractAudioFromVideo(inputFile, tempDir, ffmpeg, ffprobe);
        }

        return inputFile;
    }

    private int probeDuration(File audioFile) throws Exception {
        FFprobe ffprobe = new FFprobe(ffprobePath);
        FFmpegProbeResult probeResult = ffprobe.probe(audioFile.getAbsolutePath());
        return (int) probeResult.getFormat().duration;
    }

    private String transcodeQualityWithRetry(UUID songId, File audioFile, Path tempDir,
                                             String s3PathPrefix, String qualityName, int bitrate) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                return transcodeQuality(songId, audioFile, tempDir, s3PathPrefix, qualityName, bitrate);
            } catch (Exception e) {
                attempt++;
                lastException = e;
                log.warn("Transcode attempt {} failed for quality: {} - songId: {}", attempt, qualityName, songId);

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Transcode interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Transcode failed after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    private String transcodeQuality(UUID songId, File audioFile, Path tempDir,
                                    String s3PathPrefix, String qualityName, int bitrate) throws Exception {
        log.info("Transcoding quality: {} for songId: {}", qualityName, songId);

        File qualityDir = tempDir.resolve("hls").resolve(qualityName).toFile();
        if (!qualityDir.mkdirs() && !qualityDir.exists()) {
            throw new IOException("Failed to create quality directory: " + qualityDir);
        }

        String playlistName = "index.m3u8";
        String segmentPattern = qualityDir.getAbsolutePath() + "/segment_%03d.ts";

        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFprobe ffprobe = new FFprobe(ffprobePath);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(audioFile.getAbsolutePath())
                .addOutput(qualityDir.getAbsolutePath() + "/" + playlistName)
                .setFormat("hls")
                .setAudioCodec("aac")
                .setAudioBitRate(bitrate)
                .setAudioSampleRate(44100)
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", segmentPattern)
                .addExtraArgs("-hls_playlist_type", "vod")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        uploadHLSFiles(qualityDir, s3PathPrefix + qualityName + "/");

        log.info("Completed transcoding quality: {} for songId: {}", qualityName, songId);
        return qualityName;
    }

    private void uploadHLSFiles(File qualityDir, String s3Prefix) throws Exception {
        File[] files = qualityDir.listFiles();
        if (files == null || files.length == 0) {
            throw new IOException("No files generated in quality directory: " + qualityDir);
        }

        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();

        for (File file : files) {
            CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
                String objectKey = s3Prefix + file.getName();
                try (FileInputStream fis = new FileInputStream(file)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(fis, file.length(), -1)
                            .contentType(getContentType(file.getName()))
                            .build());
                    log.debug("Uploaded: {}", objectKey);
                } catch (Exception e) {
                    log.error("Failed to upload file: {}", objectKey, e);
                    throw new RuntimeException("Upload failed: " + objectKey, e);
                }
            }, executorService);

            uploadFutures.add(uploadFuture);
        }

        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0]))
                .get(5, TimeUnit.MINUTES);
    }

    private String uploadMasterPlaylist(String s3PathPrefix, List<String> qualityPlaylists) throws Exception {
        String masterPlaylistContent = generateMasterPlaylist(qualityPlaylists);
        String masterPlaylistKey = s3PathPrefix + "master.m3u8";

        byte[] masterBytes = masterPlaylistContent.getBytes();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(masterPlaylistKey)
                .stream(new java.io.ByteArrayInputStream(masterBytes), masterBytes.length, -1)
                .contentType("application/vnd.apple.mpegurl")
                .build());

        log.info("Uploaded master playlist: {}", masterPlaylistKey);
        return masterPlaylistKey;
    }

    private String generateMasterPlaylist(List<String> qualityLevels) {
        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");

        for (String quality : qualityLevels) {
            int bitrate = QUALITY_BITRATES.get(quality);
            sb.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(bitrate)
                    .append(",CODECS=\"mp4a.40.2\",NAME=\"").append(quality).append("\"\n");
            sb.append(quality).append("/index.m3u8\n");
        }

        return sb.toString();
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        } else if (fileName.endsWith(".ts")) {
            return "video/MP2T";
        } else if (fileName.endsWith(".aac")) {
            return "audio/aac";
        }
        return "application/octet-stream";
    }

    private void cleanupTempDirectory(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                log.debug("Cleaned up temp directory: {}", tempDir);
            }
        } catch (IOException e) {
            log.error("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    private String getFileExtension(String objectKey) {
        if (objectKey == null || !objectKey.contains(".")) {
            return "mp3";
        }
        String extension = objectKey.substring(objectKey.lastIndexOf(".") + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension) ? extension : "mp3";
    }

    private boolean isVideoFormat(String extension) {
        return Set.of("mp4", "mkv", "avi", "mov", "webm").contains(extension.toLowerCase());
    }

    private File extractAudioFromVideo(File videoFile, Path tempDir, FFmpeg ffmpeg, FFprobe ffprobe) throws Exception {
        File audioFile = tempDir.resolve("extracted_audio.mp3").toFile();

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFile.getAbsolutePath())
                .addOutput(audioFile.getAbsolutePath())
                .setFormat("mp3")
                .setAudioCodec("libmp3lame")
                .setAudioBitRate(320000)
                .addExtraArgs("-vn")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        log.info("Extracted audio from video: {} -> {}", videoFile.getName(), audioFile.getName());
        return audioFile;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down transcode executor service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public record TranscodeResult(String masterUrl, int duration, List<String> qualityLevels) {
    }
}