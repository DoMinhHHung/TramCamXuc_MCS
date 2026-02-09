package iuh.fit.se.servicemusic.service.impl;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import iuh.fit.se.servicemusic.config.RabbitMQConfig;
import iuh.fit.se.servicemusic.dto.event.TranscodeRequestEvent;
import iuh.fit.se.servicemusic.dto.request.SongCreationRequest;
import iuh.fit.se.servicemusic.dto.response.PresignedUrlResponse;
import iuh.fit.se.servicemusic.entity.Song;
import iuh.fit.se.servicemusic.entity.enums.Status;
import iuh.fit.se.servicemusic.exception.AppException;
import iuh.fit.se.servicemusic.exception.ErrorCode;
import iuh.fit.se.servicemusic.repository.SongRepository;
import iuh.fit.se.servicemusic.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongServiceImpl implements SongService {
    private final SongRepository songRepository;
    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "mp4", "m4a", "wav", "flac", "aac", "ogg", "wma", "webm", "mkv", "avi", "mov"
    );

    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{1,200}\\.(mp3|mp4|m4a|wav|flac|aac|ogg|wma|webm|mkv|avi|mov)$"
    );

    private static final long MAX_FILE_SIZE_MB = 500;
    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 5;

    @Override
    public PresignedUrlResponse getPresignedUrl(String fileName) {
        validateFileName(fileName);

        String sanitizedFileName = sanitizeFileName(fileName);
        String objectName = "raw/" + UUID.randomUUID() + "_" + sanitizedFileName;

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(PRESIGNED_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .build());

            log.info("Generated presigned URL for file: {}, objectName: {}", fileName, objectName);

            return PresignedUrlResponse.builder()
                    .uploadUrl(url)
                    .objectName(objectName)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {}", fileName, e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional
    public Song createSong(SongCreationRequest request, String objectName) {
        validateObjectName(objectName);

        Song song = Song.builder()
                .title(request.getTitle())
                .artistId(request.getArtistId())
                .rawUrl(objectName)
                .status(Status.PENDING)
                .build();

        Song savedSong = songRepository.save(song);
        log.info("Created song with ID: {}, title: {}", savedSong.getId(), savedSong.getTitle());

        TranscodeRequestEvent event = new TranscodeRequestEvent(savedSong.getId(), objectName);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.TRANSCODE_ROUTING_KEY, event);

        log.info("Sent transcode request for songId: {}", savedSong.getId());

        return savedSong;
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        if (fileName.length() > 255) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        if (!VALID_FILENAME_PATTERN.matcher(fileName).matches()) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        String extension = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }

    private void validateObjectName(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        if (!objectName.startsWith("raw/")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }

        String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
        if (!fileName.matches("^[a-f0-9-]{36}_[a-zA-Z0-9._-]+\\.(mp3|mp4|m4a|wav|flac|aac|ogg|wma|webm|mkv|avi|mov)$")) {
            throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }

    private String sanitizeFileName(String fileName) {
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = getFileExtension(fileName);

        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");

        baseName = baseName.replaceAll("_{2,}", "_");

        if (baseName.length() > 200) {
            baseName = baseName.substring(0, 200);
        }

        return baseName + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}