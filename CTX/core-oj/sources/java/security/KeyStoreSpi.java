package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;
import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public abstract class KeyStoreSpi {
    public abstract Enumeration<String> engineAliases();

    public abstract boolean engineContainsAlias(String str);

    public abstract void engineDeleteEntry(String str) throws KeyStoreException;

    public abstract java.security.cert.Certificate engineGetCertificate(String str);

    public abstract String engineGetCertificateAlias(java.security.cert.Certificate certificate);

    public abstract java.security.cert.Certificate[] engineGetCertificateChain(String str);

    public abstract Date engineGetCreationDate(String str);

    public abstract Key engineGetKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException;

    public abstract boolean engineIsCertificateEntry(String str);

    public abstract boolean engineIsKeyEntry(String str);

    public abstract void engineLoad(InputStream inputStream, char[] cArr) throws NoSuchAlgorithmException, IOException, CertificateException;

    public abstract void engineSetCertificateEntry(String str, java.security.cert.Certificate certificate) throws KeyStoreException;

    public abstract void engineSetKeyEntry(String str, Key key, char[] cArr, java.security.cert.Certificate[] certificateArr) throws KeyStoreException;

    public abstract void engineSetKeyEntry(String str, byte[] bArr, java.security.cert.Certificate[] certificateArr) throws KeyStoreException;

    public abstract int engineSize();

    public abstract void engineStore(OutputStream outputStream, char[] cArr) throws NoSuchAlgorithmException, IOException, CertificateException;

    public void engineStore(KeyStore.LoadStoreParameter loadStoreParameter) throws NoSuchAlgorithmException, IOException, CertificateException {
        throw new UnsupportedOperationException();
    }

    public void engineLoad(KeyStore.LoadStoreParameter loadStoreParameter) throws NoSuchAlgorithmException, IOException, CertificateException {
        char[] password;
        if (loadStoreParameter == null) {
            engineLoad((InputStream) null, (char[]) null);
            return;
        }
        if (loadStoreParameter instanceof KeyStore.SimpleLoadStoreParameter) {
            KeyStore.ProtectionParameter protectionParameter = loadStoreParameter.getProtectionParameter();
            if (protectionParameter instanceof KeyStore.PasswordProtection) {
                password = ((KeyStore.PasswordProtection) protectionParameter).getPassword();
            } else if (protectionParameter instanceof KeyStore.CallbackHandlerProtection) {
                CallbackHandler callbackHandler = ((KeyStore.CallbackHandlerProtection) protectionParameter).getCallbackHandler();
                PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
                try {
                    callbackHandler.handle(new Callback[]{passwordCallback});
                    password = passwordCallback.getPassword();
                    passwordCallback.clearPassword();
                    if (password == null) {
                        throw new NoSuchAlgorithmException("No password provided");
                    }
                } catch (UnsupportedCallbackException e) {
                    throw new NoSuchAlgorithmException("Could not obtain password", e);
                }
            } else {
                throw new NoSuchAlgorithmException("ProtectionParameter must be PasswordProtection or CallbackHandlerProtection");
            }
            engineLoad(null, password);
            return;
        }
        throw new UnsupportedOperationException();
    }

    public KeyStore.Entry engineGetEntry(String str, KeyStore.ProtectionParameter protectionParameter) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException {
        if (!engineContainsAlias(str)) {
            return null;
        }
        if (protectionParameter == null && engineIsCertificateEntry(str)) {
            return new KeyStore.TrustedCertificateEntry(engineGetCertificate(str));
        }
        if (protectionParameter == null || (protectionParameter instanceof KeyStore.PasswordProtection)) {
            if (engineIsCertificateEntry(str)) {
                throw new UnsupportedOperationException("trusted certificate entries are not password-protected");
            }
            if (engineIsKeyEntry(str)) {
                Key keyEngineGetKey = engineGetKey(str, protectionParameter != null ? ((KeyStore.PasswordProtection) protectionParameter).getPassword() : null);
                if (keyEngineGetKey instanceof PrivateKey) {
                    return new KeyStore.PrivateKeyEntry((PrivateKey) keyEngineGetKey, engineGetCertificateChain(str));
                }
                if (keyEngineGetKey instanceof SecretKey) {
                    return new KeyStore.SecretKeyEntry((SecretKey) keyEngineGetKey);
                }
            }
        }
        throw new UnsupportedOperationException();
    }

    public void engineSetEntry(String str, KeyStore.Entry entry, KeyStore.ProtectionParameter protectionParameter) throws KeyStoreException {
        KeyStore.PasswordProtection passwordProtection;
        char[] password;
        if (protectionParameter != null && !(protectionParameter instanceof KeyStore.PasswordProtection)) {
            throw new KeyStoreException("unsupported protection parameter");
        }
        if (protectionParameter != null) {
            passwordProtection = (KeyStore.PasswordProtection) protectionParameter;
        } else {
            passwordProtection = null;
        }
        if (passwordProtection != null) {
            password = passwordProtection.getPassword();
        } else {
            password = null;
        }
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            engineSetCertificateEntry(str, ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate());
            return;
        }
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
            engineSetKeyEntry(str, privateKeyEntry.getPrivateKey(), password, privateKeyEntry.getCertificateChain());
        } else {
            if (entry instanceof KeyStore.SecretKeyEntry) {
                engineSetKeyEntry(str, ((KeyStore.SecretKeyEntry) entry).getSecretKey(), password, (java.security.cert.Certificate[]) null);
                return;
            }
            throw new KeyStoreException("unsupported entry type: " + entry.getClass().getName());
        }
    }

    public boolean engineEntryInstanceOf(String str, Class<? extends KeyStore.Entry> cls) {
        if (cls == KeyStore.TrustedCertificateEntry.class) {
            return engineIsCertificateEntry(str);
        }
        if (cls == KeyStore.PrivateKeyEntry.class) {
            return engineIsKeyEntry(str) && engineGetCertificate(str) != null;
        }
        if (cls == KeyStore.SecretKeyEntry.class) {
            return engineIsKeyEntry(str) && engineGetCertificate(str) == null;
        }
        return false;
    }
}
