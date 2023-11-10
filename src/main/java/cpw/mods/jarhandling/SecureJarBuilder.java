package cpw.mods.jarhandling;

import cpw.mods.jarhandling.impl.Jar;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Manifest;

public final class SecureJarBuilder {
    private Supplier<Manifest> defaultManifest = Manifest::new;
    @Nullable
    private BiPredicate<String, String> pathFilter = null;
    private Function<SecureJar, JarMetadata> metadataSupplier = null;
    private Path[] paths = new Path[0];

    public SecureJarBuilder() {}

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
     * Overrides the path filter for this jar.
     * TODO: wtf are the arguments passed to the path filter
     */
    public SecureJarBuilder pathFilter(@Nullable BiPredicate<String, String> pathFilter) {
        this.pathFilter = pathFilter;
        return this;
    }

    /**
     * Overrides the {@link JarMetadata} for this jar.
     * TODO: this function is
     */
    public SecureJarBuilder metadata(Function<SecureJar, JarMetadata> metadataSupplier) {
        Objects.requireNonNull(metadataSupplier);

        this.metadataSupplier = metadataSupplier;
        return this;
    }

    /**
     * Sets the paths for the files of this jar.
     */
    public SecureJarBuilder paths(Path... paths) {
        this.paths = paths;
        return this;
    }

    /**
     * Builds the jar.
     */
    public SecureJar build() {
        if (metadataSupplier == null) {
            metadataSupplier = jar -> JarMetadata.from(jar, paths);
        }

        return new Jar(defaultManifest, metadataSupplier, pathFilter, paths);
    }
}
