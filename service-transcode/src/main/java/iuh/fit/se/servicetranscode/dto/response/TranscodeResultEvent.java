package iuh.fit.se.servicetranscode.dto.response;

import lombok.*;

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