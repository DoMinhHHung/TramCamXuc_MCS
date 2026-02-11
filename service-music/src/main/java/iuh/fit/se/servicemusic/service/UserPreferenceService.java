package iuh.fit.se.servicemusic.service;

import iuh.fit.se.servicemusic.dto.request.OnboardingRequest;
import iuh.fit.se.servicemusic.dto.response.UserPreferenceResponse;

import java.util.UUID;

public interface UserPreferenceService {
    UserPreferenceResponse completeOnboarding(UUID userId, OnboardingRequest request);
    UserPreferenceResponse getUserPreferences(UUID userId);
    boolean hasCompletedOnboarding(UUID userId);
}