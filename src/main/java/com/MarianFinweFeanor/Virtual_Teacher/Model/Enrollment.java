package com.MarianFinweFeanor.Virtual_Teacher.Model;


import jakarta.persistence.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;




@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "average_grade", length = 10)
    private Double averageGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", length = 30, nullable = false)
    private CompletionStatus completionStatus = CompletionStatus.IN_PROGRESS;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();



    // getters/setters


    // --- getters/setters (write them or use Lombok @Getter/@Setter) ---
    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }
    public User getStudent() { return student; }
    public void setStudent(User student) {
        this.student = student;
    }
    public Course getCourse() { return course; }
    public void setCourse(Course course) {
        this.course = course; }

    public enum CompletionStatus { IN_PROGRESS, COMPLETED, DROPPED }
    public void setCompletionStatus(Enum completionStatus) {
        this.completionStatus = (CompletionStatus) completionStatus;
    }

    public double getAverageGrade() { return averageGrade;}
    public void setAverageGrade(Double averageGrade) {
        this.averageGrade = averageGrade;
    }

    public LocalDateTime getEnrolledAt() {return enrolledAt;}
    public void setEnrolledAt (LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    // equals/hashCode: ONLY on enrollmentId (or on student+course if you don’t use generated id),
    // and NEVER include collections to avoid recursion/StackOverflow.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enrollment)) return false;
        Enrollment other = (Enrollment) o;
        return enrollmentId != null && enrollmentId.equals(other.enrollmentId);
    }

    @Override
    public int hashCode() {
        return (enrollmentId != null ? enrollmentId.hashCode() : 0);
    }

}
