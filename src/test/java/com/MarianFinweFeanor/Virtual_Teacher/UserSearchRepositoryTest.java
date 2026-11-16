package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.util.UserSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SCRUM-19: the user search filters, run against an in-memory database
 * (@DataJpaTest replaces the file-based H2 datasource).
 */
@DataJpaTest
@ActiveProfiles("test")
class UserSearchRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        save("Anna", "Petrova", "anna.petrova@test.com", UserRole.STUDENT);
        save("Boris", "Annenkov", "boris@school.org", UserRole.STUDENT);
        save("Carla", "Diaz", "carla_diaz@test.com", UserRole.TEACHER);
    }

    private void save(String first, String last, String email, UserRole role) {
        User user = new User();
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }

    private List<String> emails(Specification<User> spec) {
        return userRepository.findAll(spec).stream().map(User::getEmail).toList();
    }

    @Test
    void hasFirstName_shouldMatchPartOfTheName_ignoringCase() {
        assertThat(emails(UserSpecification.hasFirstName("aNN")))
                .containsExactly("anna.petrova@test.com");
    }

    @Test
    void hasLastName_shouldMatchPartOfTheName_ignoringCase() {
        assertThat(emails(UserSpecification.hasLastName("annen")))
                .containsExactly("boris@school.org");
    }

    @Test
    void hasEmail_shouldMatchPartOfTheEmail_ignoringCase() {
        assertThat(emails(UserSpecification.hasEmail("TEST.COM")))
                .containsExactlyInAnyOrder("anna.petrova@test.com", "carla_diaz@test.com");
    }

    @Test
    void filters_shouldBeIgnored_whenBlank() {
        Specification<User> spec = Specification
                .where(UserSpecification.hasFirstName(null))
                .and(UserSpecification.hasLastName("  "))
                .and(UserSpecification.hasEmail(""));

        assertThat(emails(spec)).hasSize(3);
    }

    @Test
    void filters_shouldAllHaveToMatch_whenCombined() {
        Specification<User> spec = Specification
                .where(UserSpecification.hasFirstName("anna"))
                .and(UserSpecification.hasEmail("school"));

        assertThat(emails(spec)).isEmpty();
    }

    @Test
    void matchesAnyField_shouldSearchFirstNameLastNameAndEmail() {
        // "ann" is in Anna's first name and in Boris's last name
        assertThat(emails(UserSpecification.matchesAnyField(" ann ")))
                .containsExactlyInAnyOrder("anna.petrova@test.com", "boris@school.org");

        assertThat(emails(UserSpecification.matchesAnyField("school.org")))
                .containsExactly("boris@school.org");
    }

    @Test
    void matchesAnyField_shouldReturnEveryone_whenBlank() {
        assertThat(emails(UserSpecification.matchesAnyField(""))).hasSize(3);
    }

    @Test
    void matchesAnyField_shouldTreatLikeWildcardsLiterally() {
        assertThat(emails(UserSpecification.matchesAnyField("%"))).isEmpty();
        assertThat(emails(UserSpecification.matchesAnyField("a_d")))
                .containsExactly("carla_diaz@test.com");
    }

    @Test
    void hasRole_shouldRestrictTheSearchToOneRole() {
        Specification<User> spec = Specification
                .where(UserSpecification.matchesAnyField("test.com"))
                .and(UserSpecification.hasRole(UserRole.STUDENT));

        assertThat(emails(spec)).containsExactly("anna.petrova@test.com");
        assertThat(emails(UserSpecification.hasRole(null))).hasSize(3);
    }
}
