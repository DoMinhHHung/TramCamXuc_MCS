package iuh.fit.se.servicemusic.service.impl;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import iuh.fit.se.servicemusic.dto.request.SongCreationRequest;
import iuh.fit.se.servicemusic.dto.response.PresignedUrlResponse;
import iuh.fit.se.servicemusic.entity.Song;
import iuh.fit.se.servicemusic.entity.enums.Status;
import iuh.fit.se.servicemusic.exception.AppException;
import iuh.fit.se.servicemusic.exception.ErrorCode;
import iuh.fit.se.servicemusic.repository.SongRepository;
import iuh.fit.se.servicemusic.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {
    private final SongRepository songRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public PresignedUrlResponse getPresignedUrl(String fileName) {
        String objectName = "raw/" + UUID.randomUUID() + "_" + fileName;
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(10, TimeUnit.MINUTES)
                            .build());

            return PresignedUrlResponse.builder()
                    .uploadUrl(url)
                    .objectName(objectName)
                    .build();
        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public Song createSong(SongCreationRequest request, String objectName) {
        Song song = Song.builder()
                .title(request.getTitle())
                .artistId(request.getArtistId())
                .rawUrl(objectName)
                .status(Status.PENDING)
                .build();

        return songRepository.save(song);
    }
}
