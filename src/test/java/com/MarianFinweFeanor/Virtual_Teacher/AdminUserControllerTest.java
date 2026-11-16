package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.AdminUserController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityInUseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-19: search box on the admin user list. */
@ActiveProfiles("test")
@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;

    private static User user(String first, String last, String email) {
        User user = new User();
        user.setUserId(1L);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setRole(UserRole.STUDENT);
        user.setStatus("ACTIVE");
        return user;
    }

    @Test
    void listUsers_shouldShowEveryone_whenNoQueryIsGiven() throws Exception {
        given(userService.searchUsers(isNull(), isNull()))
                .willReturn(List.of(user("Anna", "Petrova", "anna@test.com")));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("q", ""))
                .andExpect(content().string(containsString("anna@test.com")));
    }

    @Test
    void listUsers_shouldPassTheQueryToTheSearch_andKeepItInTheForm() throws Exception {
        given(userService.searchUsers(" petro ", null))
                .willReturn(List.of(user("Anna", "Petrova", "anna@test.com")));

        mockMvc.perform(get("/admin/users").param("q", " petro "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", "petro"))
                .andExpect(content().string(containsString("value=\"petro\"")))
                .andExpect(content().string(containsString("anna@test.com")));

        verify(userService).searchUsers(" petro ", null);
    }

    // --- SCRUM-15: hard delete --------------------------------------------

    private void givenAdminDeletes(User target) {
        User admin = user("Ada", "Admin", "admin@test.com");
        admin.setUserId(99L);
        admin.setRole(UserRole.ADMIN);

        given(userService.findByEmail("admin@test.com")).willReturn(admin);
        given(userService.getUserById(target.getUserId())).willReturn(Optional.of(target));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void deleteUser_shouldShowTheReason_whenUserStillTeachesCourses() throws Exception {
        User teacher = user("Tina", "Teacher", "tina@test.com");
        teacher.setRole(UserRole.TEACHER);
        givenAdminDeletes(teacher);

        doThrow(new EntityInUseException("This user still teaches 1 course (Java). Reassign them to another teacher first, or deactivate the user instead."))
                .when(userService).deleteUserById(1L);

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("error", containsString("Reassign them to another teacher first")))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void deleteUser_shouldDelete_whenNothingBlocksIt() throws Exception {
        givenAdminDeletes(user("Anna", "Petrova", "anna@test.com"));

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("msg", "User deleted."));

        verify(userService).deleteUserById(1L);
    }

    @Test
    void listUsers_shouldSaySo_whenNothingMatches() throws Exception {
        given(userService.searchUsers("zzz", null)).willReturn(List.of());

        mockMvc.perform(get("/admin/users").param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No users found.")));
    }
}
