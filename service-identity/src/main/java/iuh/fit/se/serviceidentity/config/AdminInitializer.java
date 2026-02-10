package iuh.fit.se.serviceidentity.config;

import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.entity.enums.AccountStatus;
import iuh.fit.se.serviceidentity.entity.enums.UserRole;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    /**
     * Initializes a default ADMIN account on application startup if an account with the configured admin email does not exist.
     *
     * If missing, creates and persists a User with the configured admin email and password (password is encoded),
     * first name "Super", last name "Admin", role ADMIN, and status ACTIVE. Logs whether the account was created or already existed.
     */
    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(UserRole.ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .build();

            userRepository.save(admin);
            log.info(">>> Đã tạo tài khoản ADMIN mặc định: " + adminEmail);
        } else {
            log.info(">>> Tài khoản ADMIN đã tồn tại, không tạo lại.");
        }
    }
}