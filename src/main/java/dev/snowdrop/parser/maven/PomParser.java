package dev.snowdrop.parser.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
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
        req.setLocationTracking(true);
        req.setModelResolver(new RepositoryModelResolver());

        Result<? extends Model> result;
        try {
            result = modelBuilder.buildRawModel(new File(pomPath), 0, true);
        } catch (Exception e) {
            System.out.println("Could not build effective model: " + e.getMessage());
            return Optional.empty();
        }

        return searchDependency(result.get(), groupId, artifactId, version);

    }

    public Optional<InputLocation> searchDependency(Model model, String groupId, String artifactId, String version) {
        if (model.getDependencies() != null) {
            Optional<Dependency> dep = model.getDependencies().stream()
                .filter(d -> {
                    boolean depVersion = (d.getVersion() != null && !d.getVersion().isEmpty());
                    if (!depVersion) {
                        return groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId());
                    } else {
                        return groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()) && version.equals(d.getVersion());
                    }
                })
                .findFirst();

            if (dep.isPresent()) {
                // Found it!
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        if (model.getDependencyManagement() != null) {
            Optional<Dependency> dep = model.getDependencyManagement().getDependencies().stream()
                .filter(d -> {
                    boolean depVersion = (d.getVersion() != null && !d.getVersion().isEmpty());
                    if (!depVersion) {
                        return groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId());
                    } else {
                        return groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()) && version.equals(d.getVersion());
                    }
                })
                .findFirst();

            if (dep.isPresent()) {
                // Found it!
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        Parent p = model.getParent();
        if (p != null) {
            boolean parentVersion = (p.getVersion() != null && !p.getVersion().isEmpty());
            if (!parentVersion) {
                if (groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId())) {
                    return Optional.ofNullable(p.getLocation(""));
                }
            } else {
                if (groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId()) && version.equals(p.getVersion())) {
                    return Optional.ofNullable(p.getLocation(""));
                }
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java PomParser <path-to-pom.xml> <group:artifact:version>");
            System.exit(1);
        }
        String pomPath = args[0];
        String gavString = args[1];

        // Parse GAV format (group:artifact:version)
        String[] gavParts = gavString.split(":");

        String depG = gavParts[0];
        String depA = gavParts[1];
        String depV = (gavParts.length > 2 && gavParts[2] != null) ? gavParts[2] : "";

        System.out.printf("--- Searching for %s:%s:%s starting from %s", depG, depA, depV, pomPath);

        try {
            PomParser parser = new PomParser();
            Optional<InputLocation> loc = parser.findDependencyLocation(pomPath, depG, depA, depV);

            // Show the location of the dependency
            if (loc.isPresent() && loc.get() != null) {
                System.out.printf("\n--- Dependency Found : %s:%s:%s !\n", depG, depA, depV);
                System.out.println("--- Source: " + loc.get().getSource().getLocation());
                System.out.println("--- Line:   " + loc.get().getLineNumber());
                System.out.println("--- Column: " + loc.get().getColumnNumber());
            } else {
                System.out.printf("\n--- Dependency NOT Found : %s:%s:%s !", depG, depA, depV);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}