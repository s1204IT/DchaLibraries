package java.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

public class KeyStore {
    private static final String KEYSTORE_TYPE = "keystore.type";
    private boolean initialized = false;
    private KeyStoreSpi keyStoreSpi;
    private Provider provider;
    private String type;

    public interface LoadStoreParameter {
        ProtectionParameter getProtectionParameter();
    }

    public interface ProtectionParameter {
    }

    public static class PasswordProtection implements ProtectionParameter, Destroyable {
        private volatile boolean destroyed = false;
        private final char[] password;
        private final String protectionAlgorithm;
        private final AlgorithmParameterSpec protectionParameters;

        public PasswordProtection(char[] cArr) {
            this.password = cArr == null ? null : (char[]) cArr.clone();
            this.protectionAlgorithm = null;
            this.protectionParameters = null;
        }

        public PasswordProtection(char[] cArr, String str, AlgorithmParameterSpec algorithmParameterSpec) {
            if (str == null) {
                throw new NullPointerException("invalid null input");
            }
            this.password = cArr == null ? null : (char[]) cArr.clone();
            this.protectionAlgorithm = str;
            this.protectionParameters = algorithmParameterSpec;
        }

        public String getProtectionAlgorithm() {
            return this.protectionAlgorithm;
        }

        public AlgorithmParameterSpec getProtectionParameters() {
            return this.protectionParameters;
        }

        public synchronized char[] getPassword() {
            if (this.destroyed) {
                throw new IllegalStateException("password has been cleared");
            }
            return this.password;
        }

        @Override
        public synchronized void destroy() throws DestroyFailedException {
            this.destroyed = true;
            if (this.password != null) {
                Arrays.fill(this.password, ' ');
            }
        }

        @Override
        public synchronized boolean isDestroyed() {
            return this.destroyed;
        }
    }

    public static class CallbackHandlerProtection implements ProtectionParameter {
        private final CallbackHandler handler;

        public CallbackHandlerProtection(CallbackHandler callbackHandler) {
            if (callbackHandler == null) {
                throw new NullPointerException("handler must not be null");
            }
            this.handler = callbackHandler;
        }

        public CallbackHandler getCallbackHandler() {
            return this.handler;
        }
    }

    public interface Entry {

        public interface Attribute {
            String getName();

            String getValue();
        }

        default Set<Attribute> getAttributes() {
            return Collections.emptySet();
        }
    }

    public static final class PrivateKeyEntry implements Entry {
        private final Set<Entry.Attribute> attributes;
        private final java.security.cert.Certificate[] chain;
        private final PrivateKey privKey;

        public PrivateKeyEntry(PrivateKey privateKey, java.security.cert.Certificate[] certificateArr) {
            this(privateKey, certificateArr, Collections.emptySet());
        }

        public PrivateKeyEntry(PrivateKey privateKey, java.security.cert.Certificate[] certificateArr, Set<Entry.Attribute> set) {
            if (privateKey == null || certificateArr == null || set == null) {
                throw new NullPointerException("invalid null input");
            }
            if (certificateArr.length == 0) {
                throw new IllegalArgumentException("invalid zero-length input chain");
            }
            java.security.cert.Certificate[] certificateArr2 = (java.security.cert.Certificate[]) certificateArr.clone();
            String type = certificateArr2[0].getType();
            for (int i = 1; i < certificateArr2.length; i++) {
                if (!type.equals(certificateArr2[i].getType())) {
                    throw new IllegalArgumentException("chain does not contain certificates of the same type");
                }
            }
            if (!privateKey.getAlgorithm().equals(certificateArr2[0].getPublicKey().getAlgorithm())) {
                throw new IllegalArgumentException("private key algorithm does not match algorithm of public key in end entity certificate (at index 0)");
            }
            this.privKey = privateKey;
            if ((certificateArr2[0] instanceof X509Certificate) && !(certificateArr2 instanceof X509Certificate[])) {
                this.chain = new X509Certificate[certificateArr2.length];
                System.arraycopy(certificateArr2, 0, this.chain, 0, certificateArr2.length);
            } else {
                this.chain = certificateArr2;
            }
            this.attributes = Collections.unmodifiableSet(new HashSet(set));
        }

        public PrivateKey getPrivateKey() {
            return this.privKey;
        }

        public java.security.cert.Certificate[] getCertificateChain() {
            return (java.security.cert.Certificate[]) this.chain.clone();
        }

        public java.security.cert.Certificate getCertificate() {
            return this.chain[0];
        }

        @Override
        public Set<Entry.Attribute> getAttributes() {
            return this.attributes;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Private key entry and certificate chain with " + this.chain.length + " elements:\r\n");
            for (java.security.cert.Certificate certificate : this.chain) {
                sb.append((Object) certificate);
                sb.append("\r\n");
            }
            return sb.toString();
        }
    }

    public static final class SecretKeyEntry implements Entry {
        private final Set<Entry.Attribute> attributes;
        private final SecretKey sKey;

        public SecretKeyEntry(SecretKey secretKey) {
            if (secretKey == null) {
                throw new NullPointerException("invalid null input");
            }
            this.sKey = secretKey;
            this.attributes = Collections.emptySet();
        }

        public SecretKeyEntry(SecretKey secretKey, Set<Entry.Attribute> set) {
            if (secretKey == null || set == null) {
                throw new NullPointerException("invalid null input");
            }
            this.sKey = secretKey;
            this.attributes = Collections.unmodifiableSet(new HashSet(set));
        }

        public SecretKey getSecretKey() {
            return this.sKey;
        }

        @Override
        public Set<Entry.Attribute> getAttributes() {
            return this.attributes;
        }

        public String toString() {
            return "Secret key entry with algorithm " + this.sKey.getAlgorithm();
        }
    }

    public static final class TrustedCertificateEntry implements Entry {
        private final Set<Entry.Attribute> attributes;
        private final java.security.cert.Certificate cert;

        public TrustedCertificateEntry(java.security.cert.Certificate certificate) {
            if (certificate == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = certificate;
            this.attributes = Collections.emptySet();
        }

        public TrustedCertificateEntry(java.security.cert.Certificate certificate, Set<Entry.Attribute> set) {
            if (certificate == null || set == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = certificate;
            this.attributes = Collections.unmodifiableSet(new HashSet(set));
        }

        public java.security.cert.Certificate getTrustedCertificate() {
            return this.cert;
        }

        @Override
        public Set<Entry.Attribute> getAttributes() {
            return this.attributes;
        }

        public String toString() {
            return "Trusted certificate entry:\r\n" + this.cert.toString();
        }
    }

    protected KeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String str) {
        this.keyStoreSpi = keyStoreSpi;
        this.provider = provider;
        this.type = str;
    }

    public static KeyStore getInstance(String str) throws KeyStoreException {
        try {
            Object[] impl = Security.getImpl(str, "KeyStore", (String) null);
            return new KeyStore((KeyStoreSpi) impl[0], (Provider) impl[1], str);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(str + " not found", e);
        } catch (NoSuchProviderException e2) {
            throw new KeyStoreException(str + " not found", e2);
        }
    }

    public static KeyStore getInstance(String str, String str2) throws KeyStoreException, NoSuchProviderException {
        if (str2 == null || str2.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        try {
            Object[] impl = Security.getImpl(str, "KeyStore", str2);
            return new KeyStore((KeyStoreSpi) impl[0], (Provider) impl[1], str);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(str + " not found", e);
        }
    }

    public static KeyStore getInstance(String str, Provider provider) throws KeyStoreException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        try {
            Object[] impl = Security.getImpl(str, "KeyStore", provider);
            return new KeyStore((KeyStoreSpi) impl[0], (Provider) impl[1], str);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(str + " not found", e);
        }
    }

    public static final String getDefaultType() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty(KeyStore.KEYSTORE_TYPE);
            }
        });
        if (str == null) {
            return "jks";
        }
        return str;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getType() {
        return this.type;
    }

    public final Key getKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineGetKey(str, cArr);
    }

    public final java.security.cert.Certificate[] getCertificateChain(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineGetCertificateChain(str);
    }

    public final java.security.cert.Certificate getCertificate(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineGetCertificate(str);
    }

    public final Date getCreationDate(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineGetCreationDate(str);
    }

    public final void setKeyEntry(String str, Key key, char[] cArr, java.security.cert.Certificate[] certificateArr) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        if ((key instanceof PrivateKey) && (certificateArr == null || certificateArr.length == 0)) {
            throw new IllegalArgumentException("Private key must be accompanied by certificate chain");
        }
        this.keyStoreSpi.engineSetKeyEntry(str, key, cArr, certificateArr);
    }

    public final void setKeyEntry(String str, byte[] bArr, java.security.cert.Certificate[] certificateArr) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        this.keyStoreSpi.engineSetKeyEntry(str, bArr, certificateArr);
    }

    public final void setCertificateEntry(String str, java.security.cert.Certificate certificate) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        this.keyStoreSpi.engineSetCertificateEntry(str, certificate);
    }

    public final void deleteEntry(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        this.keyStoreSpi.engineDeleteEntry(str);
    }

    public final Enumeration<String> aliases() throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineAliases();
    }

    public final boolean containsAlias(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineContainsAlias(str);
    }

    public final int size() throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineSize();
    }

    public final boolean isKeyEntry(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineIsKeyEntry(str);
    }

    public final boolean isCertificateEntry(String str) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineIsCertificateEntry(str);
    }

    public final String getCertificateAlias(java.security.cert.Certificate certificate) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineGetCertificateAlias(certificate);
    }

    public final void store(OutputStream outputStream, char[] cArr) throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        this.keyStoreSpi.engineStore(outputStream, cArr);
    }

    public final void store(LoadStoreParameter loadStoreParameter) throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        this.keyStoreSpi.engineStore(loadStoreParameter);
    }

    public final void load(InputStream inputStream, char[] cArr) throws NoSuchAlgorithmException, IOException, CertificateException {
        this.keyStoreSpi.engineLoad(inputStream, cArr);
        this.initialized = true;
    }

    public final void load(LoadStoreParameter loadStoreParameter) throws NoSuchAlgorithmException, IOException, CertificateException {
        this.keyStoreSpi.engineLoad(loadStoreParameter);
        this.initialized = true;
    }

    public final Entry getEntry(String str, ProtectionParameter protectionParameter) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException {
        if (str == null) {
            throw new NullPointerException("invalid null input");
        }
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineGetEntry(str, protectionParameter);
    }

    public final void setEntry(String str, Entry entry, ProtectionParameter protectionParameter) throws KeyStoreException {
        if (str == null || entry == null) {
            throw new NullPointerException("invalid null input");
        }
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        this.keyStoreSpi.engineSetEntry(str, entry, protectionParameter);
    }

    public final boolean entryInstanceOf(String str, Class<? extends Entry> cls) throws KeyStoreException {
        if (str == null || cls == null) {
            throw new NullPointerException("invalid null input");
        }
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        }
        return this.keyStoreSpi.engineEntryInstanceOf(str, cls);
    }

    public static abstract class Builder {
        static final int MAX_CALLBACK_TRIES = 3;

        public abstract KeyStore getKeyStore() throws KeyStoreException;

        public abstract ProtectionParameter getProtectionParameter(String str) throws KeyStoreException;

        protected Builder() {
        }

        public static Builder newInstance(final KeyStore keyStore, final ProtectionParameter protectionParameter) {
            if (keyStore != null && protectionParameter != null) {
                if (!keyStore.initialized) {
                    throw new IllegalArgumentException("KeyStore not initialized");
                }
                return new Builder() {
                    private volatile boolean getCalled;

                    @Override
                    public KeyStore getKeyStore() {
                        this.getCalled = true;
                        return keyStore;
                    }

                    @Override
                    public ProtectionParameter getProtectionParameter(String str) {
                        if (str == null) {
                            throw new NullPointerException();
                        }
                        if (!this.getCalled) {
                            throw new IllegalStateException("getKeyStore() must be called first");
                        }
                        return protectionParameter;
                    }
                };
            }
            throw new NullPointerException();
        }

        public static Builder newInstance(String str, Provider provider, File file, ProtectionParameter protectionParameter) {
            if (str == null || file == null || protectionParameter == null) {
                throw new NullPointerException();
            }
            if (!(protectionParameter instanceof PasswordProtection) && !(protectionParameter instanceof CallbackHandlerProtection)) {
                throw new IllegalArgumentException("Protection must be PasswordProtection or CallbackHandlerProtection");
            }
            if (!file.isFile()) {
                throw new IllegalArgumentException("File does not exist or it does not refer to a normal file: " + ((Object) file));
            }
            return new FileBuilder(str, provider, file, protectionParameter, AccessController.getContext());
        }

        private static final class FileBuilder extends Builder {
            private final AccessControlContext context;
            private final File file;
            private ProtectionParameter keyProtection;
            private KeyStore keyStore;
            private Throwable oldException;
            private ProtectionParameter protection;
            private final Provider provider;
            private final String type;

            FileBuilder(String str, Provider provider, File file, ProtectionParameter protectionParameter, AccessControlContext accessControlContext) {
                this.type = str;
                this.provider = provider;
                this.file = file;
                this.protection = protectionParameter;
                this.context = accessControlContext;
            }

            @Override
            public synchronized KeyStore getKeyStore() throws KeyStoreException {
                if (this.keyStore != null) {
                    return this.keyStore;
                }
                if (this.oldException != null) {
                    throw new KeyStoreException("Previous KeyStore instantiation failed", this.oldException);
                }
                try {
                    this.keyStore = (KeyStore) AccessController.doPrivileged(new PrivilegedExceptionAction<KeyStore>() {
                        @Override
                        public KeyStore run() throws Exception {
                            if (!(FileBuilder.this.protection instanceof CallbackHandlerProtection)) {
                                return run0();
                            }
                            int i = 0;
                            do {
                                i++;
                                try {
                                    return run0();
                                } catch (IOException e) {
                                    if (i >= 3) {
                                        break;
                                    }
                                    throw e;
                                }
                            } while (e.getCause() instanceof UnrecoverableKeyException);
                            throw e;
                        }

                        public KeyStore run0() throws Exception {
                            FileInputStream fileInputStream;
                            char[] password;
                            KeyStore keyStore = FileBuilder.this.provider == null ? KeyStore.getInstance(FileBuilder.this.type) : KeyStore.getInstance(FileBuilder.this.type, FileBuilder.this.provider);
                            try {
                                fileInputStream = new FileInputStream(FileBuilder.this.file);
                                try {
                                    if (FileBuilder.this.protection instanceof PasswordProtection) {
                                        password = ((PasswordProtection) FileBuilder.this.protection).getPassword();
                                        FileBuilder.this.keyProtection = FileBuilder.this.protection;
                                    } else {
                                        CallbackHandler callbackHandler = ((CallbackHandlerProtection) FileBuilder.this.protection).getCallbackHandler();
                                        PasswordCallback passwordCallback = new PasswordCallback("Password for keystore " + FileBuilder.this.file.getName(), false);
                                        callbackHandler.handle(new Callback[]{passwordCallback});
                                        password = passwordCallback.getPassword();
                                        if (password == null) {
                                            throw new KeyStoreException("No password provided");
                                        }
                                        passwordCallback.clearPassword();
                                        FileBuilder.this.keyProtection = new PasswordProtection(password);
                                    }
                                    keyStore.load(fileInputStream, password);
                                    fileInputStream.close();
                                    return keyStore;
                                } catch (Throwable th) {
                                    th = th;
                                    if (fileInputStream != null) {
                                        fileInputStream.close();
                                    }
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                fileInputStream = null;
                            }
                        }
                    }, this.context);
                    return this.keyStore;
                } catch (PrivilegedActionException e) {
                    this.oldException = e.getCause();
                    throw new KeyStoreException("KeyStore instantiation failed", this.oldException);
                }
            }

            @Override
            public synchronized ProtectionParameter getProtectionParameter(String str) {
                if (str == null) {
                    throw new NullPointerException();
                }
                if (this.keyStore == null) {
                    throw new IllegalStateException("getKeyStore() must be called first");
                }
                return this.keyProtection;
            }
        }

        public static Builder newInstance(final String str, final Provider provider, final ProtectionParameter protectionParameter) {
            if (str == null || protectionParameter == null) {
                throw new NullPointerException();
            }
            final AccessControlContext context = AccessController.getContext();
            return new Builder() {
                private final PrivilegedExceptionAction<KeyStore> action = new PrivilegedExceptionAction<KeyStore>() {
                    @Override
                    public KeyStore run() throws Exception {
                        KeyStore keyStore;
                        if (provider == null) {
                            keyStore = KeyStore.getInstance(str);
                        } else {
                            keyStore = KeyStore.getInstance(str, provider);
                        }
                        SimpleLoadStoreParameter simpleLoadStoreParameter = new SimpleLoadStoreParameter(protectionParameter);
                        if (!(protectionParameter instanceof CallbackHandlerProtection)) {
                            keyStore.load(simpleLoadStoreParameter);
                        } else {
                            int i = 0;
                            while (true) {
                                i++;
                                try {
                                    keyStore.load(simpleLoadStoreParameter);
                                    break;
                                } catch (IOException e) {
                                    if (!(e.getCause() instanceof UnrecoverableKeyException)) {
                                        break;
                                    }
                                    if (i >= 3) {
                                        AnonymousClass2.this.oldException = e;
                                        break;
                                    }
                                    throw e;
                                }
                            }
                        }
                        AnonymousClass2.this.getCalled = true;
                        return keyStore;
                    }
                };
                private volatile boolean getCalled;
                private IOException oldException;

                @Override
                public synchronized KeyStore getKeyStore() throws KeyStoreException {
                    if (this.oldException != null) {
                        throw new KeyStoreException("Previous KeyStore instantiation failed", this.oldException);
                    }
                    try {
                    } catch (PrivilegedActionException e) {
                        throw new KeyStoreException("KeyStore instantiation failed", e.getCause());
                    }
                    return (KeyStore) AccessController.doPrivileged(this.action, context);
                }

                @Override
                public ProtectionParameter getProtectionParameter(String str2) {
                    if (str2 == null) {
                        throw new NullPointerException();
                    }
                    if (!this.getCalled) {
                        throw new IllegalStateException("getKeyStore() must be called first");
                    }
                    return protectionParameter;
                }
            };
        }
    }

    static class SimpleLoadStoreParameter implements LoadStoreParameter {
        private final ProtectionParameter protection;

        SimpleLoadStoreParameter(ProtectionParameter protectionParameter) {
            this.protection = protectionParameter;
        }

        @Override
        public ProtectionParameter getProtectionParameter() {
            return this.protection;
        }
    }
}
