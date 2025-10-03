package com.asusoftware.Employee_Management_API.leave;

import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.LeaveRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveCreateRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveDecisionRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService svc;

    // LIST all leaves for current tenant (poți filtra după user în service dacă vrei)
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EMPLOYEE')")
    public List<LeaveRequest> list() {
        String tenant = TenantContext.getTenant();
        return svc.findAllForTenant(tenant);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public LeaveDto create(@Valid @RequestBody LeaveCreateRequest r){ return svc.create(r); }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public LeaveDto decide(@PathVariable UUID id, @RequestParam UUID approverId, @RequestBody LeaveDecisionRequest d){
        return svc.decide(id, approverId, d);
    }
}