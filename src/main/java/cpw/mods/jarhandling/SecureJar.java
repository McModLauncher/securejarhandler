package cpw.mods.jarhandling;

import cpw.mods.jarhandling.impl.Jar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A secure jar is the full definition for a module,
 * including all its paths and code signing metadata.
 *
 * <p>An instance can be built with {@link SecureJarBuilder}.
 */
public interface SecureJar {
    /**
     * Creates a jar from a list of paths.
     * See {@link SecureJarBuilder} for more configuration options.
     */
    static SecureJar from(final Path... paths) {
        return new SecureJarBuilder().paths(paths).build();
    }

    ModuleDataProvider moduleDataProvider();

    Path getPrimaryPath();

    CodeSigner[] getManifestSigners();

    Status verifyPath(Path path);

    Status getFileStatus(String name);

    Attributes getTrustedManifestEntries(String name);

    boolean hasSecurityData();

    Set<String> getPackages();

    List<Provider> getProviders();

    String name();

    Path getPath(String first, String... rest);

    Path getRootPath();

    interface ModuleDataProvider {
        String name();
        ModuleDescriptor descriptor();
        URI uri();
        Optional<URI> findFile(String name);
        Optional<InputStream> open(final String name);

        Manifest getManifest();

        CodeSigner[] verifyAndGetSigners(String cname, byte[] bytes);
    }

    record Provider(String serviceName, List<String> providers) {
        public static Provider fromPath(final Path path, final BiPredicate<String, String> pkgFilter) {
            final var sname = path.getFileName().toString();
            try {
                var entries = Files.readAllLines(path).stream()
                        .map(String::trim)
                        .filter(l->l.length() > 0 && !l.startsWith("#"))
                        .filter(p-> pkgFilter == null || pkgFilter.test(p.replace('.','/'), ""))
                        .toList();
                return new Provider(sname, entries);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    enum Status {
        NONE, INVALID, UNVERIFIED, VERIFIED
    }

    // The methods below are deprecated for removal - use SecureJarBuilder instead!
    // TODO: add since

    @Deprecated(forRemoval = true)
    static SecureJar from(BiPredicate<String, String> filter, final Path... paths) {
        return from(jar->JarMetadata.from(jar, paths), filter, paths);
    }

    @Deprecated(forRemoval = true)
    static SecureJar from(Function<SecureJar, JarMetadata> metadataSupplier, final Path... paths) {
        return from(Manifest::new, metadataSupplier, paths);
    }

    @Deprecated(forRemoval = true)
    static SecureJar from(Function<SecureJar, JarMetadata> metadataSupplier, BiPredicate<String, String> filter, final Path... paths) {
        return from(Manifest::new, metadataSupplier, filter, paths);
    }

    @Deprecated(forRemoval = true)
    static SecureJar from(Supplier<Manifest> defaultManifest, Function<SecureJar, JarMetadata> metadataSupplier, final Path... paths) {
        return from(defaultManifest, metadataSupplier, null, paths);
    }

    @Deprecated(forRemoval = true)
    static SecureJar from(Supplier<Manifest> defaultManifest, Function<SecureJar, JarMetadata> metadataSupplier, BiPredicate<String, String> filter, final Path... paths) {
        return new Jar(defaultManifest, metadataSupplier, filter, paths);
    }
}
