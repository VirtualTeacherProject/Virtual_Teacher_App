package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class LectureServiceImpl implements LectureService {

    private LectureRepository lectureRepository;

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }
    @Override
    public Lecture saveLectures(Lecture lectures) {

        return lectureRepository.save(lectures);
    }


    // Get all lectures
    @Override
    public List<Lecture> getAllLectures() {
        return lectureRepository.findAll();
    }

    // Get a lecture by ID
    @Override
    public Optional<Lecture> getLecturesById(Long lectureId) {
        return lectureRepository.findById(lectureId);
    }


    @Override
    public void delete(long id) {
        lectureRepository.deleteById( id);
    }
}