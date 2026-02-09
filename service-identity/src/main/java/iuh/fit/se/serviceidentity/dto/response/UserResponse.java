package iuh.fit.se.serviceidentity.dto.response;

import iuh.fit.se.serviceidentity.entity.enums.AccountStatus;
import iuh.fit.se.serviceidentity.entity.enums.UserRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String email;
    String firstName;
    String lastName;
    LocalDate dob;
    String avatarUrl;
    UserRole role;
    AccountStatus status;
}