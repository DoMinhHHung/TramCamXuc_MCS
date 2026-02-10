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

    @PostMapping
    public ApiResponse<SubscriptionPlan> createPlan(@RequestBody @Valid PlanRequest request) {
        return ApiResponse.<SubscriptionPlan>builder()
                .result(planService.createPlan(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<SubscriptionPlan>> getAllActivePlans() {
        return ApiResponse.<List<SubscriptionPlan>>builder()
                .result(planService.getAllActivePlans())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionPlan> getPlanDetail(@PathVariable UUID id) {
        return ApiResponse.<SubscriptionPlan>builder()
                .result(planService.getPlanDetail(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<SubscriptionPlan> updatePlan(@PathVariable UUID id, @RequestBody @Valid PlanRequest request) {
        return ApiResponse.<SubscriptionPlan>builder()
                .result(planService.updatePlan(id, request))
                .build();
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<Void> togglePlanStatus(@PathVariable UUID id) {
        planService.togglePlanStatus(id);
        return ApiResponse.<Void>builder()
                .message("Plan status updated successfully")
                .build();
    }
}
