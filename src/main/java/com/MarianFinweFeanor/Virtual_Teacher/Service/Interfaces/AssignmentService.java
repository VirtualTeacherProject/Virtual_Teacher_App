package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// src/main/java/…/service/AssignmentService.java
public interface AssignmentService {
    void submit(String userEmail, Long lectureId, MultipartFile file, String comment) throws IOException;

    /**
     * Fetch all submissions for a given lecture + student.
     */
    List<Assignment> getSubmissionsByLectureAndUser(Long lectureId, String userEmail);

    /**
     * (Optional) find one submission by its ID.
     */
    Assignment findById(Long assignmentId);

}
