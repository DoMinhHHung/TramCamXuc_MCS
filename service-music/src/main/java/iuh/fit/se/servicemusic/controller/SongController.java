package iuh.fit.se.servicemusic.controller;

import iuh.fit.se.servicemusic.dto.request.SongCreationRequest;
import iuh.fit.se.servicemusic.dto.response.PresignedUrlResponse;
import iuh.fit.se.servicemusic.entity.Song;
import iuh.fit.se.servicemusic.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
public class SongController {
    private final SongService songService;

    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestParam String fileName) {
        return ResponseEntity.ok(songService.getPresignedUrl(fileName));
    }

    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody SongCreationRequest request,
                                           @RequestParam String objectName) {
        return ResponseEntity.ok(songService.createSong(request, objectName));
    }
}