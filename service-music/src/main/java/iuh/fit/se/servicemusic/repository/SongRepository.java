package iuh.fit.se.servicemusic.repository;

import iuh.fit.se.servicemusic.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SongRepository extends JpaRepository<Song, UUID> {
}