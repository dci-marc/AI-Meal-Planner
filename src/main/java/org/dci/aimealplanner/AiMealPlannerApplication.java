package org.dci.aimealplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "org.dci.aimealplanner.services.utils")
public class AiMealPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiMealPlannerApplication.class, args);
	}

}
