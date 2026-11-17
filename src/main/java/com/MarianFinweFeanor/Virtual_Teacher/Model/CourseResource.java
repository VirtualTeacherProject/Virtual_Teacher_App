package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SCRUM-34: an additional resource (a link) that a teacher attaches to a course.
 * Visible only to users who can access the course content.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "course_resources")
public class CourseResource {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_URL_LENGTH = 500;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long resourceId;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "url", nullable = false, length = MAX_URL_LENGTH)
    private String url;

    @Column(name = "description", length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
