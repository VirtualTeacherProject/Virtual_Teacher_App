package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureProgress;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureState;
import com.MarianFinweFeanor.Virtual_Teacher.Model.StudentProgress;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.AssignmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureProgressRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureProgressService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LectureProgressServiceImpl implements LectureProgressService {

    static final String NOT_ENROLLED = "You must be enrolled in this course to track your progress";
    static final String LOCKED = "Finish the earlier lectures of this course first";
    static final String SUBMIT_FIRST = "Submit the assignment of this lecture before marking it as finished";

    private static final Comparator<StudentProgress> BY_NAME = Comparator
            .comparing((StudentProgress p) -> Objects.toString(p.student().getLastName(), ""),
                    String.CASE_INSENSITIVE_ORDER)
            .thenComparing(p -> Objects.toString(p.student().getFirstName(), ""), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(p -> Objects.toString(p.student().getEmail(), ""));

    private final LectureProgressRepository progressRepository;
    private final LectureRepository lectureRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final CourseAccessService courseAccessService;

    @Autowired
    public LectureProgressServiceImpl(LectureProgressRepository progressRepository,
                                      LectureRepository lectureRepository,
                                      AssignmentRepository assignmentRepository,
                                      EnrollmentRepository enrollmentRepository,
                                      UserRepository userRepository,
                                      EnrollmentService enrollmentService,
                                      CourseAccessService courseAccessService) {
        this.progressRepository = progressRepository;
        this.lectureRepository = lectureRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.enrollmentService = enrollmentService;
        this.courseAccessService = courseAccessService;
    }

    @Override
    public void markFinished(Long courseId, Long lectureId, String studentEmail) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .filter(l -> l.getCourse() != null && courseId.equals(l.getCourse().getCourseId()))
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        if (!enrollmentService.isEnrolled(studentEmail, courseId)) {
            throw new IllegalStateException(NOT_ENROLLED);
        }
        if (progressRepository.existsByLecture_LectureIdAndStudent_Email(lectureId, studentEmail)) {
            return;
        }
        ensureUnlocked(lecture, studentEmail);

        if (lecture.isAssignmentRequired()
                && assignmentRepository
                .findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(lectureId, studentEmail)
                .isEmpty()) {
            throw new IllegalStateException(SUBMIT_FIRST);
        }

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("User", studentEmail));

        LectureProgress progress = new LectureProgress();
        progress.setLecture(lecture);
        progress.setStudent(student);
        progress.setFinishedAt(LocalDateTime.now());
        progressRepository.save(progress);

        enrollmentService.recalculateProgress(student.getUserId(), courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(Lecture lecture, String userEmail) {
        Long courseId = lecture.getCourse().getCourseId();
        return getLectureStates(courseId, userEmail).stream()
                .filter(state -> state.lecture().getLectureId().equals(lecture.getLectureId()))
                .findFirst()
                .map(LectureState::locked)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureUnlocked(Lecture lecture, String userEmail) {
        if (isLocked(lecture, userEmail)) {
            throw new IllegalStateException(LOCKED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureState> getLectureStates(Long courseId, String userEmail) {
        List<Lecture> lectures =
                lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(courseId);

        Set<Long> finished = userEmail == null ? Set.of()
                : progressRepository.findByStudent_EmailAndLecture_Course_CourseId(userEmail, courseId).stream()
                .map(p -> p.getLecture().getLectureId())
                .collect(Collectors.toSet());
        Set<Long> graded = userEmail == null || finished.isEmpty() ? Set.of()
                : assignmentRepository.findByStudent_EmailOrderBySubmittedAtDesc(userEmail).stream()
                .filter(a -> a.getGrade() != null && a.getLecture() != null)
                .map(Assignment::getLecture)
                .map(Lecture::getLectureId)
                .collect(Collectors.toSet());

        boolean inOrder = !lectures.isEmpty()
                && lectures.get(0).getCourse().isLecturesInOrder()
                && !courseAccessService.canManageCourses(userEmail);

        List<LectureState> states = new ArrayList<>();
        boolean earlierUnfinished = false;
        for (Lecture lecture : lectures) {
            boolean isFinished = finished.contains(lecture.getLectureId());
            boolean isCompleted = isFinished
                    && (!lecture.isAssignmentRequired() || graded.contains(lecture.getLectureId()));
            states.add(new LectureState(lecture, isFinished, isCompleted, inOrder && earlierUnfinished));
            earlierUnfinished |= !isFinished;
        }
        return states;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentProgress> getCourseProgress(Long courseId) {
        long total = lectureRepository.countLecturesByCourseId(courseId);

        return enrollmentRepository.findByCourse_CourseId(courseId).stream()
                .map(e -> toProgress(e, courseId, total))
                .sorted(BY_NAME)
                .toList();
    }

    private StudentProgress toProgress(Enrollment enrollment, Long courseId, long total) {
        User student = enrollment.getStudent();
        long finished = progressRepository
                .findByStudent_UserIdAndLecture_Course_CourseId(student.getUserId(), courseId).size();
        long completed = progressRepository.countCompleted(student.getUserId(), courseId);
        return new StudentProgress(student, finished, completed, total, enrollment.getCompletionStatus());
    }
}
