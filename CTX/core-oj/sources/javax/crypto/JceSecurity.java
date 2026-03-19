package javax.crypto;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import sun.security.jca.GetInstance;

final class JceSecurity {
    private static final URL NULL_URL;
    private static final Map<Class<?>, URL> codeBaseCacheRef;
    static final SecureRandom RANDOM = new SecureRandom();
    private static CryptoPermissions defaultPolicy = null;
    private static CryptoPermissions exemptPolicy = null;
    private static final Map<Provider, Object> verificationResults = new IdentityHashMap();
    private static final Map<Provider, Object> verifyingProviders = new IdentityHashMap();
    private static final Object PROVIDER_VERIFIED = Boolean.TRUE;

    static {
        try {
            NULL_URL = new URL("http://null.oracle.com/");
            codeBaseCacheRef = new WeakHashMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JceSecurity() {
    }

    static GetInstance.Instance getInstance(String str, Class<?> cls, String str2, String str3) throws NoSuchAlgorithmException, NoSuchProviderException {
        Provider.Service service = GetInstance.getService(str, str2, str3);
        Exception verificationResult = getVerificationResult(service.getProvider());
        if (verificationResult != null) {
            throw ((NoSuchProviderException) new NoSuchProviderException("JCE cannot authenticate the provider " + str3).initCause(verificationResult));
        }
        return GetInstance.getInstance(service, cls);
    }

    static GetInstance.Instance getInstance(String str, Class<?> cls, String str2, Provider provider) throws NoSuchAlgorithmException {
        Provider.Service service = GetInstance.getService(str, str2, provider);
        Exception verificationResult = getVerificationResult(provider);
        if (verificationResult != null) {
            throw new SecurityException("JCE cannot authenticate the provider " + provider.getName(), verificationResult);
        }
        return GetInstance.getInstance(service, cls);
    }

    static GetInstance.Instance getInstance(String str, Class<?> cls, String str2) throws NoSuchAlgorithmException {
        NoSuchAlgorithmException e = null;
        for (Provider.Service service : GetInstance.getServices(str, str2)) {
            if (canUseProvider(service.getProvider())) {
                try {
                    return GetInstance.getInstance(service, cls);
                } catch (NoSuchAlgorithmException e2) {
                    e = e2;
                }
            }
        }
        throw new NoSuchAlgorithmException("Algorithm " + str2 + " not available", e);
    }

    static CryptoPermissions verifyExemptJar(URL url) throws Exception {
        JarVerifier jarVerifier = new JarVerifier(url, true);
        jarVerifier.verify();
        return jarVerifier.getPermissions();
    }

    static void verifyProviderJar(URL url) throws Exception {
        new JarVerifier(url, false).verify();
    }

    static synchronized Exception getVerificationResult(Provider provider) {
        Object obj = verificationResults.get(provider);
        if (obj == PROVIDER_VERIFIED) {
            return null;
        }
        if (obj != null) {
            return (Exception) obj;
        }
        if (verifyingProviders.get(provider) != null) {
            return new NoSuchProviderException("Recursion during verification");
        }
        try {
            verifyingProviders.put(provider, Boolean.FALSE);
            verifyProviderJar(getCodeBase(provider.getClass()));
            verificationResults.put(provider, PROVIDER_VERIFIED);
            return null;
        } catch (Exception e) {
            verificationResults.put(provider, e);
            return e;
        } finally {
            verifyingProviders.remove(provider);
        }
    }

    static boolean canUseProvider(Provider provider) {
        return true;
    }

    static URL getCodeBase(final Class<?> cls) {
        URL url;
        synchronized (codeBaseCacheRef) {
            url = codeBaseCacheRef.get(cls);
            if (url == null) {
                url = (URL) AccessController.doPrivileged(new PrivilegedAction<URL>() {
                    @Override
                    public URL run() {
                        CodeSource codeSource;
                        ProtectionDomain protectionDomain = cls.getProtectionDomain();
                        if (protectionDomain == null || (codeSource = protectionDomain.getCodeSource()) == null) {
                            return JceSecurity.NULL_URL;
                        }
                        return codeSource.getLocation();
                    }
                });
                codeBaseCacheRef.put(cls, url);
            }
            if (url == NULL_URL) {
                url = null;
            }
        }
        return url;
    }

    private static void loadPolicies(File file, CryptoPermissions cryptoPermissions, CryptoPermissions cryptoPermissions2) throws Exception {
        InputStream inputStream;
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> enumerationEntries = jarFile.entries();
        while (enumerationEntries.hasMoreElements()) {
            JarEntry jarEntryNextElement = enumerationEntries.nextElement();
            InputStream inputStream2 = null;
            try {
                if (jarEntryNextElement.getName().startsWith("default_")) {
                    inputStream = jarFile.getInputStream(jarEntryNextElement);
                    try {
                        cryptoPermissions.load(inputStream);
                    } catch (Throwable th) {
                        th = th;
                        inputStream2 = inputStream;
                        if (inputStream2 != null) {
                            inputStream2.close();
                        }
                        throw th;
                    }
                } else if (jarEntryNextElement.getName().startsWith("exempt_")) {
                    inputStream = jarFile.getInputStream(jarEntryNextElement);
                    cryptoPermissions2.load(inputStream);
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                JarVerifier.verifyPolicySigned(jarEntryNextElement.getCertificates());
            } catch (Throwable th2) {
                th = th2;
            }
        }
        jarFile.close();
    }

    static CryptoPermissions getDefaultPolicy() {
        return defaultPolicy;
    }

    static CryptoPermissions getExemptPolicy() {
        return exemptPolicy;
    }
}
