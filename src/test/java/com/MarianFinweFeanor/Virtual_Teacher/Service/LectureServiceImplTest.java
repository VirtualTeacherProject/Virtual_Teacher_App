package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LectureServiceImplTest {

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private LectureServiceImpl lectureService;

    private Course course;
    private Lecture lecture;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Course");

        lecture = new Lecture();
        lecture.setLectureId(100L);
        lecture.setTitle("Spring Boot");
        lecture.setDescription("Spring Boot basics");
        lecture.setVideoUrl("https://example.com/video");
        lecture.setAssignmentFilePath("assignment.txt");
        lecture.setCourse(course);
    }

    @Test
    void saveLecture_shouldSaveAndReturnLecture() {
        when(lectureRepository.save(lecture))
                .thenReturn(lecture);

        Lecture result = lectureService.saveLecture(lecture);

        assertEquals(lecture, result);

        verify(lectureRepository).save(lecture);
    }

    @Test
    void createLectures_shouldAttachCourseAndSaveLecture_whenCourseExists() {
        Course managedCourse = new Course();
        managedCourse.setCourseId(10L);
        managedCourse.setTitle("Managed Java Course");

        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(managedCourse));

        when(lectureRepository.save(lecture))
                .thenReturn(lecture);

        Lecture result = lectureService.createLectures(lecture);

        assertEquals(managedCourse, result.getCourse());

        verify(courseRepository).findById(10L);
        verify(lectureRepository).save(lecture);
    }

    @Test
    void createLectures_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseRepository.findById(10L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.createLectures(lecture)
        );

        assertEquals(
                "Course with id 10 not found",
                exception.getMessage()
        );

        verify(lectureRepository, never())
                .save(any(Lecture.class));
    }

    @Test
    void getByCourseId_shouldReturnLecturesForCourse_whenCourseExists() {
        List<Lecture> expected = List.of(lecture);

        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(course));

        when(lectureRepository.findAllByCourse(course))
                .thenReturn(expected);

        List<Lecture> result =
                lectureService.getByCourseId(10L);

        assertEquals(expected, result);

        verify(courseRepository).findById(10L);
        verify(lectureRepository).findAllByCourse(course);
    }

    @Test
    void getByCourseId_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseRepository.findById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.getByCourseId(99L)
        );

        assertEquals(
                "Course with id 99 not found",
                exception.getMessage()
        );

        verify(lectureRepository, never())
                .findAllByCourse(any(Course.class));
    }

    @Test
    void getAllLectures_shouldReturnAllLectures_whenLecturesExist() {
        when(lectureRepository.count())
                .thenReturn(2L);

        List<Lecture> expected = List.of(lecture);

        when(lectureRepository.findAll())
                .thenReturn(expected);

        List<Lecture> result =
                lectureService.getAllLectures();

        assertEquals(expected, result);

        verify(lectureRepository).count();
        verify(lectureRepository).findAll();
    }

    @Test
    void getAllLectures_shouldThrowEntityNotFoundException_whenNoLecturesExist() {
        when(lectureRepository.count())
                .thenReturn(0L);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.getAllLectures()
        );

        assertEquals(
                "Lectures not found in database",
                exception.getMessage()
        );

        verify(lectureRepository).count();
        verify(lectureRepository, never()).findAll();
    }

    @Test
    void getLecturesById_shouldReturnLecture_whenLectureExists() {
        when(lectureRepository.existsById(100L))
                .thenReturn(true);

        when(lectureRepository.findById(100L))
                .thenReturn(Optional.of(lecture));

        Optional<Lecture> result =
                lectureService.getLecturesById(100L);

        assertTrue(result.isPresent());
        assertEquals(lecture, result.get());

        verify(lectureRepository).existsById(100L);
        verify(lectureRepository).findById(100L);
    }

    @Test
    void getLecturesById_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() {
        when(lectureRepository.existsById(999L))
                .thenReturn(false);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.getLecturesById(999L)
        );

        assertEquals(
                "Lecture with id 999 not found",
                exception.getMessage()
        );

        verify(lectureRepository).existsById(999L);
        verify(lectureRepository, never()).findById(anyLong());
    }

    @Test
    void delete_shouldDeleteLecture_whenLectureExists() {
        when(lectureRepository.existsById(100L))
                .thenReturn(true);

        lectureService.delete(100L);

        verify(lectureRepository).existsById(100L);
        verify(lectureRepository).deleteById(100L);
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() {
        when(lectureRepository.existsById(999L))
                .thenReturn(false);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.delete(999L)
        );

        assertEquals(
                "Lecture with id 999 not found",
                exception.getMessage()
        );

        verify(lectureRepository, never())
                .deleteById(anyLong());
    }

    @Test
    void updateLecture_shouldUpdateFieldsAndSaveLecture() {
        Lecture updatedLecture = new Lecture();
        updatedLecture.setTitle("Updated Spring Boot");
        updatedLecture.setDescription("Updated description");
        updatedLecture.setVideoUrl("https://example.com/new-video");
        updatedLecture.setAssignmentFilePath("updated-assignment.txt");

        when(lectureRepository.findById(100L))
                .thenReturn(Optional.of(lecture));

        when(lectureRepository.save(lecture))
                .thenReturn(lecture);

        Lecture result =
                lectureService.updateLecture(
                        100L,
                        updatedLecture
                );

        assertEquals(
                "Updated Spring Boot",
                result.getTitle()
        );

        assertEquals(
                "Updated description",
                result.getDescription()
        );

        assertEquals(
                "https://example.com/new-video",
                result.getVideoUrl()
        );

        assertEquals(
                "updated-assignment.txt",
                result.getAssignmentFilePath()
        );

        verify(lectureRepository).save(lecture);
    }

    @Test
    void updateLecture_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() {
        Lecture updatedLecture = new Lecture();

        when(lectureRepository.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.updateLecture(
                        999L,
                        updatedLecture
                )
        );

        assertEquals(
                "Lecture with id 999 not found",
                exception.getMessage()
        );

        verify(lectureRepository, never())
                .save(any(Lecture.class));
    }



}