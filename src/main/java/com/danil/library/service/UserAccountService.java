package com.danil.library.service;

import com.danil.library.dto.RegisterRequest;
import com.danil.library.model.UserAccount;
import com.danil.library.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService {

    private final UserAccountRepository repo;
    private final PasswordEncoder encoder;

    public UserAccountService(UserAccountRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public void register(RegisterRequest req) {
        String username = req.getUsername();
        String password = req.getPassword();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }
        if (repo.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        validatePassword(password);

        String name = req.getName();
        if (name == null || name.isBlank()) {
            name = username;
        }

        String email = req.getEmail();
        if (email == null || email.isBlank()) {
            email = username.toLowerCase().replaceAll("[^a-z0-9._-]", "_") + "@noreply.local";
        }
        if (repo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole("USER");

        repo.save(user);
    }

    public java.util.Optional<UserAccount> findById(Long id) {
        return repo.findById(id);
    }

    public java.util.Optional<UserAccount> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 chars");
        }

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (!hasUpper) {
            throw new IllegalArgumentException("Password must contain uppercase letter");
        }
        if (!hasLower) {
            throw new IllegalArgumentException("Password must contain lowercase letter");
        }
        if (!hasDigit) {
            throw new IllegalArgumentException("Password must contain digit");
        }
        if (!hasSpecial) {
            throw new IllegalArgumentException("Password must contain special char");
        }
    }
}
