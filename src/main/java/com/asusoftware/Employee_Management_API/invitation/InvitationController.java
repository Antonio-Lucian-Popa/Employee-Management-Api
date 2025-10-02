package com.asusoftware.Employee_Management_API.invitation;

import com.asusoftware.Employee_Management_API.model.Invitation;
import com.asusoftware.Employee_Management_API.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Invitation create(@RequestParam @Email String email, @RequestParam Role role){
        return service.create(email, role);
    }

    @GetMapping("/{token}")
    public Invitation get(@PathVariable String token){
        return service.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    public void accept(@PathVariable String token,
                       @RequestParam @NotBlank String firstName,
                       @RequestParam @NotBlank String lastName,
                       @RequestParam @NotBlank String password){
        service.accept(token, firstName, lastName, password);
    }
}
