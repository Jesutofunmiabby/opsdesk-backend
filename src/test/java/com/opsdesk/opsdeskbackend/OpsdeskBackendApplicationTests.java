package com.opsdesk.opsdeskbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Starts the whole application against the in-memory H2 database
// (src/test/resources/application-test.yml), so PostgreSQL is not needed.
@SpringBootTest
@ActiveProfiles("test")
class OpsdeskBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
