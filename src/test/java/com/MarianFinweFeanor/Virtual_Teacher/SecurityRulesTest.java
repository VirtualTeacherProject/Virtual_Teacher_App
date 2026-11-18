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
    void savingLectureNotes_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        mockMvc.perform(post("/courses/1/lectures/1/notes").param("content", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void managingCourseResources_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(post("/courses/1/resources")
                        .param("title", "x").param("url", "https://example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/courses/1/resources/1/delete")).andExpect(status().isForbidden());
    }

    @Test
    void managingCourseResources_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        mockMvc.perform(post("/courses/1/resources")
                        .param("title", "x").param("url", "https://example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void resourceRequest_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        mockMvc.perform(post("/courses/1/resource-requests").param("message", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void resourceRequest_shouldBeForbidden_forTeacher() throws Exception {
        mockMvc.perform(post("/courses/1/resource-requests").param("message", "x"))
                .andExpect(status().isForbidden());
    }

    // SCRUM-36
    @Test
    @WithMockUser(roles = "STUDENT")
    void managingResourceRequests_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(post("/courses/1/resource-requests/1/status").param("status", "RESOLVED"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/courses/1/resource-requests/1/assign").param("assigneeId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void answeringResourceRequest_shouldBeForbidden_forTeacher() throws Exception {
        mockMvc.perform(post("/courses/1/resource-requests/1/response").param("response", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resourceRequestActions_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        for (String action : new String[]{"status", "assign", "response"}) {
            mockMvc.perform(post("/courses/1/resource-requests/1/" + action))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
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
    @WithMockUser(roles = "STUDENT")
    void studentSearch_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(get("/students").param("q", "a")).andExpect(status().isForbidden());
    }

    @Test
    void studentSearch_shouldRedirectAnonymousVisitorToLogin() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void userApiSearch_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(get("/api/users").param("email", "a")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void userApiDelete_shouldBeForbidden_forTeacher() throws Exception {
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
    }
}
