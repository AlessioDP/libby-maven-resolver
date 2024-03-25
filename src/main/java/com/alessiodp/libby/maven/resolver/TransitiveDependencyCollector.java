package com.alessiodp.libby.maven.resolver;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A supplier-based helper class for providing compile scope transitive dependencies.
 *
 * @see <a href=https://github.com/apache/maven-resolver>maven-resolver</a>.
 */
public class TransitiveDependencyCollector {

    /**
     * Counter used to generate ids for repositories
     *
     * @see #newDefaultRepository(String)
     */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    /**
     * Maven repository system
     *
     * @see #newRepositorySystem()
     */
    @NotNull
    private final RepositorySystem repositorySystem = newRepositorySystem();

    /**
     * Maven repository system session
     *
     * @see #newRepositorySystemSession(RepositorySystem)
     */
    @NotNull
    private final RepositorySystemSession repositorySystemSession;

    /**
     * Local repository path
     *
     * @see LocalRepository
     */
    @NotNull
    private final Path saveDirectory;

    /**
     * Creates a new {@code TransitiveDependencyCollector}.
     *
     * @param saveDirectory The download directory name
     */
    public TransitiveDependencyCollector(@NotNull Path saveDirectory) {
        this.saveDirectory = saveDirectory;
        this.repositorySystemSession = newRepositorySystemSession(repositorySystem);
    }

    /**
     * Creates a new instance of {@link RemoteRepository}
     *
     * @param url Maven repository url
     * @return New instance of {@link RemoteRepository}
     */
    @NotNull
    public static RemoteRepository newDefaultRepository(@NotNull String url) {
        return new RemoteRepository.Builder("repo" + ID_GENERATOR.getAndIncrement(), "default", url).build();
    }

    /**
     * Resolves transitive dependencies of specific maven artifact. Dependencies with scope {@code JavaScopes.COMPILE}, {@code JavaScopes.RUNTIME} returned only
     *
     * @param groupId      Maven group ID
     * @param artifactId   Maven artifact ID
     * @param version      Maven dependency version
     * @param classifier   Maven artifact classifier. May be null
     * @param repositories Maven repositories that would be used for dependency resolution
     * @return Transitive dependencies paired with their repository url, exception otherwise
     * @throws DependencyResolutionException thrown if dependency doesn't exist on provided repositories
     */
    @NotNull
    public Collection<Entry<Artifact, @Nullable String>> findTransitiveDependencies(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @NotNull String classifier, @NotNull List<RemoteRepository> repositories) throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, "jar", version);

        CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), repositories);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, new ScopeDependencyFilter(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME), Collections.emptyList()));

        DependencyResult dependencyResult = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

        return dependencyResult.getArtifactResults()
                .stream()
                .filter(ArtifactResult::isResolved)
                .map(artifactResult -> {
                    ArtifactRepository repo = artifactResult.getRepository();
                    String url = null;
                    if (repo instanceof RemoteRepository) {
                        url = ((RemoteRepository) repo).getUrl();
                    }
                    return new SimpleEntry<>(artifactResult.getArtifact(), url);
                })
                .collect(Collectors.toList());
    }

    /**
     * Resolves transitive dependencies of specific maven artifact. Dependencies with scope {@code JavaScopes.COMPILE}, {@code JavaScopes.RUNTIME} returned only. Searches provided repository urls only.
     *
     * @param groupId      Maven group ID
     * @param artifactId   Maven artifact ID
     * @param version      Maven artifact version
     * @param classifier   Maven artifact classifier. May be null
     * @param repositories Maven repositories for transitive dependencies search
     * @return Transitive dependencies paired with their repository url, exception otherwise
     * @throws DependencyResolutionException thrown if dependency doesn't exist on provided repositories
     * @see #findTransitiveDependencies(String, String, String, String, List)
     */
    @NotNull
    public Collection<Entry<Artifact, @Nullable String>> findTransitiveDependencies(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @NotNull String classifier, @NotNull Stream<String> repositories) throws DependencyResolutionException {
        return findTransitiveDependencies(groupId, artifactId, version, classifier, repositories.map(TransitiveDependencyCollector::newDefaultRepository).collect(Collectors.toList()));
    }

    /**
     * Creates new session by provided {@link RepositorySystem}
     *
     * @return new session from {@link RepositorySystem}
     * @see MavenRepositorySystemUtils#newSession()
     */
    @NotNull
    private RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(saveDirectory.toAbsolutePath().toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        Properties properties = new Properties();
        properties.putAll(System.getProperties());

        session.setSystemProperties(properties);
        session.setConfigProperties(properties);

        session.setReadOnly(); // Don't allow to modify it further since DefaultRepositorySystemSession isn't thread-safe

        return session;
    }

    /**
     * Creates a new repository system
     *
     * @return New supplier repository system
     * @see RepositorySystemSupplier
     */
    @NotNull
    private RepositorySystem newRepositorySystem() {
        return new RepositorySystemSupplier().get();
    }

}
