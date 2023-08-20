package cpw.mods.jarhandling;

import java.util.Set;

public interface SecureJarRuntime {
    /**
     * Return the set of top-level folders in the JARs that should not be searched for class files.
     */
    default Set<String> ignoredRootPackages() {
        return Set.of("META-INF");
    }
}
