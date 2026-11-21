package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private CourseRepository courseRepo;

    @Mock
    private AssignmentRepository assignmentRepo;

    @Mock
    private LectureRepository lectureRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private User student;
    private Course course;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setUserId(1L);
        student.setEmail("student@test.com");

        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Course");
        course.setPassingGrade(50.0);

        enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.IN_PROGRESS
        );
    }

    @Test
    void isEnrolled_shouldReturnTrue_whenEnrollmentExists() {
        when(enrollmentRepo
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(true);

        boolean result =
                enrollmentService.isEnrolled(
                        "student@test.com",
                        10L
                );

        assertTrue(result);

        verify(enrollmentRepo)
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                );
    }

    @Test
    void isEnrolled_shouldReturnFalse_whenEnrollmentDoesNotExist() {
        when(enrollmentRepo
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(false);

        boolean result =
                enrollmentService.isEnrolled(
                        "student@test.com",
                        10L
                );

        assertFalse(result);

        verify(enrollmentRepo)
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                );
    }

    @Test
    void enroll_shouldCreateEnrollment_whenStudentIsNotAlreadyEnrolled() {
        when(enrollmentRepo
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(false);

        when(userRepo.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(courseRepo.findById(10L))
                .thenReturn(Optional.of(course));

        enrollmentService.enroll(
                "student@test.com",
                10L
        );

        verify(userRepo).findByEmail("student@test.com");
        verify(courseRepo).findById(10L);

        verify(enrollmentRepo).save(argThat(savedEnrollment ->
                savedEnrollment.getStudent().equals(student)
                        && savedEnrollment.getCourse().equals(course)
                        && savedEnrollment.getCompletionStatus()
                        == Enrollment.CompletionStatus.IN_PROGRESS
                        && savedEnrollment.getAverageGrade() == null
        ));
    }

    @Test
    void enroll_shouldDoNothing_whenStudentIsAlreadyEnrolled() {
        when(enrollmentRepo
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(true);

        enrollmentService.enroll(
                "student@test.com",
                10L
        );

        verify(userRepo, never()).findByEmail(anyString());
        verify(courseRepo, never()).findById(anyLong());
        verify(enrollmentRepo, never()).save(any(Enrollment.class));
    }

    @Test
    void enroll_shouldThrowEntityNotFoundException_whenUserDoesNotExist() {
        when(enrollmentRepo
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(false);

        when(userRepo.findByEmail("student@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> enrollmentService.enroll(
                        "student@test.com",
                        10L
                )
        );

        verify(userRepo).findByEmail("student@test.com");

        verify(courseRepo, never()).findById(anyLong());
        verify(enrollmentRepo, never()).save(any(Enrollment.class));
    }

    @Test
    void enroll_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(enrollmentRepo
                .existsByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(false);

        when(userRepo.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(courseRepo.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> enrollmentService.enroll(
                        "student@test.com",
                        10L
                )
        );

        verify(userRepo).findByEmail("student@test.com");
        verify(courseRepo).findById(10L);
        verify(enrollmentRepo, never()).save(any(Enrollment.class));
    }

    @Test
    void unenroll_shouldDeleteEnrollment_whenEnrollmentExists() {
        when(enrollmentRepo
                .findByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(Optional.of(enrollment));

        enrollmentService.unenroll(
                "student@test.com",
                10L
        );

        verify(enrollmentRepo)
                .findByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                );

        verify(enrollmentRepo).delete(enrollment);
    }

    @Test
    void unenroll_shouldThrowEntityNotFoundException_whenEnrollmentDoesNotExist() {
        when(enrollmentRepo
                .findByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                ))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> enrollmentService.unenroll(
                        "student@test.com",
                        10L
                )
        );

        verify(enrollmentRepo, never()).delete(any(Enrollment.class));
    }

    @Test
    void getEnrolledCourseIds_shouldReturnIdsOfEnrolledCourses() {
        Course secondCourse = new Course();
        secondCourse.setCourseId(20L);

        Enrollment secondEnrollment = new Enrollment();
        secondEnrollment.setStudent(student);
        secondEnrollment.setCourse(secondCourse);

        when(enrollmentRepo.findByStudent_Email("student@test.com"))
                .thenReturn(List.of(enrollment, secondEnrollment));

        Set<Long> result =
                enrollmentService.getEnrolledCourseIds("student@test.com");

        assertEquals(Set.of(10L, 20L), result);

        verify(enrollmentRepo)
                .findByStudent_Email("student@test.com");
    }

    @Test
    void getEnrolledCourses_shouldReturnCoursesForStudent() {
        Course secondCourse = new Course();
        secondCourse.setCourseId(20L);
        secondCourse.setTitle("Spring Course");

        Enrollment secondEnrollment = new Enrollment();
        secondEnrollment.setStudent(student);
        secondEnrollment.setCourse(secondCourse);

        when(enrollmentRepo.findByStudent_Email("student@test.com"))
                .thenReturn(List.of(enrollment, secondEnrollment));

        Set<Course> result =
                enrollmentService.getEnrolledCourses("student@test.com");

        assertEquals(2, result.size());
        assertTrue(result.contains(course));
        assertTrue(result.contains(secondCourse));

        verify(enrollmentRepo)
                .findByStudent_Email("student@test.com");
    }

    @Test
    void ensureStudentEnrolled_shouldNotThrow_whenEnrollmentExists() {
        when(enrollmentRepo.findByStudent_EmailAndCourse_CourseId(
                "student@test.com",
                10L
        )).thenReturn(Optional.of(enrollment));

        assertDoesNotThrow(() ->
                enrollmentService.ensureStudentEnrolled(
                        "student@test.com",
                        10L
                )
        );

        verify(enrollmentRepo)
                .findByStudent_EmailAndCourse_CourseId(
                        "student@test.com",
                        10L
                );
    }

    @Test
    void ensureStudentEnrolled_shouldThrowIllegalStateException_whenEnrollmentDoesNotExist() {
        when(enrollmentRepo.findByStudent_EmailAndCourse_CourseId(
                "student@test.com",
                10L
        )).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.ensureStudentEnrolled(
                        "student@test.com",
                        10L
                )
        );

        assertEquals(
                "You must be enrolled in this course to comment.",
                exception.getMessage()
        );
    }

    @Test
    void getStudentsInCourse_shouldReturnStudentsFromRepository() {
        User secondStudent = new User();
        secondStudent.setUserId(2L);
        secondStudent.setEmail("student2@test.com");

        List<User> expectedStudents =
                List.of(student, secondStudent);

        when(enrollmentRepo.findStudentsByCourseId(10L))
                .thenReturn(expectedStudents);

        List<User> result =
                enrollmentService.getStudentsInCourse(10L);

        assertEquals(expectedStudents, result);

        verify(enrollmentRepo).findStudentsByCourseId(10L);
    }

    @Test
    void getStudentsInCourseEnrolledBy_shouldReturnStudentsEnrolledAtThatTime() {
        java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 12, 1, 9, 30);
        when(enrollmentRepo.findStudentsByCourseIdEnrolledBy(10L, start)).thenReturn(List.of(student));

        assertEquals(List.of(student), enrollmentService.getStudentsInCourseEnrolledBy(10L, start));
    }

    @Test
    void recalculateProgress_shouldSetCompleted_whenAllLecturesAreGraded() {
        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(
                        1L,
                        10L
                ))
                .thenReturn(Optional.of(enrollment));

        when(assignmentRepo.avgGradeForCourse(1L, 10L))
                .thenReturn(85.0);

        when(lectureRepository.countLecturesByCourseId(10L))
                .thenReturn(5L);

        when(assignmentRepo.countGradedLectures(1L, 10L))
                .thenReturn(5L);

        enrollmentService.recalculateProgress(1L, 10L);

        assertEquals(85.0, enrollment.getAverageGrade());
        assertEquals(
                Enrollment.CompletionStatus.COMPLETED,
                enrollment.getCompletionStatus()
        );

        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void recalculateProgress_shouldSetInProgress_whenNotAllLecturesAreGraded() {
        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(
                        1L,
                        10L
                ))
                .thenReturn(Optional.of(enrollment));

        when(assignmentRepo.avgGradeForCourse(1L, 10L))
                .thenReturn(70.0);

        when(lectureRepository.countLecturesByCourseId(10L))
                .thenReturn(5L);

        when(assignmentRepo.countGradedLectures(1L, 10L))
                .thenReturn(3L);

        enrollmentService.recalculateProgress(1L, 10L);

        assertEquals(70.0, enrollment.getAverageGrade());
        assertEquals(
                Enrollment.CompletionStatus.IN_PROGRESS,
                enrollment.getCompletionStatus()
        );

        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void recalculateProgress_shouldSetInProgress_whenCourseHasNoLectures() {
        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        when(assignmentRepo.avgGradeForCourse(1L, 10L))
                .thenReturn(null);

        when(lectureRepository.countLecturesByCourseId(10L))
                .thenReturn(0L);

        when(assignmentRepo.countGradedLectures(1L, 10L))
                .thenReturn(0L);

        enrollmentService.recalculateProgress(1L, 10L);

        assertNull(enrollment.getAverageGrade());

        assertEquals(
                Enrollment.CompletionStatus.IN_PROGRESS,
                enrollment.getCompletionStatus()
        );

        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void recalculateProgress_shouldThrowRuntimeException_whenEnrollmentDoesNotExist() {
        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.recalculateProgress(1L, 10L)
        );

        assertEquals("Enrollment not found", exception.getMessage());

        verify(assignmentRepo, never())
                .avgGradeForCourse(anyLong(), anyLong());

        verify(enrollmentRepo, never())
                .save(any(Enrollment.class));
    }

    @Test
    void ensureCanRateCourse_shouldThrowIllegalStateException_whenAverageGradeIsNull() {
        enrollment.setAverageGrade(null);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.COMPLETED
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );

        assertEquals(
                "You can rate only after your course grade is calculated.",
                exception.getMessage()
        );
    }

    @Test
    void ensureCanRateCourse_shouldThrowIllegalStateException_whenCourseIsNotCompleted() {
        enrollment.setAverageGrade(80.0);

        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.IN_PROGRESS
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );

        assertEquals(
                "You can rate only after completing all lectures.",
                exception.getMessage()
        );
    }

    @Test
    void ensureCanRateCourse_shouldThrowIllegalStateException_whenGradeIsBelowPassingGrade() {
        course.setPassingGrade(75.0);

        enrollment.setAverageGrade(60.0);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.COMPLETED
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );

        assertEquals(
                "Your grade is: 60.0 you can rate only after passing the course.",
                exception.getMessage()
        );
    }

    @Test
    void ensureCanRateCourse_shouldNotThrow_whenStudentPassedCourse() {
        course.setPassingGrade(75.0);

        enrollment.setAverageGrade(85.0);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.COMPLETED
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        assertDoesNotThrow(
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );

        verify(enrollmentRepo)
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L);
    }

    @Test
    void ensureCanRateCourse_shouldAllowRating_whenGradeEqualsPassingGrade() {
        course.setPassingGrade(75.0);

        enrollment.setAverageGrade(75.0);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.COMPLETED
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        assertDoesNotThrow(
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );
    }

    @Test
    void ensureCanRateCourse_shouldUseDefaultPassingGrade_whenPassingGradeIsNull() {
        course.setPassingGrade(null);

        enrollment.setAverageGrade(50.0);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.COMPLETED
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        assertDoesNotThrow(
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );
    }

    @Test
    void ensureCanRateCourse_shouldRejectGradeBelowDefaultPassingGrade_whenPassingGradeIsNull() {
        course.setPassingGrade(null);

        enrollment.setAverageGrade(49.0);
        enrollment.setCompletionStatus(
                Enrollment.CompletionStatus.COMPLETED
        );

        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );

        assertEquals(
                "Your grade is: 49.0 you can rate only after passing the course.",
                exception.getMessage()
        );
    }

    @Test
    void ensureCanRateCourse_shouldThrowRuntimeException_whenEnrollmentDoesNotExist() {
        when(enrollmentRepo
                .findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> enrollmentService.ensureCanRateCourse(1L, 10L)
        );

        assertEquals(
                "Enrollment not found",
                exception.getMessage()
        );
    }








    // --- SCRUM-31: rateCourse ------------------------------------------------

    private static final String VALID_REVIEW =
            "This course explained every topic clearly and the assignments were really useful.";

    private void givenStudentMayRate() {
        enrollment.setAverageGrade(80.0);
        enrollment.setCompletionStatus(Enrollment.CompletionStatus.COMPLETED);

        when(enrollmentRepo.findByStudent_EmailAndCourse_CourseId("student@test.com", 10L))
                .thenReturn(Optional.of(enrollment));
    }

    @Test
    void rateCourse_shouldSaveRatingAndTrimmedReview_whenValid() {
        assertTrue(VALID_REVIEW.length() >= EnrollmentService.MIN_REVIEW_LENGTH);
        givenStudentMayRate();
        when(enrollmentRepo.findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        enrollmentService.rateCourse("student@test.com", 10L, 4, "  " + VALID_REVIEW + "  ");

        assertEquals(4, enrollment.getRating());
        assertEquals(VALID_REVIEW, enrollment.getReviewText());
        assertNotNull(enrollment.getRatedAt());
        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void rateCourse_shouldAcceptReviewOfExactlyMinimumLength() {
        givenStudentMayRate();
        when(enrollmentRepo.findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        String review = "a".repeat(EnrollmentService.MIN_REVIEW_LENGTH);

        enrollmentService.rateCourse("student@test.com", 10L, 5, review);

        assertEquals(review, enrollment.getReviewText());
        verify(enrollmentRepo).save(enrollment);
    }

    @Test
    void rateCourse_shouldThrowIllegalArgumentException_whenReviewIsMissing() {
        givenStudentMayRate();

        assertThrows(
                IllegalArgumentException.class,
                () -> enrollmentService.rateCourse("student@test.com", 10L, 5, null)
        );

        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void rateCourse_shouldThrowIllegalArgumentException_whenReviewIsOneCharacterTooShort() {
        givenStudentMayRate();

        String review = "a".repeat(EnrollmentService.MIN_REVIEW_LENGTH - 1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> enrollmentService.rateCourse("student@test.com", 10L, 5, review)
        );

        assertTrue(ex.getMessage().contains("75"));
        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void rateCourse_shouldNotCountSurroundingWhitespaceTowardsTheMinimum() {
        givenStudentMayRate();

        String review = "short review" + " ".repeat(100);

        assertThrows(
                IllegalArgumentException.class,
                () -> enrollmentService.rateCourse("student@test.com", 10L, 5, review)
        );

        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void rateCourse_shouldThrowIllegalArgumentException_whenReviewIsTooLong() {
        givenStudentMayRate();

        String review = "a".repeat(EnrollmentService.MAX_REVIEW_LENGTH + 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> enrollmentService.rateCourse("student@test.com", 10L, 5, review)
        );

        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void rateCourse_shouldThrowIllegalArgumentException_whenRatingIsOutOfRange() {
        givenStudentMayRate();

        for (Integer invalid : new Integer[]{null, 0, 6}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> enrollmentService.rateCourse("student@test.com", 10L, invalid, VALID_REVIEW)
            );
        }

        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void rateCourse_shouldThrowIllegalStateException_whenCourseIsNotCompleted() {
        enrollment.setAverageGrade(80.0);
        enrollment.setCompletionStatus(Enrollment.CompletionStatus.IN_PROGRESS);

        when(enrollmentRepo.findByStudent_EmailAndCourse_CourseId("student@test.com", 10L))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepo.findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));

        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.rateCourse("student@test.com", 10L, 5, VALID_REVIEW)
        );

        assertNull(enrollment.getRating());
        verify(enrollmentRepo, never()).save(any());
    }

    @Test
    void rateCourse_shouldThrowEntityNotFoundException_whenStudentIsNotEnrolled() {
        when(enrollmentRepo.findByStudent_EmailAndCourse_CourseId("student@test.com", 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> enrollmentService.rateCourse("student@test.com", 10L, 5, VALID_REVIEW)
        );
    }

    @Test
    void getCourseReviews_shouldReturnRatedEnrollmentsFromRepository() {
        when(enrollmentRepo.findByCourse_CourseIdAndRatingIsNotNullOrderByRatedAtDesc(10L))
                .thenReturn(List.of(enrollment));

        assertEquals(List.of(enrollment), enrollmentService.getCourseReviews(10L));
    }

    @Test
    void getAverageRating_shouldReturnAverageFromRepository() {
        when(enrollmentRepo.averageRatingForCourse(10L)).thenReturn(4.5);

        assertEquals(4.5, enrollmentService.getAverageRating(10L));
    }

    @Test
    void getAverageRating_shouldReturnNull_whenCourseHasNoRatings() {
        when(enrollmentRepo.averageRatingForCourse(10L)).thenReturn(null);

        assertNull(enrollmentService.getAverageRating(10L));
    }

    // --- SCRUM-15: progress refresh after a lecture is deleted ---------------

    @Test
    void recalculateProgressForCourse_shouldRecalculateEveryEnrollmentOfTheCourse() {
        User other = new User();
        other.setUserId(2L);

        Enrollment second = new Enrollment();
        second.setStudent(other);
        second.setCourse(course);

        when(enrollmentRepo.findByCourse_CourseId(10L))
                .thenReturn(List.of(enrollment, second));
        when(enrollmentRepo.findByStudent_UserIdAndCourse_CourseId(1L, 10L))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepo.findByStudent_UserIdAndCourse_CourseId(2L, 10L))
                .thenReturn(Optional.of(second));

        // one lecture left, only the first student has a graded submission for it
        when(lectureRepository.countLecturesByCourseId(10L)).thenReturn(1L);
        when(assignmentRepo.avgGradeForCourse(1L, 10L)).thenReturn(90.0);
        when(assignmentRepo.countGradedLectures(1L, 10L)).thenReturn(1L);
        when(assignmentRepo.avgGradeForCourse(2L, 10L)).thenReturn(null);
        when(assignmentRepo.countGradedLectures(2L, 10L)).thenReturn(0L);

        enrollmentService.recalculateProgressForCourse(10L);

        assertEquals(Enrollment.CompletionStatus.COMPLETED, enrollment.getCompletionStatus());
        assertEquals(90.0, enrollment.getAverageGrade());
        assertEquals(Enrollment.CompletionStatus.IN_PROGRESS, second.getCompletionStatus());
        assertNull(second.getAverageGrade());
        verify(enrollmentRepo).save(enrollment);
        verify(enrollmentRepo).save(second);
    }

    @Test
    void recalculateProgressForCourse_shouldDoNothing_whenCourseHasNoEnrollments() {
        when(enrollmentRepo.findByCourse_CourseId(10L)).thenReturn(List.of());

        enrollmentService.recalculateProgressForCourse(10L);

        verify(enrollmentRepo, never()).save(any());
    }
}
