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

    /**
     * Serve the manifest for a song as a streamed resource.
     *
     * @param songId UUID of the song to stream.
     * @param jwt optional authenticated JWT; when present, the JWT's `quality` claim is used to select the manifest quality.
     * @return ResponseEntity containing the song manifest as an InputStreamResource; Content-Type set to the manifest's content type and Cache-Control set to "no-cache".
     */
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

    /**
     * Serves a single song segment file for the specified song and quality.
     *
     * @param songId   the UUID of the song
     * @param quality  the requested quality level (e.g., bitrate or resolution identifier)
     * @param fileName the segment file name to retrieve
     * @return a ResponseEntity containing the segment as an InputStreamResource; the response's Content-Type
     *         matches the segment media type and the Cache-Control header is set to "public, max-age=3600"
     */
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