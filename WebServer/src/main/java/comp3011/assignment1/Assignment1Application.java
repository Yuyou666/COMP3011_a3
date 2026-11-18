package comp3011.assignment1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Assignment 1 Spring Boot application.
 *
 * @SpringBootApplication enables Spring Boot auto-configuration,
 * component scanning, and application configuration.
 */
@SpringBootApplication
public class Assignment1Application {

	public static void main(String[] args) {

		// Starts the Spring application context and the embedded Tomcat web server.
		SpringApplication.run(Assignment1Application.class, args);
	}

}