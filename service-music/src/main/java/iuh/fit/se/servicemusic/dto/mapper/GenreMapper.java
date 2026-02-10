package iuh.fit.se.servicemusic.dto.mapper;

import iuh.fit.se.servicemusic.dto.request.GenreCreationRequest;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;
import iuh.fit.se.servicemusic.entity.Genre;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    Genre toEntity(GenreCreationRequest request);
    GenreResponse toResponse(Genre genre);
}