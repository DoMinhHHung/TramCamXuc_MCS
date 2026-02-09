package iuh.fit.se.servicemusic.service;

import iuh.fit.se.servicemusic.dto.request.SongCreationRequest;
import iuh.fit.se.servicemusic.dto.response.PresignedUrlResponse;
import iuh.fit.se.servicemusic.entity.Song;

public interface SongService {
    PresignedUrlResponse getPresignedUrl(String fileName);
    Song createSong(SongCreationRequest request, String objectName);

}
