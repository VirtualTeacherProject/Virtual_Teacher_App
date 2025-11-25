package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import com.MarianFinweFeanor.Virtual_Teacher.util.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final EnrollmentService enrollmentService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepo,
                           EnrollmentService enrollmentService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepo;
        this.enrollmentService = enrollmentService;
        this.passwordEncoder = passwordEncoder;
    }

//    @Override
//    @Transactional(readOnly = true)
//    public User findByEmail(String email) {
//        if(!userRepository.existsByEmail(email)) {
//            throw new EntityNotFoundException("User","email", email);
//        }
//        return userRepository.findByEmail(email);
//
//    }

    @Override
    public User saveUser(User user) {
        // check duplicate
        if (user.getUserId() == null) {
            // New user → check pure email duplicate
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new EntityDuplicateException("User", "email", user.getEmail());
            }
        } else {
            // Existing user → email must be unique for other users
            if (userRepository.existsByEmailAndUserIdNot(user.getEmail(), user.getUserId())) {
                throw new EntityDuplicateException("User", "email", user.getEmail());
            }
        }

        // default role/status if null
        if (user.getRole() == null) {
            user.setRole(UserRole.STUDENT);
        }
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ACTIVE");
        }

        // NEW: encode the raw password coming from the form
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        // if repo returns Optional<User>
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        // if repo returns User directly, use:
        // return Optional.ofNullable(userRepo.findByEmail(email))
        //         .orElseThrow(() -> new EntityNotFoundException("User", email));
    }



//    // Create or Update a user
////    @Override
////    public User saveUser(User user)
////    {
////        if (userRepository.existsByEmail(user.getEmail())) {
////            throw new EntityDuplicateException("User", "email", user.getEmail());
////        }
////
////        // If `role` is null, assign default role as STUDENT
////        if (user.getRole() == null) {
////            user.setRole(UserRole.STUDENT);
////        }
////        return userRepository.save(user);
////    }




    // Get all users

    @Override
    public List<User> getAllUsers(String firstName, String lastName, String email) {
        // implement your filtering or return userRepo.findAll();
        return userRepository.findAll();
    }
//    @Override
//    @Transactional(readOnly = true)
//    public List<User> getAllUsers(String firstName, String lastName, String email) {
//        Specification<User> spec = Specification
//                .where(UserSpecification.hasFirstName(firstName))
//                .and(UserSpecification.hasLastName(lastName))
//                .and(UserSpecification.hasEmail(email));
//        List<User> users = userRepository.findAll(spec);
//        if(users.isEmpty()) {
//            throw new EntityNotFoundException("Users","database");
//        }
//
//        return users;
//    }

    // Get a user by ID
    @Override
    public Optional<User> getUserById(Long userId) {

        return userRepository.findById(userId);
    }

    public List<User> findAllStudents() {
        return userRepository.findByRole(UserRole.STUDENT);
    }

    public List<User> findAllTeachers() {
        return userRepository.findByRole(UserRole.TEACHER);
    }


    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }



//    @Override
//    @Transactional(readOnly = true)
//    public Optional<User> getUserById(Long userId) {
//        if(!userRepository.existsById(userId)) {
//            throw new EntityNotFoundException("User", userId);
//        }
//        return userRepository.findById(userId);
//    }

    // Delete a user by ID
    @Override
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User", userId);
        }
        userRepository.deleteById(userId);
    }

    @Override
    public long countByRole(UserRole teacher) {
        return 0;
    }

    @Override
    @Transactional
    public User updateUser(User user) {

        return userRepository.save(user);
    }






    //Enrollment test
//    @Transactional
//////    public void enrollInCourse(String userEmail, Long courseId) {
//////        User user = findByEmail(userEmail);
//////        Course course = courseRepository.findById(courseId)
//////                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
//////
//////        if (user.getCourses().contains(course))
//////        {
//////            return;
//////        }
//////
//////        user.getCourses().add(course);           // uses your existing Set<Course> courses
//////        userRepository.save(user);          // updates the join table
//////    }

    @Override
    public void enrollInCourse(String userEmail, Long courseId) {
        enrollmentService.enroll(userEmail, courseId);
    }

//    @Override
//    public void unenrollFromCourse(String userEmail, Long courseId) {
//        User u = findByEmail(userEmail);
//        Course c = courseRepository.findById(courseId)
//                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
//
//        if (u.getCourses().remove(c)) {
//            userRepository.save(u);
//        }
//        // else: nothing to do if they weren’t enrolled
//    }

    @Override
    public void unenrollFromCourse(String userEmail, Long courseId) {
        enrollmentService.unenroll(userEmail, courseId);
    }

    @Override
    public Set<Course> getEnrolledCourses(String userEmail) {
        return enrollmentService.getEnrolledCourses(userEmail);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public Set<Course> getEnrolledCourses(String userEmail) {
//        User u = findByEmail(userEmail);
//        // force initialization if your mapping is LAZY
//        u.getCourses().size();
//        return u.getCourses();
//    }


}
