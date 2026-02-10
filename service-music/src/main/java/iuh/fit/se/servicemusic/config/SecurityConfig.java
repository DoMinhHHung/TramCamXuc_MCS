package iuh.fit.se.servicemusic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.signerKey}")
    private String signerKey;

    /**
     * Configures the HTTP security rules and returns the application's SecurityFilterChain.
     *
     * Disables CSRF protection, requires authentication for requests matching
     * "/songs/stream/*/play", permits all requests to "/songs/stream/**", and
     * requires authentication for all other requests. Configures the application
     * as an OAuth2 Resource Server that validates JWTs using the configured JwtDecoder.
     *
     * @return the configured SecurityFilterChain enforcing the authorization rules and JWT resource-server settings
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/songs/stream/*/play").authenticated()
                        .requestMatchers("/songs/stream/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    /**
     * Create a JwtDecoder that verifies JWT signatures using the application's signer key with the HS512 MAC algorithm.
     *
     * The decoder is configured to use the configured `signerKey` (raw bytes) and restricts signature validation to HS512.
     *
     * @return the JwtDecoder configured to verify JWTs with HS512 using the application's signer key
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}