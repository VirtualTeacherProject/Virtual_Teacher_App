package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
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
    private final Path uploadDir;

    @Autowired
    public ContentCleanupServiceImpl(AssignmentRepository assignmentRepository,
                                     LectureCommentRepository lectureCommentRepository,
                                     @Value("${app.upload.dir}") String uploadDir) {
        this.assignmentRepository = assignmentRepository;
        this.lectureCommentRepository = lectureCommentRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void deleteLectureContent(Long lectureId) {
        deleteSubmissions(assignmentRepository.findByLecture_LectureId(lectureId));
        lectureCommentRepository.deleteAll(
                lectureCommentRepository.findByLecture_LectureId(lectureId));
    }

    @Override
    public void deleteCourseContent(Long courseId) {
        deleteSubmissions(assignmentRepository.findByLecture_Course_CourseId(courseId));
        lectureCommentRepository.deleteAll(
                lectureCommentRepository.findByLecture_Course_CourseId(courseId));
    }

    @Override
    public void deleteUserContent(Long userId) {
        deleteSubmissions(assignmentRepository.findByStudent_UserId(userId));
        lectureCommentRepository.deleteAll(
                lectureCommentRepository.findByAuthor_UserId(userId));
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
