package com.research.gbjournal;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GbjournalApplicationTests {

	static {
		Dotenv.configure()
				.ignoreIfMissing()
				.systemProperties()
				.load();
	}

	@Test
	void contextLoads() {
	}

}

