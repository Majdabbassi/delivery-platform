package com.upstart.backend;

import com.upstart.backend.config.JwtProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class UpstartBackendApplication {

	public static void main(String[] args) {
		// Load .env file before Spring context initialization
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory("./")
					.ignoreIfMalformed()
					.ignoreIfMissing()
					.load();
			
			// Set system properties from .env file
			dotenv.entries().forEach(entry -> {
				String key = entry.getKey();
				String value = entry.getValue();
				if (System.getProperty(key) == null) {
					System.setProperty(key, value);
				}
			});
			
			System.out.println("Loaded .env file with " + dotenv.entries().size() + " entries");
		} catch (Exception e) {
			System.err.println("Warning: Could not load .env file: " + e.getMessage());
		}
		
		SpringApplication.run(UpstartBackendApplication.class, args);
	}

}
