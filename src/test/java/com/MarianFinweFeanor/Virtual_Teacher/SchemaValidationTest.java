package com.MarianFinweFeanor.Virtual_Teacher;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SCRUM-50: on a fresh H2 database the schema comes from Flyway alone and
 * Hibernate validates the entities against it. If an entity changes without a
 * migration, this context fails to start.
 */
@DataJpaTest
@ActiveProfiles("test")
class SchemaValidationTest {

    @Autowired private Flyway flyway;
    @Autowired private Environment environment;

    @Test
    void schemaShouldComeFromFlyway_andHibernateShouldOnlyValidateIt() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }
}
