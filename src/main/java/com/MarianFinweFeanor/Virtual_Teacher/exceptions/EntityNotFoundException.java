package com.MarianFinweFeanor.Virtual_Teacher.exceptions;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String type,long id) {
        this(type,"id",String.valueOf(id));

    }
    public EntityNotFoundException(String type, String attribute,String value) {
        super(String.format("%s with %s %s not found", type, attribute, value));

    }
    public EntityNotFoundException(String type,String location) {
        super(String.format("%s not found in %s", type,location));
    }
}
