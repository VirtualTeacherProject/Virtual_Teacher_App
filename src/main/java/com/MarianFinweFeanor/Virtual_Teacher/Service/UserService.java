package com.MarianFinweFeanor.Virtual_Teacher.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Create or Update a user
    public User saveUser(User user) {
        return userRepository.save(user);
    }


    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get a user by ID
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    // Delete a user by ID
    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }


}
