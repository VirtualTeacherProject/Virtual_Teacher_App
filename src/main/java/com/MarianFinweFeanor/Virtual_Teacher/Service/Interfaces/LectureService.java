package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import java.util.List;
import java.util.Optional;

public interface LectureService {
    Lecture saveLecture(Lecture lecture);
    Lecture createLectures(Lecture lecture);

    // Get all lectures
    List<Lecture> getAllLectures();
    List<Lecture> getByCourseId(Long courseId);


    // Get a lecture by ID
    Optional<Lecture> getLecturesById(Long lectureId);

    void delete(long id);

    Lecture updateLecture(Long id, Lecture updatedLecture);

    /**
     * SCRUM-51 part B: swaps the lecture with the one before it (up) or after it
     * (down) in its course. Does nothing at either end of the list.
     */
    void moveLecture(Long lectureId, boolean up);

    //List<Lecture> getByCourseId(Long courseId);
}
