package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import java.util.Collection;
import java.util.List;

/**
 * SCRUM-28: attendance tracker for lectures of courses that require attendance
 * (SCRUM-45). Approved teachers and admins take the roll call by marking the
 * absent students; every other enrolled student is recorded as present.
 */
public interface AttendanceService {

    /**
     * The lecture, checked to belong to the course. Only approved teachers and
     * admins may use the tracker.
     */
    Lecture getLectureForAttendance(Long courseId, Long lectureId, String managerEmail);

    /**
     * The roll call of the lecture: every student currently enrolled in the
     * course, sorted by name, with their recorded status (null when attendance
     * has not been taken yet).
     */
    List<AttendanceEntry> getLectureAttendance(Long courseId, Long lectureId, String managerEmail);

    /**
     * Records the roll call: the given students are absent, every other enrolled
     * student is present. Saving again replaces the earlier record.
     *
     * @throws IllegalStateException    when the course does not require attendance
     * @throws IllegalArgumentException when an absent id is not an enrolled student
     */
    List<AttendanceEntry> recordAttendance(Long courseId, Long lectureId, String managerEmail,
                                           Collection<Long> absentStudentIds);
}
