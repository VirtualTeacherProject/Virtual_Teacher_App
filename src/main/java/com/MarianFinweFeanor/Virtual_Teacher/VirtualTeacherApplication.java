package com.MarianFinweFeanor.Virtual_Teacher;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@SpringBootApplication
public class VirtualTeacherApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtualTeacherApplication.class, args);

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		System.out.println(encoder.encode("Admin123!"));
	}

}
