package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.CourseProgressMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.StudentProgress;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureProgressService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-51 part B: the course progress page for teachers and admins. */
@ActiveProfiles("test")
@WebMvcTest(CourseProgressMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseProgressMvcControllerTest {

    private static final String TEACHER = "teacher@test.com";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");

    @Autowired private MockMvc mockMvc;

    @MockBean private CourseService courseService;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private LectureProgressService lectureProgressService;
    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;   // GlobalModelAttributes

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(1L);
        course.setTitle("Java Basics");
        given(courseService.getCourseById(1L)).willReturn(Optional.of(course));
    }

    @Test
    void showProgress_shouldListStudentsWithTheirCounts() throws Exception {
        User anna = new User();
        anna.setFirstName("Anna");
        anna.setLastName("Adams");
        anna.setEmail("anna@test.com");
        given(lectureProgressService.getCourseProgress(1L)).willReturn(List.of(
                new StudentProgress(anna, 3, 2, 4, Enrollment.CompletionStatus.IN_PROGRESS)));

        mockMvc.perform(get("/courses/1/progress").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(view().name("course-progress"))
                .andExpect(content().string(containsString("anna@test.com")))
                .andExpect(content().string(containsString("3 / 4")))
                .andExpect(content().string(containsString("2 / 4")))
                .andExpect(content().string(containsString("In progress")));

        verify(userService).ensureApprovedTeacher(TEACHER);
        verify(courseAccessService).ensureCanManageCourse(course, TEACHER);
    }

    @Test
    void showProgress_shouldSayNoStudents_whenNobodyIsEnrolled() throws Exception {
        given(lectureProgressService.getCourseProgress(1L)).willReturn(List.of());

        mockMvc.perform(get("/courses/1/progress").principal(TEACHER_PRINCIPAL))
                .andExpect(content().string(containsString("progress-no-students")));
    }

    @Test
    void showProgress_shouldAnswer404_forMissingCourse() throws Exception {
        given(courseService.getCourseById(9L)).willReturn(Optional.empty());

        mockMvc.perform(get("/courses/9/progress").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isNotFound());
    }

    @Test
    void showProgress_shouldRedirect_whenTheUserMayNotManageTheCourse() throws Exception {
        doThrow(new AccessDeniedException("no")).when(courseAccessService).ensureCanManageCourse(course, TEACHER);

        mockMvc.perform(get("/courses/1/progress").principal(TEACHER_PRINCIPAL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));

        verify(lectureProgressService, never()).getCourseProgress(any());
    }
}
