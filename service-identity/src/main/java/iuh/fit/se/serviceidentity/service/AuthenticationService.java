package iuh.fit.se.serviceidentity.service;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;
import iuh.fit.se.serviceidentity.entity.User;

public interface AuthenticationService {
    /**
 * Authenticate a user using the supplied credentials and produce an authentication result.
 *
 * @param request the authentication request containing user credentials and any optional parameters required for authentication
 * @return an AuthenticationResponse containing issued access (and, if applicable, refresh) tokens and authenticated user details
 */
AuthenticationResponse authenticate(AuthenticationRequest request);
    /**
 * Issues new authentication credentials based on the provided refresh token request.
 *
 * @param request the refresh token request containing the refresh token and any required context
 * @return an AuthenticationResponse containing the newly issued access token, refresh token (if rotated), and related metadata
 */
AuthenticationResponse refreshToken(RefreshTokenRequest request);
    /**
 * Terminates the session described by the given logout request and invalidates any associated tokens.
 *
 * @param request logout parameters (for example, the access and/or refresh token or user identifier to be invalidated)
 */
void logout(LogoutRequest request);
    /**
 * Performs token introspection and returns the token's validity and metadata.
 *
 * @param request the introspection request containing the token to examine
 * @return an IntrospectResponse describing whether the token is active and its associated metadata
 */
IntrospectResponse introspect(IntrospectRequest request);
    /**
 * Generates an authentication token for the specified user.
 *
 * @param user the user for whom to create the token
 * @return the generated token string to be used as the user's authentication credential
 */
String generateToken(User user);
    /**
 * Generate a refresh token for the specified user.
 *
 * The refresh token can be used to obtain new access tokens without requiring the user to re-authenticate.
 *
 * @param user the user for whom the refresh token will be issued
 * @return the generated refresh token
 */
String generateRefreshToken(User user);
    /**
 * Performs an outbound token exchange or authentication against an external system using the provided request and exchange type.
 *
 * @param request the token exchange request containing credentials, assertions, or tokens to be exchanged
 * @param type a string identifying the outbound authentication or exchange flow (for example, provider or grant type)
 * @return an AuthenticationResponse containing issued tokens and related authentication metadata
 */
AuthenticationResponse outboundAuthenticate(ExchangeTokenRequest request, String type);
    /**
 * Initiates a password reset flow for the account associated with the given email.
 *
 * @param email the user's email address that will receive password reset instructions
 */
void forgotPassword(String email);
    /**
 * Reset a user's password using the information in the given request.
 *
 * @param request contains the reset token and the new password (and any other required reset data)
 */
void resetPassword(ResetPasswordRequest request);
}