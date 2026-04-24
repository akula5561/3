package com.danil.library.security;

import com.danil.library.dto.Ticket;
import com.danil.library.signature.SigningService;
import org.springframework.stereotype.Component;

@Component
/** Адаптер для лицензий: подписывает именно объект Ticket. */
public class TicketSigner {

    private final SigningService signingService;

    public TicketSigner(
            SigningService signingService
    ) {
        this.signingService = signingService;
    }

    public String sign(Ticket ticket) {
        // Делегируем в общий модуль подписи, чтобы не дублировать крипто-логику.
        return signingService.sign(ticket);
    }
}
