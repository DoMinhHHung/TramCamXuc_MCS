package iuh.fit.se.servicemusic.controller;

import iuh.fit.se.servicemusic.service.StreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/songs/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @GetMapping("/{songId}/play")
    public ResponseEntity<InputStreamResource> playSong(
            @PathVariable UUID songId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userQuality = (jwt != null) ? jwt.getClaimAsString("quality") : null;

        StreamService.StreamResponse response = streamService.getSongManifest(songId, userQuality);

        return ResponseEntity.ok()
                .contentType(response.contentType())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(response.resource());
    }

    @GetMapping("/{songId}/{quality}/{fileName}")
    public ResponseEntity<InputStreamResource> getSegment(
            @PathVariable UUID songId,
            @PathVariable String quality,
            @PathVariable String fileName
    ) {
        StreamService.StreamResponse response = streamService.getSongSegment(songId, quality, fileName);

        return ResponseEntity.ok()
                .contentType(response.contentType())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(response.resource());
    }
}