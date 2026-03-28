package com.danil.library.controller;

import com.danil.library.dto.*;
import com.danil.library.service.LicenseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public LicenseDto createLicense(@RequestBody CreateLicenseRequest request) {
        return licenseService.createLicense(request);
    }

    @PostMapping("/activate")
    public TicketResponse activate(@RequestBody ActivateLicenseRequest request) {
        return licenseService.activateLicense(request);
    }

    @PostMapping("/check")
    public TicketResponse check(@RequestBody CheckLicenseRequest request) {
        return licenseService.checkLicense(request);
    }

    @PostMapping("/renew")
    public TicketResponse renew(@RequestBody RenewLicenseRequest request) {
        return licenseService.renewLicense(request);
    }
}

