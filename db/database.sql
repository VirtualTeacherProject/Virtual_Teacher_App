create table user
(
    email           varchar(20) not null,
    user_id         int auto_increment
        primary key,
    password        varchar(20) not null,
    first_name      varchar(20) not null,
    last_name       varchar(20) not null,
    profile_picture varchar(40) not null,
    role            varchar(15) not null,
    status          varchar(20) not null,
    constraint user_pk
        unique (email)
);

create table courses
(
    course_id   int auto_increment
        primary key,
    title       varchar(30) not null,
    topic       varchar(40) null,
    description varchar(60) not null,
    column_name datetime    not null,
    status      varchar(20) not null,
    teacher_id  int         not null,
    constraint courses_user_user_id_fk
        foreign key (teacher_id) references user (user_id)
);

create table enrollments
(
    course_id         int         not null,
    enrollment_id     int auto_increment
        primary key,
    student_id        int         not null,
    average_grade     varchar(10) null,
    completion_status varchar(30) not null,
    constraint enrollments_courses_course_id_fk
        foreign key (course_id) references courses (course_id),
    constraint enrollments_user_user_id_fk
        foreign key (student_id) references user (user_id)
);

create table lectures
(
    lecture_id           int auto_increment
        primary key,
    title                varchar(50)  not null,
    description          varchar(300) not null,
    video_url            varchar(50)  not null,
    assignment_file_path varchar(50)  not null,
    course_id            int          not null,
    constraint lectures_pk
        unique (lecture_id),
    constraint lectures_courses_course_id_fk
        foreign key (course_id) references courses (course_id)
);

create table assignments
(
    assignment_id        int auto_increment
        primary key,
    student_id           int         not null,
    submission_file_path varchar(40) not null,
    grade                varchar(10) not null,
    lecture_id           int         not null,
    constraint assignments_lectures_lecture_id_fk
        foreign key (lecture_id) references lectures (lecture_id),
    constraint assignments_user_user_id_fk
        foreign key (student_id) references user (user_id)
);

create table ratings
(
    rating_id  int          not null
        primary key,
    course_id  int          not null,
    student_id int          not null,
    rating     int          not null,
    comment    varchar(300) null,
    constraint ratings_courses_course_id_fk
        foreign key (course_id) references courses (course_id),
    constraint ratings_user_user_id_fk
        foreign key (student_id) references user (user_id)
);
