package project.qldt;
import project.qldt.support.Beans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QldtApplicationTests {

	@Test
	void contextLoads() {
	}

}
