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

/**
 * Бизнес-логика лицензий: создание админом, активация/проверка/продление пользователем.
 * Проверки продуктов, типов и пользователей делегируются в отдельные сервисы (как в методичке).
 */
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
     * Выпуск новой лицензии: продукт и тип — через {@link ProductService}/{@link LicenseTypeService},
     * владелец — через {@link ApplicationUserService}. До активации {@code user_id} остаётся null.
     * Пишется запись в историю со статусом CREATED и id админа.
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

    /**
     * Активация ключа: устройство по MAC (при отсутствии — регистрируется на текущего пользователя).
     * Первая активация — проставляются пользователь, даты и связь лицензия–устройство.
     * Повторная — только новая связь, с проверкой лимита {@code deviceCount}; та же пара устройство+лицензия даёт тикет без дублей.
     */
    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, Long userId) {
        // Текущий пользователь из JWT; если неактивен/не найден — ошибка.
        UserAccount currentUser = applicationUserService.getActiveUserOrFail(userId);

        // Лицензия берётся строго по ключу активации из запроса.
        License license = licenses.findByCodeOrFail(request.activationKey());

        // Заблокированную лицензию активировать нельзя.
        if (license.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "License is blocked");
        }

        // Если лицензия уже принадлежит другому пользователю — доступ запрещён.
        if (license.getUser() != null && !license.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Лицензия, принадлежащая другому пользователю");
        }

        // Ищем устройство по MAC; если нет — создаём и привязываем к текущему пользователю.
        Device device = devices.findByMac(request.deviceMac())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setId(UUID.randomUUID());
                    d.setMacAddress(request.deviceMac());
                    d.setName(Optional.ofNullable(request.deviceName()).orElse("Device"));
                    d.setUser(currentUser);
                    return devices.save(d);
                });

        // Нельзя активировать лицензию на устройство, которое принадлежит другому пользователю.
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Несоответствие владельца устройства");
        }

        // Проверка, была ли уже связка именно этой лицензии с этим устройством.
        boolean alreadyLinked = !deviceLicenses.findByDeviceAndLicense(device, license).isEmpty();

        // Первая активация лицензии: фиксируем владельца и период действия.
        if (license.getUser() == null) {
            license.setUser(currentUser);
            license.setFirstActivationDate(LocalDate.now());
            int duration = Optional.ofNullable(license.getType().getDefaultDurationInDays()).orElse(0);
            license.setEndingDate(LocalDate.now().plusDays(duration));
            licenses.save(license);

            // Создаём связь лицензия-устройство, если её ещё нет.
            if (!alreadyLinked) {
                saveDeviceLicenseLink(license, device);
            }

            appendHistory(license, currentUser, "ACTIVATED",
                    "Лицензия, активированная для устройства " + request.deviceMac());
            return buildTicketResponse(license, currentUser.getId(), device.getId());
        }

        // Повторный вызов для уже связанной пары устройство+лицензия: просто возвращаем тикет.
        if (alreadyLinked) {
            return buildTicketResponse(license, currentUser.getId(), device.getId());
        }

        // Для нового устройства проверяем лимит deviceCount.
        long currentCount = deviceLicenses.countByLicense(license);
        if (currentCount >= license.getDeviceCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Достигнут предел для устройства");
        }

        // Лимит не превышен: добавляем новое устройство к лицензии и пишем событие в историю.
        saveDeviceLicenseLink(license, device);
        appendHistory(license, currentUser, "ACTIVATED",
                "Лицензия, активированная для устройства " + request.deviceMac());
        return buildTicketResponse(license, currentUser.getId(), device.getId());
    }

    /**
     * Проверка «можно ли работать»: устройство должно быть зарегистрировано на этого пользователя;
     * ищется активная (не заблокирована, срок не истёк) лицензия на указанный продукт с привязкой к устройству.
     */
    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request, Long userId) {
        applicationUserService.getActiveUserOrFail(userId);

        Device device = devices.findByMacAddressAndUser_Id(request.deviceMac(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Устройство не найдено"));

        LocalDate today = LocalDate.now();

        License license = deviceLicenses
                .findActiveByDeviceUserAndProduct(device.getId(), userId, request.productId(), today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лицензия не найдена"));

        return buildTicketResponse(license, license.getUser().getId(), device.getId());
    }

    /**
     * Продление: только владелец активированной лицензии; не заблокирована; срок уже истёк или заканчивается в течение 7 дней.
     * К {@code endingDate} добавляется {@code defaultDurationInDays} типа (от сегодня, если просрочена, иначе от текущего конца).
     */
    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, Long userId) {
        // Текущий пользователь из JWT; только активный пользователь может продлевать.
        UserAccount currentUser = applicationUserService.getActiveUserOrFail(userId);

        // Ищем лицензию по ключу активации, который пришёл в запросе.
        License license = licenses.findByCodeOrFail(request.activationKey());

        // Нельзя продлить лицензию, которая ещё ни разу не была активирована (нет владельца user_id).
        if (license.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Продление не допускается");
        }

        // Продлевать может только тот пользователь, на которого лицензия оформлена после активации.
        if (!license.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Несоответствие владельца лицензии");
        }

        // Заблокированная лицензия не продлевается.
        if (license.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Продление не допускается");
        }

        // Берём "сегодня" и текущую дату окончания для расчёта окна продления.
        LocalDate today = LocalDate.now();
        LocalDate ending = license.getEndingDate();

        // Разрешаем продление, если лицензия уже истекла
        // или истекает в ближайшие 7 дней (включительно).
        boolean renewable;
        if (ending == null) {
            renewable = false;
        } else {
            boolean expired = ending.isBefore(today);
            boolean expiresSoon = !ending.isBefore(today) && !ending.isAfter(today.plusDays(7));
            renewable = expired || expiresSoon;
        }

        // Если срок ещё "далеко" и не попадает в окно 7 дней, возвращаем 409.
        if (!renewable) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Renewal not allowed");
        }

        // Берём длительность продления из типа лицензии.
        int duration = Optional.ofNullable(license.getType().getDefaultDurationInDays()).orElse(0);
        // Если уже просрочена — продлеваем от сегодняшней даты; иначе от текущей endingDate.
        if (ending == null || ending.isBefore(today)) {
            license.setEndingDate(today.plusDays(duration));
        } else {
            license.setEndingDate(ending.plusDays(duration));
        }
        // Сохраняем новый срок и фиксируем событие в истории.
        licenses.save(license);

        appendHistory(license, currentUser, "RENEWED", "License renewed");

        // Возвращаем новый тикет (deviceId здесь не обязателен, поэтому null).
        return buildTicketResponse(license, currentUser.getId(), null);
    }

    /** Одна запись в {@code device_licenses}: факт активации лицензии на устройстве. */
    private void saveDeviceLicenseLink(License license, Device device) {
        DeviceLicense link = new DeviceLicense();
        link.setId(UUID.randomUUID());
        link.setLicense(license);
        link.setDevice(device);
        link.setActivationDate(LocalDate.now());
        deviceLicenses.save(link);
    }

    /** Аудит в {@code license_history}: кто совершил действие и с каким статусом. */
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

    /** Ответ API без ленивых сущностей: id владельца и активатора (если есть). */
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

    /** Уникальный строковый ключ активации (замените на свой формат при необходимости). */
    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    /**
     * Собирает {@link Ticket} (время сервера, TTL, даты лицензии, user/device, blocked) и ЭЦП через {@link TicketSigner}.
     * {@code deviceId} может быть null (например, после продления без привязки к устройству в ответе).
     */
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
