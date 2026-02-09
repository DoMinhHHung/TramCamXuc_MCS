package iuh.fit.se.servicemusic.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.servicemusic.config.RabbitMQConfig;
import iuh.fit.se.servicemusic.entity.enums.Status;
import iuh.fit.se.servicemusic.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueListener {

    private final SongRepository songRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.TRANSCODE_DLQ)
    public void handleTranscodeDLQ(Message message) {
        try {
            String messageBody = new String(message.getBody());
            log.error("Message moved to transcode DLQ: {}", messageBody);

            Map<String, Object> messageData = objectMapper.readValue(messageBody, Map.class);
            String songIdStr = (String) messageData.get("songId");

            if (songIdStr != null) {
                UUID songId = UUID.fromString(songIdStr);
                updateSongStatusToFailed(songId, "Transcode job failed after maximum retries");
            }

            logMessageHeaders(message);

        } catch (Exception e) {
            log.error("Error processing DLQ message", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.RESULT_DLQ)
    public void handleResultDLQ(Message message) {
        try {
            String messageBody = new String(message.getBody());
            log.error("Message moved to result DLQ: {}", messageBody);

            Map<String, Object> messageData = objectMapper.readValue(messageBody, Map.class);
            String songIdStr = (String) messageData.get("songId");

            if (songIdStr != null) {
                UUID songId = UUID.fromString(songIdStr);
                updateSongStatusToFailed(songId, "Failed to process transcode result");
            }

            logMessageHeaders(message);

        } catch (Exception e) {
            log.error("Error processing DLQ message", e);
        }
    }

    private void updateSongStatusToFailed(UUID songId, String reason) {
        songRepository.findById(songId).ifPresentOrElse(song -> {
            song.setStatus(Status.FAILED);
            songRepository.save(song);
            log.warn("Updated song {} status to FAILED. Reason: {}", songId, reason);
        }, () -> {
            log.warn("Song {} not found in database when processing DLQ", songId);
        });
    }

    private void logMessageHeaders(Message message) {
        log.debug("DLQ Message Headers:");
        message.getMessageProperties().getHeaders().forEach((key, value) -> {
            log.debug("  {} = {}", key, value);
        });

        Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-death-count");
        if (retryCount != null) {
            log.warn("Message failed after {} retry attempts", retryCount);
        }
    }
}