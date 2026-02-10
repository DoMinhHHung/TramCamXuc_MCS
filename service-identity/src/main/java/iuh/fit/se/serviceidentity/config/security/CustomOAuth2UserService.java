package iuh.fit.se.serviceidentity.security;

import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.entity.enums.AccountStatus;
import iuh.fit.se.serviceidentity.entity.enums.AuthProvider;
import iuh.fit.se.serviceidentity.entity.enums.UserRole;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * Loads the authenticated user's attributes from the OAuth2 provider and ensures the corresponding
     * local user record is created or updated.
     *
     * @param userRequest the OAuth2 user request containing client registration and access token
     * @return the OAuth2User received from the provider
     * @throws OAuth2AuthenticationException if authentication or user info retrieval from the provider fails
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        return processUser(provider, oAuth2User);
    }

    /**
     * Processes provider-specific OAuth2 attributes to create or update a local User and persist the result.
     *
     * Extracts email, provider-specific id, first name, last name, and avatar URL from the given OAuth2User for supported
     * providers (e.g., "google", "facebook"), then updates an existing User with that email or creates a new one. Persists
     * changes via the user repository and returns the original OAuth2User.
     *
     * @param provider     the OAuth2 provider identifier (expected values include "google" and "facebook")
     * @param oAuth2User   the OAuth2User containing attributes returned by the provider
     * @return             the original {@code OAuth2User} passed to the method
     * @throws RuntimeException if the provider did not supply an email address
     */
    private OAuth2User processUser(String provider, OAuth2User oAuth2User) {
        String email = null;
        String providerId = null;
        String firstName = null;
        String lastName = null;
        String avatarUrl = null;

        if ("google".equalsIgnoreCase(provider)) {
            email = oAuth2User.getAttribute("email");
            providerId = oAuth2User.getAttribute("sub");
            firstName = oAuth2User.getAttribute("given_name");
            lastName = oAuth2User.getAttribute("family_name");
            avatarUrl = oAuth2User.getAttribute("picture");
        }
        else if ("facebook".equalsIgnoreCase(provider)) {
            email = oAuth2User.getAttribute("email");
            providerId = oAuth2User.getAttribute("id");
            firstName = oAuth2User.getAttribute("first_name");
            lastName = oAuth2User.getAttribute("last_name");

            if (oAuth2User.getAttributes().containsKey("picture")) {
                Map<String, Object> pictureObj = oAuth2User.getAttribute("picture");
                if (pictureObj.containsKey("data")) {
                    Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                    if (dataObj.containsKey("url")) {
                        avatarUrl = (String) dataObj.get("url");
                    }
                }
            }
        }

        if (email == null) {
            log.error("Email not found from provider: {}", provider);
            throw new RuntimeException("Email is required for login");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setFirstName(firstName != null ? firstName : user.getFirstName());
            user.setLastName(lastName != null ? lastName : user.getLastName());
            user.setAvatarUrl(avatarUrl != null ? avatarUrl : user.getAvatarUrl());
            user.setProvider(AuthProvider.valueOf(provider.toUpperCase()));
            user.setProviderId(providerId);

            if (user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
                user.setStatus(AccountStatus.ACTIVE);
            }
            userRepository.save(user);
        } else {
            user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(avatarUrl)
                    .role(UserRole.USER)
                    .status(AccountStatus.ACTIVE)
                    .provider(AuthProvider.valueOf(provider.toUpperCase()))
                    .providerId(providerId)
                    .build();
            userRepository.save(user);
        }

        return oAuth2User;
    }
}