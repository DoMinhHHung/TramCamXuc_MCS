package iuh.fit.se.serviceidentity.repository;

import iuh.fit.se.serviceidentity.entity.UserFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFeaturesRepository extends JpaRepository<UserFeatures, UUID> {
    /**
 * Finds the UserFeatures entity associated with the specified user identifier.
 *
 * @param userId the UUID of the user whose features are being queried
 * @return an Optional containing the UserFeatures for the given userId, or empty if none exists
 */
Optional<UserFeatures> findByUserId(UUID userId);
}