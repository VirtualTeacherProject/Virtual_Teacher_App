package com.MarianFinweFeanor.Virtual_Teacher.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Small schema adjustments that Hibernate's ddl-auto=update does not make on
 * an existing database (it adds tables/columns but never relaxes constraints).
 * Every statement here must be safe to run on each start-up.
 */
@Component
@Profile("!test")
public class SchemaMigrations implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrations.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrations(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // SCRUM-15: a course survives the deletion of its teacher, so
        // courses.teacher_id must allow NULL. Works on H2 and PostgreSQL.
        execute("ALTER TABLE courses ALTER COLUMN teacher_id DROP NOT NULL");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Schema migration skipped: {} ({})", sql, e.getMessage());
        }
    }
}
