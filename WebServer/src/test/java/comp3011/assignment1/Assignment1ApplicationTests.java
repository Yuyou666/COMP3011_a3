package comp3011.assignment1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic Spring Boot application context test.
 *
 * This test checks that the application can start successfully
 * and that the Spring application context can be created without errors.
 */
@SpringBootTest
class Assignment1ApplicationTests {

    /**
     * Passes if the Spring application context loads successfully.
     *
     * No explicit assertions are required because the test will fail
     * automatically if Spring cannot start the application context.
     */
    @Test
    void contextLoads() {
    }
}