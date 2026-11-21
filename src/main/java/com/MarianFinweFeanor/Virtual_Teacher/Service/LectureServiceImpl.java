package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ContentCleanupService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Service
public class LectureServiceImpl implements LectureService {

    public static final String LIVE_START_REQUIRED = "A live session needs a start date and time";

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final ContentCleanupService contentCleanupService;
    private final EnrollmentService enrollmentService;

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository,
                              CourseRepository courseRepository,
                              ContentCleanupService contentCleanupService,
                              EnrollmentService enrollmentService) {
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
        this.contentCleanupService = contentCleanupService;
        this.enrollmentService = enrollmentService;
    }

    public Lecture saveLecture(Lecture lecture) {
        return lectureRepository.save(lecture);
    }

    @Override
    public Lecture createLectures(Lecture lecture) {
        Long courseId = lecture.getCourse().getCourseId();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        lecture.setCourse(course); // Attach managed entity
        normaliseLiveSession(lecture);
        // SCRUM-51 part B: a new lecture goes to the end of the course
        lecture.setLectureOrder(lectureRepository.maxLectureOrder(courseId) + 1);
        Lecture saved = lectureRepository.save(lecture);
        // a course a student had completed now has one more lecture to do
        enrollmentService.recalculateProgressForCourse(courseId);
        return saved;
        //TODO FILTERING EXCEPTION HANDLING CHECK IF LECTURE WITH THE SAME NAME EXISTS
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lecture> getByCourseId(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        return lectureRepository.findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(course.getCourseId());
        // or, if you prefer: lectureRepository.findAllByCourse_CourseId(courseId)
    }


    // Get all lectures
    @Override
    public List<Lecture> getAllLectures() {
        if(lectureRepository.count() == 0) {
            throw new EntityNotFoundException("Lectures", "database");
        }
        return lectureRepository.findAll();
    }

    // Get a lecture by ID
    @Override
    public Optional<Lecture> getLecturesById(Long lectureId) {
        if(!lectureRepository.existsById(lectureId)) {
            throw new EntityNotFoundException("Lecture", lectureId);
        }
        return lectureRepository.findById(lectureId);
    }


    @Override
    @Transactional
    public void delete(long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", id));

        // SCRUM-15: submissions and comments belong to the lecture and go with it.
        contentCleanupService.deleteLectureContent(id);
        lectureRepository.delete(lecture);

        // Grades and completion depend on the course's lectures, so refresh them.
        if (lecture.getCourse() != null) {
            enrollmentService.recalculateProgressForCourse(lecture.getCourse().getCourseId());
        }
    }
    @Override
    public Lecture updateLecture(Long id, Lecture updatedLecture) {
        return lectureRepository.findById(id)
                .map(lecture -> {
                    lecture.setTitle(updatedLecture.getTitle());
                    lecture.setDescription(updatedLecture.getDescription());
                    lecture.setVideoUrl(updatedLecture.getVideoUrl());
                    lecture.setAssignmentFilePath(updatedLecture.getAssignmentFilePath());
                    lecture.setLive(updatedLecture.isLive());
                    lecture.setLiveStart(updatedLecture.getLiveStart());
                    lecture.setMeetingUrl(updatedLecture.getMeetingUrl());
                    normaliseLiveSession(lecture);
                    boolean assignmentChanged =
                            lecture.isAssignmentRequired() != updatedLecture.isAssignmentRequired();
                    lecture.setAssignmentRequired(updatedLecture.isAssignmentRequired());
                    Lecture saved = lectureRepository.save(lecture);
                    // SCRUM-51 part B: completion depends on whether the lecture has an assignment
                    if (assignmentChanged && lecture.getCourse() != null) {
                        enrollmentService.recalculateProgressForCourse(lecture.getCourse().getCourseId());
                    }
                    return saved;
                })
                .orElseThrow(() -> new EntityNotFoundException("Lecture", id));
    }

    @Override
    @Transactional
    public void moveLecture(Long lectureId, boolean up) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));
        List<Lecture> lectures = lectureRepository
                .findAllByCourse_CourseIdOrderByLectureOrderAscLectureIdAsc(lecture.getCourse().getCourseId());

        int index = -1;
        for (int i = 0; i < lectures.size(); i++) {
            if (lectures.get(i).getLectureId().equals(lectureId)) {
                index = i;
            }
        }
        int other = up ? index - 1 : index + 1;
        if (index < 0 || other < 0 || other >= lectures.size()) {
            return;
        }

        // Renumber 1..n so duplicate or missing positions are repaired as well.
        Lecture neighbour = lectures.get(other);
        lectures.set(other, lectures.get(index));
        lectures.set(index, neighbour);
        for (int i = 0; i < lectures.size(); i++) {
            lectures.get(i).setLectureOrder(i + 1);
        }
        lectureRepository.saveAll(lectures);
    }

    /**
     * SCRUM-51 part A: a live lecture needs a start time; a self-paced lecture
     * has neither a start time nor a meeting link.
     */
    private static void normaliseLiveSession(Lecture lecture) {
        if (lecture.getMeetingUrl() != null && lecture.getMeetingUrl().isBlank()) {
            lecture.setMeetingUrl(null);
        }
        if (!lecture.isLive()) {
            lecture.setLiveStart(null);
            lecture.setMeetingUrl(null);
        } else if (lecture.getLiveStart() == null) {
            throw new IllegalArgumentException(LIVE_START_REQUIRED);
        }
    }
}
