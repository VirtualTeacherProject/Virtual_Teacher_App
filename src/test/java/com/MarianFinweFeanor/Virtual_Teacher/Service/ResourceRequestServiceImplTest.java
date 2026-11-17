package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SCRUM-35: enrolled students request additional resources (at least 100 characters). */
@ExtendWith(MockitoExtension.class)
class ResourceRequestServiceImplTest {

    private static final String STUDENT = "student@test.com";
    private static final String TEACHER = "teacher@test.com";
    private static final String VALID = "a".repeat(ResourceRequest.MIN_MESSAGE_LENGTH);

    @Mock private ResourceRequestRepository resourceRequestRepository;
    @Mock private CourseService courseService;
    @Mock private UserService userService;
    @Mock private CourseAccessService courseAccessService;

    @InjectMocks
    private ResourceRequestServiceImpl resourceRequestService;

    private Course course;
    private User student;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(10L);

        student = new User();
        student.setUserId(1L);
        student.setEmail(STUDENT);
        student.setRole(UserRole.STUDENT);
    }

    private void enrolledStudent() {
        when(courseService.getCourseById(10L)).thenReturn(Optional.of(course));
        when(userService.findByEmail(STUDENT)).thenReturn(student);
        when(courseAccessService.canAccessCourseContent(course, STUDENT)).thenReturn(true);
        when(resourceRequestRepository.save(any(ResourceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- createRequest -------------------------------------------------------

    @Test
    void createRequest_shouldSaveOpenRequestForEnrolledStudent() {
        enrolledStudent();

        ResourceRequest saved = resourceRequestService.createRequest(10L, STUDENT, VALID);

        assertEquals(course, saved.getCourse());
        assertEquals(student, saved.getStudent());
        assertEquals(VALID, saved.getMessage());
        assertEquals(ResourceRequest.STATUS_OPEN, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void createRequest_shouldAcceptExactlyTheMaximumLength() {
        enrolledStudent();
        String longest = "a".repeat(ResourceRequest.MAX_MESSAGE_LENGTH);

        assertEquals(longest, resourceRequestService.createRequest(10L, STUDENT, longest).getMessage());
    }

    @Test
    void createRequest_shouldRejectOneCharacterTooFew() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.createRequest(10L, STUDENT, "a".repeat(99)));

        assertEquals("Describe the resource you need in at least 100 characters", exception.getMessage());
        verifyNoInteractions(resourceRequestRepository, courseService, userService);
    }

    @Test
    void createRequest_shouldRejectOneCharacterTooMany() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.createRequest(
                        10L, STUDENT, "a".repeat(ResourceRequest.MAX_MESSAGE_LENGTH + 1)));

        assertEquals("A resource request cannot be longer than 2000 characters", exception.getMessage());
        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void createRequest_shouldRejectNullOrBlankMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.createRequest(10L, STUDENT, null));
        assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.createRequest(10L, STUDENT, " ".repeat(150)));
        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void createRequest_shouldNotCountSurroundingSpacesTowardsTheMinimum() {
        String padded = "   " + "a".repeat(99) + "   ";

        assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.createRequest(10L, STUDENT, padded));
    }

    @Test
    void createRequest_shouldCountWindowsLineBreaksAsOneCharacter_andStoreThemAsLf() {
        enrolledStudent();
        // 50 + CRLF + 49 = 100 characters once CRLF becomes LF
        String message = "a".repeat(50) + "\r\n" + "b".repeat(49);

        ResourceRequest saved = resourceRequestService.createRequest(10L, STUDENT, message);

        assertEquals("a".repeat(50) + "\n" + "b".repeat(49), saved.getMessage());
    }

    @Test
    void createRequest_shouldBeRefused_whenStudentIsNotEnrolled() {
        when(courseService.getCourseById(10L)).thenReturn(Optional.of(course));
        when(userService.findByEmail(STUDENT)).thenReturn(student);
        when(courseAccessService.canAccessCourseContent(course, STUDENT)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resourceRequestService.createRequest(10L, STUDENT, VALID));

        assertEquals("Enroll in this course to request resources", exception.getMessage());
        verify(resourceRequestRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TEACHER", "ADMIN"})
    void createRequest_shouldBeRefused_forTeachersAndAdmins(UserRole role) {
        User manager = new User();
        manager.setEmail(TEACHER);
        manager.setRole(role);
        when(courseService.getCourseById(10L)).thenReturn(Optional.of(course));
        when(userService.findByEmail(TEACHER)).thenReturn(manager);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resourceRequestService.createRequest(10L, TEACHER, VALID));

        assertEquals("Only students can request resources", exception.getMessage());
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    void createRequest_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseService.getCourseById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> resourceRequestService.createRequest(99L, STUDENT, VALID));

        verify(resourceRequestRepository, never()).save(any());
    }

    // --- reading -------------------------------------------------------------

    @Test
    void getRequestsForCourse_shouldReturnAllRequests_toCourseManager() {
        List<ResourceRequest> expected = List.of(new ResourceRequest(), new ResourceRequest());
        when(courseAccessService.canManageCourses(TEACHER)).thenReturn(true);
        when(resourceRequestRepository.findByCourse_CourseIdOrderByCreatedAtDesc(10L)).thenReturn(expected);

        assertEquals(expected, resourceRequestService.getRequestsForCourse(10L, TEACHER));
    }

    @Test
    void getRequestsForCourse_shouldBeRefused_forStudent() {
        when(courseAccessService.canManageCourses(STUDENT)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> resourceRequestService.getRequestsForCourse(10L, STUDENT));

        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void getOwnRequests_shouldReturnOnlyTheUsersRequests() {
        List<ResourceRequest> expected = List.of(new ResourceRequest());
        when(resourceRequestRepository.findByCourse_CourseIdAndStudent_EmailOrderByCreatedAtDesc(10L, STUDENT))
                .thenReturn(expected);

        assertEquals(expected, resourceRequestService.getOwnRequests(10L, STUDENT));
    }

    @Test
    void getOwnRequests_shouldReturnEmptyList_forAnonymousVisitor() {
        assertTrue(resourceRequestService.getOwnRequests(10L, null).isEmpty());

        verifyNoInteractions(resourceRequestRepository);
    }
}
