package iuh.fit.se.servicemusic.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranscodeRequestEvent {
    private UUID songId;
    private String rawObjectKey;
}