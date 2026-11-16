package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.ProfileMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The profile page used to offer "INACTIVE", a value the User entity rejects
 * (it only allows ACTIVE or PASSIVE).
 */
@ActiveProfiles("test")
@WebMvcTest(ProfileMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileMvcControllerTest {

    private static final String EMAIL = "anna@test.com";
    private static final Principal PRINCIPAL =
            new TestingAuthenticationToken(EMAIL, "n/a", "ROLE_STUDENT");

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setEmail(EMAIL);
        user.setFirstName("Anna");
        user.setLastName("Petrova");
        user.setRole(UserRole.STUDENT);
        user.setStatus("ACTIVE");

        given(userService.findByEmail(EMAIL)).willReturn(user);
    }

    @Test
    void profileForm_shouldOfferTheStatusValuesTheEntityAccepts() throws Exception {
        mockMvc.perform(get("/profile").principal(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"ACTIVE\"")))
                .andExpect(content().string(containsString("value=\"PASSIVE\"")))
                .andExpect(content().string(not(containsString("INACTIVE"))));
    }

    @Test
    void updateProfile_shouldSavePassiveStatus() throws Exception {
        mockMvc.perform(post("/profile").principal(PRINCIPAL)
                        .param("firstName", "Anna")
                        .param("lastName", "Petrova")
                        .param("status", "PASSIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        assertEquals("PASSIVE", user.getStatus());
        verify(userService).updateUser(user);
    }

    @Test
    void updateProfile_shouldKeepThePreviousStatus_whenAnUnknownValueIsPosted() throws Exception {
        mockMvc.perform(post("/profile").principal(PRINCIPAL)
                        .param("firstName", "Anna")
                        .param("lastName", "Petrova")
                        .param("status", "INACTIVE"))
                .andExpect(status().is3xxRedirection());

        assertEquals("ACTIVE", user.getStatus());
        verify(userService).updateUser(user);
    }
}
