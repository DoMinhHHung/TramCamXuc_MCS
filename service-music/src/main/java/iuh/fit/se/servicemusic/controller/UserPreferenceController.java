package iuh.fit.se.servicemusic.controller;

import iuh.fit.se.servicemusic.dto.request.OnboardingRequest;
import iuh.fit.se.servicemusic.dto.response.UserPreferenceResponse;
import iuh.fit.se.servicemusic.exception.ApiResponse;
import iuh.fit.se.servicemusic.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping("/onboarding")
    public ApiResponse<UserPreferenceResponse> completeOnboarding(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid OnboardingRequest request) {

        String userId = jwt.getSubject();

        return ApiResponse.<UserPreferenceResponse>builder()
                .result(userPreferenceService.completeOnboarding(
                        UUID.fromString(userId), request))
                .build();
    }

    @GetMapping("/my-preferences")
    public ApiResponse<UserPreferenceResponse> getMyPreferences(
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        return ApiResponse.<UserPreferenceResponse>builder()
                .result(userPreferenceService.getUserPreferences(
                        UUID.fromString(userId)))
                .build();
    }

    @GetMapping("/onboarding-status")
    public ApiResponse<Boolean> checkOnboardingStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        return ApiResponse.<Boolean>builder()
                .result(userPreferenceService.hasCompletedOnboarding(
                        UUID.fromString(userId)))
                .build();
    }
}