package iuh.fit.se.servicepayment.controller;

import iuh.fit.se.servicepayment.dto.request.PlanRequest;
import iuh.fit.se.servicepayment.dto.response.ApiResponse;
import iuh.fit.se.servicepayment.entity.SubscriptionPlan;

import iuh.fit.se.servicepayment.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    /**
     * Create a new subscription plan from the provided request.
     *
     * @param request the plan creation payload containing required plan details
     * @return an ApiResponse containing the created SubscriptionPlan
     */
    @PostMapping
    public ApiResponse<SubscriptionPlan> createPlan(@RequestBody @Valid PlanRequest request) {
        return ApiResponse.<SubscriptionPlan>builder()
                .result(planService.createPlan(request))
                .build();
    }

    /**
     * Retrieve all active subscription plans.
     *
     * @return an ApiResponse containing the list of active SubscriptionPlan objects
     */
    @GetMapping
    public ApiResponse<List<SubscriptionPlan>> getAllActivePlans() {
        return ApiResponse.<List<SubscriptionPlan>>builder()
                .result(planService.getAllActivePlans())
                .build();
    }

    /**
     * Retrieves details of the subscription plan identified by the given UUID.
     *
     * @param id the UUID of the subscription plan
     * @return the subscription plan with the specified id
     */
    @GetMapping("/{id}")
    public ApiResponse<SubscriptionPlan> getPlanDetail(@PathVariable UUID id) {
        return ApiResponse.<SubscriptionPlan>builder()
                .result(planService.getPlanDetail(id))
                .build();
    }

    /**
     * Update an existing subscription plan identified by its UUID.
     *
     * @param id      the UUID of the subscription plan to update
     * @param request the request payload containing updated plan fields
     * @return        the updated SubscriptionPlan
     */
    @PutMapping("/{id}")
    public ApiResponse<SubscriptionPlan> updatePlan(@PathVariable UUID id, @RequestBody @Valid PlanRequest request) {
        return ApiResponse.<SubscriptionPlan>builder()
                .result(planService.updatePlan(id, request))
                .build();
    }

    /**
     * Toggle the active/inactive status of the subscription plan with the given UUID.
     *
     * @param id the UUID of the subscription plan to toggle
     * @return an ApiResponse with no data payload; the response message indicates the operation result
     */
    @PatchMapping("/{id}/toggle")
    public ApiResponse<Void> togglePlanStatus(@PathVariable UUID id) {
        planService.togglePlanStatus(id);
        return ApiResponse.<Void>builder()
                .message("Plan status updated successfully")
                .build();
    }
}