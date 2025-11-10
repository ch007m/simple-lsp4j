package dev.snowdrop.parser.maven;

import org.apache.maven.model.InputLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SpringBoot GAV Testing")
public class SpringBootPomTest {

    private PomParser pomParser;
    private String mavenProjectPath;

    @BeforeEach
    void setUp() throws IOException {
        pomParser = new PomParser();
        mavenProjectPath = "src/test/resources/spring-boot";
    }
    
    @Test
    @DisplayName("Test :: finding GAV in dependencies section")
    void testFindGavInDependenciesSection() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "org.springframework.boot",
            "spring-boot-starter-data-jpa",
            ""
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(20, location.get().getLineNumber());
    }

    @Test
    @DisplayName("Test :: finding GAV with version in dependencies section")
    void testFindGavWithVersionInDependenciesSection() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "org.springframework.boot",
            "spring-boot-starter-data-jpa",
            "3.5.3"
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(20, location.get().getLineNumber());
    }

    @Test
    @DisplayName("Test :: finding parent GAV")
    void testFindParentGAV() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "org.springframework.boot",
            "spring-boot-starter-parent",
            ""
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(5, location.get().getLineNumber());
    }

    @Test
    @DisplayName("Test :: finding parent GAV with version")
    void testFindParentGAVWithVersion() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "3.5.3"
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(5, location.get().getLineNumber());
    }
}