package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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

    @Size(max = 120)     // DB column size
    @Column(name="title",nullable = false, length = 120)
    private String title;

    @Size(max = 1000)
    @Column(name="description",nullable = false, length = 1000)
    private String description;

    @Size(max = 500)
    @Column(name="video_url",nullable = false, length = 500)
    private String videoUrl;

    @Column(name="assignment_file_path",nullable = false, length = 50)
    private String assignmentFilePath;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

}