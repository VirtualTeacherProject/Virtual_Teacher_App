package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User student;
    private User teacher;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setUserId(1L);
        student.setEmail("student@test.com");
        student.setPassword("rawPassword");
        student.setRole(UserRole.STUDENT);
        student.setStatus("ACTIVE");

        teacher = new User();
        teacher.setUserId(2L);
        teacher.setEmail("teacher@test.com");
        teacher.setPassword("rawPassword");
        teacher.setRole(UserRole.TEACHER);
        teacher.setStatus("ACTIVE");
        teacher.setTeacherApproved(true);
    }

    @Test
    void ensureApprovedTeacher_shouldAllowAdmin() {
        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        assertDoesNotThrow(
                () -> userService.ensureApprovedTeacher("admin@test.com")
        );
    }

    @Test
    void ensureApprovedTeacher_shouldAllowApprovedTeacher() {
        when(userRepository.findByEmail("teacher@test.com"))
                .thenReturn(Optional.of(teacher));

        assertDoesNotThrow(
                () -> userService.ensureApprovedTeacher("teacher@test.com")
        );
    }

    @Test
    void ensureApprovedTeacher_shouldThrowAccessDeniedException_whenTeacherIsNotApproved() {
        teacher.setTeacherApproved(false);

        when(userRepository.findByEmail("teacher@test.com"))
                .thenReturn(Optional.of(teacher));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> userService.ensureApprovedTeacher("teacher@test.com")
        );

        assertEquals(
                "Your teacher account is pending admin approval.",
                exception.getMessage()
        );
    }

    @Test
    void ensureApprovedTeacher_shouldThrowAccessDeniedException_whenUserIsNotTeacher() {
        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.ensureApprovedTeacher("student@test.com")
        );
    }

    @Test
    void saveUser_shouldSaveNewStudentAndEncodePassword() {
        student.setUserId(null);

        when(userRepository.existsByEmail("student@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("rawPassword"))
                .thenReturn("encodedPassword");

        when(userRepository.save(student))
                .thenReturn(student);

        User result = userService.saveUser(student);

        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isTeacherApproved());

        verify(userRepository).save(student);
    }

    @Test
    void saveUser_shouldKeepNewTeacherPendingApproval() {
        teacher.setUserId(null);
        teacher.setTeacherApproved(true);

        when(userRepository.existsByEmail("teacher@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("rawPassword"))
                .thenReturn("encodedPassword");

        when(userRepository.save(teacher))
                .thenReturn(teacher);

        User result = userService.saveUser(teacher);

        assertFalse(result.isTeacherApproved());
        assertEquals("encodedPassword", result.getPassword());
    }

    @Test
    void saveUser_shouldThrowEntityDuplicateException_whenNewUserEmailAlreadyExists() {
        student.setUserId(null);

        when(userRepository.existsByEmail("student@test.com"))
                .thenReturn(true);

        assertThrows(
                EntityDuplicateException.class,
                () -> userService.saveUser(student)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void saveUser_shouldThrowEntityDuplicateException_whenExistingUserChangesToDuplicateEmail() {
        student.setUserId(1L);

        when(userRepository.existsByEmailAndUserIdNot(
                "student@test.com",
                1L
        )).thenReturn(true);

        assertThrows(
                EntityDuplicateException.class,
                () -> userService.saveUser(student)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void saveUser_shouldSetDefaultRoleAndStatus_whenTheyAreMissing() {
        User user = new User();
        user.setEmail("new@test.com");
        user.setPassword("rawPassword");
        user.setRole(null);
        user.setStatus(null);

        when(userRepository.existsByEmail("new@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("rawPassword"))
                .thenReturn("encodedPassword");

        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.saveUser(user);

        assertEquals(UserRole.STUDENT, result.getRole());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isTeacherApproved());
    }

    @Test
    void findPendingTeachers_shouldReturnPendingTeachers() {
        teacher.setTeacherApproved(false);

        List<User> expected = List.of(teacher);

        when(userRepository.findByRoleAndTeacherApprovedFalse(
                UserRole.TEACHER
        )).thenReturn(expected);

        List<User> result = userService.findPendingTeachers();

        assertEquals(expected, result);

        verify(userRepository)
                .findByRoleAndTeacherApprovedFalse(UserRole.TEACHER);
    }

    @Test
    void approveTeacher_shouldApproveTeacherAndSaveUser() {
        teacher.setTeacherApproved(false);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(teacher));

        userService.approveTeacher(2L);

        assertTrue(teacher.isTeacherApproved());
        assertEquals(UserRole.TEACHER, teacher.getRole());

        verify(userRepository).save(teacher);
    }

    @Test
    void approveTeacher_shouldThrowEntityNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.approveTeacher(99L)
        );

        assertEquals(
                "User with id 99 not found",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_shouldReturnUser_whenUserExists() {
        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        User result =
                userService.findByEmail("student@test.com");

        assertEquals(student, result);

        verify(userRepository)
                .findByEmail("student@test.com");
    }

    @Test
    void findByEmail_shouldThrowEntityNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.findByEmail("missing@test.com")
        );
    }

    @Test
    void getUserById_shouldReturnOptionalFromRepository() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(student));

        Optional<User> result =
                userService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(student, result.get());

        verify(userRepository).findById(1L);
    }

    @Test
    void updateUser_shouldSaveAndReturnUser() {
        when(userRepository.save(student))
                .thenReturn(student);

        User result = userService.updateUser(student);

        assertEquals(student, result);
        verify(userRepository).save(student);
    }

    @Test
    void getAllUsersWithFilters_shouldReturnAllUsersFromRepository() {
        List<User> expected = List.of(student, teacher);

        when(userRepository.findAll())
                .thenReturn(expected);

        List<User> result = userService.getAllUsers(
                "John",
                "Doe",
                "john@test.com"
        );

        assertEquals(expected, result);
        verify(userRepository).findAll();
    }

    @Test
    void findAllStudents_shouldReturnStudents() {
        List<User> expected = List.of(student);

        when(userRepository.findByRole(UserRole.STUDENT))
                .thenReturn(expected);

        List<User> result = userService.findAllStudents();

        assertEquals(expected, result);
        verify(userRepository).findByRole(UserRole.STUDENT);
    }

    @Test
    void findAllTeachers_shouldReturnTeachers() {
        List<User> expected = List.of(teacher);

        when(userRepository.findByRole(UserRole.TEACHER))
                .thenReturn(expected);

        List<User> result = userService.findAllTeachers();

        assertEquals(expected, result);
        verify(userRepository).findByRole(UserRole.TEACHER);
    }

    @Test
    void countUsers_shouldReturnRepositoryCount() {
        when(userRepository.count())
                .thenReturn(5L);

        long result = userService.countUsers();

        assertEquals(5L, result);
        verify(userRepository).count();
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        List<User> expected = List.of(student, teacher);

        when(userRepository.findAll())
                .thenReturn(expected);

        List<User> result = userService.getAllUsers();

        assertEquals(expected, result);
        verify(userRepository).findAll();
    }

    @Test
    void getUser_shouldReturnUser_whenUserExists() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(student));

        User result = userService.getUser(1L);

        assertEquals(student, result);
    }

    @Test
    void getUser_shouldThrowRuntimeException_whenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUser(99L)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void deleteUserById_shouldDeleteUser_whenUserExists() {
        when(userRepository.existsById(1L))
                .thenReturn(true);

        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserById_shouldThrowEntityNotFoundException_whenUserDoesNotExist() {
        when(userRepository.existsById(99L))
                .thenReturn(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.deleteUserById(99L)
        );

        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void enrollInCourse_shouldDelegateToEnrollmentService() {
        userService.enrollInCourse(
                "student@test.com",
                10L
        );

        verify(enrollmentService)
                .enroll("student@test.com", 10L);
    }

    @Test
    void unenrollFromCourse_shouldDelegateToEnrollmentService() {
        userService.unenrollFromCourse(
                "student@test.com",
                10L
        );

        verify(enrollmentService)
                .unenroll("student@test.com", 10L);
    }

    @Test
    void getEnrolledCourses_shouldReturnCoursesFromEnrollmentService() {
        Course course = new Course();
        course.setCourseId(10L);

        Set<Course> expected = Set.of(course);

        when(enrollmentService
                .getEnrolledCourses("student@test.com"))
                .thenReturn(expected);

        Set<Course> result =
                userService.getEnrolledCourses("student@test.com");

        assertEquals(expected, result);
    }









}