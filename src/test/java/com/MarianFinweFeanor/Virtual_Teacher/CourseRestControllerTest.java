package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController.CourseRestController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SCRUM-22: the course REST API must not expose drafts to users who cannot see them.
 */
@ActiveProfiles("test")
@WebMvcTest(CourseRestController.class)
class CourseRestControllerTest {

    private static final String STUDENT = "student@test.com";
    private static final String TEACHER = "teacher@test.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseAccessService courseAccessService;

    @MockBean
    private UserRepository userRepository;   // needed by GlobalModelAttributes / CustomUserDetailsService

    private static Course course(long id, String title, String status) {
        Course course = new Course();
        course.setCourseId(id);
        course.setTitle(title);
        course.setStatus(status);
        return course;
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void getAllCourses_shouldReturnOnlyVisibleCourses_forStudent() throws Exception {
        given(courseAccessService.canManageCourses(STUDENT)).willReturn(false);
        given(courseService.getVisibleCourses(STUDENT, false))
                .willReturn(List.of(course(1L, "Published", "PUBLISHED")));

        mockMvc.perform(get("/api/courses").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(courseService, never()).getAllCourses();
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void getAllCourses_shouldIncludeOwnDrafts_forCourseManager() throws Exception {
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);
        given(courseService.getVisibleCourses(TEACHER, true)).willReturn(List.of(
                course(1L, "Published", "PUBLISHED"),
                course(2L, "My draft", "DRAFT")));

        mockMvc.perform(get("/api/courses").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(courseService, never()).getAllCourses();
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void getCourseById_shouldReturn404_forDraftTheUserCannotView() throws Exception {
        Course draft = course(2L, "Draft", "DRAFT");
        given(courseService.getCourseById(2L)).willReturn(Optional.of(draft));
        given(courseAccessService.canViewCourse(draft, STUDENT)).willReturn(false);

        mockMvc.perform(get("/api/courses/{id}", 2).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void getCourseById_shouldReturnCourse_whenUserCanView() throws Exception {
        Course published = course(1L, "Published", "PUBLISHED");
        given(courseService.getCourseById(1L)).willReturn(Optional.of(published));
        given(courseAccessService.canViewCourse(published, STUDENT)).willReturn(true);

        mockMvc.perform(get("/api/courses/{id}", 1).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1))
                .andExpect(jsonPath("$.title").value("Published"));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void getCourseById_shouldReturnDraft_forCourseManager() throws Exception {
        Course draft = course(2L, "Draft", "DRAFT");
        given(courseService.getCourseById(2L)).willReturn(Optional.of(draft));
        given(courseAccessService.canViewCourse(draft, TEACHER)).willReturn(true);

        mockMvc.perform(get("/api/courses/{id}", 2).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void getCourseById_shouldReturn404_whenCourseDoesNotExist() throws Exception {
        given(courseService.getCourseById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/courses/{id}", 99).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
