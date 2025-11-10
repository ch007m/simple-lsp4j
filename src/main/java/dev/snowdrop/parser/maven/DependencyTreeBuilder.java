package dev.snowdrop.parser.maven;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.util.List;

public class DependencyTreeBuilder {

    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    /**
     * Configure the Resolution's system
     */
    public DependencyTreeBuilder() {
        this.repoSystem = newRepositorySystem();
        this.session = newRepositorySystemSession(repoSystem);
        this.repositories = List.of(
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()
        );
    }

    /**
     * Create the RespositorySystem
     */
    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    /**
     * Create Maven repository session
     */
    private DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        String userHome = System.getProperty("user.home");
        LocalRepository localRepo = new LocalRepository(userHome + File.separator + ".m2" + File.separator + "repository");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    /**
     * Build the dependency tree for a given GAV.
     * Supports both regular JARs and BOMs (Bill of Materials with type "pom").
     * For BOMs, this will resolve the dependencies defined in the BOM.
     */
    public DependencyNode buildTree(String groupId, String artifactId, String version, String type) throws Exception {
        // Use the provided type, default to "jar" if null or empty
        String artifactType = (type != null && !type.trim().isEmpty()) ? type.trim() : "jar";
        Artifact artifact = new DefaultArtifact(groupId, artifactId, artifactType, version);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, null));
        collectRequest.setRepositories(repositories);

        CollectResult collectResult = repoSystem.collectDependencies(session, collectRequest);

        return collectResult.getRoot();
    }

    /**
     * Display recursively the tree of the nodes
     */
    private void printTreeRecursive(DependencyNode node, String indent) {
        for (DependencyNode child : node.getChildren()) {
            System.out.println(indent + child.getArtifact() + " [" + child.getDependency().getScope() + "]");
            printTreeRecursive(child, indent + "  ");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java DependencyTreeBuilder <path-to-pom.xml> <group:artifact:version>");
            System.exit(1);
        }
        String pomPath = args[0];
        String gavString = args[1];

        String[] gavParts = gavString.split(":");

        String depG = gavParts[0];
        String depA = gavParts[1];
        String depV = (gavParts.length > 2 && gavParts[2] != null) ? gavParts[2] : "";
        String type = (gavParts.length > 3 && gavParts[3] != null) ? gavParts[3] : "";

        System.out.printf("--- Searching for %s:%s:%s starting from %s", depG, depA, depV, pomPath);

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        System.out.println("Tree build for: " + depG + ":" + depA + ":" + depV);

        DependencyNode root = builder.buildTree(depG, depA, depV, type);
        System.out.println(root.getArtifact() + " [" + root.getDependency().getScope() + "]");
        builder.printTreeRecursive(root, "  ");
    }
}