package iuh.fit.se.servicenotification.dto.event;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    String channel;
    String recipient;
    String templateCode;
    Map<String, Object> params;
}