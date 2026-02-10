package iuh.fit.se.servicemusic.service;

import iuh.fit.se.servicemusic.dto.request.*;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;

import java.util.List;
import java.util.UUID;

public interface GenreService {
    GenreResponse createGenre(GenreCreationRequest request);
    List<GenreResponse> getAllGenres();
    GenreResponse updateGenre(UUID id, GenreUpdateRequest request);
    void deleteGenre(UUID id);
}