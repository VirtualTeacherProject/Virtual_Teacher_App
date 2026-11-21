package com.MarianFinweFeanor.Virtual_Teacher.Controller;


import com.MarianFinweFeanor.Virtual_Teacher.Model.*;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.*;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/lectures")
public class LectureMvcController {

    private final LectureService lectureService;
    private final UserService userService;          // enrollment checks
    private final CourseService courseService;      // breadcrumbs, ownership
    private final AssignmentService assignmentService;
    private final LectureCommentService lectureCommentService;
    private final EnrollmentService enrollmentService;
    private final CourseAccessService courseAccessService;  // draft visibility + enrollment-gated content
    private final LectureNoteService lectureNoteService;    // SCRUM-20 private notes

    @Autowired
    public LectureMvcController(LectureService lectureService,
                                UserService userService,
                                CourseService courseService,
                                AssignmentService assignmentService,
                                LectureCommentService lectureCommentService,
                                EnrollmentService enrollmentService,
                                CourseAccessService courseAccessService,
                                LectureNoteService lectureNoteService) {
        this.lectureService = lectureService;
        this.userService = userService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.lectureCommentService = lectureCommentService;
        this.enrollmentService = enrollmentService;
        this.courseAccessService = courseAccessService;
        this.lectureNoteService = lectureNoteService;
    }

    // --- Add Lecture (form) --------------------------------------------------


    /** GET /courses/{courseId}/lectures/add-lecture (TEACHER) */
    @GetMapping("/add-lecture")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String showAddForm(@PathVariable Long courseId,
                              Model model,
                              Principal principal) {

        //ensureApprovedTeacher(principal);
        userService.ensureApprovedTeacher(principal.getName());

        var course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));


        model.addAttribute("course", course);
        model.addAttribute("lecture", new Lecture());   // course is attached on submit (server-side)
        return "add-lecture";
    }

    /** POST /courses/{courseId}/lectures/add-lecture (TEACHER) */
    @PostMapping("/add-lecture")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String submitAdd(@PathVariable Long courseId,
                            @Valid @ModelAttribute("lecture") Lecture lecture,
                            BindingResult br,
                            Principal principal,
                            Model model,
                            RedirectAttributes ra) {
        //ensureApprovedTeacher(principal);
        userService.ensureApprovedTeacher(principal.getName());
        rejectLiveWithoutStart(lecture, br);

        if (br.hasErrors()) {
            // view uses 'course' for form action/url
            model.addAttribute("course", courseService.getCourseById(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course", courseId)));
            return "add-lecture";
        }

        var course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        lecture.setCourse(course);                 // URL is source of truth
        lectureService.createLectures(lecture);

        ra.addFlashAttribute("msg", "Lecture added!");
        return "redirect:/courses/" + courseId + "/lectures";
    }

    // --- Edit Lecture --------------------------------------------------------

    /** GET /courses/{courseId}/lectures/{lectureId}/edit (TEACHER) */
    @GetMapping("/{lectureId}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String showEditForm(@PathVariable Long courseId,
                               @PathVariable Long lectureId,
                               Model model,
                               Principal principal) {
        userService.ensureApprovedTeacher(principal.getName());

        var lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        // ensure URL course matches lecture’s course (prevents cross-course edits)
        if (lecture.getCourse() == null || !lecture.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Lecture does not belong to this course");
        }


        model.addAttribute("lecture", lecture);
        model.addAttribute("course", lecture.getCourse());
        return "edit-lecture";
    }

    /** POST /courses/{courseId}/lectures/{lectureId}/edit (TEACHER) */
    @PostMapping("/{lectureId}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String submitEdit(@PathVariable Long courseId,
                             @PathVariable Long lectureId,
                             @Valid @ModelAttribute("lecture") Lecture updated,
                             BindingResult br,
                             RedirectAttributes ra,
                             Principal principal,
                             Model model) {
        userService.ensureApprovedTeacher(principal.getName());

        Lecture existing = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        if (existing.getCourse() == null
                || !existing.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException(
                    "Lecture does not belong to this course"
            );
        }


        rejectLiveWithoutStart(updated, br);

        if (br.hasErrors()) {
            // the form posts back without the course, so put it back for the view
            updated.setLectureId(lectureId);
            updated.setCourse(existing.getCourse());
            model.addAttribute("course", existing.getCourse());
            return "edit-lecture";
        }



        lectureService.updateLecture(lectureId, updated);
        ra.addFlashAttribute("msg", "Lecture updated!");
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }

    // --- Lists & Detail ------------------------------------------------------

    /** GET /courses/{courseId}/lectures — public for published courses; drafts only for course managers */
    @GetMapping("")
    public String listLectures(@PathVariable Long courseId,
                               Model model,
                               Principal principal) {
        var course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        // SCRUM-22: lectures of a draft course stay hidden from non-managers.
        courseAccessService.ensureCanViewCourse(course, emailOf(principal));

        var lectures = lectureService.getByCourseId(courseId);

        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(id -> id.equals(courseId));

        model.addAttribute("course", course);
        model.addAttribute("lectures", lectures);
        model.addAttribute("enrolled", enrolled);
        return "lectures";
    }

    /**
     * GET /courses/{courseId}/lectures/{lectureId}
     * The page is public for published courses, but the video (SCRUM-10) is only
     * rendered for enrolled users and course managers.
     */
    @GetMapping("/{lectureId}")
    public String lectureDetail(@PathVariable Long courseId,
                                @PathVariable Long lectureId,
                                Model model,
                                Principal principal) {

        var lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        // Lecture must belong to the course in the URL.
        if (lecture.getCourse() == null || !lecture.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Lecture does not belong to this course");
        }

        String email = emailOf(principal);

        // SCRUM-22: lectures of a draft course stay hidden from non-managers.
        courseAccessService.ensureCanViewCourse(lecture.getCourse(), email);

        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(id -> id.equals(courseId));

        // SCRUM-10: video only after enrollment (or for admins / approved teachers).
        boolean canAccessContent =
                courseAccessService.canAccessCourseContent(lecture.getCourse(), email);

        List<Assignment> subs = enrolled
                ? assignmentService.getSubmissionsByLectureAndUser(lectureId, principal.getName())
                : List.of();

        model.addAttribute("courseId", courseId);
        model.addAttribute("lecture", lecture);
        model.addAttribute("enrolled", enrolled);
        model.addAttribute("canAccessContent", canAccessContent);
        model.addAttribute("submissions", subs);
        model.addAttribute("newAssignment", new Assignment());
        model.addAttribute("comments", lectureCommentService.getCommentsForLecture(lectureId));

        // SCRUM-20: private notes, for users who can access the course content.
        boolean canTakeNotes = principal != null && canAccessContent;
        model.addAttribute("canTakeNotes", canTakeNotes);
        model.addAttribute("noteContent",
                canTakeNotes ? lectureNoteService.getNoteContent(lectureId, email) : "");
        model.addAttribute("noteMaxLength", LectureNote.MAX_LENGTH);

        return "lecture-detail";
    }

    // --- Assignments ---------------------------------------------------------

    /** POST /courses/{courseId}/lectures/{lectureId}/assignments (STUDENT/TEACHER/ADMIN) */
    @PostMapping("/{lectureId}/assignments")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public String uploadAssignment(@PathVariable Long courseId,
                                   @PathVariable Long lectureId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("comment") String comment,
                                   Principal principal,
                                   RedirectAttributes ra) {
        try {
            assignmentService.submit(principal.getName(), lectureId, file, comment);
            ra.addFlashAttribute("msg", "Assignment submitted!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }

    @PostMapping("/{lectureId}/comments")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public String addComment(@PathVariable Long courseId,
                             @PathVariable Long lectureId,
                             @RequestParam("comment") String comment,
                             Principal principal,
                             RedirectAttributes ra) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByEmail(principal.getName());

            // Only students need to be enrolled before commenting.
            // Teachers and admins can comment without being enrolled.
            if (user.getRole() == UserRole.STUDENT) {
                enrollmentService.ensureStudentEnrolled(principal.getName(), courseId);
            }

            lectureCommentService.addComment(lectureId, principal.getName(), comment);

            ra.addFlashAttribute("msg", "Comment added.");

        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }

    // --- Notes (SCRUM-20) ----------------------------------------------------

    /** POST /courses/{courseId}/lectures/{lectureId}/notes — saves the caller's own note. */
    @PostMapping("/{lectureId}/notes")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public String saveNote(@PathVariable Long courseId,
                           @PathVariable Long lectureId,
                           @RequestParam(value = "content", required = false) String content,
                           Principal principal,
                           RedirectAttributes ra) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            lectureNoteService.saveNote(lectureId, principal.getName(), content);
            ra.addFlashAttribute("msg", "Notes saved.");
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }

    private static String emailOf(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    /** SCRUM-51 part A: a live lecture needs its start time. */
    private static void rejectLiveWithoutStart(Lecture lecture, BindingResult br) {
        if (lecture.isLive() && lecture.getLiveStart() == null && !br.hasFieldErrors("liveStart")) {
            br.rejectValue("liveStart", "required", LectureServiceImpl.LIVE_START_REQUIRED);
        }
    }
}
