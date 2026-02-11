package iuh.fit.se.servicemusic.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserPreferenceResponse {
    private UUID userId;
    private List<GenreResponse> favoriteGenres;
    private List<ArtistResponse> favoriteArtists;
    private boolean onboardingCompleted;
}