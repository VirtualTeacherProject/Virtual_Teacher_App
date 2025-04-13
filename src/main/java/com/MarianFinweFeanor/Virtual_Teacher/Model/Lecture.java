package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
@Data
@NoArgsConstructor
@Entity
@Table(name = "lectures")
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name="title",nullable = false, length = 50)
    private String title;

    @Column(name="description",nullable = false, length = 300)
    private String description;

    @Column(name="video_url",nullable = false)
    private String videoUrl;

    @Column(name="assignment_file_path",nullable = false)
    private String assignmentFilePath;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

}