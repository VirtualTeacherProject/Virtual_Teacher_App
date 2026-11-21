package com.MarianFinweFeanor.Virtual_Teacher.Model;

/** SCRUM-51 part B: one row of a course's progress page. */
public record StudentProgress(User student, long finished, long completed, long totalLectures,
                              Enrollment.CompletionStatus status) {
}
