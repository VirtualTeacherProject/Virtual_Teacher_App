package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SCRUM-15: dependent submissions, comments and uploaded files. */
@ExtendWith(MockitoExtension.class)
class ContentCleanupServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private LectureCommentRepository lectureCommentRepository;

    @TempDir
    Path uploadDir;

    @TempDir
    Path elsewhere;

    private ContentCleanupServiceImpl cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new ContentCleanupServiceImpl(
                assignmentRepository, lectureCommentRepository, uploadDir.toString());
    }

    private Assignment submission(Path file) {
        Assignment assignment = new Assignment();
        assignment.setSubmissionFilePath(file == null ? null : file.toString());
        return assignment;
    }

    private Path uploaded(String name) throws IOException {
        return Files.writeString(uploadDir.resolve(name), "answer");
    }

    @Test
    void deleteLectureContent_shouldDeleteSubmissionsFilesAndComments() throws IOException {
        Path file = uploaded("a.txt");
        List<Assignment> submissions = List.of(submission(file));
        List<LectureComment> comments = List.of(new LectureComment());

        when(assignmentRepository.findByLecture_LectureId(100L)).thenReturn(submissions);
        when(lectureCommentRepository.findByLecture_LectureId(100L)).thenReturn(comments);

        cleanupService.deleteLectureContent(100L);

        verify(assignmentRepository).deleteAll(submissions);
        verify(lectureCommentRepository).deleteAll(comments);
        assertFalse(Files.exists(file));
    }

    @Test
    void deleteCourseContent_shouldDeleteSubmissionsFilesAndCommentsOfAllLectures() throws IOException {
        Path first = uploaded("a.txt");
        Path second = uploaded("b.txt");
        List<Assignment> submissions = List.of(submission(first), submission(second));
        List<LectureComment> comments = List.of(new LectureComment(), new LectureComment());

        when(assignmentRepository.findByLecture_Course_CourseId(10L)).thenReturn(submissions);
        when(lectureCommentRepository.findByLecture_Course_CourseId(10L)).thenReturn(comments);

        cleanupService.deleteCourseContent(10L);

        verify(assignmentRepository).deleteAll(submissions);
        verify(lectureCommentRepository).deleteAll(comments);
        assertFalse(Files.exists(first));
        assertFalse(Files.exists(second));
    }

    @Test
    void deleteUserContent_shouldDeleteOnlyThatUsersSubmissionsAndComments() throws IOException {
        Path mine = uploaded("mine.txt");
        Path someoneElses = uploaded("other.txt");
        List<Assignment> submissions = List.of(submission(mine));
        List<LectureComment> comments = List.of(new LectureComment());

        when(assignmentRepository.findByStudent_UserId(1L)).thenReturn(submissions);
        when(lectureCommentRepository.findByAuthor_UserId(1L)).thenReturn(comments);

        cleanupService.deleteUserContent(1L);

        verify(assignmentRepository).deleteAll(submissions);
        verify(lectureCommentRepository).deleteAll(comments);
        assertFalse(Files.exists(mine));
        assertTrue(Files.exists(someoneElses));
    }

    @Test
    void cleanup_shouldNeverDeleteFilesOutsideTheUploadDirectory() throws IOException {
        Path outside = Files.writeString(elsewhere.resolve("keep.txt"), "important");
        List<Assignment> submissions = List.of(submission(outside));

        when(assignmentRepository.findByLecture_LectureId(100L)).thenReturn(submissions);
        when(lectureCommentRepository.findByLecture_LectureId(100L)).thenReturn(List.of());

        cleanupService.deleteLectureContent(100L);

        verify(assignmentRepository).deleteAll(submissions);
        assertTrue(Files.exists(outside));
    }

    @Test
    void cleanup_shouldTolerateMissingFilesAndMissingPaths() {
        List<Assignment> submissions = List.of(
                submission(uploadDir.resolve("already-gone.txt")),
                submission(null));

        when(assignmentRepository.findByLecture_LectureId(100L)).thenReturn(submissions);
        when(lectureCommentRepository.findByLecture_LectureId(100L)).thenReturn(List.of());

        assertDoesNotThrow(() -> cleanupService.deleteLectureContent(100L));

        verify(assignmentRepository).deleteAll(submissions);
    }
}
