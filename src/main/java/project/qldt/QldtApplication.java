package project.qldt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QldtApplication {

	public static void main(String[] args) {
		SpringApplication.run(QldtApplication.class, args);
	}

}
