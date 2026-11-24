package com.MarianFinweFeanor.Virtual_Teacher.Model;

/**
 * SCRUM-29: one line of a student's own attendance in a course: a lecture and
 * the student's recorded status, or a null status when none was recorded.
 * upcoming is true for a live session that has not started yet.
 */
public record StudentAttendanceEntry(Lecture lecture, AttendanceStatus status, boolean upcoming) {

    public boolean isRecorded() {
        return status != null;
    }

    public boolean isAbsent() {
        return status == AttendanceStatus.ABSENT;
    }

    public boolean isPresent() {
        return status == AttendanceStatus.PRESENT;
    }
}
