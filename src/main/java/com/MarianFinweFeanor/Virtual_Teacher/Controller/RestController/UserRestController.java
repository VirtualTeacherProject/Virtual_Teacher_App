package com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController;

import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;


@RestController
@RequestMapping("/api/users")
public class UserRestController {
    private final UserService userService;
    @Autowired

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    /*   Filter by:
    - first name
    -last name
    -full name
    -email

    Sort by:

     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String firstName,
                                                  @RequestParam(required = false) String lastName,
                                                  @RequestParam(required = false) String email) {
        try {
            List<User> users = userService.getAllUsers(firstName, lastName, email);
            return ok(users);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    //Create or Update a user
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try{
            User savedUser = userService.saveUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        }
        catch (EntityDuplicateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
//    @PutMapping
//    public ResponseEntity<User> updateUser(@RequestBody User user) {
//        try {
//            User updatedUser = userService.updateUser(user);
//            return ResponseEntity.ok(updatedUser);
//        } catch (EntityNotFoundException e) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
//        }
//    }


    // Get a user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            return userService.getUserById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getByEmail(@PathVariable String email) {
        try {
            User user = userService.findByEmail(email);
            return ResponseEntity.ok(user);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // Delete a user by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try{
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();}
        catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }
}
