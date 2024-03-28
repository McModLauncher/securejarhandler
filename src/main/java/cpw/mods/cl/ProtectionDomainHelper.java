package cpw.mods.cl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URL;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ProtectionDomainHelper {
    private static final Map<URL, CodeSource> csCache = new HashMap<>();
    public static CodeSource createCodeSource(final URL url, final CodeSigner[] signers) {
        synchronized (csCache) {
            return csCache.computeIfAbsent(url, u->new CodeSource(url, signers));
        }
    }

    private static final Map<CodeSource, ProtectionDomain> pdCache = new HashMap<>();
    public static ProtectionDomain createProtectionDomain(CodeSource codeSource, ClassLoader cl) {
        synchronized (pdCache) {
            return pdCache.computeIfAbsent(codeSource, cs->{
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return new ProtectionDomain(codeSource, perms, cl, null);
            });
        }
    }

}
