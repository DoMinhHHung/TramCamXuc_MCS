package iuh.fit.se.servicemusic.listener;

import iuh.fit.se.servicemusic.config.RabbitMQConfig;
import iuh.fit.se.servicemusic.dto.event.TranscodeResultEvent;
import iuh.fit.se.servicemusic.entity.enums.Status;
import iuh.fit.se.servicemusic.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SongResultListener {

    private final SongRepository songRepository;

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void handleTranscodeResult(TranscodeResultEvent event) {
        log.info("Nhận kết quả Transcode cho Song ID: {} - Status: {}", event.getSongId(), event.getStatus());

        songRepository.findById(event.getSongId()).ifPresentOrElse(song -> {
            if ("SUCCESS".equals(event.getStatus())) {
                song.setStatus(Status.ACTIVE);
                song.setStreamUrl(event.getStreamUrl());
                song.setDuration(event.getDuration());

                log.info("Update thành công Song ID: {}. Stream URL: {}", song.getId(), song.getStreamUrl());
            } else {
                song.setStatus(Status.FAILED);
                log.error("Transcode thất bại cho Song ID: {}. Lỗi: {}", song.getId(), event.getMessage());
            }
            songRepository.save(song);
        }, () -> {
            log.warn("Không tìm thấy bài hát có ID: {} để update", event.getSongId());
        });
    }
}