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

/**
 * SCRUM-35: a student sends a resource request from the course page.
 * SCRUM-36: teachers set its status and assign it; the assignee answers it.
 */
@ActiveProfiles("test")
@WebMvcTest(ResourceRequestMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResourceRequestMvcControllerTest {

    private static final String STUDENT = "student@test.com";
    private static final Principal STUDENT_PRINCIPAL =
            new TestingAuthenticationToken(STUDENT, "n/a", "ROLE_STUDENT");
    private static final String TEACHER = "teacher@test.com";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");
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

    // --- SCRUM-36 ------------------------------------------------------------

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void updateStatus_shouldSaveAndRedirectToTheCourse() throws Exception {
        mockMvc.perform(post("/courses/10/resource-requests/3/status")
                        .principal(TEACHER_PRINCIPAL)
                        .param("status", "RESOLVED"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Request status updated."));

        verify(resourceRequestService).updateStatus(10L, 3L, "RESOLVED", TEACHER);
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void updateStatus_shouldShowError_forUnknownStatus() throws Exception {
        doThrow(new IllegalArgumentException("Unknown request status"))
                .when(resourceRequestService).updateStatus(10L, 3L, "DONE", TEACHER);

        mockMvc.perform(post("/courses/10/resource-requests/3/status")
                        .principal(TEACHER_PRINCIPAL)
                        .param("status", "DONE"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "Unknown request status"));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void assign_shouldAssignTheStudent() throws Exception {
        mockMvc.perform(post("/courses/10/resource-requests/3/assign")
                        .principal(TEACHER_PRINCIPAL)
                        .param("assigneeId", "2"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Request assigned."));

        verify(resourceRequestService).assign(10L, 3L, 2L, TEACHER);
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void assign_withEmptyStudent_shouldRemoveTheAssignment() throws Exception {
        mockMvc.perform(post("/courses/10/resource-requests/3/assign")
                        .principal(TEACHER_PRINCIPAL)
                        .param("assigneeId", ""))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Assignment removed."));

        verify(resourceRequestService).assign(10L, 3L, null, TEACHER);
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void assign_shouldShowError_whenStudentCannotBeAssigned() throws Exception {
        doThrow(new IllegalArgumentException("The student must be enrolled in this course"))
                .when(resourceRequestService).assign(10L, 3L, 2L, TEACHER);

        mockMvc.perform(post("/courses/10/resource-requests/3/assign")
                        .principal(TEACHER_PRINCIPAL)
                        .param("assigneeId", "2"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "The student must be enrolled in this course"));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void respond_shouldSaveTheAnswer() throws Exception {
        mockMvc.perform(post("/courses/10/resource-requests/3/response")
                        .principal(STUDENT_PRINCIPAL)
                        .param("response", "See https://cppreference.com"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Your answer was saved."));

        verify(resourceRequestService).respond(10L, 3L, STUDENT, "See https://cppreference.com");
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void respond_shouldShowErrorAndKeepTheDraft_whenRequestIsClosed() throws Exception {
        doThrow(new IllegalStateException("This request is closed"))
                .when(resourceRequestService).respond(10L, 3L, STUDENT, "Late answer");

        mockMvc.perform(post("/courses/10/resource-requests/3/response")
                        .principal(STUDENT_PRINCIPAL)
                        .param("response", "Late answer"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "This request is closed"))
                .andExpect(flash().attribute("responseDraftId", 3L))
                .andExpect(flash().attribute("responseDraft", "Late answer"));
    }
}
