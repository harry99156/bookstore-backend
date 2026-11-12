package com.pageturner.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The postgres profile must work both from one DB_URL and from the split DB_HOST/DB_PORT/DB_NAME that Render provides. */
class PostgresProfileConfigTest {

    private static String resolveUrl(Map<String, Object> overrides) throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        MutablePropertySources sources = env.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        for (PropertySource<?> ps : new PropertiesPropertySourceLoader()
                .load("postgres", new ClassPathResource("application-postgres.properties"))) {
            sources.addLast(ps);
        }
        sources.addFirst(new MapPropertySource("overrides", overrides));
        return env.getProperty("spring.datasource.url");
    }

    @Test
    void defaultsPointAtLocalPostgres() throws IOException {
        assertEquals("jdbc:postgresql://localhost:5432/bookstore", resolveUrl(Map.of()));
    }

    @Test
    void splitHostPortNameAreComposedIntoTheUrl() throws IOException {
        assertEquals("jdbc:postgresql://db.example.internal:5433/shop",
                resolveUrl(Map.of("DB_HOST", "db.example.internal", "DB_PORT", "5433", "DB_NAME", "shop")));
    }

    @Test
    void fullDbUrlStillWins() throws IOException {
        assertEquals("jdbc:postgresql://db:5432/bookstore",
                resolveUrl(Map.of("DB_URL", "jdbc:postgresql://db:5432/bookstore", "DB_HOST", "ignored")));
    }
}
