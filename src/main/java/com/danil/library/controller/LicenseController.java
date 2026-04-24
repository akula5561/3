package com.danil.library.controller;

import com.danil.library.dto.*;
import com.danil.library.security.JwtPrincipal;
import com.danil.library.service.LicenseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST-ручки модуля лицензий. Все пути под {@code /api/licenses}; для защищённых методов нужен JWT (кроме публичных эндпоинтов — здесь все под аутентификацией).
 */
@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * Создание новой лицензии (ключ + владелец, без активации на пользователя).
     * Только админ; в историю пишется, кто создал. Ответ 201 и DTO лицензии.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public LicenseDto createLicense(@RequestBody CreateLicenseRequest request, Authentication auth) {
        JwtPrincipal admin = jwtPrincipal(auth);
        return licenseService.createLicense(request, admin.userId());
    }

    /**
     * Активация по коду: привязка к текущему пользователю и устройству (MAC), лимит устройств, первая/повторная активация.
     * Возвращает подписанный тикет для клиента.
     */
    @PostMapping("/activate")
    public TicketResponse activate(@RequestBody ActivateLicenseRequest request, Authentication auth) {
        JwtPrincipal user = jwtPrincipal(auth);
        return licenseService.activateLicense(request, user.userId());
    }

    /**
     * Проверка: у этого пользователя на этом устройстве есть действующая лицензия на продукт из запроса.
     * Если да — тикет с подписью; иначе 404.
     */
    @PostMapping("/check")
    public TicketResponse check(@RequestBody CheckLicenseRequest request, Authentication auth) {
        JwtPrincipal user = jwtPrincipal(auth);
        return licenseService.checkLicense(request, user.userId());
    }

    /**
     * Продление срока по ключу (если лицензия истекла или скоро истекает — по правилам сервиса).
     * Обновляет дату окончания и пишет событие в историю; ответ — тикет.
     */
    @PostMapping("/renew")
    public TicketResponse renew(@RequestBody RenewLicenseRequest request, Authentication auth) {
        JwtPrincipal user = jwtPrincipal(auth);
        return licenseService.renewLicense(request, user.userId());
    }

    /** Достаёт из Spring Security id пользователя из JWT (поле uid), иначе 401. */
    private static JwtPrincipal jwtPrincipal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return p;
    }
}
