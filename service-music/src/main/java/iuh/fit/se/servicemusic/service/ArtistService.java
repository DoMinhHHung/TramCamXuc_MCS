package iuh.fit.se.servicemusic.service;

import iuh.fit.se.servicemusic.dto.request.ArtistRegistrationRequest;
import iuh.fit.se.servicemusic.dto.response.ArtistResponse;

import java.util.UUID;

public interface ArtistService {
    ArtistResponse registerArtist(UUID userId, ArtistRegistrationRequest request);
    ArtistResponse getArtistProfile(UUID userId);
}