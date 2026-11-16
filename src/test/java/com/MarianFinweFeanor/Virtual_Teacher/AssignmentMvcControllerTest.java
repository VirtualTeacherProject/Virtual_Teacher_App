package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.AssignmentMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
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

import static org.mockito.BDDMockito.given;
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
    @MockBean private UserRepository userRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path file = Files.writeString(tempDir.resolve("homework.txt"), "my answer");

        Lecture lecture = new Lecture();
        lecture.setLectureId(5L);

        User student = new User();
        student.setEmail(OWNER);

        Assignment assignment = new Assignment();
        assignment.setAssignmentId(9L);
        assignment.setLecture(lecture);
        assignment.setStudent(student);
        assignment.setSubmissionFilePath(file.toString());

        given(assignmentService.findById(9L)).willReturn(assignment);
    }

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
        given(courseAccessService.canManageCourses(OTHER)).willReturn(false);

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
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);

        mockMvc.perform(get(URL).principal(as(TEACHER)))
                .andExpect(status().isOk())
                .andExpect(content().string("my answer"));
    }

    @Test
    void download_shouldReturn404_whenSubmissionBelongsToAnotherLecture() throws Exception {
        mockMvc.perform(get("/courses/1/lectures/6/assignments/9/download").principal(as(OWNER)))
                .andExpect(status().isNotFound());
    }
}
