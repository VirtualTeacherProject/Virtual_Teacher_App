package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.CourseResourceMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseResourceService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-34: adding and removing additional course resources. */
@ActiveProfiles("test")
@WebMvcTest(CourseResourceMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseResourceMvcControllerTest {

    private static final String TEACHER = "teacher@test.com";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CourseResourceService courseResourceService;
    @MockBean private UserRepository userRepository;

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void addResource_shouldSaveAndRedirectToTheCourse() throws Exception {
        mockMvc.perform(post("/courses/10/resources")
                        .principal(TEACHER_PRINCIPAL)
                        .param("title", "Slides")
                        .param("url", "https://example.com/slides")
                        .param("description", "Week 1"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Resource added."));

        verify(courseResourceService)
                .addResource(10L, TEACHER, "Slides", "https://example.com/slides", "Week 1");
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void addResource_shouldShowValidationError_onTheCoursePage() throws Exception {
        doThrow(new IllegalArgumentException(
                "Resource link must be a web address starting with http:// or https://"))
                .when(courseResourceService)
                .addResource(10L, TEACHER, "Slides", "javascript:alert(1)", null);

        mockMvc.perform(post("/courses/10/resources")
                        .principal(TEACHER_PRINCIPAL)
                        .param("title", "Slides")
                        .param("url", "javascript:alert(1)"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error",
                        "Resource link must be a web address starting with http:// or https://"));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void addResource_shouldRedirectToCourses_whenTeacherIsNotApproved() throws Exception {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(courseResourceService)
                .addResource(10L, TEACHER, "Slides", "https://example.com", null);

        mockMvc.perform(post("/courses/10/resources")
                        .principal(TEACHER_PRINCIPAL)
                        .param("title", "Slides")
                        .param("url", "https://example.com"))
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("error", "You do not have access"));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void deleteResource_shouldDeleteAndRedirectToTheCourse() throws Exception {
        mockMvc.perform(post("/courses/10/resources/5/delete").principal(TEACHER_PRINCIPAL))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Resource removed."));

        verify(courseResourceService).deleteResource(10L, 5L, TEACHER);
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void deleteResource_shouldReturn404_whenResourceIsNotInThisCourse() throws Exception {
        doThrow(new EntityNotFoundException("Resource", 5L))
                .when(courseResourceService).deleteResource(10L, 5L, TEACHER);

        mockMvc.perform(post("/courses/10/resources/5/delete").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/404"));
    }
}
