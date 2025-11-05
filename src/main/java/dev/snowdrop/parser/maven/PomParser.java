package dev.snowdrop.parser.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;

import java.io.File;
import java.util.Optional;

public class PomParser {

    private final ModelBuilder modelBuilder;

    public PomParser() {
        this.modelBuilder = new DefaultModelBuilderFactory().newInstance();
    }

    public Optional<InputLocation> findDependencyLocation(String pomPath, String groupId, String artifactId, String version) {

        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(new File(pomPath));
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setSystemProperties(System.getProperties());

        req.setModelResolver(new RepositoryModelResolver());

        ModelBuildingResult result;
        try {
            result = modelBuilder.build(req);
        } catch (Exception e) {
            System.out.println("Could not build effective model: " + e.getMessage());
            return Optional.empty();
        }

        Model model = result.getEffectiveModel();
        return searchDependency(model, groupId, artifactId, version);

        /* OR
            List<String> modelIds = result.getModelIds();

            for (int i = modelIds.size() - 1; i >= 0; i--) {
            String modelId = modelIds.get(i);

            Model model = result.getRawModel(modelId);

            if (model == null) {
                continue; // Should not happen, but good to check
            }

            return searchDependency(model,groupId,artifactId,version);
            }
            return Optional.empty();
        */

    }

    public Optional searchDependency(Model model, String groupId, String artifactId, String version) {
        if (model.getDependencyManagement() != null) {
            Optional<Dependency> dep = model.getDependencyManagement().getDependencies().stream()
                .filter(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                .findFirst();

            if (dep.isPresent()) {
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        if (model.getDependencies() != null) {
            Optional<Dependency> dep = model.getDependencies().stream()
                .filter(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                .findFirst();

            if (dep.isPresent()) {
                // Found it!
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PomParser <path-to-pom.xml>");
            System.exit(1);
        }
        String pomPath = args[0];

        // --- Dependency to search for ---
        String depG = "org.springframework.boot";
        String depA = "spring-boot-starter-web";
        String depV = "3.5.3";

        System.out.printf("--- Searching for %s:%s:%s starting from %s", depG, depA, depV, pomPath);

        try {
            PomParser parser = new PomParser();
            Optional<InputLocation> location = parser.findDependencyLocation(pomPath, depG, depA, depV);
            System.out.println("\n--- Dependency Found ---");

            // Show the location of the dependency
            if (location.isPresent() && location.get().getLocation("") != null) {
                var loc = location.get().getLocation("");
                System.out.println("  Source: " + loc.getSource().getLocation());
                System.out.println("  Line:   " + loc.getLineNumber());
                System.out.println("  Column: " + loc.getColumnNumber());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}