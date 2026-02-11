package iuh.fit.se.servicemusic.service.impl;

import iuh.fit.se.servicemusic.dto.request.OnboardingRequest;
import iuh.fit.se.servicemusic.dto.response.ArtistResponse;
import iuh.fit.se.servicemusic.dto.response.GenreResponse;
import iuh.fit.se.servicemusic.dto.response.UserPreferenceResponse;
import iuh.fit.se.servicemusic.entity.Artist;
import iuh.fit.se.servicemusic.entity.Genre;
import iuh.fit.se.servicemusic.entity.UserPreference;
import iuh.fit.se.servicemusic.exception.AppException;
import iuh.fit.se.servicemusic.exception.ErrorCode;
import iuh.fit.se.servicemusic.repository.*;
import iuh.fit.se.servicemusic.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public UserPreferenceResponse completeOnboarding(UUID userId, OnboardingRequest request) {
        if (userPreferenceRepository.existsByUserId(userId)) {
            throw new AppException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        // Fetch genres
        List<Genre> genres = genreRepository.findAllById(request.getFavoriteGenreIds());
        if (genres.size() != request.getFavoriteGenreIds().size()) {
            throw new AppException(ErrorCode.GENRE_NOT_FOUND);
        }

        // Fetch artists
        List<Artist> artists = artistRepository.findAllById(request.getFavoriteArtistIds());
        if (artists.size() != request.getFavoriteArtistIds().size()) {
            throw new AppException(ErrorCode.ARTIST_NOT_FOUND);
        }

        // Create preferences
        UserPreference preferences = UserPreference.builder()
                .userId(userId)
                .favoriteGenres(new HashSet<>(genres))
                .favoriteArtists(new HashSet<>(artists))
                .onboardingCompleted(true)
                .build();

        preferences = userPreferenceRepository.save(preferences);

        return mapToResponse(preferences);
    }

    @Override
    public UserPreferenceResponse getUserPreferences(UUID userId) {
        UserPreference preferences = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_PREFERENCES_NOT_FOUND));

        return mapToResponse(preferences);
    }

    @Override
    public boolean hasCompletedOnboarding(UUID userId) {
        return userPreferenceRepository.findByUserId(userId)
                .map(UserPreference::isOnboardingCompleted)
                .orElse(false);
    }

    private UserPreferenceResponse mapToResponse(UserPreference preferences) {
        List<GenreResponse> genreResponses = preferences.getFavoriteGenres().stream()
                .map(genre -> {
                    GenreResponse gr = new GenreResponse();
                    gr.setId(genre.getId());
                    gr.setName(genre.getName());
                    gr.setKey(genre.getKey());
                    return gr;
                })
                .collect(Collectors.toList());

        List<ArtistResponse> artistResponses = preferences.getFavoriteArtists().stream()
                .map(artist -> ArtistResponse.builder()
                        .id(artist.getId())
                        .userId(artist.getUserId())
                        .stageName(artist.getStageName())
                        .avatarUrl(artist.getAvatarUrl())
                        .status(artist.getStatus())
                        .build())
                .collect(Collectors.toList());

        return UserPreferenceResponse.builder()
                .userId(preferences.getUserId())
                .favoriteGenres(genreResponses)
                .favoriteArtists(artistResponses)
                .onboardingCompleted(preferences.isOnboardingCompleted())
                .build();
    }
}
