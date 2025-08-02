INSERT INTO "PUBLIC"."USERS" VALUES

('test1@example.com', 1, 'pass1234', 'Umutt', 'Yildirim2', 'default1.jpg', 'TEACHER', 'Active'),

('test2@example.com', 33, 'pass1234', 'UmutDev', 'Yildirim', 'default2.jpg', 'ADMIN', 'Active'),

('test3@example.com', 34, 'pass1234', 'Marian', 'Maximov', 'default2.jpg', 'TEACHER', 'Active'),

('test10@example.com', 65, 'pass1234', 'Ertan', 'Haskoy', 'default1.jpg', 'TEACHER', 'Active'),

('test5@example.com', 97, 'pass1234', 'Umut', 'Thunder', 'default5.jpg', 'TEACHER', 'Active'),

('test6@example.com', 129, 'pass1234', 'Katya', 'Stepanova', 'default6.jpg', 'TEACHER', 'Active'),

('student1@example.com', 161, 'pass1234', 'Student1', 'Student1', 'default11.jpg', 'STUDENT', 'Active'),

('test4@example.com', 193, 'pass1234', 'Ivan', 'Drago', 'default.jpg', 'TEACHER', 'Active');

INSERT INTO "PUBLIC"."COURSES" VALUES

('Java Backend Tutorial', 'How to Create Spring Project', 'Beginner Friendly Java Tut', TIMESTAMP '2025-03-03 00:00:00', 'Active', 1, 1),

('Phyton Tutorial', 'How to start with AI', 'Beginner Friendly Phyton Tut', TIMESTAMP '2025-03-03 00:00:00', 'Active', 34, 33),

('C++ tut', 'How to Create c++ tut', 'Medium  C++ Tut', TIMESTAMP '2025-03-03 00:00:00', 'Active', 34, 65),

('Kotlin Course', 'Kotlin fundamentals', 'variables', TIMESTAMP '2025-08-02 16:39:00', 'Active', 1, 97);

INSERT INTO "PUBLIC"."LECTURES" VALUES

(1, 'asdasdasdads', 'asdasdasdasd', 'for loop phyton', 'youtube.com/phyton', 33),

(2, 'asldjhasdhakus', 'beginner tutorial', 'phyton if else', 'youtube.com/phyton/forloop', 33),

(33, 'test', 'test', 'test', 'test', 33),

(65, 'asdasdasdads', 'c++ deep dive with pointers', 'c++ deep', 'youtube.com/c++/pointer', 65),

(97, 'fdhfgjfjgfj', 'testing testing testing testing', 'testing testing', 'youtube.com/phyton', 1),

(98, '98 update', '98 update', '98 update', 'youtube.com/c++/pointer', 1),

(129, 'update , updated', 'update , updated', 'update , updated', 'update , updated', 1),

(161, 'asdads', 'asjdkhakjdshads', 'Arrays', 'asdasdads', 1),

(162, 'alskdjalksd', 'Fundamentals', 'Arrays for Kotlin', 'asdadsadskj', 97);