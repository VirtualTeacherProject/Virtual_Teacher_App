package com.MarianFinweFeanor.Virtual_Teacher.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @Column(name="column_name",nullable = false) //look up maybe wrong name
    private LocalDateTime startDate;

    @Column(name="status",nullable = false, length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnore
    private User teacher;

    //Getters and Setters
    public Course() {}

    public Course(String title, String topic, String description, LocalDateTime startDate,
                  String status, User teacher)
    {
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
        this.teacher = teacher;
    }

    // Getters and Setters
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return  title;}
    public void setTitle  (String title) {this.title = title; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) {this.topic = topic; }

    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Course course = (Course) obj;
        return courseId == course.courseId;
    }


}
