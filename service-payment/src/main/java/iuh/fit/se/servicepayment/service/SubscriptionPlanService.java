package iuh.fit.se.servicepayment.service;

import iuh.fit.se.servicepayment.dto.request.PlanRequest;
import iuh.fit.se.servicepayment.entity.SubscriptionPlan;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanService {
    SubscriptionPlan createPlan(PlanRequest request);
    List<SubscriptionPlan> getAllActivePlans();
    SubscriptionPlan getPlanDetail(UUID id);
    SubscriptionPlan updatePlan(UUID id, PlanRequest request);
    void togglePlanStatus(UUID id);
}
