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

/**
 * Builder for a {@link SecureJar}.
 *
 * @see JarContentsBuilder
 */
public final class SecureJarBuilder {
    private final JarContentsBuilder contentsBuilder = new JarContentsBuilder();
    @Nullable
    private JarContentsImpl contents;
    private Function<JarContents, JarMetadata> metadataSupplier = JarMetadata::from;

    public SecureJarBuilder() {}

    /**
     * @see JarContentsBuilder#paths(Path...)
     */
    public SecureJarBuilder paths(Path... paths) {
        contentsBuilder.paths(paths);
        return this;
    }

    /**
     * @see JarContentsBuilder#defaultManifest(Supplier)
     */
    public SecureJarBuilder defaultManifest(Supplier<Manifest> manifest) {
        contentsBuilder.defaultManifest(manifest);
        return this;
    }

    /**
     * @see JarContentsBuilder#pathFilter(BiPredicate)
     */
    public SecureJarBuilder pathFilter(@Nullable BiPredicate<String, String> pathFilter) {
        contentsBuilder.pathFilter(pathFilter);
        return this;
    }

    /**
     * @see JarContentsBuilder#ignoreRootPackages(String...)
     */
    public SecureJarBuilder ignoreRootPackages(String... ignoredRootPackages) {
        contentsBuilder.ignoreRootPackages(ignoredRootPackages);
        return this;
    }

    /**
     * Directly overrides the {@link JarContents} for this jar.
     * If set, all the previous builder methods are ignored.
     */
    public SecureJarBuilder contents(JarContents contents) {
        this.contents = (JarContentsImpl) contents;
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
        JarContents contents = this.contents == null ? contentsBuilder.build() : this.contents;
        JarMetadata metadata = metadataSupplier.apply(contents);
        return new Jar((JarContentsImpl) contents, metadata);
    }
}
