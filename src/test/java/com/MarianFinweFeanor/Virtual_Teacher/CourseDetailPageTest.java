package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.EnrollmentMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renders the public course page (reviews section, SCRUM-31). */
@ActiveProfiles("test")
@WebMvcTest(EnrollmentMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseDetailPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private CourseService courseService;
    @MockBean private EnrollmentRepository enrollmentRepository;
    @MockBean private EnrollmentService enrollmentService;
    @MockBean private UserRepository userRepository;

    private Course course;

    @BeforeEach
    void setUp() {
        User teacher = new User();
        teacher.setFirstName("Tina");
        teacher.setLastName("Teacher");
        teacher.setEmail("tina@test.com");
        teacher.setRole(UserRole.TEACHER);

        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Basics");
        course.setTopic("Programming");
        course.setDescription("Learn Java");
        course.setStatus("PUBLISHED");
        course.setTeacher(teacher);

        given(courseService.getCourseById(10L)).willReturn(Optional.of(course));
    }

    private Enrollment review(String first, String last, int rating, String text) {
        User student = new User();
        student.setFirstName(first);
        student.setLastName(last);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setRating(rating);
        enrollment.setReviewText(text);
        enrollment.setRatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        return enrollment;
    }

    @Test
    void courseDetail_shouldShowReviewsAndAverage_toAnonymousVisitor() throws Exception {
        given(enrollmentService.getCourseReviews(10L)).willReturn(List.of(
                review("Anna", "Petrova", 5, "Clear explanations and useful assignments."),
                review("Boris", "Annenkov", 4, "Good pace, would recommend.")));
        given(enrollmentService.getAverageRating(10L)).willReturn(4.5);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("course-detail"))
                .andExpect(content().string(containsString("4.5 / 5 (2)")))
                .andExpect(content().string(containsString("Anna Petrova")))
                .andExpect(content().string(containsString("Clear explanations and useful assignments.")))
                .andExpect(content().string(containsString("2026-11-01")));
    }

    @Test
    void courseDetail_shouldSayNoReviewsYet_whenCourseHasNoRatings() throws Exception {
        given(enrollmentService.getCourseReviews(10L)).willReturn(List.of());
        given(enrollmentService.getAverageRating(10L)).willReturn(null);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No reviews yet.")));
    }
}
