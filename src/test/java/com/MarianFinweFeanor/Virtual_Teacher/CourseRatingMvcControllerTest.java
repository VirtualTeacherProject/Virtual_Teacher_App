package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.CourseRatingMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-31: rating form handling. */
@ActiveProfiles("test")
@WebMvcTest(CourseRatingMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseRatingMvcControllerTest {

    private static final String STUDENT = "student@test.com";
    private static final Principal STUDENT_PRINCIPAL =
            new TestingAuthenticationToken(STUDENT, "n/a", "ROLE_STUDENT");
    private static final String REVIEW = "x".repeat(80);

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EnrollmentService enrollmentService;
    @MockBean private UserRepository userRepository;

    @Test
    void rate_shouldSaveAndRedirectWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/courses/10/rating")
                        .principal(STUDENT_PRINCIPAL)
                        .param("rating", "4")
                        .param("reviewText", REVIEW))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Thanks! Your rating was saved."));

        verify(enrollmentService).rateCourse(STUDENT, 10L, 4, REVIEW);
    }

    @Test
    void rate_shouldShowErrorAndKeepDraft_whenReviewIsTooShort() throws Exception {
        doThrow(new IllegalArgumentException("Please write a review of at least 75 characters (you wrote 5)."))
                .when(enrollmentService).rateCourse(STUDENT, 10L, 4, "short");

        mockMvc.perform(post("/courses/10/rating")
                        .principal(STUDENT_PRINCIPAL)
                        .param("rating", "4")
                        .param("reviewText", "short"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error",
                        "Please write a review of at least 75 characters (you wrote 5)."))
                .andExpect(flash().attribute("reviewDraft", "short"))
                .andExpect(flash().attribute("ratingDraft", 4));
    }

    @Test
    void rate_shouldShowError_whenStudentMayNotRateYet() throws Exception {
        doThrow(new IllegalStateException("You can rate only after completing all lectures."))
                .when(enrollmentService).rateCourse(STUDENT, 10L, 5, REVIEW);

        mockMvc.perform(post("/courses/10/rating")
                        .principal(STUDENT_PRINCIPAL)
                        .param("rating", "5")
                        .param("reviewText", REVIEW))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "You can rate only after completing all lectures."));
    }

    @Test
    void rate_shouldPassMissingReviewToTheService_soItIsRejectedThere() throws Exception {
        mockMvc.perform(post("/courses/10/rating")
                        .principal(STUDENT_PRINCIPAL)
                        .param("rating", "5"))
                .andExpect(status().is3xxRedirection());

        verify(enrollmentService).rateCourse(STUDENT, 10L, 5, null);
    }

    @Test
    void rate_shouldRedirectToLogin_forAnonymousVisitor() throws Exception {
        mockMvc.perform(post("/courses/10/rating")
                        .param("rating", "5")
                        .param("reviewText", REVIEW))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(enrollmentService);
    }
}
