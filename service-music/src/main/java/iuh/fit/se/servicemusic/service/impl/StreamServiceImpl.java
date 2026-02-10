package iuh.fit.se.servicemusic.service.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import iuh.fit.se.servicemusic.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamServiceImpl implements StreamService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Selects the HLS manifest path for a song based on the requested quality and returns it as a streaming response.
     *
     * @param songId      the UUID of the song whose manifest is requested
     * @param userQuality desired quality label; accepted values are "LOSSLESS", "HIGH", or "NORMAL" (defaults to "NORMAL" when null)
     * @return            a StreamResponse containing the manifest file stream and its determined media type
     */
    @Override
    public StreamResponse getSongManifest(UUID songId, String userQuality) {
        String path;
        if (userQuality == null) userQuality = "NORMAL";

        switch (userQuality.toUpperCase()) {
            case "LOSSLESS":
                path = "hls/" + songId + "/master.m3u8";
                break;
            case "HIGH":
                path = "hls/" + songId + "/256kbps/index.m3u8";
                break;
            default:
                path = "hls/" + songId + "/128kbps/index.m3u8";
                break;
        }

        log.info("Streaming song {} for quality {}", songId, userQuality);
        return streamFileFromMinio(path);
    }

    /**
     * Fetches and returns a streaming response for a specific HLS segment of a song.
     *
     * @param songId   the UUID of the song
     * @param quality  the quality directory for the segment (e.g., "128kbps", "256kbps", "LOSSLESS")
     * @param fileName the segment file name (for example, "segment.ts" or an HLS playlist file)
     * @return         a StreamResponse containing the object's input stream and its HTTP media type for the requested HLS segment
     */
    @Override
    public StreamResponse getSongSegment(UUID songId, String quality, String fileName) {
        String path = "hls/" + songId + "/" + quality + "/" + fileName;
        return streamFileFromMinio(path);
    }

    /**
     * Retrieves an object from MinIO for the given object key and prepares it as a streaming response.
     *
     * @param objectKey the MinIO object key (path) to fetch from the configured bucket
     * @return a StreamResponse containing an InputStreamResource for the object's stream and the resolved MediaType
     * @throws RuntimeException if the object cannot be retrieved from MinIO
     */
    private StreamResponse streamFileFromMinio(String objectKey) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            MediaType contentType = determineContentType(objectKey);
            return new StreamResponse(new InputStreamResource(stream), contentType);
        } catch (Exception e) {
            log.error("Lỗi lấy file MinIO: {}", objectKey, e);
            throw new RuntimeException("Không tìm thấy file nhạc");
        }
    }

    /**
     * Resolve HTTP media type from a file name or path based on its extension.
     *
     * @param fileName the file name or path used to determine the media type
     * @return `application/vnd.apple.mpegurl` for `.m3u8`, `video/MP2T` for `.ts`, or `application/octet-stream` otherwise
     */
    private MediaType determineContentType(String fileName) {
        if (fileName.endsWith(".m3u8")) return MediaType.parseMediaType("application/vnd.apple.mpegurl");
        if (fileName.endsWith(".ts")) return MediaType.parseMediaType("video/MP2T");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}