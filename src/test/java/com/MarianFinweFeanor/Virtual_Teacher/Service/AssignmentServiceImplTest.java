package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.*;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepo;

    @Mock
    private UserService userService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private LectureRepository lectureRepository;

    @TempDir
    Path tempDir;

    private AssignmentServiceImpl assignmentService;

    private User student;
    private Course course;
    private Lecture lecture;
    private Assignment assignment;

    @BeforeEach
    void setUp() throws IOException {
        assignmentService = new AssignmentServiceImpl(
                assignmentRepo,
                userService,
                enrollmentService,
                lectureRepository,
                tempDir.toString()
        );

        student = new User();
        student.setUserId(1L);
        student.setEmail("student@test.com");

        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Course");

        lecture = new Lecture();
        lecture.setLectureId(100L);
        lecture.setTitle("Spring Boot");
        lecture.setCourse(course);

        assignment = new Assignment();
        assignment.setAssignmentId(1000L);
        assignment.setStudent(student);
        assignment.setLecture(lecture);
        assignment.setGrade(null);
    }


    @Test
    void getSubmissionsByLectureAndUser_shouldReturnMatchingAssignments() {
        List<Assignment> expected = List.of(assignment);

        when(assignmentRepo
                .findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(
                        100L,
                        "student@test.com"
                ))
                .thenReturn(expected);

        List<Assignment> result =
                assignmentService.getSubmissionsByLectureAndUser(
                        100L,
                        "student@test.com"
                );

        assertEquals(expected, result);

        verify(assignmentRepo)
                .findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(
                        100L,
                        "student@test.com"
                );
    }

    @Test
    void getSubmissionsByLecture_shouldReturnAssignmentsForLecture() {
        List<Assignment> expected = List.of(assignment);

        when(assignmentRepo
                .findByLecture_LectureIdOrderBySubmittedAtDesc(100L))
                .thenReturn(expected);

        List<Assignment> result =
                assignmentService.getSubmissionsByLecture(100L);

        assertEquals(expected, result);

        verify(assignmentRepo)
                .findByLecture_LectureIdOrderBySubmittedAtDesc(100L);
    }

    @Test
    void gradeAssignment_shouldSetGradeAndCommentAndRecalculateProgress() {
        when(assignmentRepo.findById(1000L))
                .thenReturn(Optional.of(assignment));

        assignmentService.gradeAssignment(
                1000L,
                85.0,
                "Good work"
        );

        assertEquals(85.0, assignment.getGrade());
        assertEquals("Good work", assignment.getTeacherComment());

        verify(assignmentRepo).save(assignment);

        verify(enrollmentService)
                .recalculateProgress(1L, 10L);
    }

    @Test
    void gradeAssignment_shouldThrowEntityNotFoundException_whenAssignmentDoesNotExist() {
        when(assignmentRepo.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.gradeAssignment(
                        999L,
                        85.0,
                        "Good work"
                )
        );

        assertEquals(
                "Assignment with id 999 not found",
                exception.getMessage()
        );

        verify(assignmentRepo, never())
                .save(any(Assignment.class));

        verify(enrollmentService, never())
                .recalculateProgress(anyLong(), anyLong());
    }

    @Test
    void getMySubmissions_shouldReturnStudentAssignments() {
        List<Assignment> expected = List.of(assignment);

        when(assignmentRepo
                .findByStudent_EmailOrderBySubmittedAtDesc(
                        "student@test.com"
                ))
                .thenReturn(expected);

        List<Assignment> result =
                assignmentService.getMySubmissions(
                        "student@test.com"
                );

        assertEquals(expected, result);

        verify(assignmentRepo)
                .findByStudent_EmailOrderBySubmittedAtDesc(
                        "student@test.com"
                );
    }

    @Test
    void findById_shouldReturnAssignment_whenAssignmentExists() {
        when(assignmentRepo.findById(1000L))
                .thenReturn(Optional.of(assignment));

        Assignment result = assignmentService.findById(1000L);

        assertEquals(assignment, result);

        verify(assignmentRepo).findById(1000L);
    }

    @Test
    void findById_shouldThrowEntityNotFoundException_whenAssignmentDoesNotExist() {
        when(assignmentRepo.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.findById(999L)
        );

        assertEquals(
                "Assignment with id 999 not found",
                exception.getMessage()
        );
    }

    @Test
    void submit_shouldSaveAssignment_whenStudentIsEnrolled() throws IOException {
        student.getEnrollments().add(createEnrollment(student, course));

        when(userService.findByEmail("student@test.com"))
                .thenReturn(student);

        when(lectureRepository.findById(100L))
                .thenReturn(Optional.of(lecture));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "homework.txt",
                "text/plain",
                "My assignment".getBytes()
        );

        assignmentService.submit(
                "student@test.com",
                100L,
                file,
                "My comment"
        );

        verify(assignmentRepo).save(argThat(saved ->
                saved.getStudent().equals(student)
                        && saved.getLecture().equals(lecture)
                        && saved.getStudentComment().equals("My comment")
                        && saved.getGrade() == null
                        && saved.getSubmittedAt() != null
                        && saved.getSubmissionFilePath() != null
                        && saved.getSubmissionFilePath().contains("homework.txt")
        ));
    }

    private Enrollment createEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        return enrollment;
    }

    @Test
    void submit_shouldThrowAccessDeniedException_whenStudentIsNotEnrolled() throws IOException {
        when(userService.findByEmail("student@test.com"))
                .thenReturn(student);

        when(lectureRepository.findById(100L))
                .thenReturn(Optional.of(lecture));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "homework.txt",
                "text/plain",
                "My assignment".getBytes()
        );

        assertThrows(
                AccessDeniedException.class,
                () -> assignmentService.submit(
                        "student@test.com",
                        100L,
                        file,
                        "My comment"
                )
        );

        verify(assignmentRepo, never())
                .save(any(Assignment.class));
    }

    @Test
    void submit_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() throws IOException {
        when(userService.findByEmail("student@test.com"))
                .thenReturn(student);

        when(lectureRepository.findById(999L))
                .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "homework.txt",
                "text/plain",
                "My assignment".getBytes()
        );

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.submit(
                        "student@test.com",
                        999L,
                        file,
                        "My comment"
                )
        );

        assertEquals(
                "Lecture with id 999 not found",
                exception.getMessage()
        );

        verify(assignmentRepo, never())
                .save(any(Assignment.class));
    }

    @Test
    void loadAssignmentFile_shouldReturnResource_whenFileExistsAndIsReadable() throws Exception {
        Path filePath = tempDir.resolve("submitted-assignment.txt");
        Files.writeString(filePath, "assignment content");

        assignment.setSubmissionFilePath(filePath.toString());

        when(assignmentRepo.findById(1000L))
                .thenReturn(Optional.of(assignment));

        Resource result =
                assignmentService.loadAssignmentFile(1000L);

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.isReadable());
        assertEquals(
                "assignment content",
                Files.readString(result.getFile().toPath())
        );

        verify(assignmentRepo).findById(1000L);
    }

    @Test
    void loadAssignmentFile_shouldThrowEntityNotFoundException_whenAssignmentDoesNotExist() {
        when(assignmentRepo.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> assignmentService.loadAssignmentFile(999L)
        );

        assertEquals(
                "Assignment with id 999 not found",
                exception.getMessage()
        );
    }

    @Test
    void loadAssignmentFile_shouldThrowFileStorageException_whenFileDoesNotExist() {
        Path missingFile =
                tempDir.resolve("missing-assignment.txt");

        assignment.setSubmissionFilePath(
                missingFile.toString()
        );

        when(assignmentRepo.findById(1000L))
                .thenReturn(Optional.of(assignment));

        FileStorageException exception = assertThrows(
                FileStorageException.class,
                () -> assignmentService.loadAssignmentFile(1000L)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Could not read file")
        );
    }


}
