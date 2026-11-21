package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.AssignmentMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Submission files may only be downloaded by their author or a course manager. */
@ActiveProfiles("test")
@WebMvcTest(AssignmentMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssignmentMvcControllerTest {

    private static final String OWNER = "owner@test.com";
    private static final String OTHER = "other@test.com";
    private static final String TEACHER = "teacher@test.com";
    private static final String URL = "/courses/1/lectures/5/assignments/9/download";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AssignmentService assignmentService;
    @MockBean private UserService userService;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private LectureService lectureService;   // SCRUM-52
    @MockBean private UserRepository userRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path file = Files.writeString(tempDir.resolve("homework.txt"), "my answer");

        course = new Course();
        course.setCourseId(1L);

        Lecture lecture = new Lecture();
        lecture.setLectureId(5L);
        lecture.setCourse(course);
        given(lectureService.getLecturesById(5L)).willReturn(Optional.of(lecture));

        User student = new User();
        student.setEmail(OWNER);

        Assignment assignment = new Assignment();
        assignment.setAssignmentId(9L);
        assignment.setLecture(lecture);
        assignment.setStudent(student);
        assignment.setSubmissionFilePath(file.toString());

        given(assignmentService.findById(9L)).willReturn(assignment);
        this.assignment = assignment;
    }

    private Course course;
    private Assignment assignment;

    private static Principal as(String email) {
        return new TestingAuthenticationToken(email, "n/a", "ROLE_STUDENT");
    }

    @Test
    void download_shouldBeRefused_forAnonymousVisitor() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));
    }

    @Test
    void download_shouldBeRefused_forAnotherStudent() throws Exception {
        given(courseAccessService.canManageCourse(course, OTHER)).willReturn(false);

        mockMvc.perform(get(URL).principal(as(OTHER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void download_shouldReturnFile_forTheStudentWhoSubmittedIt() throws Exception {
        mockMvc.perform(get(URL).principal(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"homework.txt\""))
                .andExpect(content().string("my answer"));
    }

    @Test
    void download_shouldReturnFile_forCourseManager() throws Exception {
        given(courseAccessService.canManageCourse(course, TEACHER)).willReturn(true);

        mockMvc.perform(get(URL).principal(as(TEACHER)))
                .andExpect(status().isOk())
                .andExpect(content().string("my answer"));
    }

    @Test
    void download_shouldReturn404_whenSubmissionBelongsToAnotherLecture() throws Exception {
        mockMvc.perform(get("/courses/1/lectures/6/assignments/9/download").principal(as(OWNER)))
                .andExpect(status().isNotFound());
    }

    // --- SCRUM-52: only the course's own teacher or an admin --------------------

    private static final String GRADE_URL = "/courses/1/lectures/5/assignments/9/grade";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");

    @Test
    void download_shouldBeRefused_forATeacherOfAnotherCourse() throws Exception {
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);   // approved, but not the owner
        given(courseAccessService.canManageCourse(course, TEACHER)).willReturn(false);

        mockMvc.perform(get(URL).principal(TEACHER_PRINCIPAL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));
    }

    @Test
    void grade_shouldGrade_forTheCoursesTeacher() throws Exception {
        mockMvc.perform(post(GRADE_URL).principal(TEACHER_PRINCIPAL).param("grade", "80"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/lectures/5/assignments"));

        verify(userService).ensureApprovedTeacher(TEACHER);
        verify(courseAccessService).ensureCanManageCourse(course, TEACHER);
        verify(assignmentService).gradeAssignment(9L, 80.0, null);
    }

    @Test
    void grade_shouldBeRefused_forATeacherOfAnotherCourse() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("not yours"))
                .when(courseAccessService).ensureCanManageCourse(course, TEACHER);

        mockMvc.perform(post(GRADE_URL).principal(TEACHER_PRINCIPAL).param("grade", "80"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));

        verify(assignmentService, never()).gradeAssignment(anyLong(), any(), any());
    }

    @Test
    void grade_shouldAnswer404_forASubmissionOfAnotherLecture() throws Exception {
        Lecture other = new Lecture();
        other.setLectureId(6L);
        assignment.setLecture(other);

        mockMvc.perform(post(GRADE_URL).principal(TEACHER_PRINCIPAL).param("grade", "80"))
                .andExpect(status().isNotFound());

        verify(assignmentService, never()).gradeAssignment(anyLong(), any(), any());
    }

    @Test
    void grade_shouldAnswer404_forALectureOfAnotherCourse() throws Exception {
        mockMvc.perform(post("/courses/2/lectures/5/assignments/9/grade")
                        .principal(TEACHER_PRINCIPAL).param("grade", "80"))
                .andExpect(status().isNotFound());

        verify(assignmentService, never()).gradeAssignment(anyLong(), any(), any());
    }

    @Test
    void listSubmissions_shouldShowAllWithGrading_toTheCoursesTeacher() throws Exception {
        given(courseAccessService.canManageCourse(course, TEACHER)).willReturn(true);
        given(assignmentService.getSubmissionsByLecture(5L)).willReturn(List.of(assignment));

        mockMvc.perform(get("/courses/1/lectures/5/assignments").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canManageCourse", true))
                .andExpect(content().string(containsString(GRADE_URL)));
    }

    @Test
    void listSubmissions_shouldShowOnlyOwnSubmissions_toATeacherOfAnotherCourse() throws Exception {
        given(courseAccessService.canManageCourse(course, TEACHER)).willReturn(false);
        given(assignmentService.getSubmissionsByLectureAndUser(5L, TEACHER)).willReturn(List.of());

        mockMvc.perform(get("/courses/1/lectures/5/assignments").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canManageCourse", false))
                .andExpect(content().string(not(containsString("/grade"))));

        verify(assignmentService, never()).getSubmissionsByLecture(anyLong());
    }
}
