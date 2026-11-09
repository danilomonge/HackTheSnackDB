package de.pecus.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Entry point of the HackTheSnackDB REST API.
 *
 * All endpoints are served under the context path {@code /user-management}
 * (see application.yml). Interactive API docs: {@code /user-management/swagger-ui.html}.
 */
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
		title = "HackTheSnackDB API",
		version = "1.0",
		description = "Products, ingredients, recipes and evaluations for the HackTheSnack recipe generator"))
public class MsJavaUserManagementPecusApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsJavaUserManagementPecusApplication.class, args);
	}
}
