package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;

import java.util.List;

/**
 * SCRUM-35: enrolled students request additional course resources.
 * A request is private: only its author and course managers can read it.
 *
 * SCRUM-36: course managers set a request's status and can assign it to
 * another student enrolled in the course. That student can then read the
 * request and answer it with text (links and notes).
 */
public interface ResourceRequestService {

    /**
     * Saves a request from a student enrolled in the (published) course.
     * The message must be 100 to 2000 characters after trimming.
     */
    ResourceRequest createRequest(Long courseId, String studentEmail, String message);

    /** All requests of the course, newest first. Course managers only. */
    List<ResourceRequest> getRequestsForCourse(Long courseId, String userEmail);

    /** The user's own requests for the course, newest first. */
    List<ResourceRequest> getOwnRequests(Long courseId, String userEmail);

    /** SCRUM-36: the requests of the course assigned to the user, newest first. */
    List<ResourceRequest> getAssignedRequests(Long courseId, String userEmail);

    /**
     * SCRUM-36: sets OPEN, IN_PROGRESS, RESOLVED or IRRELEVANT.
     * Approved teachers and admins only.
     */
    ResourceRequest updateStatus(Long courseId, Long requestId, String status, String userEmail);

    /**
     * SCRUM-36: assigns the request to a student enrolled in the course who did
     * not write it, or removes the assignment when assigneeId is null.
     * Changing the assignee removes the previous assignee's answer. An OPEN
     * request becomes IN_PROGRESS when assigned, and OPEN again when unassigned.
     * Closed (RESOLVED or IRRELEVANT) requests cannot be reassigned.
     * Approved teachers and admins only.
     */
    ResourceRequest assign(Long courseId, Long requestId, Long assigneeId, String userEmail);

    /**
     * SCRUM-36: the assigned student answers the request (1 to 2000 characters).
     * Answering again replaces the answer. Closed requests cannot be answered.
     */
    ResourceRequest respond(Long courseId, Long requestId, String studentEmail, String response);
}
