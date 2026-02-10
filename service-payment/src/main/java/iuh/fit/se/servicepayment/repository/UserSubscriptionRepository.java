package iuh.fit.se.servicepayment.repository;

import iuh.fit.se.servicepayment.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    /**
 * Finds the subscription associated with a specific user.
 *
 * @param userId the UUID of the user whose subscription is being retrieved
 * @return an Optional containing the UserSubscription for the given user ID, or empty if none exists
 */
Optional<UserSubscription> findByUserId(UUID userId);
}