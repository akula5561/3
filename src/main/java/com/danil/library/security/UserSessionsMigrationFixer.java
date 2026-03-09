package com.danil.library.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

//@Component
public class UserSessionsMigrationFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public UserSessionsMigrationFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // ⚠️ если не было таблицы — просто выходим, Hibernate создаст сам
        if (!tableExists("user_sessions")) return;

        System.out.println("[MIGRATION] Fixing user_sessions schema...");

        // 1) добавляем колонки, если не было
        safeExec("ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS created_at timestamptz");
        safeExec("ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS expires_at timestamptz");
        safeExec("ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS rotated_at timestamptz");
        safeExec("ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS status varchar(16)");
        safeExec("ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS refresh_jti varchar(80)");
        safeExec("ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS user_id bigint");

        // 2) заполняем NULL дефолтами, чтобы Postgres дал сделать NOT NULL
        Instant now = Instant.now();
        safeExec(
                "UPDATE user_sessions SET created_at = ? WHERE created_at IS NULL",
                Timestamp.from(now)
        );
        safeExec(
                "UPDATE user_sessions SET expires_at = ? WHERE expires_at IS NULL",
                Timestamp.from(now.plusSeconds(60L * 60L * 24L * 30L)) // +30 дней
        );
        safeExec(
                "UPDATE user_sessions SET status = 'ACTIVE' WHERE status IS NULL"
        );

        // refresh_jti обязан быть уникальным — если есть старые строки без него, проще удалить их
        // (иначе ты не сможешь нормально обеспечивать уникальность)
        safeExec("DELETE FROM user_sessions WHERE refresh_jti IS NULL OR user_id IS NULL");

        // 3) теперь можно делать NOT NULL
        safeExec("ALTER TABLE user_sessions ALTER COLUMN created_at SET NOT NULL");
        safeExec("ALTER TABLE user_sessions ALTER COLUMN expires_at SET NOT NULL");
        safeExec("ALTER TABLE user_sessions ALTER COLUMN status SET NOT NULL");
        safeExec("ALTER TABLE user_sessions ALTER COLUMN refresh_jti SET NOT NULL");
        safeExec("ALTER TABLE user_sessions ALTER COLUMN user_id SET NOT NULL");

        // 4) индексы/уникальность
        safeExec("CREATE UNIQUE INDEX IF NOT EXISTS ix_user_sessions_refresh_jti ON user_sessions(refresh_jti)");
        safeExec("CREATE INDEX IF NOT EXISTS ix_user_sessions_user ON user_sessions(user_id)");

        System.out.println("[MIGRATION] user_sessions fixed.");
    }

    private boolean tableExists(String table) {
        List<String> r = jdbc.queryForList(
                "SELECT to_regclass(?)",
                String.class,
                table
        );
        return r != null && !r.isEmpty() && r.get(0) != null;
    }

    private void safeExec(String sql, Object... args) {
        try {
            if (args == null || args.length == 0) jdbc.execute(sql);
            else jdbc.update(sql, args);
        } catch (Exception e) {
            System.out.println("[MIGRATION] Ignore error for SQL: " + sql + " => " + e.getMessage());
        }
    }
}
