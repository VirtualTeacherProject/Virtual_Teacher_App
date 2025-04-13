package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LecturesController {
    private final LectureService lectureService;
    @Autowired
    public LecturesController(LectureService lectureService) {
        this.lectureService = lectureService;
    }
    @GetMapping
    public List<Lecture> getAllLectures() {
        return lectureService.getAllLectures();
    }
    @GetMapping("/{id}")
    public Lecture getLectureById(@PathVariable Long id) {
        return lectureService.getLecturesById(id).orElseThrow(()->new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, String.format("Lecture with id %d not found", id)));
    }
    @PostMapping
    public Lecture createLecture(@RequestBody Lecture lecture) {
        try {
            return lectureService.createLectures(lecture);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error creating lecture", e);
        }

    }
    @PutMapping("/{id}")
    public Lecture updateLecture(@PathVariable Long id, @RequestBody Lecture updatedLecture) {
        try {
            return lectureService.updateLecture(id, updatedLecture);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error updating lecture", e);
        }
    }
    @DeleteMapping("/{id}")
    public void deleteLecture(@PathVariable Long id) {
        try {
            lectureService.delete(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting lecture", e);
        }
    }

}
