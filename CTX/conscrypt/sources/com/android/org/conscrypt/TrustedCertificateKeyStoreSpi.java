package com.android.org.conscrypt;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

public final class TrustedCertificateKeyStoreSpi extends KeyStoreSpi {
    private final TrustedCertificateStore store = new TrustedCertificateStore();

    @Override
    public Key engineGetKey(String str, char[] cArr) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        return null;
    }

    @Override
    public Certificate[] engineGetCertificateChain(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        return null;
    }

    @Override
    public Certificate engineGetCertificate(String str) {
        return this.store.getCertificate(str);
    }

    @Override
    public Date engineGetCreationDate(String str) {
        return this.store.getCreationDate(str);
    }

    @Override
    public void engineSetKeyEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void engineSetKeyEntry(String str, byte[] bArr, Certificate[] certificateArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void engineSetCertificateEntry(String str, Certificate certificate) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void engineDeleteEntry(String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(this.store.aliases());
    }

    @Override
    public boolean engineContainsAlias(String str) {
        return this.store.containsAlias(str);
    }

    @Override
    public int engineSize() {
        return this.store.aliases().size();
    }

    @Override
    public boolean engineIsKeyEntry(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        return false;
    }

    @Override
    public boolean engineIsCertificateEntry(String str) {
        return engineContainsAlias(str);
    }

    @Override
    public String engineGetCertificateAlias(Certificate certificate) {
        return this.store.getCertificateAlias(certificate);
    }

    @Override
    public void engineStore(OutputStream outputStream, char[] cArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void engineLoad(InputStream inputStream, char[] cArr) {
        if (inputStream != null) {
            throw new UnsupportedOperationException();
        }
    }
}
