package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;

import java.util.List;

public interface LectureCommentService {

    List<LectureComment> getCommentsForLecture(Long lectureId);

    void addComment(Long lectureId, String userEmail, String comment);

}
