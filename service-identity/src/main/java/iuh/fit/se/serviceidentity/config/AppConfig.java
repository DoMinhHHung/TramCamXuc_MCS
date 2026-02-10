package iuh.fit.se.serviceidentity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    /**
     * Creates a BCrypt-based password encoder configured with strength 10 for use as the application's PasswordEncoder bean.
     *
     * @return the PasswordEncoder instance using BCrypt with a strength (log rounds) of 10
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}