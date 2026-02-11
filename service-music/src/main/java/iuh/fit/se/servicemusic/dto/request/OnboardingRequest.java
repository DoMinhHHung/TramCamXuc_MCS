package iuh.fit.se.servicemusic.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OnboardingRequest {
    @NotEmpty(message = "You must select at least 1 favorite genre")
    @Size(min = 1, max = 5, message = "Please select between 1 and 5 favorite genres")
    private List<UUID> favoriteGenreIds;

    @NotEmpty(message = "You must select at least 1 favorite artist")
    @Size(min = 1, max = 3, message = "Please select between 1 and 3 favorite artists")
    private List<UUID> favoriteArtistIds;
}