package com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LecturesRestController {
    private final LectureService lectureService;
    @Autowired
    public LecturesRestController(LectureService lectureService) {
        this.lectureService = lectureService;
    }
    @GetMapping
    public List<Lecture> getAllLectures() {
        try {
            return lectureService.getAllLectures();
        }
        catch (EntityNotFoundException e){
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public Lecture getLectureById(@PathVariable Long id) {
        try {
            return lectureService.getLecturesById(id).orElseThrow();
        }
        catch (EntityNotFoundException e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, e.getMessage());
        }

    }
    @PostMapping
    public Lecture createLecture(@RequestBody Lecture lecture) {
        try {
            return lectureService.createLectures(lecture);
        } catch (Exception e) { //todo fix once filtering is done
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error creating lecture", e);
        }

    }
    @PutMapping("/{id}")
    public Lecture updateLecture(@PathVariable Long id, @RequestBody Lecture updatedLecture) {
        try {
            return lectureService.updateLecture(id, updatedLecture);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    @DeleteMapping("/{id}")
    public void deleteLecture(@PathVariable Long id) {
        try {
            lectureService.delete(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
