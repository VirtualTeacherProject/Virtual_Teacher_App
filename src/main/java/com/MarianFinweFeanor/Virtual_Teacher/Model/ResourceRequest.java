package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SCRUM-35: a student's request for an additional course resource.
 * Seen only by the student who wrote it and by course managers.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "resource_requests")
public class ResourceRequest {

    public static final int MIN_MESSAGE_LENGTH = 100;
    public static final int MAX_MESSAGE_LENGTH = 2000;

    /** Only OPEN is used so far; SCRUM-36 adds the states teachers set. */
    public static final String STATUS_OPEN = "OPEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "message", nullable = false, length = MAX_MESSAGE_LENGTH)
    private String message;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_OPEN;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
