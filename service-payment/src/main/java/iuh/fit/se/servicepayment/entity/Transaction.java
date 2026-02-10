package iuh.fit.se.servicepayment.entity;

import iuh.fit.se.servicepayment.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private Long orderCode;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String paymentLinkId;
    private String checkoutUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Initialize creation and update timestamps and default the payment status before the entity is persisted.
     *
     * Sets both `createdAt` and `updatedAt` to the current time and assigns `PaymentStatus.PENDING`
     * if `status` is null. Invoked as a JPA `@PrePersist` lifecycle callback.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PaymentStatus.PENDING;
    }

    /**
     * Refreshes the entity's update timestamp immediately before it is persisted.
     *
     * Sets the `updatedAt` field to the current date and time.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}