package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;

import java.util.List;

/**
 * SCRUM-35: enrolled students request additional course resources.
 * A request is private: only its author and course managers can read it.
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
}
