package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.AttendanceMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceStatus;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AttendanceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
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

import java.security.Principal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SCRUM-28: the attendance tracker page and its save endpoint. */
@ActiveProfiles("test")
@WebMvcTest(AttendanceMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceMvcControllerTest {

    private static final String TEACHER = "teacher@test.com";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");
    private static final String URL = "/courses/1/lectures/5/attendance";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AttendanceService attendanceService;
    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;   // GlobalModelAttributes

    private Course course;
    private Lecture lecture;
    private User anna;
    private User boris;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(1L);
        course.setTitle("Java Basics");
        course.setAttendanceRequired(true);

        lecture = new Lecture();
        lecture.setLectureId(5L);
        lecture.setTitle("Intro");
        lecture.setCourse(course);

        anna = student(10L, "Anna");
        boris = student(11L, "Boris");

        given(attendanceService.getLectureForAttendance(1L, 5L, TEACHER)).willReturn(lecture);
    }

    private static User student(Long id, String name) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(name);
        user.setLastName("Student");
        user.setEmail(name.toLowerCase() + "@test.com");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    @Test
    void showAttendance_shouldListStudentsWithTheirStatus() throws Exception {
        given(attendanceService.getLectureAttendance(1L, 5L, TEACHER)).willReturn(List.of(
                new AttendanceEntry(anna, AttendanceStatus.PRESENT),
                new AttendanceEntry(boris, AttendanceStatus.ABSENT)));

        mockMvc.perform(get(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(view().name("lecture-attendance"))
                .andExpect(model().attribute("attendanceRequired", true))
                .andExpect(model().attribute("recorded", true))
                .andExpect(model().attribute("absentCount", 1L))
                .andExpect(content().string(containsString("anna@test.com")))
                .andExpect(content().string(containsString("boris@test.com")))
                .andExpect(content().string(containsString("Save attendance")))
                .andExpect(content().string(containsString("Absent: boris@test.com\" checked=\"checked\"")))
                .andExpect(content().string(not(containsString("Absent: anna@test.com\" checked"))));

        verify(userService).ensureApprovedTeacher(TEACHER);
    }

    @Test
    void showAttendance_shouldSayNotRecorded_beforeTheFirstRollCall() throws Exception {
        given(attendanceService.getLectureAttendance(1L, 5L, TEACHER))
                .willReturn(List.of(new AttendanceEntry(anna, null)));

        mockMvc.perform(get(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recorded", false))
                .andExpect(content().string(containsString("Not recorded")));
    }

    @Test
    void showAttendance_shouldSayNoStudents_whenNobodyIsEnrolled() throws Exception {
        given(attendanceService.getLectureAttendance(1L, 5L, TEACHER)).willReturn(List.of());

        mockMvc.perform(get(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("attendance-no-students")))
                .andExpect(content().string(not(containsString("Save attendance"))));
    }

    @Test
    void showAttendance_shouldBeReadOnly_whenTheCourseNoLongerRequiresAttendance() throws Exception {
        course.setAttendanceRequired(false);
        given(attendanceService.getLectureAttendance(1L, 5L, TEACHER))
                .willReturn(List.of(new AttendanceEntry(anna, AttendanceStatus.ABSENT)));

        mockMvc.perform(get(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("attendanceRequired", false))
                .andExpect(content().string(containsString("attendance-not-required")))
                .andExpect(content().string(containsString("disabled")))
                .andExpect(content().string(not(containsString("Save attendance"))));
    }

    @Test
    void showAttendance_shouldAnswer404_forLectureOfAnotherCourse() throws Exception {
        given(attendanceService.getLectureForAttendance(2L, 5L, TEACHER))
                .willThrow(new EntityNotFoundException("Lecture", 5L));

        mockMvc.perform(get("/courses/2/lectures/5/attendance").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isNotFound());
    }

    @Test
    void showAttendance_shouldRedirectUnapprovedTeacher() throws Exception {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(TEACHER);

        mockMvc.perform(get(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));

        verify(attendanceService, never()).getLectureAttendance(anyLong(), anyLong(), any());
    }

    @Test
    void recordAttendance_shouldSaveTheTickedStudentsAsAbsent() throws Exception {
        given(attendanceService.recordAttendance(1L, 5L, TEACHER, List.of(11L))).willReturn(List.of(
                new AttendanceEntry(anna, AttendanceStatus.PRESENT),
                new AttendanceEntry(boris, AttendanceStatus.ABSENT)));

        mockMvc.perform(post(URL).principal(TEACHER_PRINCIPAL).param("absent", "11"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("msg", "Attendance saved: 1 present, 1 absent."));

        verify(userService).ensureApprovedTeacher(TEACHER);
        verify(attendanceService).recordAttendance(1L, 5L, TEACHER, List.of(11L));
    }

    @Test
    void recordAttendance_withoutTicks_shouldMarkEveryonePresent() throws Exception {
        given(attendanceService.recordAttendance(1L, 5L, TEACHER, List.of()))
                .willReturn(List.of(new AttendanceEntry(anna, AttendanceStatus.PRESENT)));

        mockMvc.perform(post(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("msg", "Attendance saved: 1 present, 0 absent."));

        verify(attendanceService).recordAttendance(1L, 5L, TEACHER, List.of());
    }

    @Test
    void recordAttendance_shouldShowError_whenTheServiceRefuses() throws Exception {
        given(attendanceService.recordAttendance(eq(1L), eq(5L), eq(TEACHER), any()))
                .willThrow(new IllegalStateException("This course does not require attendance"));

        mockMvc.perform(post(URL).principal(TEACHER_PRINCIPAL).param("absent", "10"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", "This course does not require attendance"));
    }

    @Test
    void recordAttendance_shouldShowError_forStudentWhoIsNotEnrolled() throws Exception {
        given(attendanceService.recordAttendance(eq(1L), eq(5L), eq(TEACHER), any()))
                .willThrow(new IllegalArgumentException(
                        "Only students enrolled in this course can be marked absent"));

        mockMvc.perform(post(URL).principal(TEACHER_PRINCIPAL).param("absent", "99"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error",
                        "Only students enrolled in this course can be marked absent"));
    }

    @Test
    void recordAttendance_shouldRedirectUnapprovedTeacher_withoutSaving() throws Exception {
        doThrow(new AccessDeniedException("Your teacher account is pending admin approval."))
                .when(userService).ensureApprovedTeacher(TEACHER);

        mockMvc.perform(post(URL).principal(TEACHER_PRINCIPAL).param("absent", "11"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));

        verify(attendanceService, never()).recordAttendance(anyLong(), anyLong(), any(), any());
    }
}
