package com.example.university_platform;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "University Platform API", version = "v1", description = "API for University Information Platform"))
@SecurityScheme(
		name = "JSESSIONID", // Такое же имя, как в @SecurityRequirement
		type = SecuritySchemeType.APIKEY, // Использование APIKEY для представления cookie
		in = SecuritySchemeIn.COOKIE, // Указывание, что это cookie
		paramName = "JSESSIONID" // Имя самой cookie
)
public class UniversityPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(UniversityPlatformApplication.class, args);
	}

}
