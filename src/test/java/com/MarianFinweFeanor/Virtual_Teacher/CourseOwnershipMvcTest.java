package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.EnrollmentMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseResourceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ResourceRequestService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-52: a teacher manages only their own courses; admins manage every course. */
@ActiveProfiles("test")
@WebMvcTest(EnrollmentMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseOwnershipMvcTest {

    private static final String OWNER = "owner@test.com";
    private static final String OTHER = "other@test.com";

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private CourseService courseService;
    @MockBean private EnrollmentRepository enrollmentRepository;
    @MockBean private EnrollmentService enrollmentService;
    @MockBean private UserRepository userRepository;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private CourseResourceService courseResourceService;
    @MockBean private ResourceRequestService resourceRequestService;

    private Course course;

    @BeforeEach
    void setUp() {
        User owner = teacher(OWNER);
        User other = teacher(OTHER);

        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Basics");
        course.setTopic("Programming");
        course.setStatus("DRAFT");
        course.setTeacher(owner);

        given(courseService.getCourseById(10L)).willReturn(Optional.of(course));
        given(userService.findByEmail(OWNER)).willReturn(owner);
        given(userService.findByEmail(OTHER)).willReturn(other);
        given(userRepository.findByEmail(OWNER)).willReturn(Optional.of(owner));
        given(userRepository.findByEmail(OTHER)).willReturn(Optional.of(other));
        // both are approved teachers who can view the draft (viewing stays open)
        given(courseAccessService.canManageCourses(any())).willReturn(true);
        given(courseAccessService.canViewCourse(any(), any())).willReturn(true);
        given(courseAccessService.canManageCourse(course, OWNER)).willReturn(true);
        doThrow(new AccessDeniedException("Only the course's teacher or an admin can do this."))
                .when(courseAccessService).ensureCanManageCourse(course, OTHER);
    }

    private static User teacher(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("T");
        user.setLastName("Teacher");
        user.setRole(UserRole.TEACHER);
        user.setTeacherApproved(true);
        return user;
    }

    /** The course page checks the role in the security context to show drafts. */
    private static void givenApprovedTeacherSession(String email) {
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(as(email));
    }

    @org.junit.jupiter.api.AfterEach
    void clearSession() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private static TestingAuthenticationToken as(String email) {
        return new TestingAuthenticationToken(email, null, "ROLE_TEACHER");
    }

    @Test
    void editForm_shouldOpen_forTheCoursesTeacher() throws Exception {
        mockMvc.perform(get("/courses/10/edit").principal(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-course"));
    }

    @Test
    void editForm_shouldBeRefused_forAnotherTeacher() throws Exception {
        mockMvc.perform(get("/courses/10/edit").principal(as(OTHER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("error", "You do not have access"));
    }

    @Test
    void editCourse_shouldBeRefused_forAnotherTeacher_withoutSaving() throws Exception {
        mockMvc.perform(post("/courses/10/edit").principal(as(OTHER))
                        .param("title", "Taken over").param("topic", "Programming"))
                .andExpect(redirectedUrl("/courses"));

        verify(courseService, never()).updateCourse(anyLong(), any());
    }

    @Test
    void publish_shouldBeRefused_forAnotherTeacher() throws Exception {
        mockMvc.perform(post("/courses/10/publish").principal(as(OTHER)))
                .andExpect(redirectedUrl("/courses"));

        verify(courseService, never()).publishCourse(anyLong());
    }

    @Test
    void publish_shouldWork_forTheCoursesTeacher() throws Exception {
        mockMvc.perform(post("/courses/10/publish").principal(as(OWNER)))
                .andExpect(redirectedUrl("/courses/10"));

        verify(courseService).publishCourse(10L);
    }

    @Test
    void studentList_shouldBeRefused_forAnotherTeacher() throws Exception {
        mockMvc.perform(get("/courses/10/students").principal(as(OTHER)))
                .andExpect(redirectedUrl("/courses"));

        verify(enrollmentService, never()).getStudentsInCourse(anyLong());
    }

    @Test
    void coursePage_shouldShowManagementButtons_toTheCoursesTeacher() throws Exception {
        givenApprovedTeacherSession(OWNER);
        mockMvc.perform(get("/courses/10").principal(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canManageCourse", true))
                .andExpect(content().string(containsString("/courses/10/edit")))
                .andExpect(content().string(containsString("/courses/10/publish")))
                .andExpect(content().string(containsString("/courses/10/students")))
                .andExpect(content().string(containsString("course-progress-link")));
    }

    @Test
    void coursePage_shouldHideManagementButtons_fromAnotherTeacher() throws Exception {
        givenApprovedTeacherSession(OTHER);
        mockMvc.perform(get("/courses/10").principal(as(OTHER)))
                .andExpect(status().isOk())   // viewing stays open (SCRUM-52 decision)
                .andExpect(model().attribute("canManageCourse", false))
                .andExpect(content().string(not(containsString("/courses/10/edit"))))
                .andExpect(content().string(not(containsString("/courses/10/publish"))))
                .andExpect(content().string(not(containsString("/courses/10/students"))))
                .andExpect(content().string(not(containsString("course-progress-link"))));
    }
}
