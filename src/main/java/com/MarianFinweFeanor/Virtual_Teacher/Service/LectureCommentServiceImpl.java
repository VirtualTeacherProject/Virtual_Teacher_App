package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureCommentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LectureCommentServiceImpl implements LectureCommentService {

    private final LectureCommentRepository lectureCommentRepository;
    private final LectureService lectureService;
    private final UserService userService;

    public LectureCommentServiceImpl(LectureCommentRepository lectureCommentRepository,
                                     LectureService lectureService,
                                     UserService userService) {
        this.lectureCommentRepository = lectureCommentRepository;
        this.lectureService = lectureService;
        this.userService = userService;
    }

    @Override
    public List<LectureComment> getCommentsForLecture(Long lectureId) {
        System.out.println("GET COMMENTS START lectureId = " + lectureId);

        List<LectureComment> comments =
                lectureCommentRepository.findByLecture_LectureIdOrderByCreatedAtAsc(lectureId);

        System.out.println("GET COMMENTS END size = " + comments.size());

        return comments;
    }

    @Override
    public void addComment(Long lectureId, String userEmail, String comment){
        if (comment == null || comment.trim().isBlank()) {
            throw new IllegalArgumentException("Comments cannot be empty");
        }

        Lecture lecture = lectureService.getLecturesById(lectureId)
        .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        User author = userService.findByEmail(userEmail);
        // User Service is not returning optional, so for now i did minimal change.
        //later, we can use both service will return the model itself.
                //.orElseThrow(() -> new EntityNotFoundException("User", userEmail));


        LectureComment lectureComment = new LectureComment();
        lectureComment.setLecture(lecture);
        lectureComment.setAuthor(author);
        lectureComment.setComment(comment.trim());
        lectureComment.setCreatedAt(LocalDateTime.now());

        lectureCommentRepository.save(lectureComment);
    }


}
