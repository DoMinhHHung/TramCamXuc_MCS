package iuh.fit.se.serviceidentity.dto.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpgradedEvent {
    UUID userId;
    String planName;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Map<String, Object> features;
}