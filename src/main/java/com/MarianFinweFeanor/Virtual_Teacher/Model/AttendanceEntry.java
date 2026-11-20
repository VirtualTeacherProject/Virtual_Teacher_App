package com.MarianFinweFeanor.Virtual_Teacher.Model;

/**
 * SCRUM-28: one line of a lecture's roll call: an enrolled student and their
 * attendance, or a null status when attendance has not been taken yet.
 */
public record AttendanceEntry(User student, AttendanceStatus status) {

    public boolean isRecorded() {
        return status != null;
    }

    public boolean isAbsent() {
        return status == AttendanceStatus.ABSENT;
    }
}
