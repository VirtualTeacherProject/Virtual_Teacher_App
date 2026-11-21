package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ContentCleanupService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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

    @Mock
    private ContentCleanupService contentCleanupService;

    @Mock
    private EnrollmentService enrollmentService;

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

        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(10L))
                .thenReturn(expected);

        List<Lecture> result =
                lectureService.getByCourseId(10L);

        assertEquals(expected, result);

        verify(courseRepository).findById(10L);
        verify(lectureRepository).findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(10L);
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
                .findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(any());
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
    void delete_shouldDeleteLectureWithItsContent_andRefreshCourseProgress() {
        lecture.setCourse(course);

        when(lectureRepository.findById(100L))
                .thenReturn(Optional.of(lecture));

        lectureService.delete(100L);

        // SCRUM-15: submissions and comments first, then the lecture itself
        InOrder inOrder = inOrder(contentCleanupService, lectureRepository, enrollmentService);
        inOrder.verify(contentCleanupService).deleteLectureContent(100L);
        inOrder.verify(lectureRepository).delete(lecture);
        inOrder.verify(enrollmentService).recalculateProgressForCourse(10L);

        verifyNoInteractions(courseRepository);
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() {
        when(lectureRepository.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureService.delete(999L)
        );

        assertEquals(
                "Lecture with id 999 not found",
                exception.getMessage()
        );

        verify(lectureRepository, never())
                .delete(any(Lecture.class));
        verifyNoInteractions(contentCleanupService, enrollmentService);
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

    // --- SCRUM-51 part A: live sessions ----------------------------------------

    @Test
    void updateLecture_shouldCopyLiveSession_andTreatBlankMeetingLinkAsNone() {
        Lecture updated = new Lecture();
        updated.setTitle("Live Q&A");
        updated.setLive(true);
        updated.setLiveStart(java.time.LocalDateTime.of(2026, 12, 1, 9, 30));
        updated.setMeetingUrl("  ");
        when(lectureRepository.findById(100L)).thenReturn(Optional.of(lecture));
        when(lectureRepository.save(lecture)).thenReturn(lecture);

        Lecture result = lectureService.updateLecture(100L, updated);

        assertTrue(result.isLive());
        assertEquals(java.time.LocalDateTime.of(2026, 12, 1, 9, 30), result.getLiveStart());
        assertNull(result.getMeetingUrl());
    }

    @Test
    void updateLecture_toSelfPaced_shouldClearStartAndMeetingLink() {
        lecture.setLive(true);
        lecture.setLiveStart(java.time.LocalDateTime.of(2026, 12, 1, 9, 30));
        lecture.setMeetingUrl("https://meet.example.com/abc");
        Lecture updated = new Lecture();
        updated.setLiveStart(java.time.LocalDateTime.of(2026, 12, 2, 9, 30));
        updated.setMeetingUrl("https://meet.example.com/xyz");
        when(lectureRepository.findById(100L)).thenReturn(Optional.of(lecture));
        when(lectureRepository.save(lecture)).thenReturn(lecture);

        Lecture result = lectureService.updateLecture(100L, updated);

        assertFalse(result.isLive());
        assertNull(result.getLiveStart());
        assertNull(result.getMeetingUrl());
    }

    @Test
    void createLectures_shouldRefuseLiveLectureWithoutStart() {
        lecture.setLive(true);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(new Course()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lectureService.createLectures(lecture));

        assertEquals(LectureServiceImpl.LIVE_START_REQUIRED, ex.getMessage());
        verify(lectureRepository, never()).save(any(Lecture.class));
    }

    @Test
    void liveSessionStartedBy_shouldBeTrueOnlyForLiveLectureOnceItStarts() {
        java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 12, 1, 9, 30);
        assertFalse(lecture.liveSessionStartedBy(start));   // not live
        lecture.setLive(true);
        assertFalse(lecture.liveSessionStartedBy(start));   // no start time
        lecture.setLiveStart(start);
        assertFalse(lecture.liveSessionStartedBy(start.minusMinutes(1)));
        assertTrue(lecture.liveSessionStartedBy(start));
        assertTrue(lecture.liveSessionStartedBy(start.plusHours(2)));
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




    // --- SCRUM-51 part B: order and completion ----------------------------------

    @Test
    void createLectures_shouldPutTheLectureLast_andRecalculateTheCourse() {
        Course managed = new Course();
        managed.setCourseId(10L);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(managed));
        when(lectureRepository.maxLectureOrder(10L)).thenReturn(3);
        when(lectureRepository.save(lecture)).thenReturn(lecture);

        lectureService.createLectures(lecture);

        assertEquals(4, lecture.getLectureOrder());
        verify(enrollmentService).recalculateProgressForCourse(10L);
    }

    @Test
    void updateLecture_shouldRecalculateTheCourse_whenTheAssignmentFlagChanges() {
        Lecture updated = new Lecture();
        updated.setAssignmentRequired(true);
        when(lectureRepository.findById(100L)).thenReturn(Optional.of(lecture));
        when(lectureRepository.save(lecture)).thenReturn(lecture);

        lectureService.updateLecture(100L, updated);

        assertTrue(lecture.isAssignmentRequired());
        verify(enrollmentService).recalculateProgressForCourse(10L);
    }

    @Test
    void updateLecture_shouldNotRecalculate_whenTheAssignmentFlagIsUnchanged() {
        when(lectureRepository.findById(100L)).thenReturn(Optional.of(lecture));
        when(lectureRepository.save(lecture)).thenReturn(lecture);

        lectureService.updateLecture(100L, new Lecture());

        verify(enrollmentService, never()).recalculateProgressForCourse(any());
    }

    private Lecture ordered(long id, int order) {
        Lecture l = new Lecture();
        l.setLectureId(id);
        l.setLectureOrder(order);
        l.setCourse(lecture.getCourse());
        return l;
    }

    @Test
    void moveLecture_shouldSwapWithTheNeighbour_andRenumber() {
        Lecture a = ordered(1L, 1), b = ordered(2L, 2), c = ordered(3L, 5);
        when(lectureRepository.findById(3L)).thenReturn(Optional.of(c));
        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(10L))
                .thenReturn(new java.util.ArrayList<>(List.of(a, b, c)));

        lectureService.moveLecture(3L, true);

        assertEquals(List.of(1, 3, 2), List.of(a.getLectureOrder(), b.getLectureOrder(), c.getLectureOrder()));
        verify(lectureRepository).saveAll(any());
    }

    @Test
    void moveLecture_down_shouldSwapWithTheNextLecture() {
        Lecture a = ordered(1L, 1), b = ordered(2L, 2);
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(a));
        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(10L))
                .thenReturn(new java.util.ArrayList<>(List.of(a, b)));

        lectureService.moveLecture(1L, false);

        assertEquals(2, a.getLectureOrder());
        assertEquals(1, b.getLectureOrder());
    }

    @Test
    void moveLecture_shouldDoNothing_atEitherEnd() {
        Lecture a = ordered(1L, 1), b = ordered(2L, 2);
        when(lectureRepository.findById(1L)).thenReturn(Optional.of(a));
        when(lectureRepository.findById(2L)).thenReturn(Optional.of(b));
        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(10L))
                .thenReturn(new java.util.ArrayList<>(List.of(a, b)));

        lectureService.moveLecture(1L, true);
        lectureService.moveLecture(2L, false);

        assertEquals(1, a.getLectureOrder());
        assertEquals(2, b.getLectureOrder());
        verify(lectureRepository, never()).saveAll(any());
    }

    @Test
    void moveLecture_shouldThrowNotFound_forMissingLecture() {
        when(lectureRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> lectureService.moveLecture(9L, true));
    }
}
