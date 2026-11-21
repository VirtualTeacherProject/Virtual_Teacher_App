package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceImplTest {

    private static final String EMAIL = "user@test.com";
    private static final Long COURSE_ID = 10L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseAccessServiceImpl courseAccessService;

    private Course published;
    private Course draft;

    @BeforeEach
    void setUp() {
        published = course("PUBLISHED");
        draft = course("DRAFT");
    }

    private static Course course(String status) {
        Course course = new Course();
        course.setCourseId(COURSE_ID);
        course.setStatus(status);
        return course;
    }

    private void givenUser(UserRole role, boolean teacherApproved) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setRole(role);
        user.setTeacherApproved(teacherApproved);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private void givenEnrolled(boolean enrolled) {
        when(enrollmentRepository.existsByStudent_EmailAndCourse_CourseId(EMAIL, COURSE_ID))
                .thenReturn(enrolled);
    }

    // --- canManageCourses ----------------------------------------------------

    @Test
    void canManageCourses_shouldBeFalse_forAnonymous() {
        assertFalse(courseAccessService.canManageCourses(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void canManageCourses_shouldBeFalse_whenUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertFalse(courseAccessService.canManageCourses(EMAIL));
    }

    @Test
    void canManageCourses_shouldBeTrue_forAdmin() {
        givenUser(UserRole.ADMIN, false);

        assertTrue(courseAccessService.canManageCourses(EMAIL));
    }

    @Test
    void canManageCourses_shouldBeTrue_forApprovedTeacher() {
        givenUser(UserRole.TEACHER, true);

        assertTrue(courseAccessService.canManageCourses(EMAIL));
    }

    @Test
    void canManageCourses_shouldBeFalse_forUnapprovedTeacher() {
        givenUser(UserRole.TEACHER, false);

        assertFalse(courseAccessService.canManageCourses(EMAIL));
    }

    @Test
    void canManageCourses_shouldBeFalse_forStudent() {
        // students carry teacherApproved = true after registration
        givenUser(UserRole.STUDENT, true);

        assertFalse(courseAccessService.canManageCourses(EMAIL));
    }

    // --- canViewCourse -------------------------------------------------------

    @Test
    void canViewCourse_shouldBeFalse_whenCourseIsNull() {
        assertFalse(courseAccessService.canViewCourse(null, EMAIL));
    }

    @Test
    void canViewCourse_shouldBeTrue_forAnonymous_whenPublished() {
        assertTrue(courseAccessService.canViewCourse(published, null));
    }

    @Test
    void canViewCourse_shouldBeFalse_forAnonymous_whenDraft() {
        assertFalse(courseAccessService.canViewCourse(draft, null));
    }

    @Test
    void canViewCourse_shouldBeFalse_forStudent_whenDraft() {
        givenUser(UserRole.STUDENT, true);

        assertFalse(courseAccessService.canViewCourse(draft, EMAIL));
    }

    @Test
    void canViewCourse_shouldBeTrue_forApprovedTeacher_whenDraft() {
        givenUser(UserRole.TEACHER, true);

        assertTrue(courseAccessService.canViewCourse(draft, EMAIL));
    }

    // --- canAccessCourseContent ----------------------------------------------

    @Test
    void canAccessCourseContent_shouldBeFalse_whenCourseIsNull() {
        assertFalse(courseAccessService.canAccessCourseContent(null, EMAIL));
    }

    @Test
    void canAccessCourseContent_shouldBeFalse_forAnonymous() {
        assertFalse(courseAccessService.canAccessCourseContent(published, null));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void canAccessCourseContent_shouldBeTrue_forEnrolledStudent() {
        givenUser(UserRole.STUDENT, true);
        givenEnrolled(true);

        assertTrue(courseAccessService.canAccessCourseContent(published, EMAIL));
    }

    @Test
    void canAccessCourseContent_shouldBeFalse_forStudentWhoIsNotEnrolled() {
        givenUser(UserRole.STUDENT, true);
        givenEnrolled(false);

        assertFalse(courseAccessService.canAccessCourseContent(published, EMAIL));
    }

    @Test
    void canAccessCourseContent_shouldBeFalse_forUnapprovedTeacherWhoIsNotEnrolled() {
        givenUser(UserRole.TEACHER, false);
        givenEnrolled(false);

        assertFalse(courseAccessService.canAccessCourseContent(published, EMAIL));
    }

    @Test
    void canAccessCourseContent_shouldBeFalse_forEnrolledStudent_whenCourseIsDraft() {
        givenUser(UserRole.STUDENT, true);

        assertFalse(courseAccessService.canAccessCourseContent(draft, EMAIL));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void canAccessCourseContent_shouldBeTrue_forAdmin_withoutEnrollment() {
        givenUser(UserRole.ADMIN, false);

        assertTrue(courseAccessService.canAccessCourseContent(draft, EMAIL));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void canAccessCourseContent_shouldBeTrue_forApprovedTeacher_withoutEnrollment() {
        givenUser(UserRole.TEACHER, true);

        assertTrue(courseAccessService.canAccessCourseContent(published, EMAIL));
        verifyNoInteractions(enrollmentRepository);
    }

    // --- ensureCanViewCourse -------------------------------------------------

    @Test
    void ensureCanViewCourse_shouldPass_whenPublished() {
        assertDoesNotThrow(() -> courseAccessService.ensureCanViewCourse(published, null));
    }

    @Test
    void ensureCanViewCourse_shouldThrowNotFound_whenDraftAndNotManager() {
        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> courseAccessService.ensureCanViewCourse(draft, null));

        assertTrue(ex.getMessage().contains(String.valueOf(COURSE_ID)));
    }

    @Test
    void ensureCanViewCourse_shouldThrowNotFound_whenCourseIsNull() {
        assertThrows(
                EntityNotFoundException.class,
                () -> courseAccessService.ensureCanViewCourse(null, EMAIL));
    }

    // --- SCRUM-52: canManageCourse / ensureCanManageCourse ----------------------

    private void ownedBy(Course course, String email) {
        User teacher = new User();
        teacher.setEmail(email);
        course.setTeacher(teacher);
    }

    @Test
    void canManageCourse_shouldBeTrue_forTheCoursesOwnApprovedTeacher() {
        givenUser(UserRole.TEACHER, true);
        ownedBy(published, "USER@test.com");   // email compared ignoring case

        assertTrue(courseAccessService.canManageCourse(published, EMAIL));
    }

    @Test
    void canManageCourse_shouldBeFalse_forAnotherApprovedTeacher() {
        givenUser(UserRole.TEACHER, true);
        ownedBy(published, "other@test.com");

        assertFalse(courseAccessService.canManageCourse(published, EMAIL));
        // ...who can still view and create courses (viewing kept open in SCRUM-52)
        assertTrue(courseAccessService.canManageCourses(EMAIL));
        assertTrue(courseAccessService.canViewCourse(draft, EMAIL));
    }

    @Test
    void canManageCourse_shouldBeFalse_forTheOwner_whileNotApproved() {
        givenUser(UserRole.TEACHER, false);
        ownedBy(published, EMAIL);

        assertFalse(courseAccessService.canManageCourse(published, EMAIL));
    }

    @Test
    void canManageCourse_shouldBeTrue_forAdmin_ofAnyCourse() {
        givenUser(UserRole.ADMIN, false);
        ownedBy(published, "other@test.com");

        assertTrue(courseAccessService.canManageCourse(published, EMAIL));
        assertTrue(courseAccessService.canManageCourse(draft, EMAIL));   // no teacher at all
    }

    @Test
    void canManageCourse_shouldBeFalse_forStudentAnonymousUnknownUserCourseWithoutTeacherOrNoCourse() {
        assertFalse(courseAccessService.canManageCourse(published, null));
        assertFalse(courseAccessService.canManageCourse(null, EMAIL));

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertFalse(courseAccessService.canManageCourse(published, EMAIL));

        givenUser(UserRole.STUDENT, false);
        ownedBy(published, EMAIL);
        assertFalse(courseAccessService.canManageCourse(published, EMAIL));

        givenUser(UserRole.TEACHER, true);
        assertFalse(courseAccessService.canManageCourse(draft, EMAIL));   // draft has no teacher
    }

    @Test
    void ensureCanManageCourse_shouldPassForTheOwner_andRefuseEveryoneElse() {
        givenUser(UserRole.TEACHER, true);
        ownedBy(published, EMAIL);
        ownedBy(draft, "other@test.com");

        assertDoesNotThrow(() -> courseAccessService.ensureCanManageCourse(published, EMAIL));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> courseAccessService.ensureCanManageCourse(draft, EMAIL));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> courseAccessService.ensureCanManageCourse(null, EMAIL));
    }
}
