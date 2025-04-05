package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // References the User entity
    @ManyToOne
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture; // References the Lecture entity

    @Column(name="submission_file_path",nullable = false, length = 255)
    private String submissionFilePath; // Path to the  assignment file

    @Column(name="grade",nullable = false)
    private Double grade; // Grade for the assignment

    // Getters and Setters
    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Lecture getLecture() {
        return lecture;
    }

    public void setLecture(Lecture lecture) {
        this.lecture = lecture;
    }

    public String getSubmissionFilePath() {
        return submissionFilePath;
    }

    public void setSubmissionFilePath(String submissionFilePath) {
        this.submissionFilePath = submissionFilePath;
    }

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Assignment assignment = (Assignment) obj;
        return assignmentId == assignment.assignmentId;
    }
}

