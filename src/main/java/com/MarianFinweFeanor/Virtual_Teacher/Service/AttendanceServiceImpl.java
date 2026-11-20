package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceStatus;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureAttendance;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureAttendanceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AttendanceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final Comparator<User> BY_NAME = Comparator
            .comparing((User u) -> Objects.toString(u.getLastName(), ""), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(u -> Objects.toString(u.getFirstName(), ""), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(u -> Objects.toString(u.getEmail(), ""));

    private final LectureAttendanceRepository attendanceRepository;
    private final LectureService lectureService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Autowired
    public AttendanceServiceImpl(LectureAttendanceRepository attendanceRepository,
                                 LectureService lectureService,
                                 EnrollmentService enrollmentService,
                                 UserService userService) {
        this.attendanceRepository = attendanceRepository;
        this.lectureService = lectureService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public Lecture getLectureForAttendance(Long courseId, Long lectureId, String managerEmail) {
        userService.ensureApprovedTeacher(managerEmail);
        return lectureInCourse(courseId, lectureId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceEntry> getLectureAttendance(Long courseId, Long lectureId, String managerEmail) {
        userService.ensureApprovedTeacher(managerEmail);
        lectureInCourse(courseId, lectureId);
        return rollCall(courseId, lectureId);
    }

    @Override
    public List<AttendanceEntry> recordAttendance(Long courseId, Long lectureId, String managerEmail,
                                                  Collection<Long> absentStudentIds) {
        userService.ensureApprovedTeacher(managerEmail);
        Lecture lecture = lectureInCourse(courseId, lectureId);

        if (!lecture.getCourse().isAttendanceRequired()) {
            throw new IllegalStateException("This course does not require attendance");
        }

        List<User> students = enrollmentService.getStudentsInCourse(courseId);
        Set<Long> enrolledIds = students.stream().map(User::getUserId).collect(Collectors.toSet());
        Set<Long> absent = absentStudentIds == null
                ? Set.of()
                : new HashSet<>(absentStudentIds);

        if (!enrolledIds.containsAll(absent)) {
            throw new IllegalArgumentException("Only students enrolled in this course can be marked absent");
        }

        Map<Long, LectureAttendance> existing = recordsByStudent(lectureId);
        LocalDateTime now = LocalDateTime.now();

        List<LectureAttendance> records = students.stream()
                .map(student -> {
                    LectureAttendance record = existing.get(student.getUserId());
                    if (record == null) {
                        record = new LectureAttendance();
                        record.setLecture(lecture);
                        record.setStudent(student);
                    }
                    record.setStatus(absent.contains(student.getUserId())
                            ? AttendanceStatus.ABSENT
                            : AttendanceStatus.PRESENT);
                    record.setRecordedAt(now);
                    return record;
                })
                .toList();

        attendanceRepository.saveAll(records);

        return students.stream()
                .sorted(BY_NAME)
                .map(student -> new AttendanceEntry(student,
                        absent.contains(student.getUserId())
                                ? AttendanceStatus.ABSENT
                                : AttendanceStatus.PRESENT))
                .toList();
    }

    private List<AttendanceEntry> rollCall(Long courseId, Long lectureId) {
        Map<Long, LectureAttendance> records = recordsByStudent(lectureId);

        return enrollmentService.getStudentsInCourse(courseId).stream()
                .sorted(BY_NAME)
                .map(student -> {
                    LectureAttendance record = records.get(student.getUserId());
                    return new AttendanceEntry(student, record == null ? null : record.getStatus());
                })
                .toList();
    }

    private Map<Long, LectureAttendance> recordsByStudent(Long lectureId) {
        return attendanceRepository.findByLecture_LectureId(lectureId).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getUserId(), Function.identity()));
    }

    private Lecture lectureInCourse(Long courseId, Long lectureId) {
        Lecture lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        // The course in the URL must be the lecture's course.
        if (lecture.getCourse() == null || !lecture.getCourse().getCourseId().equals(courseId)) {
            throw new EntityNotFoundException("Lecture", lectureId);
        }
        return lecture;
    }
}
