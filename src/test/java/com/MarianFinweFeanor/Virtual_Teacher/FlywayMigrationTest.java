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
            "ASSIGNMENTS", "LECTURE_COMMENTS", "LECTURE_NOTES", "COURSE_RESOURCES", "RESOURCE_REQUESTS",
            "LECTURE_ATTENDANCE", "LECTURE_PROGRESS", "COURSE_PREREQUISITES");

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
        jdbc.update("insert into lectures (title, description, video_url, assignment_file_path, course_id, lecture_order) "
                + "values ('Intro', 'd', 'v', 'a', (select course_id from courses), 1)");
        jdbc.update("insert into lecture_notes (lecture_id, user_id, content) "
                + "values ((select lecture_id from lectures), (select user_id from users), 'note')");
        assertThat(jdbc.queryForObject("select count(*) from lecture_notes", Integer.class)).isEqualTo(1);

        // SCRUM-28 (V6): one attendance record per lecture and student
        String attendance = "insert into lecture_attendance (lecture_id, student_id, status, recorded_at) "
                + "values ((select lecture_id from lectures), (select user_id from users), 'ABSENT', current_timestamp)";
        jdbc.update(attendance);
        assertThat(jdbc.queryForObject("select status from lecture_attendance", String.class)).isEqualTo("ABSENT");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(attendance))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // SCRUM-51 (V7): existing lectures are self-paced; live lectures have a start and a link
        assertThat(jdbc.queryForObject("select live from lectures", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select live_start from lectures", java.sql.Timestamp.class)).isNull();
        jdbc.update("update lectures set live = true, live_start = timestamp '2026-12-01 09:30:00', "
                + "meeting_url = 'https://meet.example.com/abc'");
        assertThat(jdbc.queryForObject("select meeting_url from lectures", String.class))
                .isEqualTo("https://meet.example.com/abc");

        // SCRUM-51 (V8): order, assignment flag, in-order courses, progress rows
        assertThat(jdbc.queryForObject("select lectures_in_order from courses", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("select assignment_required from lectures", Boolean.class)).isFalse();
        String progress = "insert into lecture_progress (lecture_id, student_id, finished_at) "
                + "values ((select lecture_id from lectures), (select user_id from users), current_timestamp)";
        jdbc.update(progress);
        assertThat(jdbc.queryForObject("select count(*) from lecture_progress", Integer.class)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(progress))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // SCRUM-48 (V9): prerequisite links, recommended by default, one per pair
        String prerequisite = "insert into course_prerequisites (course_id, required_course_id) "
                + "values ((select course_id from courses), (select course_id from courses))";
        jdbc.update(prerequisite);
        assertThat(jdbc.queryForObject("select required from course_prerequisites", Boolean.class)).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(prerequisite))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void databaseFromBeforeFlyway_shouldNotBeMigratedAgain_onTheNextStart() {
        legacyDatabase();
        flyway().migrate();

        assertThat(flyway().migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
    }

    @Test
    void v8_shouldNumberLectures_flagAssignments_andKeepGradedLecturesFinished() {
        flyway("7").migrate();
        jdbc.update("insert into users (email, password, first_name, last_name, role, status, teacher_approved) "
                + "values ('anna@test.com', 'x', 'Anna', 'A', 'STUDENT', 'ACTIVE', false)");
        jdbc.update("insert into courses (title, topic, status, teacher_id) "
                + "values ('Java', 'Programming', 'PUBLISHED', (select user_id from users))");
        jdbc.update("insert into courses (title, topic, status, teacher_id) "
                + "values ('SQL', 'Databases', 'PUBLISHED', (select user_id from users))");
        String lecture = "insert into lectures (title, description, video_url, assignment_file_path, course_id) "
                + "values (?, 'd', 'v', ?, (select course_id from courses where title = ?))";
        jdbc.update(lecture, "Java 1", "task.txt", "Java");
        jdbc.update(lecture, "SQL 1", "", "SQL");
        jdbc.update(lecture, "Java 2", "  ", "Java");
        String submission = "insert into assignments (student_id, lecture_id, submission_file_path, grade, submitted_at) "
                + "values ((select user_id from users), (select lecture_id from lectures where title = ?), 'f', ?, current_timestamp)";
        jdbc.update(submission, "Java 1", 80.0);
        jdbc.update(submission, "Java 1", 90.0);   // graded twice: still one progress row
        jdbc.update(submission, "Java 2", null);   // not graded: not finished

        flyway().migrate();

        assertThat(jdbc.queryForList("select lecture_order from lectures order by lecture_id", Integer.class))
                .containsExactly(1, 1, 2);
        assertThat(jdbc.queryForList("select assignment_required from lectures order by lecture_id", Boolean.class))
                .containsExactly(true, false, false);
        assertThat(jdbc.queryForList(
                "select l.title from lecture_progress p join lectures l on l.lecture_id = p.lecture_id", String.class))
                .containsExactly("Java 1");
    }

    // --- helpers -------------------------------------------------------------

    /** Flyway set up the way Spring Boot sets it up from application.properties. */
    private Flyway flyway() {
        return flyway(null);
    }

    /** The same, stopping at the given version (null: all migrations). */
    private Flyway flyway(String target) {
        Properties app = applicationProperties();
        var config = Flyway.configure();
        if (target != null) {
            config = config.target(target);
        }
        return config
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
