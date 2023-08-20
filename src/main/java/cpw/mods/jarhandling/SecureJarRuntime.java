package cpw.mods.jarhandling;

import java.util.Set;

public interface SecureJarRuntime {
    default Set<String> ignoredRootPackages() {
        return Set.of("META-INF");
    }
}
