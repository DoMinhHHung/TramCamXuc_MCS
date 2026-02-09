package iuh.fit.se.servicemusic.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TranscodeResultEvent {
    private UUID songId;
    private String streamUrl;
    private int duration;
    private String status;
    private String message;
}