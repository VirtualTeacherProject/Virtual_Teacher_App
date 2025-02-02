package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "lectures")
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lectureId;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false)
    private String videoUrl;

    @Column(nullable = false)
    private String assignmentFilePath;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false) // Links to the course table
    private Course course;

    //Getters and Setters

    @Override
    public int hashCode() {
        return Objects.hash(lectureId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Lecture lecture = (Lecture) obj;
        return lectureId == lecture.lectureId;
    }
    // this compares by ID


}