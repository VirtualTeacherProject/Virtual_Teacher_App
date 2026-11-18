package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SCRUM-35: a student's request for an additional course resource.
 * Seen only by the student who wrote it, by course managers and (SCRUM-36)
 * by the student a teacher assigned it to.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "resource_requests")
public class ResourceRequest {

    public static final int MIN_MESSAGE_LENGTH = 100;
    public static final int MAX_MESSAGE_LENGTH = 2000;

    public static final String STATUS_OPEN = "OPEN";
    // SCRUM-36: the states a teacher sets
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_IRRELEVANT = "IRRELEVANT";
    public static final List<String> STATUSES =
            List.of(STATUS_OPEN, STATUS_IN_PROGRESS, STATUS_RESOLVED, STATUS_IRRELEVANT);

    /** SCRUM-36: the assigned student's answer. */
    public static final int MAX_RESPONSE_LENGTH = 2000;

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

    /** SCRUM-36: the student the teacher asked to find the resources, if any. */
    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /** SCRUM-36: the assignee's answer (text, links and notes). */
    @Column(name = "response", length = MAX_RESPONSE_LENGTH)
    private String response;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /** RESOLVED and IRRELEVANT requests can no longer be answered. */
    public boolean isClosed() {
        return STATUS_RESOLVED.equals(status) || STATUS_IRRELEVANT.equals(status);
    }

    /** Removes the assignee and their answer; an IN_PROGRESS request goes back to OPEN. */
    public void clearAssignment() {
        assignee = null;
        response = null;
        respondedAt = null;
        if (STATUS_IN_PROGRESS.equals(status)) {
            status = STATUS_OPEN;
        }
    }
}
