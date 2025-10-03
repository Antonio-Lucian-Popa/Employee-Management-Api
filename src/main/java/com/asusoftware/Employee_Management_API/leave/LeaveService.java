package com.asusoftware.Employee_Management_API.leave;

import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.AppUser;
import com.asusoftware.Employee_Management_API.model.LeaveRequest;
import com.asusoftware.Employee_Management_API.model.LeaveStatus;
import com.asusoftware.Employee_Management_API.model.dto.LeaveCreateRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveDecisionRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveDto;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository repo;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<LeaveRequest> findAllForTenant(String tenant) {
        return repo.findByTenantIdOrderByCreatedAtDesc(tenant);
    }

    @Transactional
    public LeaveDto create(LeaveCreateRequest r){
        String tenant = TenantContext.getTenant();
        AppUser u = users.findById(r.userId()).orElseThrow();
        LeaveRequest l = LeaveRequest.builder()
                .tenantId(tenant)
                .user(u)
                .type(r.type())
                .startDate(r.startDate())
                .endDate(r.endDate())
                .days(r.days())
                .status(LeaveStatus.PENDING)
                .reason(r.reason())
                .build();
        repo.save(l);
        return toDto(l);
    }

    @Transactional
    public LeaveDto decide(java.util.UUID id, java.util.UUID approverId, LeaveDecisionRequest d){
        LeaveRequest l = repo.findById(id).orElseThrow();
        l.setStatus(d.status());
        users.findById(approverId).ifPresent(l::setApprover);
        return toDto(repo.save(l));
    }

    private LeaveDto toDto(LeaveRequest l){
        return new LeaveDto(
                l.getId(),
                l.getUser().getId(),
                l.getType(),
                l.getStartDate(),
                l.getEndDate(),
                l.getDays(),
                l.getStatus(),
                l.getApprover() != null ? l.getApprover().getId() : null,
                l.getReason()
        );
    }
}