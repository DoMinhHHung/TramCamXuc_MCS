package iuh.fit.se.servicepayment.entity;

import iuh.fit.se.servicepayment.entity.enums.DurationUnit;
import iuh.fit.se.servicepayment.entity.json.PlanFeatures;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DurationUnit durationUnit;

    @Column(columnDefinition = "TEXT")
    private PlanFeatures features;

    private boolean isActive;


}