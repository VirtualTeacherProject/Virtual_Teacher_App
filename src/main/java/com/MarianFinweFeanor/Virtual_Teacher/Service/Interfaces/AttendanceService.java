package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.StudentAttendanceEntry;

import java.util.Collection;
import java.util.List;

/**
 * SCRUM-28: attendance tracker for lectures of courses that require attendance
 * (SCRUM-45). Since SCRUM-51 the roll call is taken only for live lectures, once
 * the session has started, and lists the students enrolled when it started.
 * Approved teachers and admins mark the absent students; every other listed
 * student is recorded as present. Self-paced lectures have no attendance.
 */
public interface AttendanceService {

    /**
     * The lecture, checked to belong to the course. Only approved teachers and
     * admins may use the tracker.
     */
    Lecture getLectureForAttendance(Long courseId, Long lectureId, String managerEmail);

    /**
     * Why attendance cannot be taken for the lecture right now, or null when it
     * can: the course must require attendance and the lecture must be a live
     * session that has started.
     */
    String attendanceClosedReason(Lecture lecture);

    /**
     * The roll call of the lecture, sorted by name, with each student's recorded
     * status (null when attendance has not been taken yet). For a live lecture
     * these are the students enrolled when the session started; otherwise every
     * student currently enrolled (so records from before SCRUM-51 stay readable).
     */
    List<AttendanceEntry> getLectureAttendance(Long courseId, Long lectureId, String managerEmail);

    /**
     * Records the roll call: the given students are absent, every other student
     * on the roll call is present. Saving again replaces the earlier record.
     *
     * @throws IllegalStateException    when attendance cannot be taken (see attendanceClosedReason)
     * @throws IllegalArgumentException when an absent id is not on the roll call
     */
    List<AttendanceEntry> recordAttendance(Long courseId, Long lectureId, String managerEmail,
                                           Collection<Long> absentStudentIds);

    /**
     * SCRUM-29: the enrolled student's own attendance in the enrolment's course,
     * in lecture order. Only that student's records are read. Lists every lecture
     * with a record, and, when the course requires attendance, the live lectures
     * still to come (upcoming) and those held since the student enrolled that have
     * no record yet. Empty for a null enrolment.
     */
    List<StudentAttendanceEntry> getOwnAttendance(Enrollment enrollment);
}
