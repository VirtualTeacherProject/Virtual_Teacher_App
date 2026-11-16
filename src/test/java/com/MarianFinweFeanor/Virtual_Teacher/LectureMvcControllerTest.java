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
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SCRUM-10: the lecture video is rendered only for users with content access.
 * SCRUM-22: lecture pages of a draft course are hidden from non-managers.
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
}
