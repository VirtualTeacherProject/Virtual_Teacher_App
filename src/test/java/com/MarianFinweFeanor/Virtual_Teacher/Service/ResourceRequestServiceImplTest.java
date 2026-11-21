package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SCRUM-35: enrolled students request additional resources (at least 100 characters).
 * SCRUM-36: teachers set the status and assign a request to another enrolled student,
 * who answers it.
 */
@ExtendWith(MockitoExtension.class)
class ResourceRequestServiceImplTest {

    private static final String STUDENT = "student@test.com";
    private static final String TEACHER = "teacher@test.com";
    private static final String VALID = "a".repeat(ResourceRequest.MIN_MESSAGE_LENGTH);

    @Mock private ResourceRequestRepository resourceRequestRepository;
    @Mock private CourseService courseService;
    @Mock private UserService userService;
    @Mock private CourseAccessService courseAccessService;
    @Mock private EnrollmentService enrollmentService;

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
        when(courseService.getCourseById(10L)).thenReturn(Optional.of(course));
        when(courseAccessService.canManageCourse(course, TEACHER)).thenReturn(true);   // SCRUM-52
        when(resourceRequestRepository.findByCourse_CourseIdOrderByCreatedAtDesc(10L)).thenReturn(expected);

        assertEquals(expected, resourceRequestService.getRequestsForCourse(10L, TEACHER));
    }

    @Test
    void getRequestsForCourse_shouldBeRefused_forStudent() {
        when(courseService.getCourseById(10L)).thenReturn(Optional.of(course));
        when(courseAccessService.canManageCourse(course, STUDENT)).thenReturn(false);

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

    // --- SCRUM-36 ------------------------------------------------------------

    private static final String HELPER = "helper@test.com";

    private User helper(Long id) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(HELPER);
        user.setRole(UserRole.STUDENT);
        return user;
    }

    /** A request by `student` in course 10, stored with id 3. */
    private ResourceRequest storedRequest(String status) {
        ResourceRequest request = new ResourceRequest();
        request.setRequestId(3L);
        request.setCourse(course);
        request.setStudent(student);
        request.setMessage(VALID);
        request.setStatus(status);
        when(resourceRequestRepository.findById(3L)).thenReturn(Optional.of(request));
        return request;
    }

    private void saveReturnsArgument() {
        when(resourceRequestRepository.save(any(ResourceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // getAssignedRequests

    @Test
    void getAssignedRequests_shouldReturnTheRequestsAssignedToTheUser() {
        List<ResourceRequest> expected = List.of(new ResourceRequest());
        when(resourceRequestRepository.findByCourse_CourseIdAndAssignee_EmailOrderByCreatedAtDesc(10L, HELPER))
                .thenReturn(expected);

        assertEquals(expected, resourceRequestService.getAssignedRequests(10L, HELPER));
    }

    @Test
    void getAssignedRequests_shouldReturnEmptyList_forAnonymousVisitor() {
        assertTrue(resourceRequestService.getAssignedRequests(10L, null).isEmpty());

        verifyNoInteractions(resourceRequestRepository);
    }

    // updateStatus

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "IN_PROGRESS", "RESOLVED", "IRRELEVANT"})
    void updateStatus_shouldSetEveryKnownStatus(String status) {
        storedRequest(ResourceRequest.STATUS_OPEN);
        saveReturnsArgument();

        assertEquals(status, resourceRequestService.updateStatus(10L, 3L, status, TEACHER).getStatus());
        verify(userService).ensureApprovedTeacher(TEACHER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DONE", "resolved", ""})
    void updateStatus_shouldRejectUnknownStatus(String status) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.updateStatus(10L, 3L, status, TEACHER));

        assertEquals("Unknown request status", exception.getMessage());
        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void updateStatus_shouldRejectNullStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.updateStatus(10L, 3L, null, TEACHER));
    }

    @Test
    void updateStatus_shouldBeRefused_forUnapprovedTeacherOrStudent() {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(STUDENT);

        assertThrows(AccessDeniedException.class,
                () -> resourceRequestService.updateStatus(10L, 3L, "RESOLVED", STUDENT));

        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void updateStatus_shouldThrowEntityNotFound_whenRequestBelongsToAnotherCourse() {
        storedRequest(ResourceRequest.STATUS_OPEN);

        assertThrows(EntityNotFoundException.class,
                () -> resourceRequestService.updateStatus(11L, 3L, "RESOLVED", TEACHER));

        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldThrowEntityNotFound_whenRequestDoesNotExist() {
        when(resourceRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> resourceRequestService.updateStatus(10L, 99L, "RESOLVED", TEACHER));
    }

    // assign

    @Test
    void assign_shouldAssignEnrolledStudent_andMoveOpenRequestToInProgress() {
        ResourceRequest request = storedRequest(ResourceRequest.STATUS_OPEN);
        User helper = helper(2L);
        when(userService.getUserById(2L)).thenReturn(Optional.of(helper));
        when(enrollmentService.isEnrolled(HELPER, 10L)).thenReturn(true);
        saveReturnsArgument();

        resourceRequestService.assign(10L, 3L, 2L, TEACHER);

        verify(userService).ensureApprovedTeacher(TEACHER);
        assertEquals(helper, request.getAssignee());
        assertEquals(ResourceRequest.STATUS_IN_PROGRESS, request.getStatus());
    }

    @Test
    void assign_shouldKeepTheAnswer_whenTheSameStudentIsAssignedAgain() {
        ResourceRequest request = storedRequest(ResourceRequest.STATUS_IN_PROGRESS);
        User helper = helper(2L);
        request.setAssignee(helper);
        request.setResponse("An answer");
        when(userService.getUserById(2L)).thenReturn(Optional.of(helper));
        when(enrollmentService.isEnrolled(HELPER, 10L)).thenReturn(true);
        saveReturnsArgument();

        resourceRequestService.assign(10L, 3L, 2L, TEACHER);

        assertEquals("An answer", request.getResponse());
    }

    @Test
    void assign_shouldRemoveThePreviousAnswer_whenAnotherStudentIsAssigned() {
        ResourceRequest request = storedRequest(ResourceRequest.STATUS_IN_PROGRESS);
        request.setAssignee(helper(2L));
        request.setResponse("An answer");
        request.setRespondedAt(java.time.LocalDateTime.now());
        User other = helper(5L);
        when(userService.getUserById(5L)).thenReturn(Optional.of(other));
        when(enrollmentService.isEnrolled(HELPER, 10L)).thenReturn(true);
        saveReturnsArgument();

        resourceRequestService.assign(10L, 3L, 5L, TEACHER);

        assertEquals(other, request.getAssignee());
        assertNull(request.getResponse());
        assertNull(request.getRespondedAt());
        assertEquals(ResourceRequest.STATUS_IN_PROGRESS, request.getStatus());
    }

    @Test
    void assign_withNoStudent_shouldRemoveAssignmentAndAnswer_andReopen() {
        ResourceRequest request = storedRequest(ResourceRequest.STATUS_IN_PROGRESS);
        request.setAssignee(helper(2L));
        request.setResponse("An answer");
        saveReturnsArgument();

        resourceRequestService.assign(10L, 3L, null, TEACHER);

        assertNull(request.getAssignee());
        assertNull(request.getResponse());
        assertEquals(ResourceRequest.STATUS_OPEN, request.getStatus());
        verify(userService, never()).getUserById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "IRRELEVANT"})
    void assign_shouldBeRefused_forClosedRequest(String status) {
        storedRequest(status);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resourceRequestService.assign(10L, 3L, 2L, TEACHER));

        assertEquals("Reopen the request before assigning it", exception.getMessage());
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    void assign_shouldRefuseTheRequestAuthor() {
        storedRequest(ResourceRequest.STATUS_OPEN);
        when(userService.getUserById(1L)).thenReturn(Optional.of(student));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.assign(10L, 3L, 1L, TEACHER));

        assertEquals("Assign the request to a student other than its author", exception.getMessage());
        verify(resourceRequestRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TEACHER", "ADMIN"})
    void assign_shouldRefuseTeachersAndAdmins(UserRole role) {
        storedRequest(ResourceRequest.STATUS_OPEN);
        User manager = helper(2L);
        manager.setRole(role);
        when(userService.getUserById(2L)).thenReturn(Optional.of(manager));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.assign(10L, 3L, 2L, TEACHER));

        assertEquals("Only students can be assigned to a request", exception.getMessage());
    }

    @Test
    void assign_shouldRefuseStudentWhoIsNotEnrolled() {
        storedRequest(ResourceRequest.STATUS_OPEN);
        when(userService.getUserById(2L)).thenReturn(Optional.of(helper(2L)));
        when(enrollmentService.isEnrolled(HELPER, 10L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.assign(10L, 3L, 2L, TEACHER));

        assertEquals("The student must be enrolled in this course", exception.getMessage());
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    void assign_shouldThrowEntityNotFound_whenStudentDoesNotExist() {
        storedRequest(ResourceRequest.STATUS_OPEN);
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> resourceRequestService.assign(10L, 3L, 99L, TEACHER));
    }

    @Test
    void assign_shouldBeRefused_forUnapprovedTeacher() {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(TEACHER);

        assertThrows(AccessDeniedException.class,
                () -> resourceRequestService.assign(10L, 3L, 2L, TEACHER));

        verifyNoInteractions(resourceRequestRepository);
    }

    // respond

    private ResourceRequest assignedToHelper(String status) {
        ResourceRequest request = storedRequest(status);
        request.setAssignee(helper(2L));
        return request;
    }

    @Test
    void respond_shouldSaveTheAssigneesAnswer() {
        ResourceRequest request = assignedToHelper(ResourceRequest.STATUS_IN_PROGRESS);
        when(courseAccessService.canAccessCourseContent(course, HELPER)).thenReturn(true);
        saveReturnsArgument();

        resourceRequestService.respond(10L, 3L, HELPER, "  See https://cppreference.com\r\nand the C++ FAQ  ");

        assertEquals("See https://cppreference.com\nand the C++ FAQ", request.getResponse());
        assertNotNull(request.getRespondedAt());
        // answering does not change the status; the teacher resolves the request
        assertEquals(ResourceRequest.STATUS_IN_PROGRESS, request.getStatus());
    }

    @Test
    void respond_shouldAcceptExactlyTheMaximumLength() {
        ResourceRequest request = assignedToHelper(ResourceRequest.STATUS_IN_PROGRESS);
        when(courseAccessService.canAccessCourseContent(course, HELPER)).thenReturn(true);
        saveReturnsArgument();
        String longest = "a".repeat(ResourceRequest.MAX_RESPONSE_LENGTH);

        resourceRequestService.respond(10L, 3L, HELPER, longest);

        assertEquals(longest, request.getResponse());
    }

    @Test
    void respond_shouldRejectOneCharacterTooMany() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.respond(10L, 3L, HELPER,
                        "a".repeat(ResourceRequest.MAX_RESPONSE_LENGTH + 1)));

        assertEquals("An answer cannot be longer than 2000 characters", exception.getMessage());
        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void respond_shouldRejectBlankAnswer() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.respond(10L, 3L, HELPER, "   "));

        assertEquals("Your answer cannot be empty", exception.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> resourceRequestService.respond(10L, 3L, HELPER, null));
        verifyNoInteractions(resourceRequestRepository);
    }

    @Test
    void respond_shouldBeRefused_forStudentWhoIsNotTheAssignee() {
        assignedToHelper(ResourceRequest.STATUS_IN_PROGRESS);

        assertThrows(AccessDeniedException.class,
                () -> resourceRequestService.respond(10L, 3L, STUDENT, "An answer"));

        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    void respond_shouldBeRefused_whenRequestIsNotAssigned() {
        storedRequest(ResourceRequest.STATUS_OPEN);

        assertThrows(AccessDeniedException.class,
                () -> resourceRequestService.respond(10L, 3L, HELPER, "An answer"));
    }

    @Test
    void respond_shouldBeRefused_whenAssigneeIsNoLongerEnrolled() {
        assignedToHelper(ResourceRequest.STATUS_IN_PROGRESS);
        when(courseAccessService.canAccessCourseContent(course, HELPER)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> resourceRequestService.respond(10L, 3L, HELPER, "An answer"));

        verify(resourceRequestRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "IRRELEVANT"})
    void respond_shouldBeRefused_forClosedRequest(String status) {
        assignedToHelper(status);
        when(courseAccessService.canAccessCourseContent(course, HELPER)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resourceRequestService.respond(10L, 3L, HELPER, "An answer"));

        assertEquals("This request is closed", exception.getMessage());
        verify(resourceRequestRepository, never()).save(any());
    }

    // --- SCRUM-52: only the course's own teacher or an admin ---------------------

    @Test
    void updateStatus_shouldBeRefused_forATeacherOfAnotherCourse() {
        ResourceRequest request = storedRequest(ResourceRequest.STATUS_OPEN);
        doThrow(new org.springframework.security.access.AccessDeniedException("not your course")).when(courseAccessService).ensureCanManageCourse(request.getCourse(), TEACHER);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> resourceRequestService.updateStatus(10L, 3L, ResourceRequest.STATUS_RESOLVED, TEACHER));
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    void assign_shouldBeRefused_forATeacherOfAnotherCourse() {
        ResourceRequest request = storedRequest(ResourceRequest.STATUS_OPEN);
        doThrow(new org.springframework.security.access.AccessDeniedException("not your course")).when(courseAccessService).ensureCanManageCourse(request.getCourse(), TEACHER);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> resourceRequestService.assign(10L, 3L, 2L, TEACHER));
        verify(resourceRequestRepository, never()).save(any());
    }
}
