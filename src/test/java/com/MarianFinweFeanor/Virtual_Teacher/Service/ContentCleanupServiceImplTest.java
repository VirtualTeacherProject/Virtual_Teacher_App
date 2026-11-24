package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureAttendance;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureNote;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseResourceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureAttendanceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureProgressRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureProgress;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureNoteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
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

    @Mock
    private LectureNoteRepository lectureNoteRepository;

    @Mock
    private CourseResourceRepository courseResourceRepository;

    @Mock
    private ResourceRequestRepository resourceRequestRepository;

    @Mock
    private LectureAttendanceRepository lectureAttendanceRepository;

    @Mock
    private LectureProgressRepository lectureProgressRepository;   // SCRUM-51 part B

    @Mock
    private com.MarianFinweFeanor.Virtual_Teacher.Repositories.CoursePrerequisiteRepository
            coursePrerequisiteRepository;   // SCRUM-48

    @TempDir
    Path uploadDir;

    @TempDir
    Path elsewhere;

    private ContentCleanupServiceImpl cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new ContentCleanupServiceImpl(
                assignmentRepository, lectureCommentRepository, lectureNoteRepository,
                courseResourceRepository, resourceRequestRepository, lectureAttendanceRepository,
                lectureProgressRepository, coursePrerequisiteRepository, uploadDir.toString());
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
        List<LectureNote> notes = List.of(new LectureNote());
        when(lectureCommentRepository.findByLecture_LectureId(100L)).thenReturn(comments);
        when(lectureNoteRepository.findByLecture_LectureId(100L)).thenReturn(notes);
        List<LectureAttendance> attendance = List.of(new LectureAttendance());
        when(lectureAttendanceRepository.findByLecture_LectureId(100L)).thenReturn(attendance);
        List<LectureProgress> progress = List.of(new LectureProgress());
        when(lectureProgressRepository.findByLecture_LectureId(100L)).thenReturn(progress);

        cleanupService.deleteLectureContent(100L);

        verify(assignmentRepository).deleteAll(submissions);
        verify(lectureCommentRepository).deleteAll(comments);
        verify(lectureNoteRepository).deleteAll(notes);
        verify(lectureAttendanceRepository).deleteAll(attendance);   // SCRUM-28
        verify(lectureProgressRepository).deleteAll(progress);      // SCRUM-51
        verifyNoInteractions(courseResourceRepository);   // resources belong to the course
        verifyNoInteractions(resourceRequestRepository);  // so do the requests
        assertFalse(Files.exists(file));
    }

    @Test
    void deleteCourseContent_shouldDeleteSubmissionsFilesAndCommentsOfAllLectures() throws IOException {
        Path first = uploaded("a.txt");
        Path second = uploaded("b.txt");
        List<Assignment> submissions = List.of(submission(first), submission(second));
        List<LectureComment> comments = List.of(new LectureComment(), new LectureComment());

        when(assignmentRepository.findByLecture_Course_CourseId(10L)).thenReturn(submissions);
        List<LectureNote> notes = List.of(new LectureNote(), new LectureNote());
        when(lectureCommentRepository.findByLecture_Course_CourseId(10L)).thenReturn(comments);
        when(lectureNoteRepository.findByLecture_Course_CourseId(10L)).thenReturn(notes);
        List<CourseResource> resources = List.of(new CourseResource());
        when(courseResourceRepository.findByCourse_CourseId(10L)).thenReturn(resources);
        List<ResourceRequest> requests = List.of(new ResourceRequest(), new ResourceRequest());
        when(resourceRequestRepository.findByCourse_CourseId(10L)).thenReturn(requests);
        List<LectureAttendance> attendance = List.of(new LectureAttendance(), new LectureAttendance());
        when(lectureAttendanceRepository.findByLecture_Course_CourseId(10L)).thenReturn(attendance);
        List<LectureProgress> progress = List.of(new LectureProgress());
        when(lectureProgressRepository.findByLecture_Course_CourseId(10L)).thenReturn(progress);
        var ownPrerequisites = List.of(new com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite());
        var asPrerequisite = List.of(new com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite());
        when(coursePrerequisiteRepository.findByCourse_CourseId(10L)).thenReturn(ownPrerequisites);
        when(coursePrerequisiteRepository.findByPrerequisiteCourse_CourseId(10L)).thenReturn(asPrerequisite);

        cleanupService.deleteCourseContent(10L);

        verify(assignmentRepository).deleteAll(submissions);
        verify(lectureCommentRepository).deleteAll(comments);
        verify(lectureNoteRepository).deleteAll(notes);
        verify(courseResourceRepository).deleteAll(resources);
        verify(resourceRequestRepository).deleteAll(requests);
        verify(lectureAttendanceRepository).deleteAll(attendance);   // SCRUM-28
        verify(lectureProgressRepository).deleteAll(progress);      // SCRUM-51
        verify(coursePrerequisiteRepository).deleteAll(ownPrerequisites);   // SCRUM-48
        verify(coursePrerequisiteRepository).deleteAll(asPrerequisite);
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
        List<LectureNote> notes = List.of(new LectureNote());
        when(lectureCommentRepository.findByAuthor_UserId(1L)).thenReturn(comments);
        when(lectureNoteRepository.findByUser_UserId(1L)).thenReturn(notes);
        List<ResourceRequest> requests = List.of(new ResourceRequest());
        when(resourceRequestRepository.findByStudent_UserId(1L)).thenReturn(requests);
        List<LectureAttendance> attendance = List.of(new LectureAttendance());
        when(lectureAttendanceRepository.findByStudent_UserId(1L)).thenReturn(attendance);
        List<LectureProgress> progress = List.of(new LectureProgress());
        when(lectureProgressRepository.findByStudent_UserId(1L)).thenReturn(progress);

        cleanupService.deleteUserContent(1L);

        verify(assignmentRepository).deleteAll(submissions);
        verify(lectureCommentRepository).deleteAll(comments);
        verify(lectureNoteRepository).deleteAll(notes);
        verify(resourceRequestRepository).deleteAll(requests);
        verify(lectureAttendanceRepository).deleteAll(attendance);   // SCRUM-28
        verify(lectureProgressRepository).deleteAll(progress);      // SCRUM-51
        assertFalse(Files.exists(mine));
        assertTrue(Files.exists(someoneElses));
    }

    @Test
    void deleteUserContent_shouldKeepRequestsAssignedToTheUser_butRemoveAssignmentAndAnswer() {
        // SCRUM-36: the request belongs to its author; only the assignee's part goes
        ResourceRequest inProgress = new ResourceRequest();
        inProgress.setStatus(ResourceRequest.STATUS_IN_PROGRESS);
        inProgress.setAssignee(new com.MarianFinweFeanor.Virtual_Teacher.Model.User());
        inProgress.setResponse("An answer");
        inProgress.setRespondedAt(java.time.LocalDateTime.now());
        ResourceRequest resolved = new ResourceRequest();
        resolved.setStatus(ResourceRequest.STATUS_RESOLVED);
        resolved.setAssignee(new com.MarianFinweFeanor.Virtual_Teacher.Model.User());
        resolved.setResponse("Another answer");
        List<ResourceRequest> assigned = List.of(inProgress, resolved);
        when(resourceRequestRepository.findByAssignee_UserId(1L)).thenReturn(assigned);

        cleanupService.deleteUserContent(1L);

        verify(resourceRequestRepository).saveAll(assigned);
        verify(resourceRequestRepository, never()).deleteAll(assigned);
        assertNull(inProgress.getAssignee());
        assertNull(inProgress.getResponse());
        assertNull(inProgress.getRespondedAt());
        assertEquals(ResourceRequest.STATUS_OPEN, inProgress.getStatus());
        assertNull(resolved.getAssignee());
        assertNull(resolved.getResponse());
        assertEquals(ResourceRequest.STATUS_RESOLVED, resolved.getStatus());
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
