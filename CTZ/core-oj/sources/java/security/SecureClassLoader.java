package java.security;

import java.nio.ByteBuffer;
import java.util.HashMap;
import sun.security.util.Debug;

public class SecureClassLoader extends ClassLoader {
    private static final Debug debug = Debug.getInstance("scl");
    private final boolean initialized;
    private final HashMap<CodeSource, ProtectionDomain> pdcache;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    protected SecureClassLoader(ClassLoader classLoader) {
        super(classLoader);
        this.pdcache = new HashMap<>(11);
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.initialized = true;
    }

    protected SecureClassLoader() {
        this.pdcache = new HashMap<>(11);
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkCreateClassLoader();
        }
        this.initialized = true;
    }

    protected final Class<?> defineClass(String str, byte[] bArr, int i, int i2, CodeSource codeSource) {
        return defineClass(str, bArr, i, i2, getProtectionDomain(codeSource));
    }

    protected final Class<?> defineClass(String str, ByteBuffer byteBuffer, CodeSource codeSource) {
        return defineClass(str, byteBuffer, getProtectionDomain(codeSource));
    }

    protected PermissionCollection getPermissions(CodeSource codeSource) {
        check();
        return new Permissions();
    }

    private ProtectionDomain getProtectionDomain(CodeSource codeSource) {
        ProtectionDomain protectionDomain;
        if (codeSource == null) {
            return null;
        }
        synchronized (this.pdcache) {
            protectionDomain = this.pdcache.get(codeSource);
            if (protectionDomain == null) {
                ProtectionDomain protectionDomain2 = new ProtectionDomain(codeSource, getPermissions(codeSource), this, null);
                this.pdcache.put(codeSource, protectionDomain2);
                if (debug != null) {
                    debug.println(" getPermissions " + ((Object) protectionDomain2));
                    debug.println("");
                }
                protectionDomain = protectionDomain2;
            }
        }
        return protectionDomain;
    }

    private void check() {
        if (!this.initialized) {
            throw new SecurityException("ClassLoader object not initialized");
        }
    }
}
