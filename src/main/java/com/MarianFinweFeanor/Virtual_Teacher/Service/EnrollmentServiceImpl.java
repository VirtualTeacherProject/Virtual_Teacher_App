package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.*;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final AssignmentRepository assignmentRepo;
    private final LectureRepository lectureRepository;

    @Autowired
    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepo,
                                 UserRepository userRepo,
                                 AssignmentRepository assignmentRepo,
                                 LectureRepository lectureRepository,
                                 CourseRepository courseRepo) {
        this.enrollmentRepo = enrollmentRepo;
        this.userRepo       = userRepo;
        this.courseRepo     = courseRepo;
        this.assignmentRepo = assignmentRepo;
        this.lectureRepository = lectureRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getEnrolledCourseIds(String userEmail) {
        //return enrollmentRepo.findEnrolledCourseIdsByEmail(userEmail);

        return enrollmentRepo.findByStudent_Email(userEmail).stream()
                .map(e -> e.getCourse().getCourseId())
                .collect(Collectors.toSet());
    }

    @Override
    public void ensureStudentEnrolled(String studentEmail, Long courseId) {
        enrollmentRepo.findByStudent_EmailAndCourse_CourseId(studentEmail, courseId)
                .orElseThrow(() -> new IllegalStateException("You must be enrolled in this course to comment."));
    }
    // add later for if student was dropped


    @Override
    @Transactional(readOnly = true)
    public Set<Course> getEnrolledCourses(String userEmail) {
        return enrollmentRepo.findByStudent_Email(userEmail)
                .stream()
                .map(Enrollment::getCourse)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getStudentsInCourse(Long courseId) {
        return enrollmentRepo.findStudentsByCourseId(courseId);
    }

    @Override
    public List<User> getStudentsInCourseEnrolledBy(Long courseId, LocalDateTime time) {
        return enrollmentRepo.findStudentsByCourseIdEnrolledBy(courseId, time);
    }

    @Override
    public void recalculateProgress(Long studentUserId, Long courseId) {

        Enrollment enrollment = enrollmentRepo.findByStudent_UserIdAndCourse_CourseId
                (studentUserId, courseId).orElseThrow(() ->
                new RuntimeException("Enrollment not found"));

        // 1) Average grade for course (null if no graded submissions yet)
        Double avg = assignmentRepo.avgGradeForCourse(studentUserId, courseId);
        enrollment.setAverageGrade(avg);

        // 2) Completion status
        long totalLectures = lectureRepository.countLecturesByCourseId(courseId);
        long gradedLectures = assignmentRepo.countGradedLectures(studentUserId, courseId);

        Enrollment.CompletionStatus status;

        if (totalLectures == 0)
        {
            status = Enrollment.CompletionStatus.IN_PROGRESS;
        }
        else if (gradedLectures >= totalLectures)
        {
            status = Enrollment.CompletionStatus.COMPLETED;
        }
        else
        {
            status = Enrollment.CompletionStatus.IN_PROGRESS;
        }

        enrollment.setCompletionStatus(status);

        enrollmentRepo.save(enrollment);

    }

    @Override
    public void recalculateProgressForCourse(Long courseId) {
        for (Enrollment enrollment : enrollmentRepo.findByCourse_CourseId(courseId)) {
            recalculateProgress(enrollment.getStudent().getUserId(), courseId);
        }
    }

    @Override
    public void ensureCanRateCourse(Long studentUserId, Long courseId) {
        Enrollment e = enrollmentRepo.findByStudent_UserIdAndCourse_CourseId(studentUserId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        if (e.getAverageGrade() == null) {
            throw new IllegalStateException("You can rate only after your course grade is calculated.");
        }

        if (e.getCompletionStatus() != Enrollment.CompletionStatus.COMPLETED) {
            throw new IllegalStateException("You can rate only after completing all lectures.");
        }

        double passingGrade = e.getCourse().getPassingGrade() != null
                ? e.getCourse().getPassingGrade()
                : 50.0;

            if (e.getAverageGrade() < passingGrade) {
                throw new IllegalStateException("Your grade is: " + e.getAverageGrade() +  " you can rate only after passing the course.");
            }
    }

    @Override
    public void rateCourse(String studentEmail, Long courseId, Integer rating, String reviewText) {
        Enrollment e = enrollmentRepo.findByStudent_EmailAndCourse_CourseId(studentEmail, courseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Enrollment for user", studentEmail + " & course " + courseId));

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        String review = reviewText == null ? "" : reviewText.trim();

        if (review.length() < MIN_REVIEW_LENGTH) {
            throw new IllegalArgumentException(
                    "Please write a review of at least " + MIN_REVIEW_LENGTH
                            + " characters (you wrote " + review.length() + ").");
        }

        if (review.length() > MAX_REVIEW_LENGTH) {
            throw new IllegalArgumentException(
                    "A review can have at most " + MAX_REVIEW_LENGTH + " characters.");
        }

        ensureCanRateCourse(e.getStudent().getUserId(), courseId);

        e.setRating(rating);
        e.setReviewText(review);
        e.setRatedAt(LocalDateTime.now());
        enrollmentRepo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getCourseReviews(Long courseId) {
        return enrollmentRepo.findByCourse_CourseIdAndRatingIsNotNullOrderByRatedAtDesc(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long courseId) {
        return enrollmentRepo.averageRatingForCourse(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(String userEmail, Long courseId) {
        return enrollmentRepo.existsByStudent_EmailAndCourse_CourseId(userEmail, courseId);
    }

    @Override
    public void enroll(String userEmail, Long courseId) {
        if (isEnrolled(userEmail, courseId)) return; // already enrolled

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User", userEmail));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        Enrollment e = new Enrollment();
        e.setStudent(user);
        e.setCourse(course);
        e.setCompletionStatus(Enrollment.CompletionStatus.IN_PROGRESS);  // defaults; adjust to your enum/strings
        e.setAverageGrade(null);

        enrollmentRepo.save(e);
    }

    @Override
    public void unenroll(String userEmail, Long courseId) {
        Enrollment e = enrollmentRepo.findByStudent_EmailAndCourse_CourseId(userEmail, courseId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment for user", userEmail + " & course " + courseId));
        enrollmentRepo.delete(e);
    }
}
