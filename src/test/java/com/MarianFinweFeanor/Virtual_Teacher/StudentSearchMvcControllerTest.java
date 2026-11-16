package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.StudentSearchMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-6: approved teachers and admins can search students. */
@ActiveProfiles("test")
@WebMvcTest(StudentSearchMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentSearchMvcControllerTest {

    private static final String TEACHER = "teacher@test.com";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;

    private static User student(String first, String last, String email) {
        User user = new User();
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setRole(UserRole.STUDENT);
        return user;
    }

    @Test
    void searchStudents_shouldListMatchingStudentsOnly() throws Exception {
        given(userService.searchUsers("anna", UserRole.STUDENT))
                .willReturn(List.of(student("Anna", "Petrova", "anna@test.com")));

        mockMvc.perform(get("/students").param("q", "anna").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("q", "anna"))
                .andExpect(content().string(containsString("Anna Petrova")))
                .andExpect(content().string(containsString("anna@test.com")));

        verify(userService).ensureApprovedTeacher(TEACHER);
        verify(userService).searchUsers("anna", UserRole.STUDENT);
    }

    @Test
    void searchStudents_shouldListAllStudents_whenQueryIsMissing() throws Exception {
        given(userService.searchUsers(null, UserRole.STUDENT)).willReturn(List.of());

        mockMvc.perform(get("/students").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", ""))
                .andExpect(content().string(containsString("No students found.")));
    }

    @Test
    void searchStudents_shouldBeRefused_forTeacherWaitingForApproval() throws Exception {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(TEACHER);

        mockMvc.perform(get("/students").param("q", "anna").principal(TEACHER_PRINCIPAL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));

        verify(userService, never()).searchUsers(any(), any());
    }
}
