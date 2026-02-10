package iuh.fit.se.servicepayment.repository;

import iuh.fit.se.servicepayment.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    boolean existsByName(String name);
    List<SubscriptionPlan> findByIsActiveTrue();
}