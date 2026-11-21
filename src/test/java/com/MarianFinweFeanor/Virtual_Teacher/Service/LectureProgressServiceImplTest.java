package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureProgress;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureState;
import com.MarianFinweFeanor.Virtual_Teacher.Model.StudentProgress;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureProgressRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SCRUM-51 part B: lecture completion and in-order unlocking. */
@ExtendWith(MockitoExtension.class)
class LectureProgressServiceImplTest {

    private static final String STUDENT = "student@test.com";

    @Mock private LectureProgressRepository progressRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private EnrollmentService enrollmentService;
    @Mock private CourseAccessService courseAccessService;

    @InjectMocks
    private LectureProgressServiceImpl progressService;

    private Course course;
    private Lecture first;
    private Lecture second;
    private Lecture third;
    private User student;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(1L);
        course.setLecturesInOrder(true);

        first = lecture(10L, 1);
        second = lecture(11L, 2);
        second.setAssignmentRequired(true);
        third = lecture(12L, 3);

        student = new User();
        student.setUserId(7L);
        student.setEmail(STUDENT);
        student.setFirstName("Sam");
        student.setLastName("Student");
    }

    private Lecture lecture(Long id, int order) {
        Lecture lecture = new Lecture();
        lecture.setLectureId(id);
        lecture.setLectureOrder(order);
        lecture.setCourse(course);
        return lecture;
    }

    private LectureProgress finished(Lecture lecture) {
        LectureProgress progress = new LectureProgress();
        progress.setLecture(lecture);
        progress.setStudent(student);
        progress.setFinishedAt(LocalDateTime.now());
        return progress;
    }

    private Assignment graded(Lecture lecture) {
        Assignment assignment = new Assignment();
        assignment.setLecture(lecture);
        assignment.setStudent(student);
        assignment.setGrade(80.0);
        return assignment;
    }

    private void lectures() {
        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(1L))
                .thenReturn(List.of(first, second, third));
    }

    private void finishedLectures(LectureProgress... progress) {
        when(progressRepository.findByStudent_EmailAndLecture_Course_CourseId(STUDENT, 1L))
                .thenReturn(List.of(progress));
    }

    // --- states and locking ---------------------------------------------------

    @Test
    void getLectureStates_inOrderCourse_shouldLockEverythingAfterTheFirstUnfinishedLecture() {
        lectures();
        finishedLectures();

        List<LectureState> states = progressService.getLectureStates(1L, STUDENT);

        assertEquals(List.of(false, true, true), states.stream().map(LectureState::locked).toList());
        assertTrue(states.stream().noneMatch(LectureState::finished));
    }

    @Test
    void getLectureStates_shouldUnlockTheNextLecture_onceTheEarlierOnesAreFinished() {
        lectures();
        finishedLectures(finished(first), finished(second));
        when(assignmentRepository.findByStudent_EmailOrderBySubmittedAtDesc(STUDENT)).thenReturn(List.of());

        List<LectureState> states = progressService.getLectureStates(1L, STUDENT);

        assertEquals(List.of(false, false, false), states.stream().map(LectureState::locked).toList());
        // the second lecture has an ungraded assignment: finished, not completed
        assertTrue(states.get(0).completed());
        assertTrue(states.get(1).finished());
        assertFalse(states.get(1).completed());
        assertTrue(states.get(1).isAwaitingGrade());
    }

    @Test
    void getLectureStates_shouldCountAGradedAssignmentLectureAsCompleted() {
        lectures();
        finishedLectures(finished(first), finished(second));
        when(assignmentRepository.findByStudent_EmailOrderBySubmittedAtDesc(STUDENT))
                .thenReturn(List.of(graded(second)));

        assertTrue(progressService.getLectureStates(1L, STUDENT).get(1).completed());
    }

    @Test
    void getLectureStates_shouldNotLock_whenTheCourseAllowsAnyOrder() {
        course.setLecturesInOrder(false);
        lectures();
        finishedLectures();

        assertTrue(progressService.getLectureStates(1L, STUDENT).stream().noneMatch(LectureState::locked));
    }

    @Test
    void getLectureStates_shouldNotLock_forCourseManagers() {
        lectures();
        finishedLectures();
        when(courseAccessService.canManageCourses(STUDENT)).thenReturn(true);

        assertTrue(progressService.getLectureStates(1L, STUDENT).stream().noneMatch(LectureState::locked));
    }

    @Test
    void getLectureStates_forAnonymousVisitor_shouldShowNothingFinished() {
        lectures();

        List<LectureState> states = progressService.getLectureStates(1L, null);

        assertEquals(3, states.size());
        assertTrue(states.stream().noneMatch(LectureState::finished));
        verifyNoInteractions(progressRepository);
    }

    @Test
    void getLectureStates_shouldBeEmpty_forCourseWithoutLectures() {
        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(1L)).thenReturn(List.of());
        when(progressRepository.findByStudent_EmailAndLecture_Course_CourseId(STUDENT, 1L)).thenReturn(List.of());

        assertTrue(progressService.getLectureStates(1L, STUDENT).isEmpty());
    }

    @Test
    void isLocked_andEnsureUnlocked_shouldFollowTheLectureStates() {
        lectures();
        finishedLectures(finished(first));
        when(assignmentRepository.findByStudent_EmailOrderBySubmittedAtDesc(STUDENT)).thenReturn(List.of());

        assertFalse(progressService.isLocked(second, STUDENT));
        assertTrue(progressService.isLocked(third, STUDENT));
        assertDoesNotThrow(() -> progressService.ensureUnlocked(second, STUDENT));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> progressService.ensureUnlocked(third, STUDENT));
        assertEquals(LectureProgressServiceImpl.LOCKED, ex.getMessage());
    }

    @Test
    void isLocked_shouldBeFalse_forALectureNoLongerInTheCourseList() {
        when(lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(1L))
                .thenReturn(List.of(first));
        finishedLectures();

        assertFalse(progressService.isLocked(third, STUDENT));
    }

    // --- marking as finished --------------------------------------------------

    @Test
    void markFinished_shouldSaveProgress_andRecalculateTheCourse() {
        when(lectureRepository.findById(10L)).thenReturn(Optional.of(first));
        when(enrollmentService.isEnrolled(STUDENT, 1L)).thenReturn(true);
        lectures();
        finishedLectures();
        when(userRepository.findByEmail(STUDENT)).thenReturn(Optional.of(student));

        progressService.markFinished(1L, 10L, STUDENT);

        verify(progressRepository).save(argThat(p ->
                p.getLecture() == first && p.getStudent() == student && p.getFinishedAt() != null));
        verify(enrollmentService).recalculateProgress(7L, 1L);
    }

    @Test
    void markFinished_shouldDoNothing_whenAlreadyFinished() {
        when(lectureRepository.findById(10L)).thenReturn(Optional.of(first));
        when(enrollmentService.isEnrolled(STUDENT, 1L)).thenReturn(true);
        when(progressRepository.existsByLecture_LectureIdAndStudent_Email(10L, STUDENT)).thenReturn(true);

        progressService.markFinished(1L, 10L, STUDENT);

        verify(progressRepository, never()).save(any());
        verify(enrollmentService, never()).recalculateProgress(any(), any());
    }

    @Test
    void markFinished_shouldBeRefused_whenNotEnrolled() {
        when(lectureRepository.findById(10L)).thenReturn(Optional.of(first));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> progressService.markFinished(1L, 10L, STUDENT));

        assertEquals(LectureProgressServiceImpl.NOT_ENROLLED, ex.getMessage());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void markFinished_shouldBeRefused_whenTheLectureIsLocked() {
        when(lectureRepository.findById(12L)).thenReturn(Optional.of(third));
        when(enrollmentService.isEnrolled(STUDENT, 1L)).thenReturn(true);
        lectures();
        finishedLectures();

        assertThrows(IllegalStateException.class, () -> progressService.markFinished(1L, 12L, STUDENT));
        verify(progressRepository, never()).save(any());
    }

    @Test
    void markFinished_shouldRequireASubmission_forALectureWithAnAssignment() {
        course.setLecturesInOrder(false);
        when(lectureRepository.findById(11L)).thenReturn(Optional.of(second));
        when(enrollmentService.isEnrolled(STUDENT, 1L)).thenReturn(true);
        lectures();
        finishedLectures();
        when(assignmentRepository.findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(11L, STUDENT))
                .thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> progressService.markFinished(1L, 11L, STUDENT));

        assertEquals(LectureProgressServiceImpl.SUBMIT_FIRST, ex.getMessage());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void markFinished_shouldAccept_aLectureWithASubmittedAssignment() {
        course.setLecturesInOrder(false);
        when(lectureRepository.findById(11L)).thenReturn(Optional.of(second));
        when(enrollmentService.isEnrolled(STUDENT, 1L)).thenReturn(true);
        lectures();
        finishedLectures();
        when(assignmentRepository.findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(11L, STUDENT))
                .thenReturn(List.of(new Assignment()));
        when(userRepository.findByEmail(STUDENT)).thenReturn(Optional.of(student));

        progressService.markFinished(1L, 11L, STUDENT);

        verify(progressRepository).save(any(LectureProgress.class));
    }

    @Test
    void markFinished_shouldThrowNotFound_forLectureOfAnotherCourse() {
        when(lectureRepository.findById(10L)).thenReturn(Optional.of(first));

        assertThrows(EntityNotFoundException.class, () -> progressService.markFinished(2L, 10L, STUDENT));
        verifyNoInteractions(enrollmentService);
    }

    // --- course progress page -------------------------------------------------

    @Test
    void getCourseProgress_shouldCountFinishedAndCompletedLecturesPerStudent() {
        User anna = new User();
        anna.setUserId(8L);
        anna.setFirstName("Anna");
        anna.setLastName("Adams");
        Enrollment annas = new Enrollment();
        annas.setStudent(anna);
        annas.setCompletionStatus(Enrollment.CompletionStatus.COMPLETED);
        Enrollment sams = new Enrollment();
        sams.setStudent(student);

        when(lectureRepository.countLecturesByCourseId(1L)).thenReturn(3L);
        when(enrollmentRepository.findByCourse_CourseId(1L)).thenReturn(List.of(sams, annas));
        when(progressRepository.findByStudent_UserIdAndLecture_Course_CourseId(7L, 1L))
                .thenReturn(List.of(finished(first), finished(second)));
        when(progressRepository.countCompleted(7L, 1L)).thenReturn(1L);
        when(progressRepository.findByStudent_UserIdAndLecture_Course_CourseId(8L, 1L))
                .thenReturn(List.of(finished(first), finished(second), finished(third)));
        when(progressRepository.countCompleted(8L, 1L)).thenReturn(3L);

        List<StudentProgress> rows = progressService.getCourseProgress(1L);

        assertEquals(List.of(anna, student), rows.stream().map(StudentProgress::student).toList());
        assertEquals(new StudentProgress(anna, 3, 3, 3, Enrollment.CompletionStatus.COMPLETED), rows.get(0));
        assertEquals(new StudentProgress(student, 2, 1, 3, Enrollment.CompletionStatus.IN_PROGRESS), rows.get(1));
    }
}
