package com.danil.library.controller;

import com.danil.library.dto.*;
import com.danil.library.security.JwtPrincipal;
import com.danil.library.service.LicenseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * Методичка: {@code LicenseController.createLicense} → {@code LicenseService.createLicense(request, adminId)}.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public LicenseDto createLicense(@RequestBody CreateLicenseRequest request, Authentication auth) {
        JwtPrincipal admin = jwtPrincipal(auth);
        return licenseService.createLicense(request, admin.userId());
    }

    @PostMapping("/activate")
    public TicketResponse activate(@RequestBody ActivateLicenseRequest request, Authentication auth) {
        JwtPrincipal user = jwtPrincipal(auth);
        return licenseService.activateLicense(request, user.userId());
    }

    @PostMapping("/check")
    public TicketResponse check(@RequestBody CheckLicenseRequest request, Authentication auth) {
        JwtPrincipal user = jwtPrincipal(auth);
        return licenseService.checkLicense(request, user.userId());
    }

    @PostMapping("/renew")
    public TicketResponse renew(@RequestBody RenewLicenseRequest request, Authentication auth) {
        JwtPrincipal user = jwtPrincipal(auth);
        return licenseService.renewLicense(request, user.userId());
    }

    private static JwtPrincipal jwtPrincipal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return p;
    }
}
