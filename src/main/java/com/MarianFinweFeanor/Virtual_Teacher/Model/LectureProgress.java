package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SCRUM-51 part B: a student marked a lecture as finished. A lecture with an
 * assignment can only be marked once the assignment is submitted, and counts as
 * completed only once it is graded. At most one row per student per lecture.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lecture_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_progress_lecture_student",
                columnNames = {"lecture_id", "student_id"}))
public class LectureProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long progressId;

    @ManyToOne
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;
}
