package com.danil.library.security;

import com.danil.library.model.UserAccount;
import com.danil.library.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("dbUserDetailsService")
public class DbUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repo;

    public DbUserDetailsService(UserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // В БД  role хранится как "USER" или "ADMIN"
        // Spring Security ждёт "ROLE_USER" / "ROLE_ADMIN"
        String role = user.getRole();
        String springRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(springRole))
        );
    }
}
