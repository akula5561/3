package com.danil.library.security;

import com.danil.library.dto.Ticket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TicketSigner {

    private final ObjectMapper objectMapper;
    private final SecretKeySpec keySpec;

    public TicketSigner(
            ObjectMapper objectMapper,
            @Value("${security.ticket.secret:${security.jwt.secret}}") String secret
    ) {
        this.objectMapper = objectMapper;
        this.keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String sign(Ticket ticket) {
        try {
            String payload = objectMapper.writeValueAsString(ticket);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] sig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ticket for signing", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign ticket", e);
        }
    }
}

