package com.MarianFinweFeanor.Virtual_Teacher.util;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<User> hasFirstName(String firstName) {
        return (root, query, cb) -> {
            if (firstName == null || firstName.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("firstName"), firstName);
        };
    }

    public static Specification<User> hasLastName(String lastName) {
        return (root, query, cb) -> {
            if (lastName == null || lastName.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("lastName"), lastName);
        };
    }

    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("email"), email);
        };
    }
}
