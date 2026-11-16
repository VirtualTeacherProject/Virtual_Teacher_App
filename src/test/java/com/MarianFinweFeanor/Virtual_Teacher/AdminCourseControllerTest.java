package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.AdminCourseController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-12: admins can hand a course to another teacher. */
@ActiveProfiles("test")
@WebMvcTest(AdminCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CourseService courseService;
    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;

    private User tina;
    private User omar;
    private Course course;

    @BeforeEach
    void setUp() {
        tina = teacher(40L, "Tina", "Turner", "tina@test.com", "ACTIVE");
        omar = teacher(50L, "Omar", "Olsen", "omar@test.com", "ACTIVE");

        course = new Course();
        course.setCourseId(1L);
        course.setTitle("Java Basics");
        course.setTopic("Programming");
        course.setDescription("Learn Java");
        course.setStatus("DRAFT");
        course.setTeacher(tina);

        given(courseService.getCourseById(1L)).willReturn(Optional.of(course));
        given(userService.findAssignableTeachers()).willReturn(List.of(omar, tina));
    }

    private static User teacher(Long id, String first, String last, String email, String status) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setRole(UserRole.TEACHER);
        user.setTeacherApproved(true);
        user.setStatus(status);
        return user;
    }

    private MockHttpServletRequestBuilder editForm(String teacherId) {
        return post("/admin/courses/1/edit")
                .param("title", "Java Basics")
                .param("topic", "Programming")
                .param("description", "Learn Java")
                .param("status", "DRAFT")
                .param("passingGrade", "50")
                .param("teacherId", teacherId);
    }

    @Test
    void editForm_shouldListAssignableTeachers_withTheCurrentOneSelected() throws Exception {
        mockMvc.perform(get("/admin/courses/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/course-edit"))
                .andExpect(model().attribute("selectedTeacherId", 40L))
                .andExpect(content().string(containsString("name=\"teacherId\"")))
                .andExpect(content().string(containsString("Omar Olsen (omar@test.com)")))
                // Tina's option (id 40) is the selected one, Omar's (id 50) is not
                .andExpect(content().string(matchesPattern(
                        "(?s).*<option[^>]*value=\"40\"[^>]*selected[^>]*>.*")))
                .andExpect(content().string(not(matchesPattern(
                        "(?s).*<option[^>]*value=\"50\"[^>]*selected[^>]*>.*"))));
    }

    @Test
    void editForm_shouldStillShowTheCurrentTeacher_whenTheyAreDeactivated() throws Exception {
        tina.setStatus("PASSIVE");
        given(userService.findAssignableTeachers()).willReturn(List.of(omar));

        mockMvc.perform(get("/admin/courses/1/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tina Turner (tina@test.com) — TEACHER, deactivated")))
                .andExpect(content().string(containsString("Omar Olsen (omar@test.com)")));
    }

    @Test
    void submitEdit_shouldAssignTheNewTeacher_whenADifferentOneIsChosen() throws Exception {
        mockMvc.perform(editForm("50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses"))
                .andExpect(flash().attribute("msg", "Course updated."));

        verify(courseService).assignTeacher(1L, 50L);
        verify(courseService).updateCourse(eq(1L), any(Course.class));
    }

    @Test
    void submitEdit_shouldNotReassign_whenTheTeacherIsUnchanged() throws Exception {
        mockMvc.perform(editForm("40"))
                .andExpect(status().is3xxRedirection());

        verify(courseService, never()).assignTeacher(anyLong(), anyLong());
        verify(courseService).updateCourse(eq(1L), any(Course.class));
    }

    @Test
    void submitEdit_shouldShowTheReason_andSaveNothing_whenTheChosenUserMayNotTeach() throws Exception {
        given(courseService.assignTeacher(1L, 60L))
                .willThrow(new IllegalArgumentException(
                        "Only an admin or an approved teacher can be assigned to a course."));

        mockMvc.perform(editForm("60"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/course-edit"))
                .andExpect(content().string(containsString(
                        "Only an admin or an approved teacher can be assigned to a course.")))
                // the form still posts back to this course
                .andExpect(content().string(containsString("/admin/courses/1/edit")));

        verify(courseService, never()).updateCourse(anyLong(), any(Course.class));
    }
}
