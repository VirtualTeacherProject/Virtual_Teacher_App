package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

//    /**
////     * @param uploadDirPath directory path from config (or tmpdir by default)
////     */

    @Autowired
    public AssignmentServiceImpl(AssignmentRepository assignmentRepo,
                                 UserService           userService,
                                 LectureRepository     lectureRepository,
                                 @Value("${app.upload.dir}") String uploadDirStr) throws IOException {
        this.assignmentRepo     = assignmentRepo;
        this.userService        = userService;
        this.lectureRepository  = lectureRepository;
        //this.uploadDir          = Paths.get(uploadDirPath);

        Path base = Paths.get(uploadDirStr);
        if (!Files.exists(base)) {
            Files.createDirectories(base);
        }
        this.uploadDir = base;
    }

    @Override
    public List<Assignment> getSubmissionsByLectureAndUser(Long lectureId, String userEmail) {
        return assignmentRepo.
                findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(lectureId, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getSubmissionsByLecture(Long lectureId) {
        return assignmentRepo.findByLecture_LectureIdOrderBySubmittedAtDesc(lectureId);
    }


    @Override
    public void gradeAssignment(Long assignmentId, Double grade, String teacherComment) {
        Assignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment", assignmentId));
        assignment.setGrade(grade);
        assignment.setComment(teacherComment);   // for now reuse comment for teacher note
        assignmentRepo.save(assignment);
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
        a.setComment(comment);
        a.setSubmittedAt(LocalDateTime.now());
        a.setGrade(null);       // pending
        assignmentRepo.save(a);
    }



    // add a helper to load the file:
    public Resource loadAssignmentFile(Long assignmentId) throws MalformedURLException {
        Assignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment", assignmentId));
        Path target = Paths.get(assignment.getSubmissionFilePath());
        Resource resource = new UrlResource(target.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Could not read file: " + target);
        }
        return resource;
    }





    @Override
    @Transactional (readOnly = true)
    public List <Assignment> getMySubmissions (String userEmail)
   {
       return assignmentRepo.findByStudent_EmailOrderBySubmittedAtDesc(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment findById(Long assignmentId) {
        return assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment", assignmentId));
    }

}

