package iuh.fit.se.servicemusic.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SongCreationRequest {
    private String title;
    private String artistId;
    private String fileName;

    private List<String> genreIds;
}