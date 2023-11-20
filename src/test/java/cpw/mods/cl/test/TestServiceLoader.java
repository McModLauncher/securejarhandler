package cpw.mods.cl.test;

import cpw.mods.cl.ModuleClassLoader;
import org.junit.jupiter.api.Test;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;
import java.nio.file.spi.FileSystemProvider;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestServiceLoader {
    /**
     * Tests that we can load services from modules that are part of the boot layer.
     * In principle this also tests that services correctly get loaded from parent module layers too.
     */
    @Test
    public void testLoadServiceFromBootLayer() throws Exception {
        TestjarUtil.withTestJarSetup(cl -> {
            // We expect to find at least the unionfs provider
            ServiceLoader<FileSystemProvider> sl = TestjarUtil.loadTestjar(cl, FileSystemProvider.class);
            boolean foundUnionFsProvider = sl.stream().map(ServiceLoader.Provider::get).anyMatch(p -> p.getScheme().equals("union"));

            assertTrue(foundUnionFsProvider, "Expected to be able to find the UFS provider");
        });
    }

    /**
     * Tests that services that would normally be loaded from the classpath
     * do not get loaded by {@link ModuleClassLoader}.
     * In other words, test that our class loader isolation also works with services.
     */
    @Test
    public void testClassPathServiceDoesNotLeak() throws Exception {
        // Test that the DummyURLStreamHandlerProvider service provider can be loaded from the classpath
        var foundService = TestjarUtil.loadClasspath(TestServiceLoader.class.getClassLoader(), URLStreamHandlerProvider.class)
                        .stream()
                        .anyMatch(p -> p.type().getName().startsWith("cpw.mods.classpathtestjar"));

        assertTrue(foundService, "Could not find service in classpath using application class loader!");

        TestjarUtil.withTestJarSetup(cl -> {
            // Test that the DummyURLStreamHandlerProvider service provider cannot be loaded
            // from the classpath via ModuleClassLoader
            var foundServiceMCL = TestjarUtil.loadTestjar(cl, URLStreamHandlerProvider.class)
                    .stream()
                    .anyMatch(p -> p.type().getName().startsWith("cpw.mods.classpathtestjar"));

            assertFalse(foundServiceMCL, "Could find service in classpath using application class loader!");
        });
    }
}
