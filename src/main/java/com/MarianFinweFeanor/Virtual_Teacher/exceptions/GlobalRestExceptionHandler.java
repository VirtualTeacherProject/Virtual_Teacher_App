package com.MarianFinweFeanor.Virtual_Teacher.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.File;
import java.time.LocalDateTime;

// SCRUM-52: ahead of the unscoped GlobalMvcExceptionHandler, so errors from REST
// controllers (e.g. AccessDeniedException) answer JSON instead of an MVC redirect
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(annotations = RestController.class)
public class GlobalRestExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex,
                                                           HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleFileStorageException(FileStorageException ex,
                                                                       HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "File Storage Error",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(EntityDuplicateException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(EntityDuplicateException ex,
                                                            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(EntityInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleInUse(EntityInUseException ex,
                                                        HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex,
                                                             HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    // SCRUM-52: e.g. changing a course or lecture the caller does not manage
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex,
                                                          HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error occurred",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter or path variable",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status,
                                                                String message,
                                                                String path) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.status(status).body(body);
    }
}