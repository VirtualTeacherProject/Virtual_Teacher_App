package com.MarianFinweFeanor.Virtual_Teacher;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;



@SpringBootApplication
public class VirtualTeacherApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtualTeacherApplication.class, args);

		System.out.println(new BCryptPasswordEncoder(12).encode("pass1234"));

	}

}
