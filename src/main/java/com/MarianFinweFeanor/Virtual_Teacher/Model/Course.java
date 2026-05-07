package com.MarianFinweFeanor.Virtual_Teacher.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Entity
@Table (name = "courses")
public class Course {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "course_id")
    private Long courseId;

    @Column (name="title",nullable = false, length = 100)
    private String title;

    @Column (name="topic",nullable = false, length = 250)
    private String topic;

    @Column (name="description",length = 1000)
    private String description;

    @Column(name = "passing_grade")
    private Double passingGrade = 50.0;

//    @Column(name="start_date",nullable = false)
//    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // lets Spring parse the html5 datetime-local
    private LocalDateTime startDate;

    @jakarta.validation.constraints.Pattern(regexp = "ACTIVE|PASSIVE", message = "Status must be ACTIVE or PASSIVE")
    @Column(name="status",nullable = false, length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;


    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Enrollment> enrollments = new HashSet<>();

    public Set<User> getStudents() {
        return enrollments.stream()
                .map(Enrollment::getStudent)   // requires Enrollment#getStudent()
                .collect(java.util.stream.Collectors.toSet());
    }

    public Double getPassingGrade() {
        return passingGrade;
    }

    public void setPassingGrade(Double passingGrade) {
        this.passingGrade = passingGrade;
    }

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
