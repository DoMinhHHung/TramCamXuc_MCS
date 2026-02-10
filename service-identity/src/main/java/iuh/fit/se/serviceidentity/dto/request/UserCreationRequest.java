package iuh.fit.se.serviceidentity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    String email;

    @Size(min = 8, message = "PASSWORD_MIN_LENGTH")
    String password;

    @NotBlank(message = "FIRST_NAME_REQUIRED")
    String firstName;

    @NotBlank(message = "LAST_NAME_REQUIRED")
    String lastName;

    LocalDate dob;
}