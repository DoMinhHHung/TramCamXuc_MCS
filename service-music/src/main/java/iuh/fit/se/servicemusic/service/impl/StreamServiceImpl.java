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

    @Override
    public StreamResponse getSongSegment(UUID songId, String quality, String fileName) {
        String path = "hls/" + songId + "/" + quality + "/" + fileName;
        return streamFileFromMinio(path);
    }

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

    private MediaType determineContentType(String fileName) {
        if (fileName.endsWith(".m3u8")) return MediaType.parseMediaType("application/vnd.apple.mpegurl");
        if (fileName.endsWith(".ts")) return MediaType.parseMediaType("video/MP2T");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
