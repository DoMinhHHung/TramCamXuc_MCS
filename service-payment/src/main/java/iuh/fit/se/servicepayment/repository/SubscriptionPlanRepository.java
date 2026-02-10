package iuh.fit.se.servicepayment.repository;

import iuh.fit.se.servicepayment.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    /**
 * Checks whether a subscription plan with the given name exists.
 *
 * @param name the subscription plan name to check
 * @return `true` if a SubscriptionPlan with the given name exists, `false` otherwise
 */
boolean existsByName(String name);
    /**
 * Retrieve all subscription plans that are marked active.
 *
 * @return a list of {@link SubscriptionPlan} objects whose `isActive` field is `true`; an empty list if none exist
 */
List<SubscriptionPlan> findByIsActiveTrue();
}