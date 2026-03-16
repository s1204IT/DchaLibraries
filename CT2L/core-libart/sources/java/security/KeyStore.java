package java.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.callback.CallbackHandler;
import libcore.io.IoUtils;
import org.apache.harmony.security.fortress.Engine;

public class KeyStore {
    private static final String DEFAULT_KEYSTORE_TYPE = "jks";
    private static final String PROPERTY_NAME = "keystore.type";
    private final KeyStoreSpi implSpi;
    private boolean isInit = false;
    private final Provider provider;
    private final String type;
    private static final String SERVICE = "KeyStore";
    private static final Engine ENGINE = new Engine(SERVICE);

    public interface Entry {
    }

    public interface LoadStoreParameter {
        ProtectionParameter getProtectionParameter();
    }

    public interface ProtectionParameter {
    }

    protected KeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
        this.type = type;
        this.provider = provider;
        this.implSpi = keyStoreSpi;
    }

    private static void throwNotInitialized() throws KeyStoreException {
        throw new KeyStoreException("KeyStore was not initialized");
    }

    public static KeyStore getInstance(String type) throws KeyStoreException {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        try {
            Engine.SpiAndProvider sap = ENGINE.getInstance(type, (Object) null);
            return new KeyStore((KeyStoreSpi) sap.spi, sap.provider, type);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        }
    }

    public static KeyStore getInstance(String type, String provider) throws KeyStoreException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Provider impProvider = Security.getProvider(provider);
        if (impProvider == null) {
            throw new NoSuchProviderException(provider);
        }
        try {
            return getInstance(type, impProvider);
        } catch (Exception e) {
            throw new KeyStoreException(e);
        }
    }

    public static KeyStore getInstance(String type, Provider provider) throws KeyStoreException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        try {
            Object spi = ENGINE.getInstance(type, provider, null);
            return new KeyStore((KeyStoreSpi) spi, provider, type);
        } catch (Exception e) {
            throw new KeyStoreException(e);
        }
    }

    public static final String getDefaultType() {
        String dt = Security.getProperty(PROPERTY_NAME);
        return dt == null ? DEFAULT_KEYSTORE_TYPE : dt;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getType() {
        return this.type;
    }

    public final Key getKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineGetKey(alias, password);
    }

    public final java.security.cert.Certificate[] getCertificateChain(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineGetCertificateChain(alias);
    }

    public final java.security.cert.Certificate getCertificate(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineGetCertificate(alias);
    }

    public final Date getCreationDate(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineGetCreationDate(alias);
    }

    public final void setKeyEntry(String alias, Key key, char[] password, java.security.cert.Certificate[] chain) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        if (key != null && (key instanceof PrivateKey) && (chain == null || chain.length == 0)) {
            throw new IllegalArgumentException("Certificate chain is not defined for Private key");
        }
        this.implSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    public final void setKeyEntry(String alias, byte[] key, java.security.cert.Certificate[] chain) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        this.implSpi.engineSetKeyEntry(alias, key, chain);
    }

    public final void setCertificateEntry(String alias, java.security.cert.Certificate cert) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        this.implSpi.engineSetCertificateEntry(alias, cert);
    }

    public final void deleteEntry(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        this.implSpi.engineDeleteEntry(alias);
    }

    public final Enumeration<String> aliases() throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineAliases();
    }

    public final boolean containsAlias(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineContainsAlias(alias);
    }

    public final int size() throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineSize();
    }

    public final boolean isKeyEntry(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineIsKeyEntry(alias);
    }

    public final boolean isCertificateEntry(String alias) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineIsCertificateEntry(alias);
    }

    public final String getCertificateAlias(java.security.cert.Certificate cert) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineGetCertificateAlias(cert);
    }

    public final void store(OutputStream stream, char[] password) throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        this.implSpi.engineStore(stream, password);
    }

    public final void store(LoadStoreParameter param) throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        this.implSpi.engineStore(param);
    }

    public final void load(InputStream stream, char[] password) throws NoSuchAlgorithmException, IOException, CertificateException {
        this.implSpi.engineLoad(stream, password);
        this.isInit = true;
    }

    public final void load(LoadStoreParameter param) throws NoSuchAlgorithmException, IOException, CertificateException {
        this.implSpi.engineLoad(param);
        this.isInit = true;
    }

    public final Entry getEntry(String alias, ProtectionParameter param) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineGetEntry(alias, param);
    }

    public final void setEntry(String alias, Entry entry, ProtectionParameter param) throws KeyStoreException {
        if (!this.isInit) {
            throwNotInitialized();
        }
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        if (entry == null) {
            throw new NullPointerException("entry == null");
        }
        this.implSpi.engineSetEntry(alias, entry, param);
    }

    public final boolean entryInstanceOf(String alias, Class<? extends Entry> entryClass) throws KeyStoreException {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        if (entryClass == null) {
            throw new NullPointerException("entryClass == null");
        }
        if (!this.isInit) {
            throwNotInitialized();
        }
        return this.implSpi.engineEntryInstanceOf(alias, entryClass);
    }

    public static abstract class Builder {
        public abstract KeyStore getKeyStore() throws KeyStoreException;

        public abstract ProtectionParameter getProtectionParameter(String str) throws KeyStoreException;

        protected Builder() {
        }

        public static Builder newInstance(KeyStore keyStore, ProtectionParameter protectionParameter) {
            if (keyStore == null) {
                throw new NullPointerException("keyStore == null");
            }
            if (protectionParameter != null) {
                if (!keyStore.isInit) {
                    throw new IllegalArgumentException("KeyStore was not initialized");
                }
                return new BuilderImpl(keyStore, protectionParameter, null, null, null);
            }
            throw new NullPointerException("protectionParameter == null");
        }

        public static Builder newInstance(String type, Provider provider, File file, ProtectionParameter protectionParameter) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            if (protectionParameter == null) {
                throw new NullPointerException("protectionParameter == null");
            }
            if (file == null) {
                throw new NullPointerException("file == null");
            }
            if (!(protectionParameter instanceof PasswordProtection) && !(protectionParameter instanceof CallbackHandlerProtection)) {
                throw new IllegalArgumentException("protectionParameter is neither PasswordProtection nor CallbackHandlerProtection instance");
            }
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + file.getName());
            }
            if (!file.isFile()) {
                throw new IllegalArgumentException("Not a regular file: " + file.getName());
            }
            return new BuilderImpl(null, protectionParameter, file, type, provider);
        }

        public static Builder newInstance(String type, Provider provider, ProtectionParameter protectionParameter) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            if (protectionParameter == null) {
                throw new NullPointerException("protectionParameter == null");
            }
            return new BuilderImpl(null, protectionParameter, null, type, provider);
        }

        private static class BuilderImpl extends Builder {
            private final File fileForLoad;
            private boolean isGetKeyStore;
            private KeyStore keyStore;
            private KeyStoreException lastException = null;
            private ProtectionParameter protParameter;
            private final Provider providerForKeyStore;
            private final String typeForKeyStore;

            BuilderImpl(KeyStore ks, ProtectionParameter pp, File file, String type, Provider provider) {
                this.isGetKeyStore = false;
                this.keyStore = ks;
                this.protParameter = pp;
                this.fileForLoad = file;
                this.typeForKeyStore = type;
                this.providerForKeyStore = provider;
                this.isGetKeyStore = false;
            }

            @Override
            public synchronized KeyStore getKeyStore() throws KeyStoreException {
                KeyStore ks;
                char[] passwd;
                if (this.lastException != null) {
                    throw this.lastException;
                }
                if (this.keyStore != null) {
                    this.isGetKeyStore = true;
                    ks = this.keyStore;
                } else {
                    try {
                        ks = this.providerForKeyStore == null ? KeyStore.getInstance(this.typeForKeyStore) : KeyStore.getInstance(this.typeForKeyStore, this.providerForKeyStore);
                        if (this.protParameter instanceof PasswordProtection) {
                            passwd = ((PasswordProtection) this.protParameter).getPassword();
                        } else if (this.protParameter instanceof CallbackHandlerProtection) {
                            passwd = KeyStoreSpi.getPasswordFromCallBack(this.protParameter);
                        } else {
                            throw new KeyStoreException("protectionParameter is neither PasswordProtection nor CallbackHandlerProtection instance");
                        }
                        if (this.fileForLoad != null) {
                            FileInputStream fis = null;
                            try {
                                FileInputStream fis2 = new FileInputStream(this.fileForLoad);
                                try {
                                    ks.load(fis2, passwd);
                                    IoUtils.closeQuietly(fis2);
                                } catch (Throwable th) {
                                    th = th;
                                    fis = fis2;
                                    IoUtils.closeQuietly(fis);
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } else {
                            ks.load(new TmpLSParameter(this.protParameter));
                        }
                        this.isGetKeyStore = true;
                    } catch (KeyStoreException e) {
                        this.lastException = e;
                        throw e;
                    } catch (Exception e2) {
                        KeyStoreException keyStoreException = new KeyStoreException(e2);
                        this.lastException = keyStoreException;
                        throw keyStoreException;
                    }
                }
                return ks;
            }

            @Override
            public synchronized ProtectionParameter getProtectionParameter(String alias) throws KeyStoreException {
                if (alias == null) {
                    throw new NullPointerException("alias == null");
                }
                if (!this.isGetKeyStore) {
                    throw new IllegalStateException("getKeyStore() was not invoked");
                }
                return this.protParameter;
            }
        }

        private static class TmpLSParameter implements LoadStoreParameter {
            private final ProtectionParameter protPar;

            public TmpLSParameter(ProtectionParameter protPar) {
                this.protPar = protPar;
            }

            @Override
            public ProtectionParameter getProtectionParameter() {
                return this.protPar;
            }
        }
    }

    public static class CallbackHandlerProtection implements ProtectionParameter {
        private final CallbackHandler callbackHandler;

        public CallbackHandlerProtection(CallbackHandler handler) {
            if (handler == null) {
                throw new NullPointerException("handler == null");
            }
            this.callbackHandler = handler;
        }

        public CallbackHandler getCallbackHandler() {
            return this.callbackHandler;
        }
    }

    public static class PasswordProtection implements ProtectionParameter, Destroyable {
        private boolean isDestroyed = false;
        private char[] password;

        public PasswordProtection(char[] password) {
            if (password != null) {
                this.password = (char[]) password.clone();
            }
        }

        public synchronized char[] getPassword() {
            if (this.isDestroyed) {
                throw new IllegalStateException("Password was destroyed");
            }
            return this.password;
        }

        @Override
        public synchronized void destroy() throws DestroyFailedException {
            this.isDestroyed = true;
            if (this.password != null) {
                Arrays.fill(this.password, (char) 0);
                this.password = null;
            }
        }

        @Override
        public synchronized boolean isDestroyed() {
            return this.isDestroyed;
        }
    }

    public static final class PrivateKeyEntry implements Entry {
        private java.security.cert.Certificate[] chain;
        private PrivateKey privateKey;

        public PrivateKeyEntry(PrivateKey privateKey, java.security.cert.Certificate[] chain) {
            if (privateKey == null) {
                throw new NullPointerException("privateKey == null");
            }
            if (chain == null) {
                throw new NullPointerException("chain == null");
            }
            if (chain.length == 0) {
                throw new IllegalArgumentException("chain.length == 0");
            }
            String s = chain[0].getType();
            if (!chain[0].getPublicKey().getAlgorithm().equals(privateKey.getAlgorithm())) {
                throw new IllegalArgumentException("Algorithm of private key does not match algorithm of public key in end certificate of entry (with index number: 0)");
            }
            for (int i = 1; i < chain.length; i++) {
                if (!s.equals(chain[i].getType())) {
                    throw new IllegalArgumentException("Certificates from the given chain have different types");
                }
            }
            boolean isAllX509Certificates = true;
            int len$ = chain.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                java.security.cert.Certificate cert = chain[i$];
                if (cert instanceof X509Certificate) {
                    i$++;
                } else {
                    isAllX509Certificates = false;
                    break;
                }
            }
            if (isAllX509Certificates) {
                this.chain = new X509Certificate[chain.length];
            } else {
                this.chain = new java.security.cert.Certificate[chain.length];
            }
            System.arraycopy(chain, 0, this.chain, 0, chain.length);
            this.privateKey = privateKey;
        }

        public PrivateKey getPrivateKey() {
            return this.privateKey;
        }

        public java.security.cert.Certificate[] getCertificateChain() {
            return (java.security.cert.Certificate[]) this.chain.clone();
        }

        public java.security.cert.Certificate getCertificate() {
            return this.chain[0];
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("PrivateKeyEntry: number of elements in certificate chain is ");
            sb.append(Integer.toString(this.chain.length));
            sb.append("\n");
            for (int i = 0; i < this.chain.length; i++) {
                sb.append(this.chain[i].toString());
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    public static final class SecretKeyEntry implements Entry {
        private final SecretKey secretKey;

        public SecretKeyEntry(SecretKey secretKey) {
            if (secretKey == null) {
                throw new NullPointerException("secretKey == null");
            }
            this.secretKey = secretKey;
        }

        public SecretKey getSecretKey() {
            return this.secretKey;
        }

        public String toString() {
            return "SecretKeyEntry: algorithm - " + this.secretKey.getAlgorithm();
        }
    }

    public static final class TrustedCertificateEntry implements Entry {
        private final java.security.cert.Certificate trustCertificate;

        public TrustedCertificateEntry(java.security.cert.Certificate trustCertificate) {
            if (trustCertificate == null) {
                throw new NullPointerException("trustCertificate == null");
            }
            this.trustCertificate = trustCertificate;
        }

        public java.security.cert.Certificate getTrustedCertificate() {
            return this.trustCertificate;
        }

        public String toString() {
            return "Trusted certificate entry:\n" + this.trustCertificate;
        }
    }
}
