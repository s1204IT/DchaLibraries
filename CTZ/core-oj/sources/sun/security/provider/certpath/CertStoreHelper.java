package sun.security.provider.certpath;

import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Cache;

public abstract class CertStoreHelper {
    private static final int NUM_TYPES = 2;
    private static Cache<String, CertStoreHelper> cache;
    private static final Map<String, String> classMap = new HashMap(2);

    public abstract CertStore getCertStore(URI uri) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    public abstract boolean isCausedByNetworkIssue(CertStoreException certStoreException);

    public abstract X509CRLSelector wrap(X509CRLSelector x509CRLSelector, Collection<X500Principal> collection, String str) throws IOException;

    public abstract X509CertSelector wrap(X509CertSelector x509CertSelector, X500Principal x500Principal, String str) throws IOException;

    static {
        classMap.put("LDAP", "sun.security.provider.certpath.ldap.LDAPCertStoreHelper");
        classMap.put("SSLServer", "sun.security.provider.certpath.ssl.SSLServerCertStoreHelper");
        cache = Cache.newSoftMemoryCache(2);
    }

    public static CertStoreHelper getInstance(final String str) throws NoSuchAlgorithmException {
        CertStoreHelper certStoreHelper = cache.get(str);
        if (certStoreHelper != null) {
            return certStoreHelper;
        }
        final String str2 = classMap.get(str);
        if (str2 == null) {
            throw new NoSuchAlgorithmException(str + " not available");
        }
        try {
            return (CertStoreHelper) AccessController.doPrivileged(new PrivilegedExceptionAction<CertStoreHelper>() {
                @Override
                public CertStoreHelper run() throws ClassNotFoundException {
                    try {
                        CertStoreHelper certStoreHelper2 = (CertStoreHelper) Class.forName(str2, true, null).newInstance();
                        CertStoreHelper.cache.put(str, certStoreHelper2);
                        return certStoreHelper2;
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new AssertionError(e);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw new NoSuchAlgorithmException(str + " not available", e.getException());
        }
    }

    static boolean isCausedByNetworkIssue(String str, CertStoreException certStoreException) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 84300) {
            if (iHashCode != 2331559) {
                b = (iHashCode == 133315663 && str.equals("SSLServer")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("LDAP")) {
                b = 0;
            }
        } else if (str.equals("URI")) {
            b = 2;
        }
        switch (b) {
            case 2:
                Throwable cause = certStoreException.getCause();
                if (cause == null || !(cause instanceof IOException)) {
                }
                break;
        }
        return false;
    }
}
