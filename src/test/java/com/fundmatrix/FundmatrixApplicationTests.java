package com.fundmatrix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Smoke test: the full application context boots against an isolated in-memory database
 * (the {@code test} profile), independent of any running instance.
 */
@SpringBootTest
@ActiveProfiles("test")
class FundmatrixApplicationTests {

	@Test
	void contextLoads() {
	}

}


