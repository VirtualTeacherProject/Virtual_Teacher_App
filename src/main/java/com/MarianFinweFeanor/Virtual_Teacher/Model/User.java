package com.MarianFinweFeanor.Virtual_Teacher.Model;

import jakarta.persistence.*;
import org.springframework.scheduling.support.SimpleTriggerContext;

@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String email;

    @Column(nullable = false, length = 20)
    private String password;

    @Column(nullable = true, length = 40)
    private String profilePicture;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 20)
    private String status;

    //Getters and Setters

}
