package com.MarianFinweFeanor.Virtual_Teacher.exceptions;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ApiErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    // getters/setters/constructors
    //we added lombok so no need for getter / setter
}
