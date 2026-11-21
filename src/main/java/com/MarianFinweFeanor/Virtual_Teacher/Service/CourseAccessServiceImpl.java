package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseAccessServiceImpl implements CourseAccessService {

    private static final String PUBLISHED = "PUBLISHED";

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Autowired
    public CourseAccessServiceImpl(UserRepository userRepository,
                                   EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public boolean canManageCourses(String userEmail) {
        if (userEmail == null) {
            return false;
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        return user.getRole() == UserRole.TEACHER && user.isTeacherApproved();
    }

    @Override
    public boolean canViewCourse(Course course, String userEmail) {
        if (course == null) {
            return false;
        }

        return isPublished(course) || canManageCourses(userEmail);
    }

    @Override
    public boolean canAccessCourseContent(Course course, String userEmail) {
        if (course == null || userEmail == null) {
            return false;
        }

        if (canManageCourses(userEmail)) {
            return true;
        }

        return isPublished(course)
                && enrollmentRepository.existsByStudent_EmailAndCourse_CourseId(
                        userEmail, course.getCourseId());
    }

    @Override
    public void ensureCanViewCourse(Course course, String userEmail) {
        if (!canViewCourse(course, userEmail)) {
            // Same answer as a missing course, so drafts are not revealed.
            throw new EntityNotFoundException(
                    "Course",
                    course == null || course.getCourseId() == null ? 0L : course.getCourseId());
        }
    }

    @Override
    public void ensureCanManageCourse(Course course, String userEmail) {
        // TODO(course ownership): also require course.getTeacher() to be the user, unless admin.
        if (course == null || !canManageCourses(userEmail)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the course's teachers and admins can do this.");
        }
    }

    private boolean isPublished(Course course) {
        return PUBLISHED.equals(course.getStatus());
    }
}
