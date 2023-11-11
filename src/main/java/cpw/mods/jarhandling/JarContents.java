package cpw.mods.jarhandling;

import org.jetbrains.annotations.ApiStatus;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Access to the contents of a list of {@link Path}s, interpreted as a jar file.
 * Typically used to build the {@linkplain JarMetadata metadata} for a {@link SecureJar}.
 */
@ApiStatus.NonExtendable
public interface JarContents {
    /**
     * @see SecureJar#getPrimaryPath()
     */
    // TODO: document what this is actually used for
    Path getPrimaryPath();

    /**
     * Looks for a file in the jar.
     */
    Optional<URI> findFile(String name);

    /**
     * Gets the manifest of the jar.
     * Empty if no manifest is present in the jar.
     */
    Manifest getManifest();

    /**
     * Gets all the packages in the jar.
     * (Every folder containing a {@code .class} file is considered a package.)
     */
    Set<String> getPackages();

    /**
     * Parses the {@code META-INF/services} files in the jar, and returns the list of service providers.
     */
    List<SecureJar.Provider> getMetaInfServices();
}
