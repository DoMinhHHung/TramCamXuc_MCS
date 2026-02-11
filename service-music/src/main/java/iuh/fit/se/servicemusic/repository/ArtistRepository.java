package iuh.fit.se.servicemusic.repository;

import iuh.fit.se.servicemusic.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, UUID> {
    Optional<Artist> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}