package iuh.fit.se.servicemusic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArtistRegistrationRequest {
    @NotBlank(message = "Stage name is required")
    private String stageName;

    private String bio;
    private String avatarUrl;
    private String coverImageUrl;

    @NotNull(message = "You must accept the terms and conditions")
    private Boolean termsAccepted;
}