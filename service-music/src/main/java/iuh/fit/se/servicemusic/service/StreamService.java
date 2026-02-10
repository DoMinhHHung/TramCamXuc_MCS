package iuh.fit.se.servicemusic.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import java.util.UUID;

public interface StreamService {
    StreamResponse getSongManifest(UUID songId, String userQuality);
    StreamResponse getSongSegment(UUID songId, String quality, String fileName);
    record StreamResponse(InputStreamResource resource, MediaType contentType) {}
}