package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CoursePrerequisiteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CoursePrerequisiteService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SCRUM-48. Depends only on repositories and CourseAccessService, so
 * EnrollmentServiceImpl can use it without a dependency cycle.
 */
@Service
@Transactional
public class CoursePrerequisiteServiceImpl implements CoursePrerequisiteService {

    static final String SELF = "A course cannot be its own prerequisite.";
    static final String NOT_PUBLISHED = "Only a published course can be a prerequisite.";
    static final String ALREADY_LINKED = "This course is already a prerequisite.";
    static final String OTHER_TOPIC =
            "A required prerequisite must have the same topic. Add it as recommended instead.";
    static final String CYCLE =
            "These courses would require each other, so nobody could enrol in either.";

    private static final Comparator<Course> BY_TITLE =
            Comparator.comparing(c -> Objects.toString(c.getTitle(), ""), String.CASE_INSENSITIVE_ORDER);

    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseAccessService courseAccessService;

    @Autowired
    public CoursePrerequisiteServiceImpl(CoursePrerequisiteRepository prerequisiteRepository,
                                         CourseRepository courseRepository,
                                         EnrollmentRepository enrollmentRepository,
                                         CourseAccessService courseAccessService) {
        this.prerequisiteRepository = prerequisiteRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseAccessService = courseAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoursePrerequisite> getPrerequisites(Long courseId) {
        return prerequisiteRepository.findByCourse_CourseId(courseId).stream()
                .sorted(Comparator.comparing((CoursePrerequisite p) -> !p.isRequired())
                        .thenComparing(CoursePrerequisite::getPrerequisiteCourse, BY_TITLE))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> getCandidates(Long courseId) {
        Set<Long> linked = prerequisiteRepository.findByCourse_CourseId(courseId).stream()
                .map(p -> p.getPrerequisiteCourse().getCourseId())
                .collect(Collectors.toSet());
        return courseRepository.findByStatus("PUBLISHED").stream()
                .filter(c -> !c.getCourseId().equals(courseId) && !linked.contains(c.getCourseId()))
                .sorted(BY_TITLE)
                .toList();
    }

    @Override
    public CoursePrerequisite addPrerequisite(Long courseId, Long prerequisiteCourseId, boolean required,
                                              String managerEmail) {
        Course course = managedCourse(courseId, managerEmail);

        if (courseId.equals(prerequisiteCourseId)) {
            throw new IllegalArgumentException(SELF);
        }
        Course prerequisite = courseRepository.findById(prerequisiteCourseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", prerequisiteCourseId));

        if (!"PUBLISHED".equals(prerequisite.getStatus())) {
            throw new IllegalArgumentException(NOT_PUBLISHED);
        }
        if (prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(
                courseId, prerequisiteCourseId).isPresent()) {
            throw new IllegalArgumentException(ALREADY_LINKED);
        }
        if (required) {
            if (!sameTopic(course, prerequisite)) {
                throw new IllegalArgumentException(OTHER_TOPIC);
            }
            if (requiresTransitively(prerequisiteCourseId, courseId)) {
                throw new IllegalArgumentException(CYCLE);
            }
        }

        CoursePrerequisite link = new CoursePrerequisite();
        link.setCourse(course);
        link.setPrerequisiteCourse(prerequisite);
        link.setRequired(required);
        return prerequisiteRepository.save(link);
    }

    @Override
    public void removePrerequisite(Long courseId, Long prerequisiteCourseId, String managerEmail) {
        managedCourse(courseId, managerEmail);
        CoursePrerequisite link = prerequisiteRepository
                .findByCourse_CourseIdAndPrerequisiteCourse_CourseId(courseId, prerequisiteCourseId)
                .orElseThrow(() -> new EntityNotFoundException("Prerequisite", prerequisiteCourseId));
        prerequisiteRepository.delete(link);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> getMissingRequired(Long courseId, String userEmail) {
        return prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(courseId).stream()
                .map(CoursePrerequisite::getPrerequisiteCourse)
                .filter(required -> userEmail == null || !hasCompleted(userEmail, required.getCourseId()))
                .sorted(BY_TITLE)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void ensurePrerequisitesMet(String userEmail, Long courseId) {
        List<Course> missing = getMissingRequired(courseId, userEmail);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Complete these courses before enrolling: "
                    + missing.stream().map(Course::getTitle).collect(Collectors.joining(", ")) + ".");
        }
    }

    private boolean hasCompleted(String userEmail, Long courseId) {
        return enrollmentRepository.findByStudent_EmailAndCourse_CourseId(userEmail, courseId)
                .map(e -> e.getCompletionStatus() == Enrollment.CompletionStatus.COMPLETED)
                .orElse(false);
    }

    /** True when start already requires target through a chain of required links. */
    private boolean requiresTransitively(Long start, Long target) {
        Set<Long> seen = new HashSet<>();
        Deque<Long> todo = new ArrayDeque<>(List.of(start));
        while (!todo.isEmpty()) {
            Long current = todo.pop();
            if (!seen.add(current)) {
                continue;
            }
            for (CoursePrerequisite link : prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(current)) {
                Long next = link.getPrerequisiteCourse().getCourseId();
                if (next.equals(target)) {
                    return true;
                }
                todo.push(next);
            }
        }
        return false;
    }

    private static boolean sameTopic(Course a, Course b) {
        return a.getTopic() != null && b.getTopic() != null
                && a.getTopic().trim().equalsIgnoreCase(b.getTopic().trim());
    }

    /** SCRUM-52: only the course's own teacher or an admin. */
    private Course managedCourse(Long courseId, String managerEmail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        courseAccessService.ensureCanManageCourse(course, managerEmail);
        return course;
    }
}
