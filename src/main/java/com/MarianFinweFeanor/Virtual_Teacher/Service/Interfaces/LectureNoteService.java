package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

/**
 * SCRUM-20: private plain-text notes, one per user per lecture.
 */
public interface LectureNoteService {

    /** The user's note text for the lecture, or an empty string when there is none. */
    String getNoteContent(Long lectureId, String userEmail);

    /**
     * Creates or replaces the user's note for the lecture. Blank content removes the note.
     * Only users who can access the course content (enrolled students, approved
     * teachers, admins) may keep notes.
     */
    void saveNote(Long lectureId, String userEmail, String content);
}
