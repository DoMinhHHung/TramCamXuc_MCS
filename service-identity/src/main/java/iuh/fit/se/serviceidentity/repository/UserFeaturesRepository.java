package iuh.fit.se.serviceidentity.repository;

import iuh.fit.se.serviceidentity.entity.UserFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFeaturesRepository extends JpaRepository<UserFeatures, UUID> {
    Optional<UserFeatures> findByUserId(UUID userId);
}