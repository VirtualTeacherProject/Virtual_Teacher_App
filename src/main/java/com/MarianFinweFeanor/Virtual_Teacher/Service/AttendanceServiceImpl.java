package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceStatus;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureAttendance;
import com.MarianFinweFeanor.Virtual_Teacher.Model.StudentAttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureAttendanceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AttendanceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
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

    static final String NOT_REQUIRED = "This course does not require attendance";
    static final String NOT_LIVE = "Attendance is taken only for live sessions; this lecture is self-paced";
    static final String NOT_STARTED = "Attendance can be taken once the live session has started";
    static final String NOT_ON_ROLL_CALL =
            "Only students enrolled when the session started can be marked absent";

    private static final Comparator<User> BY_NAME = Comparator
            .comparing((User u) -> Objects.toString(u.getLastName(), ""), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(u -> Objects.toString(u.getFirstName(), ""), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(u -> Objects.toString(u.getEmail(), ""));

    private final LectureAttendanceRepository attendanceRepository;
    private final LectureService lectureService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;
    private final CourseAccessService courseAccessService;   // SCRUM-52

    @Autowired
    public AttendanceServiceImpl(LectureAttendanceRepository attendanceRepository,
                                 LectureService lectureService,
                                 EnrollmentService enrollmentService,
                                 UserService userService,
                                 CourseAccessService courseAccessService) {
        this.attendanceRepository = attendanceRepository;
        this.lectureService = lectureService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
        this.courseAccessService = courseAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public Lecture getLectureForAttendance(Long courseId, Long lectureId, String managerEmail) {
        userService.ensureApprovedTeacher(managerEmail);
        return managedLecture(courseId, lectureId, managerEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceEntry> getLectureAttendance(Long courseId, Long lectureId, String managerEmail) {
        userService.ensureApprovedTeacher(managerEmail);
        Lecture lecture = managedLecture(courseId, lectureId, managerEmail);
        Map<Long, LectureAttendance> records = recordsByStudent(lectureId);

        return roster(lecture).stream()
                .sorted(BY_NAME)
                .map(student -> {
                    LectureAttendance record = records.get(student.getUserId());
                    return new AttendanceEntry(student, record == null ? null : record.getStatus());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String attendanceClosedReason(Lecture lecture) {
        if (!lecture.getCourse().isAttendanceRequired()) {
            return NOT_REQUIRED;
        }
        if (!lecture.isLive()) {
            return NOT_LIVE;
        }
        if (!lecture.liveSessionStartedBy(LocalDateTime.now())) {
            return NOT_STARTED;
        }
        return null;
    }

    @Override
    public List<AttendanceEntry> recordAttendance(Long courseId, Long lectureId, String managerEmail,
                                                  Collection<Long> absentStudentIds) {
        userService.ensureApprovedTeacher(managerEmail);
        Lecture lecture = managedLecture(courseId, lectureId, managerEmail);

        String closed = attendanceClosedReason(lecture);
        if (closed != null) {
            throw new IllegalStateException(closed);
        }

        List<User> students = roster(lecture);
        Set<Long> enrolledIds = students.stream().map(User::getUserId).collect(Collectors.toSet());
        Set<Long> absent = absentStudentIds == null
                ? Set.of()
                : new HashSet<>(absentStudentIds);

        if (!enrolledIds.containsAll(absent)) {
            throw new IllegalArgumentException(NOT_ON_ROLL_CALL);
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

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceEntry> getOwnAttendance(Enrollment enrollment) {
        if (enrollment == null || enrollment.getStudent() == null || enrollment.getCourse() == null) {
            return List.of();
        }
        Long studentId = enrollment.getStudent().getUserId();
        Long courseId = enrollment.getCourse().getCourseId();
        boolean required = enrollment.getCourse().isAttendanceRequired();

        Map<Long, AttendanceStatus> own = attendanceRepository
                .findByStudent_UserIdAndLecture_Course_CourseId(studentId, courseId).stream()
                .collect(Collectors.toMap(r -> r.getLecture().getLectureId(), LectureAttendance::getStatus));

        if (!required && own.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime enrolledAt = enrollment.getEnrolledAt();

        return lectureService.getByCourseId(courseId).stream()
                .map(lecture -> {
                    AttendanceStatus status = own.get(lecture.getLectureId());
                    if (status != null) {
                        return new StudentAttendanceEntry(lecture, status, false);
                    }
                    if (!required || !lecture.isLive()) {
                        return null;
                    }
                    if (!lecture.liveSessionStartedBy(now)) {
                        return new StudentAttendanceEntry(lecture, null, true);
                    }
                    // A session held before the student enrolled was not theirs to attend.
                    if (enrolledAt != null && lecture.getLiveStart().isBefore(enrolledAt)) {
                        return null;
                    }
                    return new StudentAttendanceEntry(lecture, null, false);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Who is on the roll call: for a live lecture with a start time, the students
     * enrolled when the session started (later enrolments were not expected at it);
     * otherwise everyone enrolled now.
     */
    private List<User> roster(Lecture lecture) {
        Long courseId = lecture.getCourse().getCourseId();
        if (lecture.isLive() && lecture.getLiveStart() != null) {
            return enrollmentService.getStudentsInCourseEnrolledBy(courseId, lecture.getLiveStart());
        }
        return enrollmentService.getStudentsInCourse(courseId);
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

    /** SCRUM-52: the lecture of the course, for its own teacher or an admin. */
    private Lecture managedLecture(Long courseId, Long lectureId, String managerEmail) {
        Lecture lecture = lectureInCourse(courseId, lectureId);
        courseAccessService.ensureCanManageCourse(lecture.getCourse(), managerEmail);
        return lecture;
    }
}
