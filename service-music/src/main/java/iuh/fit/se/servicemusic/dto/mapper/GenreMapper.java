package iuh.fit.se.servicemusic.dto.mapper;

import iuh.fit.se.servicemusic.dto.request.GenreCreationRequest;
import iuh.fit.se.servicemusic.dto.request.GenreUpdateRequest;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;
import iuh.fit.se.servicemusic.entity.Genre;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    Genre toEntity(GenreCreationRequest request);
    GenreResponse toResponse(Genre genre);
    void updateGenre(@MappingTarget Genre genre, GenreUpdateRequest request);
}