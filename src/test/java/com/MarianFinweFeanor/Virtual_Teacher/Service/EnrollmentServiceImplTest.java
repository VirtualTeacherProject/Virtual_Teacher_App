package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
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







}
