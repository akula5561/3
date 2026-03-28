package com.danil.library.service;

import com.danil.library.dto.*;
import com.danil.library.model.*;
import com.danil.library.repository.*;
import com.danil.library.security.TicketSigner;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class LicenseService {

    private final ProductRepository products;
    private final LicenseTypeRepository types;
    private final LicenseRepository licenses;
    private final DeviceRepository devices;
    private final DeviceLicenseRepository deviceLicenses;
    private final LicenseHistoryRepository history;
    private final UserAccountService userAccounts;
    private final TicketSigner ticketSigner;

    public LicenseService(
            ProductRepository products,
            LicenseTypeRepository types,
            LicenseRepository licenses,
            DeviceRepository devices,
            DeviceLicenseRepository deviceLicenses,
            LicenseHistoryRepository history,
            UserAccountService userAccounts,
            TicketSigner ticketSigner
    ) {
        this.products = products;
        this.types = types;
        this.licenses = licenses;
        this.devices = devices;
        this.deviceLicenses = deviceLicenses;
        this.history = history;
        this.userAccounts = userAccounts;
        this.ticketSigner = ticketSigner;
    }

    @Transactional
    public LicenseDto createLicense(CreateLicenseRequest request) {
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is blocked");
        }

        LicenseType type = types.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License type not found"));

        UserAccount ownerUser = userAccounts.findById(request.ownerUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner user not found"));

        License license = new License();
        license.setId(UUID.randomUUID());
        license.setCode(generateCode());
        license.setUser(ownerUser);
        license.setProduct(product);
        license.setType(type);
        license.setBlocked(false);
        license.setDeviceCount(Optional.ofNullable(request.deviceCount()).orElse(1));
        license.setOwnerId(request.ownerUserId() != null ? UUID.randomUUID() : null);
        license.setDescription(request.description());

        licenses.save(license);

        LicenseHistory entry = new LicenseHistory();
        entry.setId(UUID.randomUUID());
        entry.setLicense(license);
        entry.setUser(getCurrentUserOrThrow());
        entry.setStatus("CREATED");
        entry.setChangeDate(Instant.now());
        entry.setDescription("License created");
        history.save(entry);

        return toDto(license);
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request) {
        License license = licenses.findByCode(request.activationKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        if (license.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License is blocked");
        }

        UserAccount currentUser = getCurrentUserOrThrow();
        if (!license.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License owner mismatch");
        }

        Device device = devices.findByMacAddress(request.deviceMac())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setId(UUID.randomUUID());
                    d.setMacAddress(request.deviceMac());
                    d.setName(Optional.ofNullable(request.deviceName()).orElse("Device"));
                    d.setUser(currentUser);
                    return devices.save(d);
                });

        long currentCount = deviceLicenses.countByLicense(license);
        if (currentCount >= license.getDeviceCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device limit reached");
        }

        if (deviceLicenses.findByDeviceAndLicense(device, license).isEmpty()) {
            DeviceLicense link = new DeviceLicense();
            link.setId(UUID.randomUUID());
            link.setLicense(license);
            link.setDevice(device);
            link.setActivationDate(LocalDate.now());
            deviceLicenses.save(link);
        }

        if (license.getFirstActivationDate() == null) {
            license.setFirstActivationDate(LocalDate.now());
            int duration = Optional.ofNullable(license.getType().getDefaultDurationInDays()).orElse(0);
            license.setEndingDate(LocalDate.now().plusDays(duration));
        }
        licenses.save(license);

        LicenseHistory entry = new LicenseHistory();
        entry.setId(UUID.randomUUID());
        entry.setLicense(license);
        entry.setUser(currentUser);
        entry.setStatus("ACTIVATED");
        entry.setChangeDate(Instant.now());
        entry.setDescription("License activated for device " + request.deviceMac());
        history.save(entry);

        return buildTicketResponse(license, currentUser.getId(), device.getId());
    }

    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request) {
        UserAccount currentUser = getCurrentUserOrThrow();

        Device device = devices.findByMacAddress(request.deviceMac())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device owner mismatch");
        }

        LocalDate today = LocalDate.now();

        License license = deviceLicenses.findAll().stream()
                .filter(dl -> dl.getDevice().getId().equals(device.getId()))
                .map(DeviceLicense::getLicense)
                .filter(l -> !l.isBlocked())
                .filter(l -> l.getProduct().getId().equals(request.productId()))
                .filter(l -> l.getEndingDate() == null || !l.getEndingDate().isBefore(today))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active license not found"));

        return buildTicketResponse(license, license.getUser().getId(), device.getId());
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request) {
        UserAccount currentUser = getCurrentUserOrThrow();

        License license = licenses.findByCode(request.activationKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        if (!license.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License owner mismatch");
        }

        LocalDate today = LocalDate.now();
        LocalDate ending = license.getEndingDate();

        boolean renewable;
        if (ending == null) {
            renewable = false;
        } else {
            boolean expired = ending.isBefore(today);
            boolean expiresSoon = !ending.isBefore(today) && !ending.isAfter(today.plusDays(7));
            renewable = expired || expiresSoon;
        }

        if (!renewable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Renewal not allowed");
        }

        int duration = Optional.ofNullable(license.getType().getDefaultDurationInDays()).orElse(0);
        if (ending == null || ending.isBefore(today)) {
            license.setEndingDate(today.plusDays(duration));
        } else {
            license.setEndingDate(ending.plusDays(duration));
        }
        licenses.save(license);

        LicenseHistory entry = new LicenseHistory();
        entry.setId(UUID.randomUUID());
        entry.setLicense(license);
        entry.setUser(currentUser);
        entry.setStatus("RENEWED");
        entry.setChangeDate(Instant.now());
        entry.setDescription("License renewed");
        history.save(entry);

        return buildTicketResponse(license, currentUser.getId(), null);
    }

    private LicenseDto toDto(License license) {
        return new LicenseDto(
                license.getId(),
                license.getCode(),
                license.getUser().getId(),
                license.getProduct().getId(),
                license.getType().getId(),
                license.getFirstActivationDate(),
                license.getEndingDate(),
                license.isBlocked(),
                license.getDeviceCount(),
                license.getOwnerId(),
                license.getDescription()
        );
    }

    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    private TicketResponse buildTicketResponse(License license, Long userId, UUID deviceId) {
        Ticket ticket = new Ticket(
                Instant.now(),
                300,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                userId,
                deviceId,
                license.isBlocked()
        );
        String signature = ticketSigner.sign(ticket);
        return new TicketResponse(ticket, signature);
    }

    private UserAccount getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        String username = auth.getName();
        return userAccounts.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}

