package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SCRUM-28: whether an enrolled student attended a lecture of a course that
 * requires attendance. There is at most one record per student per lecture.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lecture_attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_attendance_lecture_student",
                columnNames = {"lecture_id", "student_id"}))
public class LectureAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
