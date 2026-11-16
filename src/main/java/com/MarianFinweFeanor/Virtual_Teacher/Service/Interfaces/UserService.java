package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface UserService {
    // Create or Update a user
    User saveUser(User user);

    User findByEmail(String email);

    User updateUser(User user);

    /** Enrolls the user in the course if not already enrolled. */
    void enrollInCourse(String userEmail, Long courseId);

    /** Unenrolls (removes) the user from the course, if enrolled. */
    void unenrollFromCourse(String userEmail, Long courseId);

    /** Returns all courses the user is currently enrolled in. */
    Set<Course> getEnrolledCourses(String userEmail);


    long countUsers();
    List<User> getAllUsers(); // simpler generic version
    User getUser(Long id);     // return actual User, not Optional
    // Get all users
    List<User> getAllUsers(String firstName, String lastName, String email);

    /**
     * Search by one text that may match first name, last name or email
     * (case-insensitive, partial). A blank query returns everyone.
     * role may be null to search all roles.
     */
    List<User> searchUsers(String query, UserRole role);

    // Get a user by ID
    Optional<User> getUserById(Long userId);

    List<User> findPendingTeachers();

    void approveTeacher(Long userId);

    void ensureApprovedTeacher(String email);


    List<User> findAllStudents();
    List<User> findAllTeachers();


    // Delete a user by ID
    void deleteUserById(Long userId);

    long countByRole(UserRole teacher);

    /** Account status values (see User.status). A PASSIVE user cannot log in. */
    String STATUS_ACTIVE = "ACTIVE";
    String STATUS_PASSIVE = "PASSIVE";

    /**
     * Soft delete (SCRUM-12): the account is switched to PASSIVE and can no longer
     * log in. Courses, enrollments, submissions, grades and reviews stay.
     */
    User deactivateUser(Long userId);

    /** Undo of deactivateUser. */
    User activateUser(Long userId);

    /** Active admins and active, approved teachers: who a course can be assigned to. */
    List<User> findAssignableTeachers();
}
