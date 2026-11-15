package io.github.rochona.notificationsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationSystemV2Application {

	public static void main(String[] args) {
		SpringApplication.run(NotificationSystemV2Application.class, args);
	}

}
