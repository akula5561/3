package com.danil.library.service;

import com.danil.library.model.UserAccount;
import com.danil.library.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationUserService {

    private final UserAccountRepository users;

    public ApplicationUserService(UserAccountRepository users) {
        this.users = users;
    }

    /**
     * Методичка: {@code ApplicationUserService.getActiveUserOrFail(ownerId)} —
     * пользователь не найден или неактивен → 404.
     */
    public UserAccount getActiveUserOrFail(Long userId) {
        UserAccount u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.isDisabled() || u.isAccountLocked() || u.isAccountExpired() || u.isCredentialsExpired()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User inactive");
        }
        return u;
    }
}
