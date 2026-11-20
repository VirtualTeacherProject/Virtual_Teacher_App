package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.LectureMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureCommentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureNoteService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SCRUM-10: the lecture video is rendered only for users with content access.
 * SCRUM-22: lecture pages of a draft course are hidden from non-managers.
 * SCRUM-20: the private notes card and its save endpoint.
 * SCRUM-28: the link to the attendance tracker.
 */
@ActiveProfiles("test")
@WebMvcTest(LectureMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class LectureMvcControllerTest {

    private static final String VIDEO_ID = "abc123XYZ";
    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=" + VIDEO_ID;
    private static final String STUDENT = "student@test.com";
    private static final Principal STUDENT_PRINCIPAL =
            new TestingAuthenticationToken(STUDENT, "n/a", "ROLE_STUDENT");

    @Autowired
    private MockMvc mockMvc;

    @MockBean private LectureService lectureService;
    @MockBean private UserService userService;
    @MockBean private CourseService courseService;
    @MockBean private AssignmentService assignmentService;
    @MockBean private LectureCommentService lectureCommentService;
    @MockBean private EnrollmentService enrollmentService;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private LectureNoteService lectureNoteService;
    @MockBean private UserRepository userRepository;

    private Course course;
    private Lecture lecture;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(1L);
        course.setTitle("Java Basics");
        course.setStatus("PUBLISHED");

        lecture = new Lecture();
        lecture.setLectureId(5L);
        lecture.setTitle("Intro");
        lecture.setDescription("First lecture");
        lecture.setVideoUrl(VIDEO_URL);
        lecture.setCourse(course);

        given(lectureService.getLecturesById(5L)).willReturn(Optional.of(lecture));
        given(courseService.getCourseById(1L)).willReturn(Optional.of(course));
        given(lectureService.getByCourseId(1L)).willReturn(List.of(lecture));
        given(lectureCommentService.getCommentsForLecture(5L)).willReturn(List.of());
        given(userService.getEnrolledCourses(STUDENT)).willReturn(Set.of());
    }

    @Test
    void lectureDetail_shouldHideVideo_forAnonymousVisitor() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, null)).willReturn(false);

        mockMvc.perform(get("/courses/1/lectures/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("lecture-detail"))
                .andExpect(model().attribute("canAccessContent", false))
                .andExpect(content().string(containsString("lecture-video-locked")))
                .andExpect(content().string(not(containsString(VIDEO_ID))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void lectureDetail_shouldHideVideo_forStudentWhoIsNotEnrolled() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(false);

        mockMvc.perform(get("/courses/1/lectures/5").principal(STUDENT_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canAccessContent", false))
                .andExpect(content().string(containsString("lecture-video-locked")))
                .andExpect(content().string(not(containsString(VIDEO_ID))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void lectureDetail_shouldShowVideo_forEnrolledStudent() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(true);
        given(userService.getEnrolledCourses(STUDENT)).willReturn(Set.of(course));
        given(assignmentService.getSubmissionsByLectureAndUser(5L, STUDENT)).willReturn(List.of());

        mockMvc.perform(get("/courses/1/lectures/5").principal(STUDENT_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canAccessContent", true))
                .andExpect(content().string(containsString("https://www.youtube.com/embed/" + VIDEO_ID)))
                .andExpect(content().string(not(containsString("lecture-video-locked"))));
    }

    @Test
    void lectureDetail_shouldNotShowLockedNotice_whenLectureHasNoVideo() throws Exception {
        lecture.setVideoUrl("");
        given(courseAccessService.canAccessCourseContent(course, null)).willReturn(false);

        mockMvc.perform(get("/courses/1/lectures/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("lecture-video-locked"))));
    }

    @Test
    void lectureDetail_shouldReturn404_whenCourseIsDraftAndVisitorCannotViewIt() throws Exception {
        course.setStatus("DRAFT");
        doThrow(new EntityNotFoundException("Course", 1L))
                .when(courseAccessService).ensureCanViewCourse(eq(course), isNull());

        mockMvc.perform(get("/courses/1/lectures/5"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/404"))
                .andExpect(content().string(not(containsString(VIDEO_ID))));
    }

    @Test
    void listLectures_shouldReturn404_whenCourseIsDraftAndVisitorCannotViewIt() throws Exception {
        course.setStatus("DRAFT");
        doThrow(new EntityNotFoundException("Course", 1L))
                .when(courseAccessService).ensureCanViewCourse(eq(course), any());

        mockMvc.perform(get("/courses/1/lectures"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/404"));
    }

    @Test
    void listLectures_shouldRender_forPublishedCourse_withoutLeakingVideoUrls() throws Exception {
        mockMvc.perform(get("/courses/1/lectures"))
                .andExpect(status().isOk())
                .andExpect(view().name("lectures"))
                .andExpect(content().string(not(containsString(VIDEO_ID))));
    }

    // --- SCRUM-20: notes -----------------------------------------------------

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void lectureDetail_shouldShowOwnNote_forEnrolledStudent() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(true);
        given(userService.getEnrolledCourses(STUDENT)).willReturn(Set.of(course));
        given(lectureNoteService.getNoteContent(5L, STUDENT)).willReturn("Remember <generics>");

        mockMvc.perform(get("/courses/1/lectures/5").principal(STUDENT_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canTakeNotes", true))
                .andExpect(model().attribute("noteContent", "Remember <generics>"))
                .andExpect(content().string(containsString("id=\"lecture-notes\"")))
                // plain text: markup in a note is escaped, never rendered
                .andExpect(content().string(containsString("Remember &lt;generics&gt;")));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void lectureDetail_shouldHideNotes_forStudentWhoIsNotEnrolled() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(false);

        mockMvc.perform(get("/courses/1/lectures/5").principal(STUDENT_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canTakeNotes", false))
                .andExpect(content().string(not(containsString("id=\"lecture-notes\""))));

        verify(lectureNoteService, never()).getNoteContent(any(), any());
    }

    @Test
    void lectureDetail_shouldHideNotes_forAnonymousVisitor() throws Exception {
        mockMvc.perform(get("/courses/1/lectures/5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canTakeNotes", false))
                .andExpect(content().string(not(containsString("id=\"lecture-notes\""))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void saveNote_shouldSaveForTheLoggedInUser_andRedirectToTheLecture() throws Exception {
        mockMvc.perform(post("/courses/1/lectures/5/notes")
                        .principal(STUDENT_PRINCIPAL)
                        .param("content", "My note"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/1/lectures/5"))
                .andExpect(flash().attribute("msg", "Notes saved."));

        verify(lectureNoteService).saveNote(5L, STUDENT, "My note");
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void saveNote_shouldShowError_whenStudentIsNotEnrolled() throws Exception {
        doThrow(new AccessDeniedException("Enroll in this course to take notes"))
                .when(lectureNoteService).saveNote(5L, STUDENT, "My note");

        mockMvc.perform(post("/courses/1/lectures/5/notes")
                        .principal(STUDENT_PRINCIPAL)
                        .param("content", "My note"))
                .andExpect(redirectedUrl("/courses/1/lectures/5"))
                .andExpect(flash().attribute("error", "Enroll in this course to take notes"));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void saveNote_shouldShowError_whenNoteIsTooLong() throws Exception {
        doThrow(new IllegalArgumentException("Notes cannot be longer than 5000 characters"))
                .when(lectureNoteService).saveNote(eq(5L), eq(STUDENT), any());

        mockMvc.perform(post("/courses/1/lectures/5/notes")
                        .principal(STUDENT_PRINCIPAL)
                        .param("content", "x"))
                .andExpect(redirectedUrl("/courses/1/lectures/5"))
                .andExpect(flash().attribute("error", "Notes cannot be longer than 5000 characters"));
    }

    // --- SCRUM-28: attendance link -------------------------------------------

    private static final String TEACHER = "teacher@test.com";
    private static final Principal TEACHER_PRINCIPAL =
            new TestingAuthenticationToken(TEACHER, "n/a", "ROLE_TEACHER");

    private void approvedTeacher() {
        com.MarianFinweFeanor.Virtual_Teacher.Model.User teacher =
                new com.MarianFinweFeanor.Virtual_Teacher.Model.User();
        teacher.setEmail(TEACHER);
        teacher.setFirstName("Tina");
        teacher.setLastName("Teacher");
        teacher.setRole(com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole.TEACHER);
        teacher.setTeacherApproved(true);
        given(userRepository.findByEmail(TEACHER)).willReturn(Optional.of(teacher));
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void lectureDetail_shouldLinkToAttendance_forManager_whenCourseRequiresAttendance() throws Exception {
        approvedTeacher();
        course.setAttendanceRequired(true);

        mockMvc.perform(get("/courses/1/lectures/5").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lecture-attendance-link")))
                .andExpect(content().string(containsString("/courses/1/lectures/5/attendance")));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void lectureDetail_shouldNotLinkToAttendance_whenCourseDoesNotRequireIt() throws Exception {
        approvedTeacher();
        course.setAttendanceRequired(false);

        mockMvc.perform(get("/courses/1/lectures/5").principal(TEACHER_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("lecture-attendance-link"))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void lectureDetail_shouldNotLinkToAttendance_forStudent() throws Exception {
        course.setAttendanceRequired(true);
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(true);

        mockMvc.perform(get("/courses/1/lectures/5").principal(STUDENT_PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("lecture-attendance-link"))));
    }
}
