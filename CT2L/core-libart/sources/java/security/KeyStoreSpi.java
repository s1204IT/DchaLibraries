package java.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;
import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

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

    public void engineStore(KeyStore.LoadStoreParameter param) throws NoSuchAlgorithmException, IOException, CertificateException {
        throw new UnsupportedOperationException();
    }

    public void engineLoad(KeyStore.LoadStoreParameter param) throws NoSuchAlgorithmException, IOException, CertificateException {
        if (param == null) {
            engineLoad(null, null);
            return;
        }
        KeyStore.ProtectionParameter pp = param.getProtectionParameter();
        if (pp instanceof KeyStore.PasswordProtection) {
            try {
                char[] pwd = ((KeyStore.PasswordProtection) pp).getPassword();
                engineLoad(null, pwd);
                return;
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (pp instanceof KeyStore.CallbackHandlerProtection) {
            try {
                char[] pwd2 = getPasswordFromCallBack(pp);
                engineLoad(null, pwd2);
                return;
            } catch (UnrecoverableEntryException e2) {
                throw new IllegalArgumentException(e2);
            }
        }
        throw new UnsupportedOperationException("protectionParameter is neither PasswordProtection nor CallbackHandlerProtection instance");
    }

    public KeyStore.Entry engineGetEntry(String alias, KeyStore.ProtectionParameter protParam) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException {
        if (!engineContainsAlias(alias)) {
            return null;
        }
        if (engineIsCertificateEntry(alias)) {
            return new KeyStore.TrustedCertificateEntry(engineGetCertificate(alias));
        }
        char[] passW = null;
        if (protParam != null) {
            if (protParam instanceof KeyStore.PasswordProtection) {
                try {
                    passW = ((KeyStore.PasswordProtection) protParam).getPassword();
                } catch (IllegalStateException ee) {
                    throw new KeyStoreException("Password was destroyed", ee);
                }
            } else if (protParam instanceof KeyStore.CallbackHandlerProtection) {
                passW = getPasswordFromCallBack(protParam);
            } else {
                throw new UnrecoverableEntryException("ProtectionParameter object is not PasswordProtection: " + protParam);
            }
        }
        if (engineIsKeyEntry(alias)) {
            Key key = engineGetKey(alias, passW);
            if (key instanceof PrivateKey) {
                return new KeyStore.PrivateKeyEntry((PrivateKey) key, engineGetCertificateChain(alias));
            }
            if (key instanceof SecretKey) {
                return new KeyStore.SecretKeyEntry((SecretKey) key);
            }
        }
        throw new NoSuchAlgorithmException("Unknown KeyStore.Entry object");
    }

    public void engineSetEntry(String alias, KeyStore.Entry entry, KeyStore.ProtectionParameter protParam) throws KeyStoreException {
        if (entry == null) {
            throw new KeyStoreException("entry == null");
        }
        if (engineContainsAlias(alias)) {
            engineDeleteEntry(alias);
        }
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            KeyStore.TrustedCertificateEntry trE = (KeyStore.TrustedCertificateEntry) entry;
            engineSetCertificateEntry(alias, trE.getTrustedCertificate());
            return;
        }
        char[] passW = null;
        if (protParam != null) {
            if (protParam instanceof KeyStore.PasswordProtection) {
                try {
                    passW = ((KeyStore.PasswordProtection) protParam).getPassword();
                } catch (IllegalStateException ee) {
                    throw new KeyStoreException("Password was destroyed", ee);
                }
            } else if (protParam instanceof KeyStore.CallbackHandlerProtection) {
                try {
                    passW = getPasswordFromCallBack(protParam);
                } catch (Exception e) {
                    throw new KeyStoreException(e);
                }
            } else {
                throw new KeyStoreException("protParam should be PasswordProtection or CallbackHandlerProtection");
            }
        }
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            KeyStore.PrivateKeyEntry prE = (KeyStore.PrivateKeyEntry) entry;
            engineSetKeyEntry(alias, prE.getPrivateKey(), passW, prE.getCertificateChain());
        } else {
            if (entry instanceof KeyStore.SecretKeyEntry) {
                KeyStore.SecretKeyEntry skE = (KeyStore.SecretKeyEntry) entry;
                engineSetKeyEntry(alias, skE.getSecretKey(), passW, null);
                return;
            }
            throw new KeyStoreException("Entry object is neither PrivateKeyObject nor SecretKeyEntry nor TrustedCertificateEntry: " + entry);
        }
    }

    public boolean engineEntryInstanceOf(String alias, Class<? extends KeyStore.Entry> entryClass) {
        boolean zIsAssignableFrom = false;
        if (engineContainsAlias(alias)) {
            try {
                if (engineIsCertificateEntry(alias)) {
                    zIsAssignableFrom = entryClass.isAssignableFrom(Class.forName("java.security.KeyStore$TrustedCertificateEntry"));
                } else if (engineIsKeyEntry(alias)) {
                    if (entryClass.isAssignableFrom(Class.forName("java.security.KeyStore$PrivateKeyEntry"))) {
                        zIsAssignableFrom = engineGetCertificate(alias) != null;
                    } else if (entryClass.isAssignableFrom(Class.forName("java.security.KeyStore$SecretKeyEntry"))) {
                        zIsAssignableFrom = engineGetCertificate(alias) == null;
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        }
        return zIsAssignableFrom;
    }

    static char[] getPasswordFromCallBack(KeyStore.ProtectionParameter protParam) throws UnrecoverableEntryException {
        if (protParam == null) {
            return null;
        }
        if (!(protParam instanceof KeyStore.CallbackHandlerProtection)) {
            throw new UnrecoverableEntryException("Incorrect ProtectionParameter");
        }
        String clName = Security.getProperty("auth.login.defaultCallbackHandler");
        if (clName == null) {
            throw new UnrecoverableEntryException("Default CallbackHandler was not defined");
        }
        try {
            Class<?> cl = Class.forName(clName);
            CallbackHandler cbHand = (CallbackHandler) cl.newInstance();
            PasswordCallback[] pwCb = {new PasswordCallback("password: ", true)};
            cbHand.handle(pwCb);
            return pwCb[0].getPassword();
        } catch (Exception e) {
            throw new UnrecoverableEntryException(e.toString());
        }
    }
}
