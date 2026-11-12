package com.pageturner.bookstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bookstoreOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PageTurner Bookstore API")
                .version("1.0.0")
                .description("Catalog, cart, checkout and order management for an online bookstore."));
    }
}
