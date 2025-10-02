package com.asusoftware.Employee_Management_API.attendance;

import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.AppUser;
import com.asusoftware.Employee_Management_API.model.Attendance;
import com.asusoftware.Employee_Management_API.model.dto.AttendanceDto;
import com.asusoftware.Employee_Management_API.model.dto.AttendanceUpsertRequest;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository repo;
    private final UserRepository users;

    @Transactional
    public AttendanceDto upsert(AttendanceUpsertRequest r){
        String tenant = TenantContext.getTenant();
        AppUser user = users.findById(r.userId()).orElseThrow();
        Attendance a = repo.findByTenantIdAndUser_IdAndDate(tenant, r.userId(), r.date())
                .orElseGet(() -> Attendance.builder().tenantId(tenant).user(user).date(r.date()).build());
        a.setStatus(r.status());
        a.setNotes(r.notes());
        repo.save(a);
        return new AttendanceDto(a.getId(), user.getId(), a.getDate(), a.getStatus(), a.getNotes());
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto> daily(java.time.LocalDate date){
        String tenant = TenantContext.getTenant();
        return repo.findByTenantIdAndDate(tenant, date).stream()
                .map(a -> new AttendanceDto(a.getId(), a.getUser().getId(), a.getDate(), a.getStatus(), a.getNotes()))
                .toList();
    }
}
