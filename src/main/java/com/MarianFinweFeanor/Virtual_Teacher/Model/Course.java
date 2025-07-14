package com.MarianFinweFeanor.Virtual_Teacher.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table (name = "courses")
public class Course {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "course_id")
    private Long courseId;

    @Column (name="title",nullable = false, length = 30)
    private String title;

    @Column (name="topic",nullable = false, length = 40)
    private String topic;

    @Column (name="description",length = 1000)
    private String description;

    @Column(name="start_date",nullable = false)
    private LocalDateTime startDate;

    @Column(name="status",nullable = false, length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToMany(mappedBy = "courses")
    @JsonIgnore
    private Set<User> users = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course other = (Course) o;   // cast to Course
        return courseId != null && courseId.equals(other.getCourseId());
    }

    @Override
    public int hashCode() {
        return (courseId != null ? courseId.hashCode() : 0);
    }


}
