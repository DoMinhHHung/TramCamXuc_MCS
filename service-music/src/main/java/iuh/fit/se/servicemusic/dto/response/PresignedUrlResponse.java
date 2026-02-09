package iuh.fit.se.servicemusic.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PresignedUrlResponse {
    private String uploadUrl;
    private String objectName;
    private UUID songId;
}