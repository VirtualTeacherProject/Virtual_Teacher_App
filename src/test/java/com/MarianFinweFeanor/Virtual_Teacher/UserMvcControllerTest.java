package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.UserMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.UserServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(UserMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void showRegisterForm_shouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @WithMockUser
    void registerUser_shouldRedirectToLogin_whenValidInput() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Umut")
                        .param("lastName", "Yildirim")
                        .param("email", "umut@test.com")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("msg", "Account created! Please log in."));
    }

    @Test
    @WithMockUser
    void registerUser_shouldReturnRegisterView_whenDuplicateEmail() throws Exception {
        doThrow(new EntityDuplicateException("duplicate"))
                .when(userService).saveUser(any());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "john@test.com")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));
    }

    @Test
    @WithMockUser
    void registerUser_shouldReturnRegisterView_whenDatabaseErrorOccurs() throws Exception {
        doThrow(new DataIntegrityViolationException("db error"))
                .when(userService).saveUser(any());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "john@test.com")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("user"));
    }

}