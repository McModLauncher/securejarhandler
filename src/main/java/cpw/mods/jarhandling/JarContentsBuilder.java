package cpw.mods.jarhandling;

import cpw.mods.jarhandling.impl.JarContentsImpl;
import cpw.mods.niofs.union.UnionPathFilter;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.Manifest;

/**
 * Builder for {@link JarContents}.
 */
public final class JarContentsBuilder {
    private Path[] paths = new Path[0];
    private Supplier<Manifest> defaultManifest = Manifest::new;
    private Set<String> ignoredRootPackages = Set.of();
    @Nullable
    private UnionPathFilter pathFilter = null;

    public JarContentsBuilder() {}

    /**
     * Sets the root paths for the files of this jar.
     */
    public JarContentsBuilder paths(Path... paths) {
        this.paths = paths;
        return this;
    }

    /**
     * Overrides the default manifest for this jar.
     * The default manifest is only used when the jar does not provide a manifest already.
     */
    public JarContentsBuilder defaultManifest(Supplier<Manifest> manifest) {
        Objects.requireNonNull(manifest);

        this.defaultManifest = manifest;
        return this;
    }

    /**
     * Overrides the path filter for this jar, to exclude some entries from the underlying file system.
     *
     * @see UnionPathFilter
     */
    public JarContentsBuilder pathFilter(@Nullable UnionPathFilter pathFilter) {
        this.pathFilter = pathFilter;
        return this;
    }

    /**
     * Exclude some root folders from being scanned for code.
     * This can be used to skip scanning of folders that are known to not contain code,
     * but would be expensive to go through.
     */
    public JarContentsBuilder ignoreRootPackages(String... ignoredRootPackages) {
        Objects.requireNonNull(ignoredRootPackages);

        this.ignoredRootPackages = Set.of(ignoredRootPackages);
        return this;
    }

    /**
     * Builds the jar.
     */
    public JarContents build() {
        return new JarContentsImpl(paths, defaultManifest, ignoredRootPackages, pathFilter == null ? null : pathFilter::test);
    }
}
