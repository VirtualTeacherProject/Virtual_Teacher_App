package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SCRUM-48: a course another course builds on. A required prerequisite has the
 * same topic and must be completed before enrolling; a recommended one can have
 * any topic and is only shown on the course page.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "course_prerequisites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_prerequisites_pair",
                columnNames = {"course_id", "required_course_id"}))
public class CoursePrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prerequisite_id")
    private Long prerequisiteId;

    /** The course that has the prerequisite. */
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** The course to take first. */
    @ManyToOne
    @JoinColumn(name = "required_course_id", nullable = false)
    private Course prerequisiteCourse;

    /** True: blocks enrolment until completed. False: only recommended. */
    @Column(name = "required", nullable = false)
    private boolean required;
}
