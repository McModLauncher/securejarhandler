package cpw.mods.jarhandling.impl;

import cpw.mods.jarhandling.SecureJarRuntime;

import java.util.ServiceLoader;
import java.util.Set;

class SecureJarEnvironment {
    static final SecureJarRuntime runtime = findRuntime();
    static final Set<String> ignoredRootPackages = findIgnoredRootPackages();

    private static SecureJarRuntime findRuntime() {
        return ServiceLoader.load(SecureJarRuntime.class).findFirst().orElse(new SecureJarRuntime() {});
    }

    private static Set<String> findIgnoredRootPackages() {
        var packages = runtime.ignoredRootPackages();
        // Validate root packages
        for (var pkg : packages) {
            if (pkg.contains(".") || pkg.contains("/")) {
                throw new IllegalArgumentException("Invalid root package: " + pkg);
            }
        }
        return packages;
    }

    public static boolean isIgnoredRootPackage(String pkg) {
        return ignoredRootPackages.contains(pkg);
    }
}
