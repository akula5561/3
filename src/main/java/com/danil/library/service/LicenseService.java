package com.danil.library.service;

import com.danil.library.dto.*;
import com.danil.library.model.*;
import com.danil.library.repository.DeviceLicenseRepository;
import com.danil.library.repository.DeviceRepository;
import com.danil.library.repository.LicenseHistoryRepository;
import com.danil.library.repository.LicenseRepository;
import com.danil.library.security.TicketSigner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class LicenseService {

    private final ProductService productService;
    private final LicenseTypeService licenseTypeService;
    private final ApplicationUserService applicationUserService;
    private final LicenseRepository licenses;
    private final DeviceRepository devices;
    private final DeviceLicenseRepository deviceLicenses;
    private final LicenseHistoryRepository history;
    private final TicketSigner ticketSigner;

    public LicenseService(
            ProductService productService,
            LicenseTypeService licenseTypeService,
            ApplicationUserService applicationUserService,
            LicenseRepository licenses,
            DeviceRepository devices,
            DeviceLicenseRepository deviceLicenses,
            LicenseHistoryRepository history,
            TicketSigner ticketSigner
    ) {
        this.productService = productService;
        this.licenseTypeService = licenseTypeService;
        this.applicationUserService = applicationUserService;
        this.licenses = licenses;
        this.devices = devices;
        this.deviceLicenses = deviceLicenses;
        this.history = history;
        this.ticketSigner = ticketSigner;
    }

    /**
     * Методичка: {@code LicenseService.createLicense(request, adminId)} → проверки через сервисы,
     * {@code License (user=null)}, транзакция: save license + history CREATED.
     */
    @Transactional
    public LicenseDto createLicense(CreateLicenseRequest request, Long adminId) {
        UserAccount admin = applicationUserService.getActiveUserOrFail(adminId);

        Product product = productService.getProductOrFail(request.productId());
        if (product.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is blocked");
        }

        LicenseType type = licenseTypeService.getTypeOrFail(request.typeId());
        UserAccount ownerUser = applicationUserService.getActiveUserOrFail(request.ownerUserId());

        License license = new License();
        license.setId(UUID.randomUUID());
        license.setCode(generateCode());
        license.setUser(null);
        license.setOwner(ownerUser);
        license.setProduct(product);
        license.setType(type);
        license.setBlocked(false);
        license.setDeviceCount(Optional.ofNullable(request.deviceCount()).orElse(1));
        license.setDescription(request.description());

        licenses.save(license);

        appendHistory(license, admin, "CREATED", "License created");

        return toDto(license);
    }

    /** Методичка: {@code LicenseService.activateLicense(request, userId)}. */
    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, Long userId) {
        UserAccount currentUser = applicationUserService.getActiveUserOrFail(userId);

        License license = licenses.findByCodeOrFail(request.activationKey());

        if (license.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License is blocked");
        }

        if (license.getUser() != null && !license.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License owned by another user");
        }

        Device device = devices.findByMac(request.deviceMac())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setId(UUID.randomUUID());
                    d.setMacAddress(request.deviceMac());
                    d.setName(Optional.ofNullable(request.deviceName()).orElse("Device"));
                    d.setUser(currentUser);
                    return devices.save(d);
                });

        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device owner mismatch");
        }

        boolean alreadyLinked = !deviceLicenses.findByDeviceAndLicense(device, license).isEmpty();

        if (license.getUser() == null) {
            license.setUser(currentUser);
            license.setFirstActivationDate(LocalDate.now());
            int duration = Optional.ofNullable(license.getType().getDefaultDurationInDays()).orElse(0);
            license.setEndingDate(LocalDate.now().plusDays(duration));
            licenses.save(license);

            if (!alreadyLinked) {
                saveDeviceLicenseLink(license, device);
            }

            appendHistory(license, currentUser, "ACTIVATED",
                    "License activated for device " + request.deviceMac());
            return buildTicketResponse(license, currentUser.getId(), device.getId());
        }

        if (alreadyLinked) {
            return buildTicketResponse(license, currentUser.getId(), device.getId());
        }

        long currentCount = deviceLicenses.countByLicense(license);
        if (currentCount >= license.getDeviceCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device limit reached");
        }

        saveDeviceLicenseLink(license, device);
        appendHistory(license, currentUser, "ACTIVATED",
                "License activated for device " + request.deviceMac());
        return buildTicketResponse(license, currentUser.getId(), device.getId());
    }

    /** Методичка: {@code LicenseService.checkLicense(request, userId)}; устройство не найдено → 404. */
    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request, Long userId) {
        applicationUserService.getActiveUserOrFail(userId);

        Device device = devices.findByMacAddressAndUser_Id(request.deviceMac(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        LocalDate today = LocalDate.now();

        License license = deviceLicenses
                .findActiveByDeviceUserAndProduct(device.getId(), userId, request.productId(), today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        return buildTicketResponse(license, license.getUser().getId(), device.getId());
    }

    /** Методичка: {@code LicenseService.renewLicense(request, userId)}. */
    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, Long userId) {
        UserAccount currentUser = applicationUserService.getActiveUserOrFail(userId);

        License license = licenses.findByCodeOrFail(request.activationKey());

        if (license.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Renewal not allowed");
        }

        if (!license.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License owner mismatch");
        }

        if (license.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Renewal not allowed");
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

        appendHistory(license, currentUser, "RENEWED", "License renewed");

        return buildTicketResponse(license, currentUser.getId(), null);
    }

    private void saveDeviceLicenseLink(License license, Device device) {
        DeviceLicense link = new DeviceLicense();
        link.setId(UUID.randomUUID());
        link.setLicense(license);
        link.setDevice(device);
        link.setActivationDate(LocalDate.now());
        deviceLicenses.save(link);
    }

    private void appendHistory(License license, UserAccount actor, String status, String description) {
        LicenseHistory entry = new LicenseHistory();
        entry.setId(UUID.randomUUID());
        entry.setLicense(license);
        entry.setUser(actor);
        entry.setStatus(status);
        entry.setChangeDate(Instant.now());
        entry.setDescription(description);
        history.save(entry);
    }

    private LicenseDto toDto(License license) {
        return new LicenseDto(
                license.getId(),
                license.getCode(),
                license.getUser() != null ? license.getUser().getId() : null,
                license.getProduct().getId(),
                license.getType().getId(),
                license.getFirstActivationDate(),
                license.getEndingDate(),
                license.isBlocked(),
                license.getDeviceCount(),
                license.getOwner().getId(),
                license.getDescription()
        );
    }

    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    private TicketResponse buildTicketResponse(License license, Long ticketUserId, UUID deviceId) {
        Ticket ticket = new Ticket(
                Instant.now(),
                300,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                ticketUserId,
                deviceId,
                license.isBlocked()
        );
        String signature = ticketSigner.sign(ticket);
        return new TicketResponse(ticket, signature);
    }
}
