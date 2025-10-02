package com.asusoftware.Employee_Management_API.company;

import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companies;

    @GetMapping
    public ResponseEntity<Company> get() {
        var c = companies.findBySlug(TenantContext.getTenant()).orElseThrow();
        return ResponseEntity.ok(c);
    }
}
