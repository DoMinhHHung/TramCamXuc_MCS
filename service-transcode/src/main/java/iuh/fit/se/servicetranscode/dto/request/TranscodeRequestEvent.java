package iuh.fit.se.servicetranscode.dto.request;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranscodeRequestEvent {
    private UUID songId;
    private String rawObjectKey;
}