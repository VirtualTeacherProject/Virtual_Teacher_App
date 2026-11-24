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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-45: teachers mark a course as requiring attendance; the course pages show it. */
@ActiveProfiles("test")
@WebMvcTest(EnrollmentMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseAttendanceRequiredTest {

    private static final String TEACHER = "tina@test.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private CourseService courseService;
    @MockBean private EnrollmentRepository enrollmentRepository;
    @MockBean private EnrollmentService enrollmentService;
    @MockBean private UserRepository userRepository;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private CourseResourceService courseResourceService;
    @MockBean private ResourceRequestService resourceRequestService;
    @MockBean private com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AttendanceService attendanceService;   // SCRUM-29
    @MockBean private com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CoursePrerequisiteService prerequisiteService;   // SCRUM-48

    private User teacher;
    private Course course;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setFirstName("Tina");
        teacher.setLastName("Teacher");
        teacher.setEmail(TEACHER);
        teacher.setRole(UserRole.TEACHER);
        teacher.setTeacherApproved(true);

        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Basics");
        course.setTopic("Programming");
        course.setDescription("Learn Java");
        course.setStatus("PUBLISHED");
        course.setTeacher(teacher);

        given(courseService.getCourseById(10L)).willReturn(Optional.of(course));
        given(courseService.getVisibleCourses(null, false)).willReturn(List.of(course));
        given(userService.findByEmail(TEACHER)).willReturn(teacher);
    }

    private static TestingAuthenticationToken asTeacher() {
        return new TestingAuthenticationToken(TEACHER, null, "ROLE_TEACHER");
    }

    @Test
    void newCourse_shouldNotRequireAttendance_byDefault() {
        assertThat(new Course().isAttendanceRequired()).isFalse();
    }

    @Test
    void courseDetail_shouldShowAttendanceRequired() throws Exception {
        course.setAttendanceRequired(true);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Attendance")))
                .andExpect(content().string(containsString(">Required<")));
    }

    @Test
    void courseDetail_shouldShowAttendanceNotRequired() throws Exception {
        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Not required")))
                .andExpect(content().string(not(containsString(">Required<"))));
    }

    @Test
    void courseList_shouldBadgeCoursesThatRequireAttendance() throws Exception {
        course.setAttendanceRequired(true);

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Attendance required")));
    }

    @Test
    void courseList_shouldNotBadgeCoursesWithoutAttendance() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Attendance required"))));
    }

    @Test
    void addCourseForm_shouldOfferTheCheckbox() throws Exception {
        mockMvc.perform(get("/courses/add").principal(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"attendanceRequired\"")));
    }

    @Test
    void addCourse_shouldStoreAttendanceRequired() throws Exception {
        mockMvc.perform(post("/courses/add").principal(asTeacher())
                        .param("title", "Java Basics")
                        .param("topic", "Programming")
                        .param("attendanceRequired", "true")
                        .param("_attendanceRequired", "on"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseService).createCourse(saved.capture());
        assertThat(saved.getValue().isAttendanceRequired()).isTrue();
    }

    @Test
    void editCourse_shouldTurnAttendanceOn() throws Exception {
        mockMvc.perform(post("/courses/10/edit").principal(asTeacher())
                        .param("title", "Java Basics")
                        .param("topic", "Programming")
                        .param("attendanceRequired", "true")
                        .param("_attendanceRequired", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/10"));

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseService).updateCourse(eq(10L), saved.capture());
        assertThat(saved.getValue().isAttendanceRequired()).isTrue();
    }

    @Test
    void editCourse_shouldTurnAttendanceOff_whenTheBoxIsUnchecked() throws Exception {
        course.setAttendanceRequired(true);

        // An unchecked box sends only Spring's "_attendanceRequired" marker.
        mockMvc.perform(post("/courses/10/edit").principal(asTeacher())
                        .param("title", "Java Basics")
                        .param("topic", "Programming")
                        .param("_attendanceRequired", "on"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseService).updateCourse(eq(10L), saved.capture());
        assertThat(saved.getValue().isAttendanceRequired()).isFalse();
    }
}
