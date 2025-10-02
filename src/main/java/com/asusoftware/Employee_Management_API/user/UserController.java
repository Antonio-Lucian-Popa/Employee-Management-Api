package com.asusoftware.Employee_Management_API.user;

import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.AppUser;
import com.asusoftware.Employee_Management_API.model.AuthProvider;
import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.UserStatus;
import com.asusoftware.Employee_Management_API.model.dto.UserDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<UserDto> list(){
        String tenant = TenantContext.getTenant();
        return users.findAllByTenantId(tenant).stream()
                .map(u -> new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole(), u.getStatus()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserDto create(@RequestParam @Email String email,
                          @RequestParam @NotBlank String firstName,
                          @RequestParam @NotBlank String lastName,
                          @RequestParam Role role,
                          @RequestParam(required = false) String password){
        String tenant = TenantContext.getTenant();
        AppUser u = AppUser.builder()
                .tenantId(tenant)
                .email(email.toLowerCase())
                .password(password != null && !password.isBlank() ? encoder.encode(password) : null)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .provider(password != null && !password.isBlank() ? AuthProvider.LOCAL : AuthProvider.GOOGLE)
                .build();
        users.save(u);
        return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole(), u.getStatus());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public UserDto update(@PathVariable UUID id, @RequestBody UpdateUserRequest body){
        AppUser u = users.findById(id).orElseThrow();
        if (body.role()!=null) u.setRole(body.role());
        if (body.status()!=null) u.setStatus(body.status());
        if (body.firstName()!=null) u.setFirstName(body.firstName());
        if (body.lastName()!=null) u.setLastName(body.lastName());
        users.save(u);
        return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole(), u.getStatus());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        users.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}