package iuh.fit.se.servicemusic.service;

import iuh.fit.se.servicemusic.dto.request.GenreCreationRequest;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;

import java.util.List;

public interface GenreService {
    GenreResponse createGenre(GenreCreationRequest request);
    List<GenreResponse> getAllGenres();
}