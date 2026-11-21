package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController.LecturesRestController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SCRUM-52: lectures can be changed over the API only by the course's own teacher or an admin. */
@ActiveProfiles("test")
@WebMvcTest(LecturesRestController.class)
@WithMockUser(username = LecturesRestControllerTest.TEACHER, roles = "TEACHER")
class LecturesRestControllerTest {

    static final String TEACHER = "teacher@test.com";

    @Autowired private MockMvc mockMvc;

    @MockBean private LectureService lectureService;
    @MockBean private CourseService courseService;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private UserRepository userRepository;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(1L);
        Lecture lecture = new Lecture();
        lecture.setLectureId(5L);
        lecture.setCourse(course);
        given(courseService.getCourseById(1L)).willReturn(Optional.of(course));
        given(lectureService.getLecturesById(5L)).willReturn(Optional.of(lecture));
    }

    private void notTheOwner() {
        doThrow(new AccessDeniedException("not yours")).when(courseAccessService).ensureCanManageCourse(course, TEACHER);
    }

    @Test
    void createLecture_shouldWork_inOwnCourse() throws Exception {
        mockMvc.perform(post("/api/lectures").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"course\":{\"courseId\":1}}"))
                .andExpect(status().isOk());

        verify(lectureService).createLectures(any());
    }

    @Test
    void createLecture_shouldAnswer403_inAnotherTeachersCourse() throws Exception {
        notTheOwner();

        mockMvc.perform(post("/api/lectures").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"course\":{\"courseId\":1}}"))
                .andExpect(status().isForbidden());

        verify(lectureService, never()).createLectures(any());
    }

    @Test
    void createLecture_shouldAnswer400_withoutACourse() throws Exception {
        mockMvc.perform(post("/api/lectures").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete_shouldAnswer403_forAnotherTeachersLecture() throws Exception {
        notTheOwner();

        mockMvc.perform(put("/api/lectures/5").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mine now\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/lectures/5").with(csrf()))
                .andExpect(status().isForbidden());

        verify(lectureService, never()).updateLecture(anyLong(), any());
        verify(lectureService, never()).delete(anyLong());
    }

    @Test
    void delete_shouldWork_forOwnLecture() throws Exception {
        mockMvc.perform(delete("/api/lectures/5").with(csrf())).andExpect(status().isOk());

        verify(lectureService).delete(5L);
    }
}
