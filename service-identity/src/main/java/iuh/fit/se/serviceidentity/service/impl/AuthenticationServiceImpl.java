package iuh.fit.se.serviceidentity.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import iuh.fit.se.serviceidentity.config.RabbitMQConfig;
import iuh.fit.se.serviceidentity.dto.event.NotificationEvent;
import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.AuthenticationResponse;
import iuh.fit.se.serviceidentity.dto.response.IntrospectResponse;
import iuh.fit.se.serviceidentity.dto.response.OutboundUserResponse;
import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.entity.enums.AccountStatus;
import iuh.fit.se.serviceidentity.entity.enums.AuthProvider;
import iuh.fit.se.serviceidentity.entity.enums.UserRole;
import iuh.fit.se.serviceidentity.exception.*;
import iuh.fit.se.serviceidentity.repository.UserFeaturesRepository;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import iuh.fit.se.serviceidentity.repository.httpclient.*;
import iuh.fit.se.serviceidentity.service.AuthenticationService;
import iuh.fit.se.serviceidentity.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OutboundIdentityClient outboundIdentityClient;
    private final FacebookIdentityClient facebookIdentityClient;
    private OtpService otpService;
    private final RabbitTemplate rabbitTemplate;
    private final UserFeaturesRepository userFeaturesRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    /**
     * Authenticate a user using their email and password and issue access and refresh tokens.
     *
     * @param request contains the user's email and raw password for authentication
     * @return an AuthenticationResponse containing an access token, a refresh token, and authenticated = true
     * @throws AppException with ErrorCode.USER_NOT_EXISTED if no user exists for the given email
     * @throws AppException with ErrorCode.UNAUTHENTICATED if the password does not match
     * @throws AppException with ErrorCode.USER_LOCKED if the user's account status is LOCKED
     */
    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);
        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        var token = generateToken(user);
        var refreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Issues a new access token by validating and consuming the provided refresh token.
     *
     * Validates the refresh token, checks and updates the refresh-token blacklist to prevent reuse,
     * loads the subject user, and returns a freshly generated access token for that user.
     *
     * @param request the refresh token request containing the token to refresh
     * @return an AuthenticationResponse containing a new access token and authenticated = true
     * @throws AppException with ErrorCode.UNAUTHENTICATED if the token is invalid, expired, revoked/blacklisted, or the user does not exist
     */
    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        try {
            var signedJWT = verifyToken(request.getToken(), true);

            var jit = signedJWT.getJWTClaimsSet().getJWTID();
            var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            // Check blacklist
            if (redisTemplate.hasKey("BLACKLIST:" + jit)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            long remainingTime = expiryTime.getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set("BLACKLIST:" + jit, "REFRESHED", remainingTime, TimeUnit.MILLISECONDS);
            }

            var userId = signedJWT.getJWTClaimsSet().getSubject();
            var user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

            var newToken = generateToken(user);

            return AuthenticationResponse.builder()
                    .accessToken(newToken)
                    .authenticated(true)
                    .build();

        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    /**
     * Revokes a refresh token by adding its JWT ID to the Redis blacklist for the remaining token lifetime.
     *
     * Verifies the provided token as a refresh token; if verification succeeds and the token has remaining
     * validity, stores a "BLACKLIST:{jti}" entry with value "REVOKED" and TTL equal to the remaining milliseconds.
     * Invalid or expired tokens are ignored.
     *
     * @param request the logout request containing the token to revoke
     */
    @Override
    public void logout(LogoutRequest request) {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            long remainingTime = expiryTime.getTime() - System.currentTimeMillis();

            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(
                        "BLACKLIST:" + jit,
                        "REVOKED",
                        remainingTime,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.warn("Token already expired or invalid");
        }
    }

    /**
     * Determines whether the provided token is valid and not revoked.
     *
     * @param request the introspection request containing the token to check
     * @return an IntrospectResponse whose `valid` is `true` if the token's signature and expiry are valid and the token is not blacklisted, `false` otherwise
     */
    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException | JOSEException | ParseException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    /**
     * Create a signed JWT access token for the given user.
     *
     * The token's claims include subject (user id), issuer, issued-at, expiration,
     * jwt id, a `scope` claim derived from the user's role, and — if the user's
     * subscription is active — any user feature entries as additional claims.
     *
     * @param user the user for whom to generate the token; its id and role are used to build claims
     * @return the serialized, signed JWT access token
     * @throws RuntimeException if the token cannot be signed
     */
    @Override
    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("phazelsound.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user));

        if (user.isSubscriptionActive()
                && user.getSubscriptionEndDate() != null
                && user.getSubscriptionEndDate().isAfter(LocalDateTime.now())) {

            // Query vào bảng user_features
            userFeaturesRepository.findByUserId(user.getId()).ifPresent(userFeatures -> {
                if (userFeatures.getFeatures() != null) {
                    userFeatures.getFeatures().forEach((key, value) -> {
                        claimsBuilder.claim(key, value);
                    });
                }
            });
        }
        JWTClaimsSet jwtClaimsSet = claimsBuilder.build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify a JWT string, ensure its signature and expiry (optionally using refreshable duration), and return the parsed SignedJWT.
     *
     * @param token the serialized JWT to verify
     * @param isRefresh if true, consider the token refreshable and compute expiry from the token's issue time plus the refreshable duration; otherwise use the token's expiration claim
     * @return the parsed and verified SignedJWT
     * @throws JOSEException   if signature verification or cryptographic operations fail
     * @throws ParseException  if the token cannot be parsed
     * @throws AppException    if the token is invalid, expired, or has been blacklisted
     */
    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (redisTemplate.hasKey("BLACKLIST:" + signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    /**
     * Builds a space-delimited scope string based on the user's role.
     *
     * @param user the user whose role will be included in the scope
     * @return the scope string containing "ROLE_<ROLE_NAME>" when a role is present, or an empty string otherwise
     */
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRole() != null) {
            stringJoiner.add("ROLE_" + user.getRole().name());
        }
        return stringJoiner.toString();
    }

    /**
     * Create a signed JWT refresh token for the given user.
     *
     * The token's claims include subject (user ID), issuer ("phazelsound.com"), issue time,
     * expiration time set to now plus REFRESHABLE_DURATION seconds, a JWT ID, and a `scope` claim
     * derived from the user's roles/features. The token is signed with HS512 using SIGNER_KEY.
     *
     * @param user the user for whom to generate the refresh token
     * @return the serialized JWT refresh token string
     * @throws RuntimeException if the token cannot be signed
     */
    public String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("phazelsound.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create refresh token", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Authenticates a user (or creates a new account) using an external identity provider token.
     *
     * This method accepts a provider token, obtains the user's email and profile from the provider
     * (currently supports "google" and "facebook"), looks up or provisions a local User record,
     * and returns newly issued access and refresh tokens.
     *
     * @param request the exchange token request containing the provider token
     * @param type the identity provider type (case-insensitive), e.g. "google" or "facebook"
     * @return an AuthenticationResponse containing an access token, a refresh token, and authenticated = true
     * @throws AppException with ErrorCode.UNAUTHENTICATED if the provider response does not contain an email or the token is invalid
     */
    @Override
    public AuthenticationResponse outboundAuthenticate(ExchangeTokenRequest request, String type) {
        String email = null;
        String firstName = null;
        String lastName = null;
        String avatarUrl = null;

        if ("google".equalsIgnoreCase(type)) {
            // --- XỬ LÝ GOOGLE ---
            var response = outboundIdentityClient.getUserInfo(request.getToken());

            if (response != null) {
                email = response.getEmail();
                firstName = response.getGivenName();
                lastName = response.getFamilyName();
                avatarUrl = response.getPicture();
            }
        }
        else if ("facebook".equalsIgnoreCase(type)) {
            var response = facebookIdentityClient.getUserInfo(
                    "id,name,email,first_name,last_name,picture",
                    request.getToken()
            );

            if (response != null) {
                email = response.getEmail();
                firstName = response.getFirstName();
                lastName = response.getLastName();

                if (response.getPicture() != null && response.getPicture().getData() != null) {
                    avatarUrl = response.getPicture().getData().getUrl();
                }
            }
        }

        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String finalEmail = email;
        String finalFirstName = firstName;
        String finalLastName = lastName;
        String finalAvatarUrl = avatarUrl;

        var user = userRepository.findByEmail(email).orElseGet(() -> {
            return userRepository.save(User.builder()
                    .email(finalEmail)
                    .firstName(finalFirstName)
                    .lastName(finalLastName)
                    .avatarUrl(finalAvatarUrl)
                    .role(UserRole.USER)
                    .status(AccountStatus.ACTIVE)
                    .provider(AuthProvider.valueOf(type.toUpperCase()))
                    .build());
        });

        var accessToken = generateToken(user);
        var refreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Initiates a forgot-password flow by generating a one-time password (OTP) and sending it to the user's email.
     *
     * @param email the email address of the user requesting password reset
     * @throws AppException if no user exists with the provided email (ErrorCode.USER_NOT_EXISTED)
     */
    @Override
    public void forgotPassword(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String otp = otpService.generateOtp(email);

        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(user.getEmail())
                .templateCode("FORGOT_PASSWORD_OTP")
                .params(Map.of("otp", otp, "name", user.getFirstName()))
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, event);
    }

    /**
     * Resets a user's password after validating a one-time password (OTP).
     *
     * Validates the provided OTP for the email in the request, then replaces the user's stored password with the encoded new password.
     *
     * @param request contains the target email, the OTP to validate, and the new password to set
     * @throws AppException if the OTP is invalid or if no user exists for the given email
     */
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!isValid) throw new AppException(ErrorCode.INVALID_OTP);

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}