package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@SpringBootApplication
public class VirtualTeacherApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtualTeacherApplication.class, args);

		//System.out.println(new BCryptPasswordEncoder(12).encode("Admin12345"));

	}

	//This is for Admin Seeding, creating Admin, since its needs to be seeded
	@Bean
	@Profile("!test")
	CommandLineRunner seedAdmin (UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			String email = "admin1@example.com";

			if(userRepository.findByEmail(email).isEmpty()) {
				User admin = new User();
				admin.setEmail(email);
				admin.setPassword(passwordEncoder.encode("Admin12345"));
				admin.setFirstName("Admin1");
				admin.setLastName("Admin1");
				admin.setRole(UserRole.ADMIN);
				admin.setStatus("ACTIVE");
				admin.setProfilePicture("default.png");

				userRepository.save(admin);
				System.out.println("Admin seeded successfully");


			}

		};

	}

}
