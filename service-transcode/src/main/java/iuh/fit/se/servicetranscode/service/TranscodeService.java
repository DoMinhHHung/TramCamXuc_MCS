package iuh.fit.se.servicetranscode.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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

    private static final Map<String, Integer> QUALITY_BITRATES = new LinkedHashMap<>() {{
        put("128kbps", 128000);
        put("256kbps", 256000);
        put("320kbps", 320000);
    }};

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "mp3", "mp4", "m4a", "wav", "flac", "aac", "ogg", "wma", "webm", "mkv", "avi", "mov", "-vn"
    );
    public TranscodeResult processAudio(UUID songId, String rawObjectKey) throws Exception {
        Path tempDir = Files.createTempDirectory("transcode_" + songId);
        
        try {
            String extension = getFileExtension(rawObjectKey);
            File inputFile = tempDir.resolve("input." + extension).toFile();

            try (var stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName).object(rawObjectKey).build())) {
                Files.copy(stream, inputFile.toPath());
            }

            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);

            File audioFile = inputFile;
            if (isVideoFormat(extension)) {
                audioFile = extractAudioFromVideo(inputFile, tempDir, ffmpeg, ffprobe);
                log.info("Extracted audio from video file for song {}", songId);
            }

            FFmpegProbeResult probeResult = ffprobe.probe(audioFile.getAbsolutePath());
            int duration = (int) probeResult.getFormat().duration;

            String s3PathPrefix = "hls/" + songId + "/";
            List<String> qualityPlaylists = new ArrayList<>();

            for (Map.Entry<String, Integer> quality : QUALITY_BITRATES.entrySet()) {
                String qualityName = quality.getKey();
                int bitrate = quality.getValue();

                File qualityDir = tempDir.resolve("hls").resolve(qualityName).toFile();
                qualityDir.mkdirs();

                String playlistName = "index.m3u8";
                String segmentPattern = qualityDir.getAbsolutePath() + "/segment_%03d.ts";

                FFmpegBuilder builder = new FFmpegBuilder()
                        .setInput(audioFile.getAbsolutePath())
                        .addOutput(qualityDir.getAbsolutePath() + "/" + playlistName)
                        .setFormat("hls")
                        .setAudioCodec("aac")
                        .setAudioBitRate(bitrate)
                        .addExtraArgs("-hls_time", "10")  
                        .addExtraArgs("-hls_list_size", "0") 
                        .addExtraArgs("-hls_segment_filename", segmentPattern)
                        .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                        .done();

                FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                executor.createJob(builder).run();

                log.info("Transcoded {} quality for song {}", qualityName, songId);

                File[] files = qualityDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String objectKey = s3PathPrefix + qualityName + "/" + file.getName();
                        try (FileInputStream fis = new FileInputStream(file)) {
                            minioClient.putObject(PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .stream(fis, file.length(), -1)
                                    .contentType(getContentType(file.getName()))
                                    .build());
                        }
                    }
                }

                qualityPlaylists.add(qualityName);
            }

            String masterPlaylistContent = generateMasterPlaylist(qualityPlaylists);
            String masterPlaylistKey = s3PathPrefix + "master.m3u8";
            
            byte[] masterBytes = masterPlaylistContent.getBytes();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(masterPlaylistKey)
                    .stream(new java.io.ByteArrayInputStream(masterBytes), masterBytes.length, -1)
                    .contentType("application/vnd.apple.mpegurl")
                    .build());

            log.info("Created master playlist for song {}", songId);

            return new TranscodeResult(masterPlaylistKey, duration, new ArrayList<>(QUALITY_BITRATES.keySet()));

        } finally {
            cleanupTempDirectory(tempDir);
        }
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
                log.info("Cleaned up temp directory: {}", tempDir);
            }
        } catch (IOException e) {
            log.error("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    public record TranscodeResult(String masterUrl, int duration, List<String> qualityLevels) {}

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
                .setAudioBitRate(320000) // Extract với quality cao nhất
                .addExtraArgs("-vn") // Bỏ video stream
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        log.info("Extracted audio from video: {} -> {}", videoFile.getName(), audioFile.getName());
        return audioFile;
    }
}
