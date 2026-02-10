package iuh.fit.se.servicemusic.service.impl;

import iuh.fit.se.servicemusic.dto.mapper.GenreMapper;
import iuh.fit.se.servicemusic.dto.request.GenreCreationRequest;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;
import iuh.fit.se.servicemusic.entity.Genre;
import iuh.fit.se.servicemusic.exception.AppException;
import iuh.fit.se.servicemusic.exception.ErrorCode;
import iuh.fit.se.servicemusic.repository.GenreRepository;
import iuh.fit.se.servicemusic.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    public GenreResponse createGenre(GenreCreationRequest request) {
        if (genreRepository.existsByKey(request.getKey())) {
            throw new AppException(ErrorCode.GENRE_EXISTED);
        }
        Genre genre = genreMapper.toEntity(request);
        return genreMapper.toResponse(genreRepository.save(genre));
    }

    @Override
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genreMapper::toResponse)
                .toList();
    }
}
