package iuh.fit.se.servicepayment.service;

import iuh.fit.se.servicepayment.dto.request.PlanRequest;
import iuh.fit.se.servicepayment.entity.SubscriptionPlan;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanService {
    /**
 * Create a new subscription plan using the provided creation parameters.
 *
 * @param request the plan creation parameters (name, price, duration, features, etc.)
 * @return the created {@link SubscriptionPlan}
 */
SubscriptionPlan createPlan(PlanRequest request);
    /**
 * Retrieves all subscription plans that are currently active.
 *
 * @return a list of active SubscriptionPlan objects; empty list if no active plans exist.
 */
List<SubscriptionPlan> getAllActivePlans();
    /**
 * Retrieve detailed information for a subscription plan identified by its UUID.
 *
 * @param id the UUID of the subscription plan to fetch
 * @return the subscription plan with the specified UUID
 */
SubscriptionPlan getPlanDetail(UUID id);
    /**
 * Updates the subscription plan identified by the given UUID with values from the provided request.
 *
 * @param id      UUID of the subscription plan to update
 * @param request fields to apply to the plan update
 * @return the updated SubscriptionPlan
 */
SubscriptionPlan updatePlan(UUID id, PlanRequest request);
    /**
 * Toggle the active state of the subscription plan identified by the given UUID.
 *
 * @param id the UUID of the subscription plan whose active/inactive status should be toggled
 */
void togglePlanStatus(UUID id);
}