package cpw.mods.jarhandling;

import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.JarContentsImpl;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Manifest;

public final class SecureJarBuilder {
    private Path[] paths = new Path[0];
    private Supplier<Manifest> defaultManifest = Manifest::new;
    private String[] ignoredRootPackages = new String[0];
    @Nullable
    private BiPredicate<String, String> pathFilter = null;
    private Function<JarContents, JarMetadata> metadataSupplier = JarMetadata::from;

    public SecureJarBuilder() {}

    /**
     * Sets the paths for the files of this jar.
     */
    public SecureJarBuilder paths(Path... paths) {
        this.paths = paths;
        return this;
    }

    /**
     * Overrides the default manifest for this jar.
     * The default manifest is only used when the jar does not provide a manifest already.
     */
    public SecureJarBuilder defaultManifest(Supplier<Manifest> manifest) {
        Objects.requireNonNull(manifest);

        this.defaultManifest = manifest;
        return this;
    }

    /**
     * Overrides the path filter for this jar, to exclude some entries from the underlying file system.
     *
     * <p>The second parameter to the filter is the base path, i.e. one of the paths passed to {@link #paths(Path...)}.
     * The first parameter to the filter is the path of the entry being checked, relative to the base path.
     */
    public SecureJarBuilder pathFilter(@Nullable BiPredicate<String, String> pathFilter) {
        this.pathFilter = pathFilter;
        return this;
    }

    /**
     * Exclude some root folders from being scanned for code.
     * This can be used to skip scanning of folders that are known to not contain code,
     * but would be expensive to go through.
     */
    public SecureJarBuilder ignoreRootPackages(String... ignoredRootPackages) {
        Objects.requireNonNull(ignoredRootPackages);

        this.ignoredRootPackages = ignoredRootPackages;
        return this;
    }

    /**
     * Overrides the {@link JarMetadata} for this jar.
     */
    public SecureJarBuilder metadata(Function<JarContents, JarMetadata> metadataSupplier) {
        Objects.requireNonNull(metadataSupplier);

        this.metadataSupplier = metadataSupplier;
        return this;
    }

    /**
     * Builds the jar.
     */
    public SecureJar build() {
        JarContentsImpl contents = new JarContentsImpl(paths, defaultManifest, ignoredRootPackages, pathFilter);
        JarMetadata metadata = metadataSupplier.apply(contents);
        return new Jar(contents, metadata);
    }
}
