package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table (name = "courses")
public class Courses {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long courseId;

    @Column (nullable = false, length = 30)
    private String title;

    @Column (nullable = false, length = 40)
    private String topic;

    @Column (length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime columnName;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    //Getters and Setters







}
