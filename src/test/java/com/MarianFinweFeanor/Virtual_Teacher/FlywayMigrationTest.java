package com.MarianFinweFeanor.Virtual_Teacher;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SCRUM-50: the Flyway migrations on their own, without Spring, against in-memory H2.
 * Flyway is configured from the real application.properties, so the baseline
 * settings tested here are the ones the application uses.
 */
class FlywayMigrationTest {

    private static final List<String> TABLES = List.of(
            "USERS", "COURSES", "LECTURES", "ENROLLMENTS",
            "ASSIGNMENTS", "LECTURE_COMMENTS", "LECTURE_NOTES", "COURSE_RESOURCES", "RESOURCE_REQUESTS");

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // A new, empty in-memory database for every test.
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void emptyDatabase_shouldGetTheWholeSchemaFromTheMigrations() {
        int available = flyway().info().pending().length;

        MigrateResult result = flyway().migrate();

        assertThat(available).isPositive();
        assertThat(result.migrationsExecuted).isEqualTo(available);
        assertThat(tables()).containsAll(TABLES);
        assertThat(sequences()).contains("LECTURE_COMMENTS_SEQ");
        assertThat(flyway().info().pending()).isEmpty();
    }

    @Test
    void migratingTwice_shouldChangeNothingTheSecondTime() {
        flyway().migrate();

        assertThat(flyway().migrate().migrationsExecuted).isZero();
    }

    @Test
    void databaseFromBeforeFlyway_shouldKeepItsData_andGetOnlyWhatIsMissing() {
        legacyDatabase();
        assertThat(tables()).doesNotContain("LECTURE_NOTES", "flyway_schema_history");

        flyway().migrate();

        // baselined, then V1 ran on top of the existing tables, then the later migrations
        assertThat(appliedVersions()).startsWith("0", "1");
        assertThat(flyway().info().pending()).isEmpty();
        assertThat(tables()).containsAll(TABLES);

        // existing rows and existing columns are untouched
        assertThat(jdbc.queryForObject(
                "select email from users where first_name = 'Anna'", String.class))
                .isEqualTo("anna@test.com");
        assertThat(jdbc.queryForObject("select count(*) from courses", Integer.class)).isEqualTo(1);
        // SCRUM-45 (V5): existing courses do not require attendance
        assertThat(jdbc.queryForObject("select attendance_required from courses", Boolean.class)).isFalse();

        // the table that was missing is usable
        jdbc.update("insert into lectures (title, description, video_url, assignment_file_path, course_id) "
                + "values ('Intro', 'd', 'v', 'a', (select course_id from courses))");
        jdbc.update("insert into lecture_notes (lecture_id, user_id, content) "
                + "values ((select lecture_id from lectures), (select user_id from users), 'note')");
        assertThat(jdbc.queryForObject("select count(*) from lecture_notes", Integer.class)).isEqualTo(1);
    }

    @Test
    void databaseFromBeforeFlyway_shouldNotBeMigratedAgain_onTheNextStart() {
        legacyDatabase();
        flyway().migrate();

        assertThat(flyway().migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
    }

    // --- helpers -------------------------------------------------------------

    /** Flyway set up the way Spring Boot sets it up from application.properties. */
    private Flyway flyway() {
        Properties app = applicationProperties();
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(Boolean.parseBoolean(
                        app.getProperty("spring.flyway.baseline-on-migrate", "false")))
                .baselineVersion(app.getProperty("spring.flyway.baseline-version", "1"))
                .load();
    }

    private static Properties applicationProperties() {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    /** A database as Hibernate's ddl-auto=update left it, with a user and a course in it. */
    private void legacyDatabase() {
        new ResourceDatabasePopulator(
                new ClassPathResource("db/legacy/h2-schema-before-flyway.sql")).execute(dataSource);

        jdbc.update("insert into users (email, password, first_name, last_name, role, status, teacher_approved) "
                + "values ('anna@test.com', 'x', 'Anna', 'A', 'TEACHER', 'ACTIVE', true)");
        jdbc.update("insert into courses (title, topic, status, teacher_id) "
                + "values ('Java', 'Programming', 'DRAFT', (select user_id from users))");
    }

    private List<String> tables() {
        return jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'",
                String.class);
    }

    private List<String> sequences() {
        return jdbc.queryForList(
                "select sequence_name from information_schema.sequences where sequence_schema = 'PUBLIC'",
                String.class);
    }

    private List<String> appliedVersions() {
        return java.util.Arrays.stream(flyway().info().applied())
                .map(MigrationInfo::getVersion)
                .map(Object::toString)
                .toList();
    }
}
