package iuh.fit.se.servicetranscode.listener;

import iuh.fit.se.servicetranscode.config.RabbitMQConfig;
import iuh.fit.se.servicetranscode.dto.request.TranscodeRequestEvent;
import iuh.fit.se.servicetranscode.dto.response.TranscodeResultEvent;
import iuh.fit.se.servicetranscode.service.TranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscodeListener {

    private final TranscodeService transcodeService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.TRANSCODE_QUEUE)
    public void handleTranscodeRequest(TranscodeRequestEvent event) {
        log.info("Nhận job transcode cho bài hát: {}", event.getSongId());
        try {
            var result = transcodeService.processAudio(event.getSongId(), event.getRawObjectKey());

            TranscodeResultEvent resultEvent = TranscodeResultEvent.builder()
                    .songId(event.getSongId())
                    .status("SUCCESS")
                    .streamUrl(result.masterUrl())
                    .duration(result.duration())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RESULT_ROUTING_KEY, resultEvent);
            log.info("Transcode thành công. Đã gửi kết quả.");

        } catch (Exception e) {
            log.error("Transcode thất bại: ", e);
            TranscodeResultEvent failedEvent = TranscodeResultEvent.builder()
                    .songId(event.getSongId())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RESULT_ROUTING_KEY, failedEvent);
        }
    }
}