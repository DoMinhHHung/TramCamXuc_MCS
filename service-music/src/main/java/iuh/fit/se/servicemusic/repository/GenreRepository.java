package iuh.fit.se.servicemusic.repository;

import iuh.fit.se.servicemusic.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenreRepository extends JpaRepository<Genre, UUID> {
    Optional<Genre> findByKey(String key);
    boolean existsByKey(String key);
}