package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import com.MarianFinweFeanor.Virtual_Teacher.util.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if(!userRepository.existsByEmail(email)) {
            throw new EntityNotFoundException("User","email", email);
        }
        return userRepository.findByEmail(email);

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
    @Transactional(readOnly = true)
    public List<User> getAllUsers(String firstName, String lastName, String email) {
        Specification<User> spec = Specification
                .where(UserSpecification.hasFirstName(firstName))
                .and(UserSpecification.hasLastName(lastName))
                .and(UserSpecification.hasEmail(email));
        List<User> users = userRepository.findAll(spec);
        if(users.isEmpty()) {
            throw new EntityNotFoundException("Users","database");
        }

        return users;
    }

    // Get a user by ID
    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }


    //Enrollment test
    @Transactional
    public void enrollInCourse(String userEmail, Long courseId) {
        User user = findByEmail(userEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        if (user.getCourses().contains(course))
        {
            return;
        }

        user.getCourses().add(course);           // uses your existing Set<Course> courses
        userRepository.save(user);          // updates the join table
    }

    @Override
    public void unenrollFromCourse(String userEmail, Long courseId) {
        User u = findByEmail(userEmail);
        Course c = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        if (u.getCourses().remove(c)) {
            userRepository.save(u);
        }
        // else: nothing to do if they weren’t enrolled
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Course> getEnrolledCourses(String userEmail) {
        User u = findByEmail(userEmail);
        // force initialization if your mapping is LAZY
        u.getCourses().size();
        return u.getCourses();
    }


}
