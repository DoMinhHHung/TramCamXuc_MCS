package iuh.fit.se.servicemusic.dto.response;

import iuh.fit.se.servicemusic.entity.enums.ArtistStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ArtistResponse {
    private UUID id;
    private UUID userId;
    private String stageName;
    private String bio;
    private String avatarUrl;
    private String coverImageUrl;
    private ArtistStatus status;
    private LocalDateTime createdAt;
}