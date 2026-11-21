package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AttendanceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * SCRUM-28: the attendance tracker of a lecture. Approved teachers and admins
 * mark the absent students; everyone else on the roll call is recorded as present.
 * SCRUM-51: open only for live lectures of attendance-required courses, once the
 * session has started; otherwise the page is read-only and says why.
 */
@Controller
@RequestMapping("/courses/{courseId}/lectures/{lectureId}/attendance")
public class AttendanceMvcController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    @Autowired
    public AttendanceMvcController(AttendanceService attendanceService, UserService userService) {
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    /** GET /courses/{courseId}/lectures/{lectureId}/attendance (TEACHER/ADMIN) */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String showAttendance(@PathVariable Long courseId,
                                 @PathVariable Long lectureId,
                                 Principal principal,
                                 Model model) {
        userService.ensureApprovedTeacher(principal.getName());

        Lecture lecture = attendanceService.getLectureForAttendance(courseId, lectureId, principal.getName());
        List<AttendanceEntry> entries =
                attendanceService.getLectureAttendance(courseId, lectureId, principal.getName());

        model.addAttribute("courseId", courseId);
        model.addAttribute("course", lecture.getCourse());
        model.addAttribute("lecture", lecture);
        model.addAttribute("entries", entries);
        String closedReason = attendanceService.attendanceClosedReason(lecture);
        model.addAttribute("attendanceOpen", closedReason == null);
        model.addAttribute("closedReason", closedReason);
        model.addAttribute("recorded", entries.stream().anyMatch(AttendanceEntry::isRecorded));
        model.addAttribute("absentCount", entries.stream().filter(AttendanceEntry::isAbsent).count());
        // Students without a record are neither present nor absent.
        model.addAttribute("presentCount", entries.stream()
                .filter(e -> e.isRecorded() && !e.isAbsent()).count());
        return "lecture-attendance";
    }

    /**
     * POST /courses/{courseId}/lectures/{lectureId}/attendance (TEACHER/ADMIN)
     * The ticked students are absent; no ticks means everyone was present.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String recordAttendance(@PathVariable Long courseId,
                                   @PathVariable Long lectureId,
                                   @RequestParam(value = "absent", required = false) List<Long> absent,
                                   Principal principal,
                                   RedirectAttributes ra) {
        userService.ensureApprovedTeacher(principal.getName());

        try {
            List<AttendanceEntry> saved = attendanceService.recordAttendance(
                    courseId, lectureId, principal.getName(), absent == null ? List.of() : absent);
            long absentCount = saved.stream().filter(AttendanceEntry::isAbsent).count();
            ra.addFlashAttribute("msg", "Attendance saved: " + (saved.size() - absentCount)
                    + " present, " + absentCount + " absent.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            // Two saves of the same roll call at once: one insert hits the unique key.
            ra.addFlashAttribute("error",
                    "Attendance for this lecture was saved by someone else at the same time. Check the list and save again.");
        }
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId + "/attendance";
    }
}
