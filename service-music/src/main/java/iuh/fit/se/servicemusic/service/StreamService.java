package iuh.fit.se.servicemusic.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import java.util.UUID;

public interface StreamService {
    /**
 * Retrieve the streaming manifest for a song at the requested quality.
 *
 * @param songId     the UUID of the song whose manifest to retrieve
 * @param userQuality the requested quality level or profile (for example "low", "medium", "high")
 * @return           a StreamResponse containing the manifest resource and its media type
 */
StreamResponse getSongManifest(UUID songId, String userQuality);
    /**
 * Retrieve a streaming response for a specific segment of a song.
 *
 * @param songId   the UUID of the song
 * @param quality  the requested quality level (for example, "128k", "320k")
 * @param fileName the segment file name to stream
 * @return         a StreamResponse containing the segment's input stream resource and its media type
 */
StreamResponse getSongSegment(UUID songId, String quality, String fileName);
    record StreamResponse(InputStreamResource resource, MediaType contentType) {}
}