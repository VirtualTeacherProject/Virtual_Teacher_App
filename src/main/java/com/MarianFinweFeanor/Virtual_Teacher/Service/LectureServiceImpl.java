package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository, CourseRepository courseRepository) {
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
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
        return lectureRepository.save(lecture);
        //TODO FILTERING EXCEPTION HANDLING CHECK IF LECTURE WITH THE SAME NAME EXISTS
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
    public void delete(long id) {
        if (!lectureRepository.existsById(id)) {
            throw new EntityNotFoundException("Lecture", id);
        }else {
            lectureRepository.deleteById(id);
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
                    return lectureRepository.save(lecture);
                })
                .orElseThrow(() -> new EntityNotFoundException("Lecture", id));
    }
}