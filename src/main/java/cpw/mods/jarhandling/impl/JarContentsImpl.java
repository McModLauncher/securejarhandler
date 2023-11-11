package cpw.mods.jarhandling.impl;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.niofs.union.UnionFileSystem;
import cpw.mods.niofs.union.UnionFileSystemProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static java.util.stream.Collectors.*;

public class JarContentsImpl implements JarContents {
    private static final UnionFileSystemProvider UFSP = (UnionFileSystemProvider) FileSystemProvider.installedProviders()
            .stream()
            .filter(fsp->fsp.getScheme().equals("union"))
            .findFirst()
            .orElseThrow(()->new IllegalStateException("Couldn't find UnionFileSystemProvider"));

    final UnionFileSystem filesystem;
    // Code signing data
    final JarSigningData signingData = new JarSigningData();
    // Manifest of the jar
    private final Manifest manifest;
    // Name overrides, if the jar is a multi-release jar
    private final Map<Path, Integer> nameOverrides;

    // Folders known to not contain packages
    private final Set<String> ignoredRootPackages;
    // Cache for repeated getPackages calls
    private Set<String> packages;
    // Cache for repeated getMetaInfServices calls
    private List<SecureJar.Provider> providers;

    public JarContentsImpl(Path[] paths, Supplier<Manifest> defaultManifest, String[] ignoredRootPackages, @Nullable BiPredicate<String, String> pathFilter) {
        var validPaths = Arrays.stream(paths).filter(Files::exists).toArray(Path[]::new);
        if (validPaths.length == 0)
            throw new UncheckedIOException(new IOException("Invalid paths argument, contained no existing paths: " + Arrays.toString(paths)));
        this.filesystem = UFSP.newFileSystem(pathFilter, validPaths);
        // Find the manifest, and read its signing data
        this.manifest = readManifestAndSigningData(defaultManifest, validPaths);
        // Read multi-release jar information
        this.nameOverrides = readMultiReleaseInfo();

        this.ignoredRootPackages = Set.of(ignoredRootPackages);
    }

    private Manifest readManifestAndSigningData(Supplier<Manifest> defaultManifest, Path[] validPaths) {
        try {
            for (int x = validPaths.length - 1; x >= 0; x--) { // Walk backwards because this is what cpw wanted?
                var path = validPaths[x];
                if (Files.isDirectory(path)) {
                    // Just a directory: read the manifest file, but don't do any signature verification
                    var manfile = path.resolve(JarFile.MANIFEST_NAME);
                    if (Files.exists(manfile)) {
                        try (var is = Files.newInputStream(manfile)) {
                            return new Manifest(is);
                        }
                    }
                } else {
                    try (var jis = new JarInputStream(Files.newInputStream(path))) {
                        // Jar file: use the signature verification code
                        signingData.readJarSigningData(jis);

                        if (jis.getManifest() != null) {
                            return new Manifest(jis.getManifest());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return defaultManifest.get();
    }

    private Map<Path, Integer> readMultiReleaseInfo() {
        boolean isMultiRelease = Boolean.parseBoolean(getManifest().getMainAttributes().getValue("Multi-Release"));
        if (isMultiRelease) {
            var vers = filesystem.getRoot().resolve("META-INF/versions");
            try (var walk = Files.walk(vers)){
                var allnames = walk.filter(p1 ->!p1.isAbsolute())
                        .filter(path1 -> !Files.isDirectory(path1))
                        .map(p1 -> p1.subpath(2, p1.getNameCount()))
                        .collect(groupingBy(p->p.subpath(1, p.getNameCount()),
                                mapping(p->Integer.parseInt(p.getName(0).toString()), toUnmodifiableList())));
                return allnames.entrySet().stream()
                        .map(e->Map.entry(e.getKey(), e.getValue().stream().reduce(Integer::max).orElse(8)))
                        .filter(e-> e.getValue() < Runtime.version().feature())
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        } else {
            return Map.of();
        }
    }

    @Override
    public Path getPrimaryPath() {
        return filesystem.getPrimaryPath();
    }

    @Override
    public Optional<URI> findFile(String name) {
        var rel = filesystem.getPath(name);
        if (this.nameOverrides.containsKey(rel)) {
            rel = this.filesystem.getPath("META-INF", "versions", this.nameOverrides.get(rel).toString()).resolve(rel);
        }
        return Optional.of(this.filesystem.getRoot().resolve(rel)).filter(Files::exists).map(Path::toUri);
    }

    @Override
    public Manifest getManifest() {
        return manifest;
    }

    @Override
    public Set<String> getPackages() {
        if (this.packages == null) {
            Set<String> packages = new HashSet<>();
            try {
                Files.walkFileTree(this.filesystem.getRoot(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(".class") && attrs.isRegularFile()) {
                            var pkg = file.getParent().toString().replace('/', '.');
                            if (!pkg.isEmpty()) {
                                packages.add(pkg);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
                        if (path.getNameCount() > 0 && ignoredRootPackages.contains(path.getName(0).toString())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                this.packages = Set.copyOf(packages);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this.packages;
    }

    @Override
    public List<SecureJar.Provider> getMetaInfServices() {
        if (this.providers == null) {
            final var services = this.filesystem.getRoot().resolve("META-INF/services/");
            if (Files.exists(services)) {
                try (var walk = Files.walk(services)) {
                    this.providers = walk.filter(path->!Files.isDirectory(path))
                            .map((Path path1) -> SecureJar.Provider.fromPath(path1, filesystem.getFilesystemFilter()))
                            .toList();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                this.providers = List.of();
            }
        }
        return this.providers;
    }
}
