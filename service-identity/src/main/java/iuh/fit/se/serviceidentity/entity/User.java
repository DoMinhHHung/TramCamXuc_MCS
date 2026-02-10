package iuh.fit.se.serviceidentity.entity;

import iuh.fit.se.serviceidentity.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false)
    String email;

    String password;

    String firstName;
    String lastName;

    LocalDate dob;

    String avatarUrl;

    @Enumerated(EnumType.STRING)
    UserRole role;

    @Enumerated(EnumType.STRING)
    AuthProvider provider;

    String providerId;

    @Enumerated(EnumType.STRING)
    AccountStatus status;

// Subscription fields
    @Column(name = "is_subscription_active")
    @Builder.Default
    boolean isSubscriptionActive = false;

    @Column(name = "subscription_end_date")
    LocalDateTime subscriptionEndDate;

    @Column(name = "current_plan_name")
    String currentPlanName;
// End of subscription fields

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    /**
     * Initialize creation/update timestamps and set default provider, account status, and role before the entity is first persisted.
     *
     * Sets `createdAt` and `updatedAt` to the current time. If `provider`, `status`, or `role` are null, they are set to
     * `AuthProvider.LOCAL`, `AccountStatus.PENDING_VERIFICATION`, and `UserRole.USER` respectively.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (provider == null) provider = AuthProvider.LOCAL;
        if (status == null) status = AccountStatus.PENDING_VERIFICATION;
        if (role == null) role = UserRole.USER;
    }

    /**
     * Refreshes the entity's last-modified timestamp before a persistence update.
     *
     * Sets {@code updatedAt} to the current date and time.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}