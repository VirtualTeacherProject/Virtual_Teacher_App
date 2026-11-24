package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CoursePrerequisiteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseResourceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureAttendanceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureNoteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureProgressRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ContentCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ContentCleanupServiceImpl implements ContentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ContentCleanupServiceImpl.class);

    private final AssignmentRepository assignmentRepository;
    private final LectureCommentRepository lectureCommentRepository;
    private final LectureNoteRepository lectureNoteRepository;
    private final CourseResourceRepository courseResourceRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final LectureAttendanceRepository lectureAttendanceRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;   // SCRUM-48
    private final Path uploadDir;

    @Autowired
    public ContentCleanupServiceImpl(AssignmentRepository assignmentRepository,
                                     LectureCommentRepository lectureCommentRepository,
                                     LectureNoteRepository lectureNoteRepository,
                                     CourseResourceRepository courseResourceRepository,
                                     ResourceRequestRepository resourceRequestRepository,
                                     LectureAttendanceRepository lectureAttendanceRepository,
                                     LectureProgressRepository lectureProgressRepository,
                                     CoursePrerequisiteRepository coursePrerequisiteRepository,
                                     @Value("${app.upload.dir}") String uploadDir) {
        this.assignmentRepository = assignmentRepository;
        this.lectureCommentRepository = lectureCommentRepository;
        this.lectureNoteRepository = lectureNoteRepository;
        this.courseResourceRepository = courseResourceRepository;
        this.resourceRequestRepository = resourceRequestRepository;
        this.lectureAttendanceRepository = lectureAttendanceRepository;
        this.lectureProgressRepository = lectureProgressRepository;
        this.coursePrerequisiteRepository = coursePrerequisiteRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void deleteLectureContent(Long lectureId) {
        deleteSubmissions(assignmentRepository.findByLecture_LectureId(lectureId));
        lectureCommentRepository.deleteAll(
                lectureCommentRepository.findByLecture_LectureId(lectureId));
        lectureNoteRepository.deleteAll(
                lectureNoteRepository.findByLecture_LectureId(lectureId));
        lectureAttendanceRepository.deleteAll(
                lectureAttendanceRepository.findByLecture_LectureId(lectureId));
        lectureProgressRepository.deleteAll(
                lectureProgressRepository.findByLecture_LectureId(lectureId));
    }

    @Override
    public void deleteCourseContent(Long courseId) {
        deleteSubmissions(assignmentRepository.findByLecture_Course_CourseId(courseId));
        lectureCommentRepository.deleteAll(
                lectureCommentRepository.findByLecture_Course_CourseId(courseId));
        lectureNoteRepository.deleteAll(
                lectureNoteRepository.findByLecture_Course_CourseId(courseId));
        lectureAttendanceRepository.deleteAll(
                lectureAttendanceRepository.findByLecture_Course_CourseId(courseId));
        lectureProgressRepository.deleteAll(
                lectureProgressRepository.findByLecture_Course_CourseId(courseId));
        courseResourceRepository.deleteAll(
                courseResourceRepository.findByCourse_CourseId(courseId));
        resourceRequestRepository.deleteAll(
                resourceRequestRepository.findByCourse_CourseId(courseId));
        // SCRUM-48: the course's own prerequisites, and links where it is the prerequisite
        coursePrerequisiteRepository.deleteAll(
                coursePrerequisiteRepository.findByCourse_CourseId(courseId));
        coursePrerequisiteRepository.deleteAll(
                coursePrerequisiteRepository.findByPrerequisiteCourse_CourseId(courseId));
    }

    @Override
    public void deleteUserContent(Long userId) {
        deleteSubmissions(assignmentRepository.findByStudent_UserId(userId));
        lectureCommentRepository.deleteAll(
                lectureCommentRepository.findByAuthor_UserId(userId));
        lectureNoteRepository.deleteAll(
                lectureNoteRepository.findByUser_UserId(userId));
        lectureAttendanceRepository.deleteAll(
                lectureAttendanceRepository.findByStudent_UserId(userId));
        lectureProgressRepository.deleteAll(
                lectureProgressRepository.findByStudent_UserId(userId));
        resourceRequestRepository.deleteAll(
                resourceRequestRepository.findByStudent_UserId(userId));
        // SCRUM-36: requests assigned to the user belong to their authors and stay;
        // only the assignment and the user's answer go.
        List<ResourceRequest> assigned = resourceRequestRepository.findByAssignee_UserId(userId);
        assigned.forEach(ResourceRequest::clearAssignment);
        resourceRequestRepository.saveAll(assigned);
    }

    private void deleteSubmissions(List<Assignment> submissions) {
        List<String> filePaths = submissions.stream()
                .map(Assignment::getSubmissionFilePath)
                .filter(Objects::nonNull)
                .toList();

        assignmentRepository.deleteAll(submissions);

        // Files cannot be rolled back, so they are removed only once the
        // database delete has really been committed.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFiles(filePaths);
                }
            });
        } else {
            deleteFiles(filePaths);
        }
    }

    private void deleteFiles(List<String> filePaths) {
        for (String filePath : filePaths) {
            Path file = Paths.get(filePath).toAbsolutePath().normalize();

            // Never touch anything outside the upload directory.
            if (!file.startsWith(uploadDir)) {
                log.warn("Skipping submission file outside the upload directory: {}", file);
                continue;
            }

            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Could not delete submission file {}", file, e);
            }
        }
    }
}
