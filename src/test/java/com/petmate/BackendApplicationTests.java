package com.petmate;

import com.petmate.config.TestS3Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestS3Config.class)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
