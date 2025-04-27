package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Create or Update a user
    @Override
    public User saveUser(User user)
    {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EntityDuplicateException("User", "email", user.getEmail());
        }

        // If `role` is null, assign default role as STUDENT
        if (user.getRole() == null) {
            user.setRole(UserRole.STUDENT);
        }
        return userRepository.save(user);
    }


    // Get all users
    @Override
    public List<User> getAllUsers() {
        if(userRepository.count() == 0) {
            throw new EntityNotFoundException("Users","database");
        }

        return userRepository.findAll();
    }

    // Get a user by ID
    @Override
    public Optional<User> getUserById(Long userId) {
        if(!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User", userId);
        }
        return userRepository.findById(userId);
    }

    // Delete a user by ID
    @Override
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User", userId);
        }
        userRepository.deleteById(userId);
    }


}
