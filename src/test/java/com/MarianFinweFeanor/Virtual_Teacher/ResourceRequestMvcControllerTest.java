package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.ResourceRequestMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ResourceRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-35: a student sends a resource request from the course page. */
@ActiveProfiles("test")
@WebMvcTest(ResourceRequestMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResourceRequestMvcControllerTest {

    private static final String STUDENT = "student@test.com";
    private static final Principal STUDENT_PRINCIPAL =
            new TestingAuthenticationToken(STUDENT, "n/a", "ROLE_STUDENT");
    private static final String MESSAGE = "Could you share extra exercises on recursion? ".repeat(3);

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ResourceRequestService resourceRequestService;
    @MockBean private UserRepository userRepository;

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void createRequest_shouldSaveAndRedirectToTheCourse() throws Exception {
        mockMvc.perform(post("/courses/10/resource-requests")
                        .principal(STUDENT_PRINCIPAL)
                        .param("message", MESSAGE))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Your request was sent to the teacher."))
                .andExpect(flash().attributeCount(1));

        verify(resourceRequestService).createRequest(10L, STUDENT, MESSAGE);
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void createRequest_shouldShowErrorAndKeepTheDraft_whenMessageIsTooShort() throws Exception {
        doThrow(new IllegalArgumentException("Describe the resource you need in at least 100 characters"))
                .when(resourceRequestService).createRequest(10L, STUDENT, "Too short");

        mockMvc.perform(post("/courses/10/resource-requests")
                        .principal(STUDENT_PRINCIPAL)
                        .param("message", "Too short"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "Describe the resource you need in at least 100 characters"))
                .andExpect(flash().attribute("requestDraft", "Too short"));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void createRequest_shouldShowError_whenStudentIsNotEnrolled() throws Exception {
        doThrow(new IllegalStateException("Enroll in this course to request resources"))
                .when(resourceRequestService).createRequest(10L, STUDENT, MESSAGE);

        mockMvc.perform(post("/courses/10/resource-requests")
                        .principal(STUDENT_PRINCIPAL)
                        .param("message", MESSAGE))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "Enroll in this course to request resources"));
    }
}
