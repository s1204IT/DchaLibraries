package java.security;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class SecureClassLoader extends ClassLoader {
    private HashMap<CodeSource, ProtectionDomain> pds;

    protected SecureClassLoader() {
        this.pds = new HashMap<>();
    }

    protected SecureClassLoader(ClassLoader parent) {
        super(parent);
        this.pds = new HashMap<>();
    }

    protected PermissionCollection getPermissions(CodeSource codesource) {
        return new Permissions();
    }

    protected final Class<?> defineClass(String name, byte[] b, int off, int len, CodeSource cs) {
        return cs == null ? defineClass(name, b, off, len) : defineClass(name, b, off, len, getPD(cs));
    }

    protected final Class<?> defineClass(String name, ByteBuffer b, CodeSource cs) {
        byte[] data = b.array();
        return cs == null ? defineClass(name, data, 0, data.length) : defineClass(name, data, 0, data.length, getPD(cs));
    }

    private ProtectionDomain getPD(CodeSource cs) {
        ProtectionDomain pd = null;
        if (cs != null) {
            synchronized (this.pds) {
                pd = this.pds.get(cs);
                if (pd == null) {
                    PermissionCollection perms = getPermissions(cs);
                    pd = new ProtectionDomain(cs, perms, this, null);
                    this.pds.put(cs, pd);
                }
            }
        }
        return pd;
    }
}
