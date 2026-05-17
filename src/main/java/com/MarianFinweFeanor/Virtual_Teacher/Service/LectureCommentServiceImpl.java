package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureCommentService;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LectureCommentServiceImpl implements LectureCommentService {

    private final LectureCommentRepository lectureCommentRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    public LectureCommentServiceImpl(LectureCommentRepository lectureCommentRepository,
                                     LectureRepository lectureRepository,
                                     UserRepository userRepository){
        this.lectureCommentRepository = lectureCommentRepository;
        this.lectureRepository = lectureRepository;
        this.userRepository = userRepository;
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
            throw new IllegalArgumentException("Comments can not be empty");
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture Not found"));

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User Not found"));


        LectureComment lectureComment = new LectureComment();
        lectureComment.setLecture(lecture);
        lectureComment.setAuthor(author);
        lectureComment.setComment(comment.trim());
        lectureComment.setCreatedAt(LocalDateTime.now());

        lectureCommentRepository.save(lectureComment);

    }


}
