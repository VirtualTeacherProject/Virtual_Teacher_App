package com.MarianFinweFeanor.Virtual_Teacher.Config;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * CustomUserDetailsService is a Spring Security infrastructure class.
 *
 * Its ONLY responsibility is to tell Spring Security:
 *  - how to load a user from the database
 *  - how to convert that user into a UserDetails object
 *
 * It is NOT a business service (like UserService),
 * so it usually lives in Config / Security package.
 */
// Must be a Spring-managed bean so Spring Security can find it

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final    UserRepository userRepository;   // Repository used to fetch users from the database
    private User user;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * This method is CALLED AUTOMATICALLY by Spring Security
     * during the login/authentication process.
     *
     * @param username the value entered in the login form
     *                 (in your case: email)
     *
     * @return UserDetails object that Spring Security understands
     *
     * @throws UsernameNotFoundException if no user exists with this email
     */

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Unwrap Optional or throw
        /*
         * 1) Fetch the user from the database using the repository.
         *    If no user exists, authentication immediately fails.
         */
        com.MarianFinweFeanor.Virtual_Teacher.Model.User user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user with email: " + username)
                );

        /*
         * 2) Convert your application's role (enum)
         *    into Spring Security authorities.
         *
         *    Spring Security REQUIRES roles to be prefixed with "ROLE_".
         *    Example:
         *      TEACHER -> ROLE_TEACHER
         */
        // Build authorities (ensure enum is not null)
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        /*
         * 3) Build a Spring Security UserDetails object.
         *
         *    IMPORTANT:
         *    - This is NOT the User entity.
         *    - Spring Security does not work with your domain model directly.
         *    - It only understands UserDetails.
         */


        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                // optionally drive enabled/disabled off your "status" column
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus()))
                .build();
    }

}
