package com.research.gbjournal;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
public class GbjournalApplication {

	public static void main(String[] args) {
		// Load .env file if it exists (dev). In production, real env vars take precedence.
		// ignoreIfMissing() ensures the app starts fine without a .env (e.g. on the server).
		Dotenv.configure()
			  .ignoreIfMissing()
			  .systemProperties()   // injects .env values as System properties
			  .load();

		SpringApplication.run(GbjournalApplication.class, args);
	}

}
