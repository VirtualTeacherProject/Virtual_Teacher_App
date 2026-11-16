package com.MarianFinweFeanor.Virtual_Teacher.exceptions;

/**
 * Thrown when something cannot be deleted because other records still
 * depend on it (SCRUM-15), e.g. a user who still teaches courses.
 */
public class EntityInUseException extends RuntimeException {
    public EntityInUseException(String message) {
        super(message);
    }
}
