package dev.snowdrop.parser.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class PomParser {

    private ModelBuilder modelBuilder = null;

    public PomParser() {
        this.modelBuilder = new DefaultModelBuilderFactory().newInstance();
    }

    public Optional<InputLocation> findDependencyLocation(String pomPath, String groupId, String artifactId, String version) {
        ModelBuildingResult result = buildModel(pomPath);

        // First try with effective model (current behavior)
        Optional<InputLocation> location = searchDependency(result.getEffectiveModel(), pomPath, groupId, artifactId, version, true);

        // If not found with effective model, try with raw model to BOM's case
        if (!location.isPresent()) {
            location = searchDependency(result.getRawModel(), pomPath, groupId, artifactId, version, false);
        }

        return location;
    }

    public Optional<InputLocation> searchDependency(Model model, String pomPath, String groupId, String artifactId, String version, boolean isEffectiveModel) {

        if (model.getDependencies() != null) {
            //System.out.println("========= Dependencies ==========");
            //printDependencies(model.getDependencies());
            Optional<Dependency> dep = model.getDependencies().stream()
                .filter(d -> matchesGav(d, groupId, artifactId, version, model, isEffectiveModel))
                .findFirst();

            if (dep.isPresent()) {
                // Found it!
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        if (model.getDependencyManagement() != null) {
            //System.out.println("========= Dependencies of DependencyManagement ==========");
            //printDependencies(model.getDependencyManagement().getDependencies());
            Optional<Dependency> dep = model.getDependencyManagement().getDependencies().stream()
                .filter(d -> matchesGav(d, groupId, artifactId, version, model, isEffectiveModel))
                .findFirst();

            if (dep.isPresent()) {
                // Found it!
                return Optional.ofNullable(dep.get().getLocation(""));
            }
        }

        Parent p = model.getParent();
        if (p != null) {
            // If the GAV has not been found within the parent tag, then we will search about it within the parent pom: dependencies, dependencyManagement
            String parentRelativePath = p.getRelativePath();
            if (parentRelativePath != "") {
                String parentPomPath = Paths.get(new File(pomPath).getParent(), parentRelativePath).toString();
                return searchDependency(buildModel(parentPomPath).getEffectiveModel(), parentPomPath, groupId, artifactId, version, true);
            } else {
                // GAV is defined part of the pom parent section
                if (matchesGav(p, groupId, artifactId, version, model, isEffectiveModel)) {
                    return Optional.ofNullable(p.getLocation(""));
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private void printDependencies(List<Dependency> deps) {
        for (Dependency d : deps) {
            System.out.printf("Dep: %s:%s:%s\n", d.getGroupId(), d.getArtifactId(), d.getVersion());
        }
    }

    /**
     * Resolves property placeholders in a given value using the model's properties
     */
    private String resolveProperty(String value, Model model) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        String resolved = value;
        Properties properties = model.getProperties();

        // Simple property resolution - replace ${property.name} with actual value
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            String propertyName = (String) property.getKey();
            String propertyValue = (String) property.getValue();
            String placeholder = "${" + propertyName + "}";

            if (resolved.contains(placeholder)) {
                resolved = resolved.replace(placeholder, propertyValue);
            }
        }

        return resolved;
    }

    /**
     * Matches a dependency GAV against the search criteria, handling both effective and raw models
     */
    private boolean matchesGav(Dependency dependency, String groupId, String artifactId, String version, Model model, boolean isEffectiveModel) {
        return matchesArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                             groupId, artifactId, version, model, isEffectiveModel);
    }

    /**
     * Matches a parent GAV against the search criteria, handling both effective and raw models
     */
    private boolean matchesGav(Parent parent, String groupId, String artifactId, String version, Model model, boolean isEffectiveModel) {
        return matchesArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(),
                             groupId, artifactId, version, model, isEffectiveModel);
    }

    /**
     * Common logic for matching artifacts (dependencies or parents) against search criteria
     */
    private boolean matchesArtifact(String artifactGroupId, String artifactArtifactId, String artifactVersion,
                                   String searchGroupId, String searchArtifactId, String searchVersion,
                                   Model model, boolean isEffectiveModel) {
        String resolvedGroupId = artifactGroupId;
        String resolvedArtifactId = artifactArtifactId;
        String resolvedVersion = artifactVersion;

        // For raw model, resolve properties to match against the search criteria
        if (!isEffectiveModel) {
            resolvedGroupId = resolveProperty(resolvedGroupId, model);
            resolvedArtifactId = resolveProperty(resolvedArtifactId, model);
            resolvedVersion = resolveProperty(resolvedVersion, model);
        }

        // Check if groupId and artifactId match
        if (!searchGroupId.equals(resolvedGroupId) || !searchArtifactId.equals(resolvedArtifactId)) {
            return false;
        }

        // Check version if specified
        boolean hasSearchVersion = (searchVersion != null && !searchVersion.isEmpty());
        if (!hasSearchVersion) {
            return true; // Match without version check
        } else {
            return searchVersion.equals(resolvedVersion);
        }
    }

    private ModelBuildingResult buildModel(String pomPath) {
        RepositoryModelResolver repositoryModelResolver = new RepositoryModelResolver();
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(new File(pomPath));
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setSystemProperties(System.getProperties());
        req.setLocationTracking(true);
        req.setModelResolver(repositoryModelResolver);

        ModelBuildingResult result = null;
        try {
            return modelBuilder.build(req);
            //return modelBuilder.buildRawModel(new File(pomPath), 0, true);
        } catch (Exception e) {
            System.out.println("Could not build effective model: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
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