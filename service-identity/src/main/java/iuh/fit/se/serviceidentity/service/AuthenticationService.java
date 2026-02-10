package iuh.fit.se.serviceidentity.service;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;
import iuh.fit.se.serviceidentity.entity.User;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    AuthenticationResponse refreshToken(RefreshTokenRequest request);
    void logout(LogoutRequest request);
    IntrospectResponse introspect(IntrospectRequest request);
    String generateToken(User user);
    String generateRefreshToken(User user);
    AuthenticationResponse outboundAuthenticate(ExchangeTokenRequest request, String type);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
}
