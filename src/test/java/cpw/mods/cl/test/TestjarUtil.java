package cpw.mods.cl.test;

import cpw.mods.cl.JarModuleFinder;
import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.jarhandling.SecureJar;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;

public class TestjarUtil {
    /**
     * Load the {@code testjar} source set as new module into a new layer,
     * and run the callback with the new layer's classloader.
     */
    public static void withTestJarSetup(TestCallback callback) throws Exception {
        // Setup a child layer with a jar from the testjar sourceset.
        var testjarPath = Paths.get(System.getenv("sjh-testjar"));
        var childLayerJar = SecureJar.from(testjarPath);

        var roots = List.of(childLayerJar.name());
        var jf = JarModuleFinder.of(childLayerJar);
        var conf = Configuration.resolveAndBind(jf, List.of(ModuleLayer.boot().configuration()), ModuleFinder.of(), roots);
        var cl = new ModuleClassLoader("testLoadBootService", conf, List.of(ModuleLayer.boot()));
        ModuleLayer.defineModules(conf, List.of(ModuleLayer.boot()), m->cl);

        // Context classloader setup (to load services from the module CL)
        var previousCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        try {
            callback.test(cl);
        } finally {
            Thread.currentThread().setContextClassLoader(previousCl);
        }
    }

    @FunctionalInterface
    public interface TestCallback {
        void test(ClassLoader cl) throws Exception;
    }

    /**
     * Instantiates a {@link ServiceLoader} within the testjar module.
     */
    public static <S> ServiceLoader<S> loadTestjar(ClassLoader cl, Class<S> clazz) throws Exception {
        // Use the `load` method from the testjar sourceset.
        var testClass = cl.loadClass("cpw.mods.cl.testjar.ServiceLoaderTest");
        var loadMethod = testClass.getMethod("load", Class.class);
        //noinspection unchecked
        return (ServiceLoader<S>) loadMethod.invoke(null, clazz);
    }

    /**
     * Instantiates a {@link ServiceLoader} within the classpath source set.
     */
    public static <S> ServiceLoader<S> loadClasspath(ClassLoader cl, Class<S> clazz) throws Exception {
        // Use the `load` method from the testjar sourceset.
        var testClass = cl.loadClass("cpw.mods.classpathtestjar.ServiceLoaderTest");
        var loadMethod = testClass.getMethod("load", Class.class);
        //noinspection unchecked
        return (ServiceLoader<S>) loadMethod.invoke(null, clazz);
    }
}
