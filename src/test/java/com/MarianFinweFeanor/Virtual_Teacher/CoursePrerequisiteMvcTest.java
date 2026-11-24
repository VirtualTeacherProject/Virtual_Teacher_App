package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.EnrollmentMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AttendanceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CoursePrerequisiteService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-48: managing prerequisites, showing them and the blocked enrolment. */
@ActiveProfiles("test")
@WebMvcTest(EnrollmentMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CoursePrerequisiteMvcTest {

    private static final String TEACHER = "tina@test.com";
    private static final String STUDENT = "student@test.com";

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private CourseService courseService;
    @MockBean private EnrollmentRepository enrollmentRepository;
    @MockBean private EnrollmentService enrollmentService;
    @MockBean private UserRepository userRepository;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private CourseResourceService courseResourceService;
    @MockBean private ResourceRequestService resourceRequestService;
    @MockBean private AttendanceService attendanceService;
    @MockBean private CoursePrerequisiteService prerequisiteService;

    private Course course;
    private Course basics;
    private Course math;

    @BeforeEach
    void setUp() {
        course = course(10L, "Advanced Java", "Java");
        basics = course(11L, "Java basics", "Java");
        math = course(12L, "Discrete math", "Math");
        given(courseService.getCourseById(10L)).willReturn(Optional.of(course));
        given(courseAccessService.canViewCourse(any(), any())).willReturn(true);
    }

    private static Course course(Long id, String title, String topic) {
        Course course = new Course();
        course.setCourseId(id);
        course.setTitle(title);
        course.setTopic(topic);
        course.setStatus("PUBLISHED");
        return course;
    }

    private CoursePrerequisite link(Course prerequisite, boolean required) {
        CoursePrerequisite link = new CoursePrerequisite();
        link.setCourse(course);
        link.setPrerequisiteCourse(prerequisite);
        link.setRequired(required);
        return link;
    }

    private Principal loggedIn(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(role);
        user.setTeacherApproved(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userService.findByEmail(email)).willReturn(user);
        given(userService.getEnrolledCourses(email)).willReturn(Set.of());
        return new TestingAuthenticationToken(email, "n/a", "ROLE_" + role.name());
    }

    // --- managing ----------------------------------------------------------------

    @Test
    void addPrerequisite_shouldAddIt_andSaySo() throws Exception {
        given(prerequisiteService.addPrerequisite(10L, 11L, true, TEACHER)).willReturn(link(basics, true));

        mockMvc.perform(post("/courses/10/prerequisites").principal(loggedIn(TEACHER, UserRole.TEACHER))
                        .param("prerequisiteCourseId", "11").param("required", "true"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "\"Java basics\" added as a required prerequisite."));

        verify(userService).ensureApprovedTeacher(TEACHER);
    }

    @Test
    void addPrerequisite_shouldBeRecommended_whenRequiredIsNotTicked() throws Exception {
        given(prerequisiteService.addPrerequisite(10L, 12L, false, TEACHER)).willReturn(link(math, false));

        mockMvc.perform(post("/courses/10/prerequisites").principal(loggedIn(TEACHER, UserRole.TEACHER))
                        .param("prerequisiteCourseId", "12"))
                .andExpect(flash().attribute("msg", "\"Discrete math\" added as a recommended prerequisite."));
    }

    @Test
    void addPrerequisite_shouldShowTheReason_whenRefused() throws Exception {
        given(prerequisiteService.addPrerequisite(10L, 12L, true, TEACHER))
                .willThrow(new IllegalArgumentException("A required prerequisite must have the same topic."));

        mockMvc.perform(post("/courses/10/prerequisites").principal(loggedIn(TEACHER, UserRole.TEACHER))
                        .param("prerequisiteCourseId", "12").param("required", "true"))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "A required prerequisite must have the same topic."));
    }

    @Test
    void addPrerequisite_shouldBeRefused_forTeacherAwaitingApproval() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(TEACHER);

        mockMvc.perform(post("/courses/10/prerequisites").principal(principal)
                        .param("prerequisiteCourseId", "11"))
                .andExpect(redirectedUrl("/courses"));

        verify(prerequisiteService, never()).addPrerequisite(anyLong(), anyLong(), anyBoolean(), any());
    }

    @Test
    void removePrerequisite_shouldRemoveIt() throws Exception {
        mockMvc.perform(post("/courses/10/prerequisites/11/delete").principal(loggedIn(TEACHER, UserRole.TEACHER)))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("msg", "Prerequisite removed."));

        verify(prerequisiteService).removePrerequisite(10L, 11L, TEACHER);
    }

    // --- enrolling ---------------------------------------------------------------

    @Test
    void enroll_shouldShowWhatIsMissing_whenRequiredCoursesAreNotCompleted() throws Exception {
        doThrow(new IllegalStateException("Complete these courses before enrolling: Java basics."))
                .when(enrollmentService).enroll(STUDENT, 10L);

        mockMvc.perform(post("/courses/10/enroll").principal(loggedIn(STUDENT, UserRole.STUDENT)))
                .andExpect(redirectedUrl("/courses/10"))
                .andExpect(flash().attribute("error", "Complete these courses before enrolling: Java basics."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    void enroll_shouldWork_whenPrerequisitesAreMet() throws Exception {
        mockMvc.perform(post("/courses/10/enroll").principal(loggedIn(STUDENT, UserRole.STUDENT)))
                .andExpect(flash().attribute("msg", "Enrolled successfully!"));

        verify(enrollmentService).enroll(STUDENT, 10L);
    }

    // --- the course page -----------------------------------------------------------

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldShowPrerequisites_andBlockEnrolling_whenRequiredAreMissing() throws Exception {
        Principal principal = loggedIn(STUDENT, UserRole.STUDENT);
        given(prerequisiteService.getPrerequisites(10L)).willReturn(List.of(link(basics, true), link(math, false)));
        given(prerequisiteService.getMissingRequired(10L, STUDENT)).willReturn(List.of(basics));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"course-prerequisites\"")))
                .andExpect(content().string(containsString("Java basics")))
                .andExpect(content().string(containsString("Discrete math")))
                .andExpect(content().string(containsString(">Recommended<")))
                .andExpect(content().string(containsString("Not completed")))
                .andExpect(content().string(containsString("id=\"enroll-blocked\"")))
                .andExpect(content().string(not(containsString("/courses/10/enroll"))))
                .andExpect(content().string(not(containsString("add-prerequisite-form"))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldLetTheStudentEnroll_whenRequiredAreCompleted() throws Exception {
        Principal principal = loggedIn(STUDENT, UserRole.STUDENT);
        given(prerequisiteService.getPrerequisites(10L)).willReturn(List.of(link(basics, true)));
        given(prerequisiteService.getMissingRequired(10L, STUDENT)).willReturn(List.of());

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("✓ Completed")))
                .andExpect(content().string(containsString("/courses/10/enroll")))
                .andExpect(content().string(not(containsString("enroll-blocked"))));
    }

    @Test
    void courseDetail_shouldHidePrerequisites_whenThereAreNone() throws Exception {
        given(prerequisiteService.getPrerequisites(10L)).willReturn(List.of());

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("course-prerequisites"))));

        verify(prerequisiteService, never()).getMissingRequired(anyLong(), any());
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldOfferTheManagerTheAddFormAndRemoveButtons() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canManageCourse(course, TEACHER)).willReturn(true);
        given(prerequisiteService.getPrerequisites(10L)).willReturn(List.of(link(basics, true)));
        given(prerequisiteService.getCandidates(10L)).willReturn(List.of(math));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"add-prerequisite-form\"")))
                .andExpect(content().string(containsString("Discrete math (Math)")))
                .andExpect(content().string(containsString("/courses/10/prerequisites/11/delete")))
                .andExpect(content().string(not(containsString("Not completed"))));
    }
}
