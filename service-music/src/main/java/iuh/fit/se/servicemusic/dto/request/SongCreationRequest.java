package iuh.fit.se.servicemusic.dto.request;

import lombok.Data;

@Data
public class SongCreationRequest {
    private String title;
    private String artistId;
    private String fileName;
}