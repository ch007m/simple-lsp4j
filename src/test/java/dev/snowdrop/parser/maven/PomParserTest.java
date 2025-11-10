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
@DisplayName("PomParser GAV Testing")
public class PomParserTest {

    private PomParser pomParser;
    private String mavenProjectPath;

    @BeforeEach
    void setUp() throws IOException {
        pomParser = new PomParser();
        mavenProjectPath = "src/test/resources/parent-child";
    }
    
    @Test
    @DisplayName("Test :: finding GAV in dependencies section of the parent")
    void testFindGavInDependenciesSectionParent() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "com.github.freva",
            "ascii-table",
            "1.8.0"
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(101, location.get().getLineNumber());
    }

    @Test
    @DisplayName("Test :: finding GAV without version in dependencies section of the parent")
    void testFindGavWithoutVersionInDependenciesSectionParent() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "com.github.freva",
            "ascii-table",
            ""
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(101, location.get().getLineNumber());
    }

    @Test
    @DisplayName("Test :: finding GAV in DependencyManagement section of the parent")
    void testFindGavInDependencyManagementSectionParent() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "org.eclipse.lsp4j",
            "org.eclipse.lsp4j",
            "0.24.0"
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(67, location.get().getLineNumber());
    }


    @Test
    @DisplayName("Test :: finding GAV without version in DependencyManagement section of the parent")
    void testFindGavWithoutVersionInDependencyManagementSectionParent() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "org.eclipse.lsp4j",
            "org.eclipse.lsp4j",
            ""
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(67, location.get().getLineNumber());
    }

    @Test
    @DisplayName("Test :: finding GAV in BOM of the parent")
    void testFindGavInBOMOfParent() throws Exception {
        Optional<InputLocation> location = pomParser.findDependencyLocation(
            String.format("%s/pom.xml",mavenProjectPath),
            "io.quarkus.platform",
            "quarkus-bom",
            "3.29.0"
        );

        assertTrue(location.isPresent(), "Should find gav");
        assertNotNull(location.get(), "Location should not be null");
        assertEquals(43, location.get().getLineNumber());
    }
}