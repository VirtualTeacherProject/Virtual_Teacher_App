package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ContentCleanupService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityInUseException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ContentCleanupService contentCleanupService;

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
    void getAllUsersWithFilters_shouldQueryRepositoryWithSpecification() {
        List<User> expected = List.of(student, teacher);

        when(userRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(expected);

        List<User> result = userService.getAllUsers(
                "John",
                "Doe",
                "john@test.com"
        );

        assertEquals(expected, result);
        verify(userRepository).findAll(any(Specification.class), any(Sort.class));
        verify(userRepository, never()).findAll();
    }

    @Test
    void searchUsers_shouldQueryRepositoryWithSpecification_sortedByName() {
        List<User> expected = List.of(student);

        when(userRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(expected);

        List<User> result = userService.searchUsers("stud", UserRole.STUDENT);

        assertEquals(expected, result);
        verify(userRepository).findAll(
                any(Specification.class),
                eq(Sort.by("lastName", "firstName", "email")));
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
    void deleteUserById_shouldDeleteTheUsersOwnContentFirst() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUserById(1L);

        InOrder inOrder = inOrder(contentCleanupService, userRepository);
        inOrder.verify(contentCleanupService).deleteUserContent(1L);
        inOrder.verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserById_shouldBeRefused_whileTheUserStillTeachesCourses() {
        Course java = new Course();
        java.setCourseId(10L);
        java.setTitle("Java Basics");
        java.setTeacher(teacher);

        Course python = new Course();
        python.setCourseId(11L);
        python.setTitle("Python Basics");
        python.setTeacher(teacher);

        when(userRepository.existsById(2L)).thenReturn(true);
        when(courseRepository.findByTeacher_UserId(2L)).thenReturn(List.of(java, python));

        EntityInUseException exception = assertThrows(
                EntityInUseException.class,
                () -> userService.deleteUserById(2L)
        );

        // SCRUM-15: the message tells the admin what to do and which courses block it
        assertTrue(exception.getMessage().contains("2 courses"));
        assertTrue(exception.getMessage().contains("Java Basics"));
        assertTrue(exception.getMessage().contains("Python Basics"));
        assertTrue(exception.getMessage().contains("Reassign"));

        // nothing is deleted or changed
        assertEquals(teacher, java.getTeacher());
        verify(userRepository, never()).deleteById(anyLong());
        verify(courseRepository, never()).save(any());
        verifyNoInteractions(contentCleanupService);
    }

    @Test
    void countByRole_shouldReturnCountFromRepository() {
        when(userRepository.countByRole(UserRole.TEACHER)).thenReturn(3L);

        assertEquals(3L, userService.countByRole(UserRole.TEACHER));
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
        verifyNoInteractions(contentCleanupService, courseRepository);
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










    // --- SCRUM-12: deactivate / activate / assignable teachers ---------------

    @Test
    void deactivateUser_shouldSetStatusPassive_andKeepTheUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        User result = userService.deactivateUser(1L);

        assertEquals("PASSIVE", result.getStatus());
        verify(userRepository).save(student);
        verify(userRepository, never()).deleteById(anyLong());
        verifyNoInteractions(contentCleanupService, courseRepository);
    }

    @Test
    void activateUser_shouldSetStatusActive() {
        student.setStatus("PASSIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        User result = userService.activateUser(1L);

        assertEquals("ACTIVE", result.getStatus());
        verify(userRepository).save(student);
    }

    @Test
    void deactivateUser_shouldThrowEntityNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.deactivateUser(99L)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void findAssignableTeachers_shouldReturnOnlyActiveAdminsAndActiveApprovedTeachers() {
        User admin = new User();
        admin.setUserId(3L);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus("ACTIVE");

        User pendingTeacher = new User();
        pendingTeacher.setUserId(4L);
        pendingTeacher.setRole(UserRole.TEACHER);
        pendingTeacher.setStatus("ACTIVE");
        pendingTeacher.setTeacherApproved(false);

        User deactivatedTeacher = new User();
        deactivatedTeacher.setUserId(5L);
        deactivatedTeacher.setRole(UserRole.TEACHER);
        deactivatedTeacher.setStatus("PASSIVE");
        deactivatedTeacher.setTeacherApproved(true);

        // students carry teacherApproved = true after registration
        student.setTeacherApproved(true);

        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(student, teacher, admin, pendingTeacher, deactivatedTeacher));

        List<User> result = userService.findAssignableTeachers();

        assertEquals(List.of(teacher, admin), result);
    }
}
