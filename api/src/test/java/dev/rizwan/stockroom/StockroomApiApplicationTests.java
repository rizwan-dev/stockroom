package dev.rizwan.stockroom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The context loads, Flyway migrates, and every entity matches the schema.
 *
 * <p>This is not a trivial test: ddl-auto is 'validate', so a mismatch between
 * a migration and an entity fails here rather than in production.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StockroomApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
