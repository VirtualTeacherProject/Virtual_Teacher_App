package com.MarianFinweFeanor.Virtual_Teacher.util;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters for searching users (SCRUM-19).
 * Text filters are case-insensitive "contains" matches; a blank value means "no filter".
 */
public class UserSpecification {

    private static final char ESCAPE = '\\';

    private UserSpecification() {
    }

    public static Specification<User> hasFirstName(String firstName) {
        return contains("firstName", firstName);
    }

    public static Specification<User> hasLastName(String lastName) {
        return contains("lastName", lastName);
    }

    public static Specification<User> hasEmail(String email) {
        return contains("email", email);
    }

    /** One search box: the text may appear in the first name, the last name or the email. */
    public static Specification<User> matchesAnyField(String text) {
        return (root, query, cb) -> {
            if (isBlank(text)) {
                return null;
            }
            String pattern = toPattern(text);
            return cb.or(
                    like(cb, root.get("firstName"), pattern),
                    like(cb, root.get("lastName"), pattern),
                    like(cb, root.get("email"), pattern)
            );
        };
    }

    public static Specification<User> hasRole(UserRole role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    private static Specification<User> contains(String field, String value) {
        return (root, query, cb) ->
                isBlank(value) ? null : like(cb, root.get(field), toPattern(value));
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, ESCAPE);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Lower-cased "%text%" with LIKE wildcards in the user's input treated literally. */
    private static String toPattern(String value) {
        String escaped = value.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
