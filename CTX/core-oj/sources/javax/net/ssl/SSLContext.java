package javax.net.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import sun.security.jca.GetInstance;

public class SSLContext {
    private static SSLContext defaultContext;
    private final SSLContextSpi contextSpi;
    private final String protocol;
    private final Provider provider;

    protected SSLContext(SSLContextSpi sSLContextSpi, Provider provider, String str) {
        this.contextSpi = sSLContextSpi;
        this.provider = provider;
        this.protocol = str;
    }

    public static synchronized SSLContext getDefault() throws NoSuchAlgorithmException {
        if (defaultContext == null) {
            defaultContext = getInstance("Default");
        }
        return defaultContext;
    }

    public static synchronized void setDefault(SSLContext sSLContext) {
        if (sSLContext == null) {
            throw new NullPointerException();
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new SSLPermission("setDefaultSSLContext"));
        }
        defaultContext = sSLContext;
    }

    public static SSLContext getInstance(String str) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("SSLContext", (Class<?>) SSLContextSpi.class, str);
        return new SSLContext((SSLContextSpi) getInstance.impl, getInstance.provider, str);
    }

    public static SSLContext getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        GetInstance.Instance getInstance = GetInstance.getInstance("SSLContext", (Class<?>) SSLContextSpi.class, str, str2);
        return new SSLContext((SSLContextSpi) getInstance.impl, getInstance.provider, str);
    }

    public static SSLContext getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("SSLContext", (Class<?>) SSLContextSpi.class, str, provider);
        return new SSLContext((SSLContextSpi) getInstance.impl, getInstance.provider, str);
    }

    public final String getProtocol() {
        return this.protocol;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(KeyManager[] keyManagerArr, TrustManager[] trustManagerArr, SecureRandom secureRandom) throws KeyManagementException {
        this.contextSpi.engineInit(keyManagerArr, trustManagerArr, secureRandom);
    }

    public final SSLSocketFactory getSocketFactory() {
        return this.contextSpi.engineGetSocketFactory();
    }

    public final SSLServerSocketFactory getServerSocketFactory() {
        return this.contextSpi.engineGetServerSocketFactory();
    }

    public final SSLEngine createSSLEngine() {
        try {
            return this.contextSpi.engineCreateSSLEngine();
        } catch (AbstractMethodError e) {
            UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("Provider: " + ((Object) getProvider()) + " doesn't support this operation");
            unsupportedOperationException.initCause(e);
            throw unsupportedOperationException;
        }
    }

    public final SSLEngine createSSLEngine(String str, int i) {
        try {
            return this.contextSpi.engineCreateSSLEngine(str, i);
        } catch (AbstractMethodError e) {
            UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException("Provider: " + ((Object) getProvider()) + " does not support this operation");
            unsupportedOperationException.initCause(e);
            throw unsupportedOperationException;
        }
    }

    public final SSLSessionContext getServerSessionContext() {
        return this.contextSpi.engineGetServerSessionContext();
    }

    public final SSLSessionContext getClientSessionContext() {
        return this.contextSpi.engineGetClientSessionContext();
    }

    public final SSLParameters getDefaultSSLParameters() {
        return this.contextSpi.engineGetDefaultSSLParameters();
    }

    public final SSLParameters getSupportedSSLParameters() {
        return this.contextSpi.engineGetSupportedSSLParameters();
    }
}
