package iuh.fit.se.servicepayment.dto.request;

import iuh.fit.se.servicepayment.entity.json.PlanFeatures;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanRequest {
    @NotBlank(message = "Plan name is required")
    String name;

    String description;

    @NotNull
    @Min(value = 0, message = "Price must be >= 0")
    BigDecimal price;

    int duration;

    String durationUnit;

    PlanFeatures features;
}
