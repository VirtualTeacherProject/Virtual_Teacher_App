package com.MarianFinweFeanor.Virtual_Teacher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * URL rules added with SCRUM-10. Every request here is rejected by the security
 * filter chain before it reaches a controller, so no data is read or changed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submissionDownload_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        mockMvc.perform(get("/courses/1/lectures/1/assignments/1/download"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void submissionList_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        mockMvc.perform(get("/courses/1/lectures/1/assignments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void lectureApi_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(get("/api/lectures")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/lectures/1")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/lectures/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void courseApiWrites_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/courses/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/courses/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void userApiDelete_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void userApiDelete_shouldBeForbidden_forTeacher() throws Exception {
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
    }
}
