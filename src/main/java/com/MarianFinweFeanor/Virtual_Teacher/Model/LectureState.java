package com.MarianFinweFeanor.Virtual_Teacher.Model;

/**
 * SCRUM-51 part B: where a student stands with one lecture.
 * finished  = marked as finished (and the assignment submitted, if any);
 * completed = finished and, for a lecture with an assignment, graded;
 * locked    = the course wants lectures in order and an earlier one is not finished.
 */
public record LectureState(Lecture lecture, boolean finished, boolean completed, boolean locked) {

    public boolean isAwaitingGrade() {
        return finished && !completed;
    }
}
