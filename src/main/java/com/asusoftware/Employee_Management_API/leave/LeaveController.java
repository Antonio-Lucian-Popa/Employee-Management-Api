package com.asusoftware.Employee_Management_API.leave;

import com.asusoftware.Employee_Management_API.model.dto.LeaveCreateRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveDecisionRequest;
import com.asusoftware.Employee_Management_API.model.dto.LeaveDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService svc;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public LeaveDto create(@Valid @RequestBody LeaveCreateRequest r){ return svc.create(r); }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public LeaveDto decide(@PathVariable UUID id, @RequestParam UUID approverId, @RequestBody LeaveDecisionRequest d){
        return svc.decide(id, approverId, d);
    }
}