package com.MarianFinweFeanor.Virtual_Teacher.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI virtualTeacherOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Virtual Teacher API")
                        .description("API documentation for the Virtual Teacher LMS platform")
                        .version("v1.0")
                        .contact(new Contact()
                                .name(("Umut Yildirim / Marian Maximov"))
                                .email("umutyildirim1905@gmail.com / marianmgm@yahoo.com"))
                        .license(new License()
                                .name("Internal Portfolio Project")));
    }
}
