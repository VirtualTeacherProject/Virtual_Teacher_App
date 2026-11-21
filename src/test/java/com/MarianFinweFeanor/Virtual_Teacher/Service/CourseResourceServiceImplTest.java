package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseResourceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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

/** SCRUM-34: additional course resources, visible only after enrollment. */
@ExtendWith(MockitoExtension.class)
class CourseResourceServiceImplTest {

    private static final String STUDENT = "student@test.com";
    private static final String TEACHER = "teacher@test.com";
    private static final String URL = "https://example.com/slides.pdf";

    @Mock private CourseResourceRepository courseResourceRepository;
    @Mock private CourseService courseService;
    @Mock private UserService userService;
    @Mock private CourseAccessService courseAccessService;

    @InjectMocks
    private CourseResourceServiceImpl courseResourceService;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(10L);
    }

    private void courseExists() {
        when(courseService.getCourseById(10L)).thenReturn(Optional.of(course));
    }

    private CourseResource resourceOf(Long courseId) {
        Course owner = new Course();
        owner.setCourseId(courseId);
        CourseResource resource = new CourseResource();
        resource.setResourceId(5L);
        resource.setCourse(owner);
        return resource;
    }

    // --- getResources --------------------------------------------------------

    @Test
    void getResources_shouldReturnResources_whenUserCanAccessCourseContent() {
        courseExists();
        List<CourseResource> expected = List.of(new CourseResource(), new CourseResource());
        when(courseAccessService.canAccessCourseContent(course, STUDENT)).thenReturn(true);
        when(courseResourceRepository.findByCourse_CourseIdOrderByCreatedAtAsc(10L))
                .thenReturn(expected);

        assertEquals(expected, courseResourceService.getResources(10L, STUDENT));
    }

    @Test
    void getResources_shouldBeRefused_whenUserIsNotEnrolled() {
        courseExists();
        when(courseAccessService.canAccessCourseContent(course, STUDENT)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> courseResourceService.getResources(10L, STUDENT));

        verifyNoInteractions(courseResourceRepository);
    }

    @Test
    void getResources_shouldBeRefused_forAnonymousVisitor() {
        courseExists();
        when(courseAccessService.canAccessCourseContent(course, null)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> courseResourceService.getResources(10L, null));

        verifyNoInteractions(courseResourceRepository);
    }

    @Test
    void getResources_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseService.getCourseById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> courseResourceService.getResources(99L, STUDENT));
    }

    @Test
    void hasResources_shouldAskTheRepository() {
        when(courseResourceRepository.existsByCourse_CourseId(10L)).thenReturn(true);

        assertTrue(courseResourceService.hasResources(10L));
    }

    // --- addResource ---------------------------------------------------------

    @Test
    void addResource_shouldSaveTrimmedResourceForTheCourse() {
        courseExists();
        when(courseResourceRepository.save(any(CourseResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseResource saved = courseResourceService.addResource(
                10L, TEACHER, "  Slides  ", "  " + URL + " ", "  Week 1 slides  ");

        verify(userService).ensureApprovedTeacher(TEACHER);
        assertEquals(course, saved.getCourse());
        assertEquals("Slides", saved.getTitle());
        assertEquals(URL, saved.getUrl());
        assertEquals("Week 1 slides", saved.getDescription());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void addResource_shouldStoreNullDescription_whenDescriptionIsBlank() {
        courseExists();
        when(courseResourceRepository.save(any(CourseResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNull(courseResourceService.addResource(10L, TEACHER, "Slides", URL, "   ")
                .getDescription());
        assertNull(courseResourceService.addResource(10L, TEACHER, "Slides", URL, null)
                .getDescription());
    }

    @Test
    void addResource_shouldBeRefused_whenUserIsNotAnApprovedTeacher() {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(STUDENT);

        assertThrows(AccessDeniedException.class,
                () -> courseResourceService.addResource(10L, STUDENT, "Slides", URL, null));

        verifyNoInteractions(courseResourceRepository, courseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void addResource_shouldRejectBlankTitle(String title) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, title, URL, null));

        assertEquals("Resource title cannot be empty", exception.getMessage());
        verify(courseResourceRepository, never()).save(any());
    }

    @Test
    void addResource_shouldRejectNullTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, null, URL, null));
    }

    @Test
    void addResource_shouldRejectTitleLongerThanTheMaximum() {
        String title = "a".repeat(CourseResource.MAX_TITLE_LENGTH + 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, title, URL, null));

        assertEquals("Resource title cannot be longer than 120 characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "not a link", "example.com/slides", "ftp://example.com/file",
            "javascript:alert(1)", "data:text/html,<script>alert(1)</script>",
            "https://", "http:///no-host"})
    void addResource_shouldRejectLinksThatAreNotHttpOrHttps(String url) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, "Slides", url, null));

        assertEquals("Resource link must be a web address starting with http:// or https://",
                exception.getMessage());
        verify(courseResourceRepository, never()).save(any());
    }

    @Test
    void addResource_shouldRejectNullLink() {
        assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, "Slides", null, null));
    }

    @Test
    void addResource_shouldAcceptPlainHttpLinks() {
        courseExists();
        when(courseResourceRepository.save(any(CourseResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("HTTP://example.com/a?b=c",
                courseResourceService.addResource(
                        10L, TEACHER, "Slides", "HTTP://example.com/a?b=c", null).getUrl());
    }

    @Test
    void addResource_shouldRejectLinkLongerThanTheMaximum() {
        String url = "https://example.com/" + "a".repeat(CourseResource.MAX_URL_LENGTH);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, "Slides", url, null));

        assertEquals("Resource link cannot be longer than 500 characters", exception.getMessage());
    }

    @Test
    void addResource_shouldRejectDescriptionLongerThanTheMaximum() {
        String description = "a".repeat(CourseResource.MAX_DESCRIPTION_LENGTH + 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> courseResourceService.addResource(10L, TEACHER, "Slides", URL, description));

        assertEquals("Resource description cannot be longer than 1000 characters",
                exception.getMessage());
    }

    @Test
    void addResource_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseService.getCourseById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> courseResourceService.addResource(99L, TEACHER, "Slides", URL, null));

        verify(courseResourceRepository, never()).save(any());
    }

    // --- deleteResource ------------------------------------------------------

    @Test
    void deleteResource_shouldDeleteResourceOfTheCourse() {
        CourseResource resource = resourceOf(10L);
        when(courseResourceRepository.findById(5L)).thenReturn(Optional.of(resource));

        courseResourceService.deleteResource(10L, 5L, TEACHER);

        verify(userService).ensureApprovedTeacher(TEACHER);
        verify(courseResourceRepository).delete(resource);
    }

    @Test
    void deleteResource_shouldTreatResourceOfAnotherCourseAsMissing() {
        when(courseResourceRepository.findById(5L)).thenReturn(Optional.of(resourceOf(11L)));

        assertThrows(EntityNotFoundException.class,
                () -> courseResourceService.deleteResource(10L, 5L, TEACHER));

        verify(courseResourceRepository, never()).delete(any());
    }

    @Test
    void deleteResource_shouldThrowEntityNotFoundException_whenResourceDoesNotExist() {
        when(courseResourceRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> courseResourceService.deleteResource(10L, 5L, TEACHER));
    }

    @Test
    void deleteResource_shouldBeRefused_whenUserIsNotAnApprovedTeacher() {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(STUDENT);

        assertThrows(AccessDeniedException.class,
                () -> courseResourceService.deleteResource(10L, 5L, STUDENT));

        verifyNoInteractions(courseResourceRepository);
    }

    // --- SCRUM-52: only the course's own teacher or an admin ---------------------

    @Test
    void addResource_shouldBeRefused_forATeacherOfAnotherCourse() {
        courseExists();
        doThrow(new org.springframework.security.access.AccessDeniedException("not your course")).when(courseAccessService).ensureCanManageCourse(course, TEACHER);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> courseResourceService.addResource(10L, TEACHER, "Slides", URL, null));
        verify(courseResourceRepository, never()).save(any());
    }

    @Test
    void deleteResource_shouldBeRefused_forATeacherOfAnotherCourse() {
        CourseResource resource = resourceOf(10L);
        when(courseResourceRepository.findById(5L)).thenReturn(Optional.of(resource));
        doThrow(new org.springframework.security.access.AccessDeniedException("not your course")).when(courseAccessService).ensureCanManageCourse(resource.getCourse(), TEACHER);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> courseResourceService.deleteResource(10L, 5L, TEACHER));
        verify(courseResourceRepository, never()).delete(any());
    }
}
