package iuh.fit.se.servicemusic.service.impl;

import iuh.fit.se.servicemusic.dto.request.ArtistRegistrationRequest;
import iuh.fit.se.servicemusic.dto.response.ArtistResponse;
import iuh.fit.se.servicemusic.entity.Artist;
import iuh.fit.se.servicemusic.entity.enums.ArtistStatus;
import iuh.fit.se.servicemusic.exception.AppException;
import iuh.fit.se.servicemusic.exception.ErrorCode;
import iuh.fit.se.servicemusic.repository.ArtistRepository;
import iuh.fit.se.servicemusic.service.ArtistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public ArtistResponse registerArtist(UUID userId, ArtistRegistrationRequest request) {
        if (artistRepository.existsByUserId(userId)) {
            throw new AppException(ErrorCode.ARTIST_ALREADY_REGISTERED);
        }

        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new AppException(ErrorCode.TERMS_NOT_ACCEPTED);
        }

        Artist artist = Artist.builder()
                .userId(userId)
                .stageName(request.getStageName())
                .bio(request.getBio())
                .avatarUrl(request.getAvatarUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .status(ArtistStatus.ACTIVE)
                .termsAccepted(true)
                .termsAcceptedAt(LocalDateTime.now())
                .build();

        artist = artistRepository.save(artist);
        log.info("Artist profile created for userId: {}", userId);

        return mapToResponse(artist);
    }

    @Override
    public ArtistResponse getArtistProfile(UUID userId) {
        Artist artist = artistRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.ARTIST_NOT_FOUND));

        return mapToResponse(artist);
    }

    private ArtistResponse mapToResponse(Artist artist) {
        return ArtistResponse.builder()
                .id(artist.getId())
                .userId(artist.getUserId())
                .stageName(artist.getStageName())
                .bio(artist.getBio())
                .avatarUrl(artist.getAvatarUrl())
                .coverImageUrl(artist.getCoverImageUrl())
                .status(artist.getStatus())
                .createdAt(artist.getCreatedAt())
                .build();
    }
}