package iuh.fit.se.servicepayment.service.impl;

import iuh.fit.se.servicepayment.dto.request.PlanRequest;
import iuh.fit.se.servicepayment.entity.SubscriptionPlan;
import iuh.fit.se.servicepayment.entity.enums.DurationUnit;
import iuh.fit.se.servicepayment.exception.*;
import iuh.fit.se.servicepayment.repository.SubscriptionPlanRepository;
import iuh.fit.se.servicepayment.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {
    private final SubscriptionPlanRepository planRepository;

    /**
     * Creates and persists a new subscription plan from the provided request.
     *
     * The method validates that no existing plan uses the same name, converts the request's
     * duration unit to the domain enum, marks the new plan as active, and saves it.
     *
     * @param request the plan creation payload containing name, description, price, duration,
     *                durationUnit (string), and features
     * @return the persisted {@link SubscriptionPlan} with generated identifiers and persisted state
     * @throws AppException with {@code ErrorCode.PLAN_EXISTED} if a plan with the same name already exists
     */
    @Override
    public SubscriptionPlan createPlan(PlanRequest request) {
        if (planRepository.existsByName(request.getName()))
            throw new AppException(ErrorCode.PLAN_EXISTED);

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .duration(request.getDuration())
                .durationUnit(DurationUnit.valueOf(request.getDurationUnit()))
                .features(request.getFeatures())
                .isActive(true)
                .build();

        return planRepository.save(plan);
    }

    /**
     * Retrieve all subscription plans that are currently active.
     *
     * @return a list of SubscriptionPlan entities with `isActive` equal to true; an empty list if none exist
     */
    @Override
    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findByIsActiveTrue();
    }

    /**
     * Retrieve a subscription plan by its unique identifier.
     *
     * @param id the UUID of the subscription plan to fetch
     * @return the subscription plan with the specified id
     * @throws AppException if no subscription plan exists with the given id (ErrorCode.PLAN_NOT_FOUND)
     */
    @Override
    public SubscriptionPlan getPlanDetail(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
    }

    /**
     * Update an existing subscription plan with values from the request.
     *
     * @param id      the UUID of the subscription plan to update
     * @param request the request payload containing new plan values
     * @return the updated SubscriptionPlan
     * @throws AppException if the plan with the given id does not exist (ErrorCode.PLAN_NOT_FOUND)
     *                      or if another plan with the requested name already exists (ErrorCode.PLAN_EXISTED)
     */
    @Override
    public SubscriptionPlan updatePlan(UUID id, PlanRequest request) {
        SubscriptionPlan plan = getPlanDetail(id);

        if (!plan.getName().equals(request.getName()) && planRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.PLAN_EXISTED);
        }

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setDuration(request.getDuration());
        plan.setDurationUnit(DurationUnit.valueOf(request.getDurationUnit()));
        plan.setFeatures(request.getFeatures());

        return planRepository.save(plan);
    }

    /**
     * Toggles the active status of the subscription plan identified by the given UUID and persists the change.
     *
     * @param id the UUID of the subscription plan to update
     */
    @Override
    public void togglePlanStatus(UUID id) {
        SubscriptionPlan plan = getPlanDetail(id);
        plan.setActive(!plan.isActive());
        planRepository.save(plan);
    }
}