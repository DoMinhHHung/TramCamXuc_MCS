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

    @Override
    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findByIsActiveTrue();
    }

    @Override
    public SubscriptionPlan getPlanDetail(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
    }

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

    @Override
    public void togglePlanStatus(UUID id) {
        SubscriptionPlan plan = getPlanDetail(id);
        plan.setActive(!plan.isActive());
        planRepository.save(plan);
    }
}
