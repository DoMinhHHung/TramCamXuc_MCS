package iuh.fit.se.serviceidentity.controller;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;
import iuh.fit.se.serviceidentity.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    /**
     * Authenticates a user using the provided credentials and returns the authentication result.
     *
     * @param request authentication credentials and options
     * @return an ApiResponse wrapping an AuthenticationResponse containing issued tokens and authentication details
     */
    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /**
     * Processes a token introspection request and returns the introspection details.
     *
     * @param request the introspection request containing the token to inspect
     * @return an ApiResponse containing the resulting IntrospectResponse
     */
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    /**
     * Refreshes authentication using a refresh token and returns updated authentication details.
     *
     * @param request the refresh token request containing the refresh token and any required metadata
     * @return an ApiResponse whose result is an AuthenticationResponse with refreshed authentication tokens
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /**
     * Invalidates the user's session or tokens and returns a confirmation response.
     *
     * @param request the logout request containing identifiers or tokens required to perform logout
     * @return an ApiResponse with message "Logout successful" and no payload
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }

    /**
     * Authenticate a user using an external provider's exchange token.
     *
     * @param request the exchange token request containing the external provider token and any required metadata
     * @param type the external provider type (for example, "google" or "facebook")
     * @return an ApiResponse containing an AuthenticationResponse with authentication tokens and user information
     */
    @PostMapping("/outbound/authentication")
    public ApiResponse<AuthenticationResponse> outboundAuthenticate(
            @RequestBody ExchangeTokenRequest request,
            @RequestParam String type // "google" hoặc "facebook"
    ) {
        var result = authenticationService.outboundAuthenticate(request, type);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    /**
     * Initiates a password reset by sending a one-time password (OTP) to the specified email address.
     *
     * @param email the recipient email address where the OTP will be sent
     * @return an ApiResponse with message "OTP sent to email"
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestParam String email) {
        authenticationService.forgotPassword(email);
        return ApiResponse.<Void>builder().message("OTP sent to email").build();
    }

    /**
     * Reset a user's password using the provided reset request.
     *
     * @param request the reset parameters containing the verification token and the new password
     * @return an ApiResponse with no payload and message "Password reset successfully"
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ApiResponse.<Void>builder().message("Password reset successfully").build();
    }
}