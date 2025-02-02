package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "lectures")
public class Lectures {
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


}
