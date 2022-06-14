package cpw.mods.cl;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.util.LambdaExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JarModuleFinder implements ModuleFinder {
    private final Map<String, ModuleReference> moduleReferenceMap;

    JarModuleFinder(final Map<String, Set<String>> additionalPackages, final SecureJar... jars) {
        this.moduleReferenceMap = Arrays.stream(jars)
                .collect(Collectors.toMap(SecureJar::name, jar -> new JarModuleReference((Jar)jar, additionalPackages.getOrDefault(jar.name(), Set.of())), (j1, j2) -> j1));
    }

    @Override
    public Optional<ModuleReference> find(final String name) {
        return Optional.ofNullable(moduleReferenceMap.get(name));
    }

    @Override
    public Set<ModuleReference> findAll() {
        return new HashSet<>(moduleReferenceMap.values());
    }
    
    public static JarModuleFinder of(final SecureJar... jars) {
        return of(Map.of(), jars);
    }

    public static JarModuleFinder of(final Map<String, Set<String>> additionalPackages, final SecureJar... jars) {
        return new JarModuleFinder(additionalPackages, jars);
    }

    static class JarModuleReference extends ModuleReference {
        private final Jar jar;

        JarModuleReference(final Jar jar, final Set<String> additionalPackages) {
            super(jar.computeDescriptor(additionalPackages), jar.getURI());
            this.jar = jar;
        }

        @Override
        public ModuleReader open() throws IOException {
            return new JarModuleReader(this.jar);
        }

        public Jar jar() {
            return this.jar;
        }
    }

    static class JarModuleReader implements ModuleReader {
        private final Jar jar;

        public JarModuleReader(final Jar jar) {
            this.jar = jar;
        }

        @Override
        public Optional<URI> find(final String name) throws IOException {
            return jar.findFile(name);
        }

        @Override
        public Optional<InputStream> open(final String name) throws IOException {
            return jar.findFile(name).map(Paths::get).map(LambdaExceptionUtils.rethrowFunction(Files::newInputStream));
        }

        @Override
        public Stream<String> list() throws IOException {
            return null;
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public String toString() {
            return this.getClass().getName() + "[jar=" + jar + "]";
        }
    }
}
