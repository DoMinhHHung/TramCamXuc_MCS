package iuh.fit.se.servicemusic.controller;

import iuh.fit.se.servicemusic.dto.request.ArtistRegistrationRequest;
import iuh.fit.se.servicemusic.dto.response.ArtistResponse;
import iuh.fit.se.servicemusic.exception.ApiResponse;
import iuh.fit.se.servicemusic.exception.AppException;
import iuh.fit.se.servicemusic.exception.ErrorCode;
import iuh.fit.se.servicemusic.service.ArtistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @PostMapping("/register")
    public ApiResponse<ArtistResponse> registerArtist(
            @Valid @RequestBody ArtistRegistrationRequest request,
            Authentication authentication) {

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();

        Object artistClaim = jwt.getClaim("artist");

        boolean isArtist = false;
        if (artistClaim instanceof Boolean) {
            isArtist = (Boolean) artistClaim;
        } else if (artistClaim instanceof String) {
            isArtist = Boolean.parseBoolean((String) artistClaim);
        }

        if (!isArtist) {
            throw new AppException(ErrorCode.NOT_ELIGIBLE_FOR_ARTIST);
        }

        String userId = authentication.getName();
        ArtistResponse response = artistService.registerArtist(UUID.fromString(userId), request);

        return ApiResponse.<ArtistResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/my-profile")
    public ApiResponse<ArtistResponse> getMyArtistProfile(Authentication authentication) {
        String userId = authentication.getName();

        return ApiResponse.<ArtistResponse>builder()
                .result(artistService.getArtistProfile(UUID.fromString(userId)))
                .build();
    }

    @GetMapping
    public ApiResponse<ArtistResponse> getAllArtists() {
        return ApiResponse.<ArtistResponse>builder()
                .result(artistService.getAllArtist())
                .build();
    }
}