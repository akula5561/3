package com.danil.library.bootstrap;

import com.danil.library.model.License;
import com.danil.library.model.LicenseType;
import com.danil.library.model.Product;
import com.danil.library.model.UserAccount;
import com.danil.library.repository.LicenseRepository;
import com.danil.library.repository.LicenseTypeRepository;
import com.danil.library.repository.ProductRepository;
import com.danil.library.repository.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Демо-данные после Flyway: admin / user и одна несактивированная лицензия.
 * Отключить: {@code app.seed-demo-data=false}
 */
@Component
@Profile("postgres")
@Order(100)
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class PostgresDemoDataSeed implements ApplicationRunner {

    public static final UUID DEMO_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    public static final UUID DEMO_TYPE_TRIAL_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    private final UserAccountRepository users;
    private final ProductRepository products;
    private final LicenseTypeRepository types;
    private final LicenseRepository licenses;
    private final PasswordEncoder encoder;

    public PostgresDemoDataSeed(
            UserAccountRepository users,
            ProductRepository products,
            LicenseTypeRepository types,
            LicenseRepository licenses,
            PasswordEncoder encoder
    ) {
        this.users = users;
        this.products = products;
        this.types = types;
        this.licenses = licenses;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Product product = products.findById(DEMO_PRODUCT_ID).orElse(null);
        LicenseType trial = types.findById(DEMO_TYPE_TRIAL_ID).orElse(null);
        if (product == null || trial == null) {
            return;
        }

        UserAccount admin = ensureUser("admin", "Администратор", "admin@demo.local", "ADMIN", "Admin123!");
        ensureUser("user", "Демо пользователь", "user@demo.local", "USER", "User1234!");

        if (licenses.findByCode("DEMO-ACTIVATION-KEY-001").isEmpty()) {
            UserAccount owner = users.findByUsername("user").orElse(admin);
            License license = new License();
            license.setId(UUID.randomUUID());
            license.setCode("DEMO-ACTIVATION-KEY-001");
            license.setUser(null);
            license.setOwner(owner);
            license.setProduct(product);
            license.setType(trial);
            license.setBlocked(false);
            license.setDeviceCount(3);
            license.setDescription("Демо-ключ для активации в клиенте");
            licenses.save(license);
        }
    }

    private UserAccount ensureUser(String username, String name, String email, String role, String rawPassword) {
        return users.findByUsername(username).orElseGet(() -> {
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setName(name);
            u.setEmail(email);
            u.setPassword(encoder.encode(rawPassword));
            u.setRole(role);
            return users.save(u);
        });
    }
}
