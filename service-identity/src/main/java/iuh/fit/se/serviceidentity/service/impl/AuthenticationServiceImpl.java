package iuh.fit.se.serviceidentity.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.AuthenticationResponse;
import iuh.fit.se.serviceidentity.dto.response.IntrospectResponse;
import iuh.fit.se.serviceidentity.dto.response.OutboundUserResponse;
import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.entity.enums.AccountStatus;
import iuh.fit.se.serviceidentity.entity.enums.AuthProvider;
import iuh.fit.se.serviceidentity.entity.enums.UserRole;
import iuh.fit.se.serviceidentity.exception.*;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import iuh.fit.se.serviceidentity.repository.httpclient.*;
import iuh.fit.se.serviceidentity.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);
        var refreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

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

    @Override
    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issuer("phazelsound.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
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
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

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

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRole() != null) {
            stringJoiner.add("ROLE_" + user.getRole().name());
        }
        return stringJoiner.toString();
    }

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

    @Override
    public AuthenticationResponse outboundAuthenticate(ExchangeTokenRequest request, String type) {
        // 1. Khai báo biến tạm để hứng dữ liệu đã chuẩn hóa
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

        // 4. Tạo token hệ thống
        var accessToken = generateToken(user);
        var refreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }
}
