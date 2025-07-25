package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

// src/main/java/…/service/impl/AssignmentServiceImpl.java
@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {
    private final AssignmentRepository assignmentRepo;
    private final UserService           userService;
    private final LectureRepository     lectureRepository;
    private final Path                  uploadDir;  // now a Path instead of String

    // where to drop uploaded files
    //@Value("${app.upload.dir:${java.io.tmpdir}}")
    //private String uploadDir;

    /**
     * @param uploadDirPath directory path from config (or tmpdir by default)
     */

    @Autowired
    public AssignmentServiceImpl(AssignmentRepository assignmentRepo,
                                 UserService           userService,
                                 LectureRepository     lectureRepository,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${app.upload.dir:${java.io.tmpdir}}"
                                 ) String uploadDirPath) {
        this.assignmentRepo     = assignmentRepo;
        this.userService        = userService;
        this.lectureRepository  = lectureRepository;
        this.uploadDir          = Paths.get(uploadDirPath);
    }

    @Override
    public void submit(String userEmail, Long lectureId,
                       MultipartFile file, String comment) throws IOException {
        // 1) Fetch & verify
        User student = userService.findByEmail(userEmail);
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        // 2) Optional: check enrollment
        boolean enrolled = student.getCourses()
                .stream()
                .map(Course::getCourseId)
                .anyMatch(id -> id.equals(lecture.getCourse().getCourseId()));
        if (!enrolled) {
            throw new AccessDeniedException("You’re not enrolled in that course");
        }

        // 3) Save file to disk
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path   target   = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 4) Persist Assignment
        Assignment a = new Assignment();
        a.setStudent(student);
        a.setLecture(lecture);
        a.setSubmissionFilePath(target.toString());
        a.setGrade(null);       // pending
        assignmentRepo.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getSubmissionsByLectureAndUser(Long lectureId, String userEmail) {
        return assignmentRepo.findByLecture_LectureIdAndStudent_Email(lectureId, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment findById(Long assignmentId) {
        return assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment", assignmentId));
    }

}

