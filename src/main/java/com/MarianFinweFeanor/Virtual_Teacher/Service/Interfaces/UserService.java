package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    // Create or Update a user
    User saveUser(User user);

    User findByEmail(String email);


    // Get all users
    List<User> getAllUsers();

    // Get a user by ID
    Optional<User> getUserById(Long userId);

    // Delete a user by ID
    void deleteUserById(Long userId);
}
