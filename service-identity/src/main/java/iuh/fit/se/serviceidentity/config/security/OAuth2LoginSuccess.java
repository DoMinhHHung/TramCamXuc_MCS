package iuh.fit.se.serviceidentity.config.security;

import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.exception.AppException;
import iuh.fit.se.serviceidentity.exception.ErrorCode;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import iuh.fit.se.serviceidentity.service.AuthenticationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccess implements AuthenticationSuccessHandler {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Handles a successful OAuth2 authentication by issuing tokens and redirecting the client to the frontend with an access token.
     *
     * <p>Loads the authenticated user's email from the OAuth2 principal, resolves the corresponding User
     * entity, generates an access (and refresh) token, and sends an HTTP redirect to
     * "{frontendUrl}/oauth2/redirect?token={accessToken}".</p>
     *
     * @param request        the HTTP request for the authentication exchange
     * @param response       the HTTP response used to send the redirect
     * @param authentication the authentication object whose principal is an OAuth2User containing an "email" attribute
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        log.info("OAuth2 Login success for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String accessToken = authenticationService.generateToken(user);

         String refreshToken = authenticationService.generateRefreshToken(user);

        String redirectUrl = String.format("%s/oauth2/redirect?token=%s", frontendUrl, accessToken);

        response.sendRedirect(redirectUrl);
    }
}