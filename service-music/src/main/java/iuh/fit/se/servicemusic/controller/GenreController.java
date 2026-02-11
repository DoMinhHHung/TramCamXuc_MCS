package iuh.fit.se.servicemusic.controller;

import iuh.fit.se.servicemusic.dto.request.*;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;
import iuh.fit.se.servicemusic.exception.ApiResponse;
import iuh.fit.se.servicemusic.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;

    @PostMapping
    public ApiResponse<GenreResponse> createGenre(@RequestBody @Valid GenreCreationRequest request) {
        return ApiResponse.<GenreResponse>builder()
                .result(genreService.createGenre(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<GenreResponse>> getAllGenres() {
        return ApiResponse.<List<GenreResponse>>builder()
                .result(genreService.getAllGenres())
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<GenreResponse> updateGenre(@PathVariable UUID id,
                                                  @RequestBody @Valid GenreUpdateRequest request) {
        return ApiResponse.<GenreResponse>builder()
                .result(genreService.updateGenre(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteGenre(@PathVariable UUID id) {
        genreService.deleteGenre(id);
        return ApiResponse.<String>builder()
                .result("Genre has been deleted")
                .build();
    }
}