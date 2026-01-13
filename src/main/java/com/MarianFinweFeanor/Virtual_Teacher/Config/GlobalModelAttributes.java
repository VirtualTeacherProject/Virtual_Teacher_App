package com.MarianFinweFeanor.Virtual_Teacher.Config;

// src/main/java/.../config/GlobalModelAttributes.java

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Map;

/**
 * Provides global model attributes for MVC views.
 *
 * Anything returned from a @ModelAttribute method here
 * is automatically available in ALL Thymeleaf templates.
 */
@ControllerAdvice
@Component
public class GlobalModelAttributes {

    // Used to load user details from the database
    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Adds logged-in user data to the model for ALL controllers/views.
     *
     * The returned Map becomes available as:
     *   ${currentUserData}
     *
     * @param principal injected by Spring Security (email/username)
     * @param auth Authentication object containing roles/authorities
     */

    @ModelAttribute("currentUserData")
    public Map<String, Object> addCurrentUser(Principal principal, Authentication auth) {
        if (principal == null) return Map.of(); // anonymous
        User u = userRepository.findByEmail(principal.getName()).orElse(null);
        if (u == null) return Map.of();

        /*
         * Extract role from Spring Security authorities.
         * like: ROLE_TEACHER
         */

        String role = (auth == null || auth.getAuthorities().isEmpty())
                ? null
                : auth.getAuthorities().iterator().next().getAuthority(); // e.g., ROLE_TEACHER

        return Map.of(
                "email", u.getEmail(),
                "firstName", u.getFirstName(),
                "lastName", u.getLastName(),
                "role", role
        );
    }
}