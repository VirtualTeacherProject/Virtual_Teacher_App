package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
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
}

