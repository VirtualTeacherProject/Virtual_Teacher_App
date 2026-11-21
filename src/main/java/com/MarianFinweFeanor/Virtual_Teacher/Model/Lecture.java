package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@Entity
@Table(name = "lectures")
public class    Lecture {
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

    // --- SCRUM-51 part B: order and completion --------------------------------

    /** Position of the lecture in its course, starting at 1. */
    @Column(name = "lecture_order", nullable = false)
    private Integer lectureOrder = 0;   // createLectures puts a new lecture at the end

    /** The lecture has an assignment: it is completed only once that is graded. */
    @Column(name = "assignment_required", nullable = false)
    private boolean assignmentRequired;

    // --- SCRUM-51 part A: live sessions ---------------------------------------

    /** A live session given by the teacher at liveStart; otherwise the lecture is self-paced. */
    @Column(name = "live", nullable = false)
    private boolean live;

    /** When the live session starts; required for live lectures, null otherwise. */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "live_start")
    private LocalDateTime liveStart;

    /** Optional link to the online meeting of a live session. */
    @Size(max = 500)
    @Pattern(regexp = "^(https?://\\S+)?$", message = "The meeting link must start with http:// or https://")
    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;

    /** True for a live lecture whose session has started by the given time. */
    public boolean liveSessionStartedBy(LocalDateTime time) {
        return live && liveStart != null && !liveStart.isAfter(time);
    }
}