package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * SCRUM-20: a user's private plain-text note for one lecture.
 * There is at most one note per user per lecture.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lecture_notes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_notes_lecture_user",
                columnNames = {"lecture_id", "user_id"}))
public class LectureNote {

    public static final int MAX_LENGTH = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, length = MAX_LENGTH)
    private String content;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
