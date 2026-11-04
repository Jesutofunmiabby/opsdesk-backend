package com.opsdesk.opsdeskbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Users log in with a token (see SecurityConfig), so Spring Boot's default
// user with a generated password is switched off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OpsdeskBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpsdeskBackendApplication.class, args);
	}

}
