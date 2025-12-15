package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// src/main/java/…/service/AssignmentService.java
public interface AssignmentService {

    // Student: submissions for THIS lecture by THIS student
    List<Assignment> getSubmissionsByLectureAndUser(Long lectureId, String userEmail);

    // Teacher: ALL submissions for THIS lecture (all students)
    List<Assignment> getSubmissionsByLecture(Long lectureId);

    // Student: ALL submissions across all courses/lectures
    List<Assignment> getMySubmissions(String userEmail);

    // Find one submission (for download / grading)
    Assignment findById(Long assignmentId);

    // Student uploads assignment
    void submit(String userEmail, Long lectureId, MultipartFile file, String comment) throws IOException;

    // Teacher grades assignment
    void gradeAssignment(Long assignmentId, Double grade, String teacherComment);
}

