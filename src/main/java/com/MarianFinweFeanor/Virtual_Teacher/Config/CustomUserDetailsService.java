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

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private UserRepository userRepository;
    private User user;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    @Override
//    public UserDetails loadUserByUsername(String userName)
//            throws UsernameNotFoundException {
//
//        User user = userRepository.findByEmail(userName);
//        if (user != null) {
//            // Single role
//            System.out.println("Login attempt for user: " + user.getEmail());
//
//            List<GrantedAuthority> authorities = List.of(
//                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
//            );
//
//            return new org.springframework.security.core.userdetails.User(
//                    user.getEmail(),
//                    user.getPassword(),
//                    authorities
//            );
//        } else {
//            throw new UsernameNotFoundException("User not found with name: " + userName);
//        }
//    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Unwrap Optional or throw
        com.MarianFinweFeanor.Virtual_Teacher.Model.User user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user with email: " + username)
                );

        // Build authorities (ensure your enum is not null)
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Build Spring Security UserDetails
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
