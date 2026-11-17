package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureNote;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureNoteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureNoteService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class LectureNoteServiceImpl implements LectureNoteService {

    private final LectureNoteRepository lectureNoteRepository;
    private final LectureService lectureService;
    private final UserService userService;
    private final CourseAccessService courseAccessService;

    @Autowired
    public LectureNoteServiceImpl(LectureNoteRepository lectureNoteRepository,
                                  LectureService lectureService,
                                  UserService userService,
                                  CourseAccessService courseAccessService) {
        this.lectureNoteRepository = lectureNoteRepository;
        this.lectureService = lectureService;
        this.userService = userService;
        this.courseAccessService = courseAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public String getNoteContent(Long lectureId, String userEmail) {
        if (userEmail == null) {
            return "";
        }

        return lectureNoteRepository.findByLecture_LectureIdAndUser_Email(lectureId, userEmail)
                .map(LectureNote::getContent)
                .orElse("");
    }

    @Override
    public void saveNote(Long lectureId, String userEmail, String content) {
        // Browsers send line breaks as CRLF; store and count them as one character.
        String text = content == null ? "" : content.replace("\r\n", "\n");

        if (text.length() > LectureNote.MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Notes cannot be longer than " + LectureNote.MAX_LENGTH + " characters");
        }

        Lecture lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        if (!courseAccessService.canAccessCourseContent(lecture.getCourse(), userEmail)) {
            throw new AccessDeniedException("Enroll in this course to take notes");
        }

        Optional<LectureNote> existing =
                lectureNoteRepository.findByLecture_LectureIdAndUser_Email(lectureId, userEmail);

        if (text.isBlank()) {
            existing.ifPresent(lectureNoteRepository::delete);
            return;
        }

        LectureNote note = existing.orElseGet(() -> {
            LectureNote created = new LectureNote();
            created.setLecture(lecture);
            created.setUser(userService.findByEmail(userEmail));
            return created;
        });

        note.setContent(text);
        note.setUpdatedAt(LocalDateTime.now());
        lectureNoteRepository.save(note);
    }
}
