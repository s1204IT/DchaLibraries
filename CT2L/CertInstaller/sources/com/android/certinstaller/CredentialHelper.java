package com.android.certinstaller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.x509.BasicConstraints;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

class CredentialHelper {
    private HashMap<String, byte[]> mBundle;
    private List<X509Certificate> mCaCerts;
    private String mName;
    private int mUid;
    private X509Certificate mUserCert;
    private PrivateKey mUserKey;

    CredentialHelper() {
        this.mBundle = new HashMap<>();
        this.mName = "";
        this.mUid = -1;
        this.mCaCerts = new ArrayList();
    }

    CredentialHelper(Intent intent) {
        this.mBundle = new HashMap<>();
        this.mName = "";
        this.mUid = -1;
        this.mCaCerts = new ArrayList();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String name = bundle.getString("name");
            bundle.remove("name");
            if (name != null) {
                this.mName = name;
            }
            this.mUid = bundle.getInt("install_as_uid", -1);
            bundle.remove("install_as_uid");
            Log.d("CredentialHelper", "# extras: " + bundle.size());
            for (String key : bundle.keySet()) {
                byte[] bytes = bundle.getByteArray(key);
                Log.d("CredentialHelper", "   " + key + ": " + (bytes == null ? -1 : bytes.length));
                this.mBundle.put(key, bytes);
            }
            parseCert(getData("CERT"));
        }
    }

    synchronized void onSaveStates(Bundle outStates) {
        try {
            outStates.putSerializable("data", this.mBundle);
            outStates.putString("name", this.mName);
            outStates.putInt("install_as_uid", this.mUid);
            if (this.mUserKey != null) {
                outStates.putByteArray("USRPKEY_", this.mUserKey.getEncoded());
            }
            ArrayList<byte[]> certs = new ArrayList<>(this.mCaCerts.size() + 1);
            if (this.mUserCert != null) {
                certs.add(this.mUserCert.getEncoded());
            }
            for (X509Certificate cert : this.mCaCerts) {
                certs.add(cert.getEncoded());
            }
            outStates.putByteArray("crts", Util.toBytes(certs));
        } catch (CertificateEncodingException e) {
            throw new AssertionError(e);
        }
    }

    void onRestoreStates(Bundle savedStates) {
        this.mBundle = (HashMap) savedStates.getSerializable("data");
        this.mName = savedStates.getString("name");
        this.mUid = savedStates.getInt("install_as_uid", -1);
        byte[] bytes = savedStates.getByteArray("USRPKEY_");
        if (bytes != null) {
            setPrivateKey(bytes);
        }
        ArrayList<byte[]> certs = (ArrayList) Util.fromBytes(savedStates.getByteArray("crts"));
        for (byte[] cert : certs) {
            parseCert(cert);
        }
    }

    X509Certificate getUserCertificate() {
        return this.mUserCert;
    }

    private void parseCert(byte[] bytes) {
        if (bytes != null) {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                if (isCa(cert)) {
                    Log.d("CredentialHelper", "got a CA cert");
                    this.mCaCerts.add(cert);
                } else {
                    Log.d("CredentialHelper", "got a user cert");
                    this.mUserCert = cert;
                }
            } catch (CertificateException e) {
                Log.w("CredentialHelper", "parseCert(): " + e);
            }
        }
    }

    private boolean isCa(X509Certificate cert) {
        try {
            byte[] asn1EncodedBytes = cert.getExtensionValue("2.5.29.19");
            if (asn1EncodedBytes == null) {
                return false;
            }
            DEROctetString derOctetString = new ASN1InputStream(asn1EncodedBytes).readObject();
            byte[] octets = derOctetString.getOctets();
            ASN1Sequence sequence = new ASN1InputStream(octets).readObject();
            return BasicConstraints.getInstance(sequence).isCA();
        } catch (IOException e) {
            return false;
        }
    }

    boolean hasPkcs12KeyStore() {
        return this.mBundle.containsKey("PKCS12");
    }

    boolean hasKeyPair() {
        return this.mBundle.containsKey("KEY") && this.mBundle.containsKey("PKEY");
    }

    boolean hasUserCertificate() {
        return this.mUserCert != null;
    }

    boolean hasCaCerts() {
        return !this.mCaCerts.isEmpty();
    }

    boolean hasAnyForSystemInstall() {
        return this.mUserKey != null || hasUserCertificate() || hasCaCerts();
    }

    void setPrivateKey(byte[] bytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.mUserKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (InvalidKeySpecException e2) {
            throw new AssertionError(e2);
        }
    }

    boolean containsAnyRawData() {
        return !this.mBundle.isEmpty();
    }

    byte[] getData(String key) {
        return this.mBundle.get(key);
    }

    CharSequence getDescription(Context context) {
        StringBuilder sb = new StringBuilder();
        if (this.mUserKey != null) {
            sb.append(context.getString(R.string.one_userkey)).append("<br>");
        }
        if (this.mUserCert != null) {
            sb.append(context.getString(R.string.one_usercrt)).append("<br>");
        }
        int n = this.mCaCerts.size();
        if (n > 0) {
            if (n == 1) {
                sb.append(context.getString(R.string.one_cacrt));
            } else {
                sb.append(context.getString(R.string.n_cacrts, Integer.valueOf(n)));
            }
        }
        return Html.fromHtml(sb.toString());
    }

    void setName(String name) {
        this.mName = name;
    }

    String getName() {
        return this.mName;
    }

    void setInstallAsUid(int uid) {
        this.mUid = uid;
    }

    boolean isInstallAsUidSet() {
        return this.mUid != -1;
    }

    int getInstallAsUid() {
        return this.mUid;
    }

    Intent createSystemInstallIntent() {
        Intent intent = new Intent("com.android.credentials.INSTALL");
        intent.setClassName("com.android.settings", "com.android.settings.CredentialStorage");
        intent.putExtra("install_as_uid", this.mUid);
        try {
            if (this.mUserKey != null) {
                intent.putExtra("user_private_key_name", "USRPKEY_" + this.mName);
                intent.putExtra("user_private_key_data", this.mUserKey.getEncoded());
            }
            if (this.mUserCert != null) {
                intent.putExtra("user_certificate_name", "USRCERT_" + this.mName);
                intent.putExtra("user_certificate_data", Credentials.convertToPem(new Certificate[]{this.mUserCert}));
            }
            if (!this.mCaCerts.isEmpty()) {
                intent.putExtra("ca_certificates_name", "CACERT_" + this.mName);
                X509Certificate[] caCerts = (X509Certificate[]) this.mCaCerts.toArray(new X509Certificate[this.mCaCerts.size()]);
                intent.putExtra("ca_certificates_data", Credentials.convertToPem(caCerts));
            }
            return intent;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (CertificateEncodingException e2) {
            throw new AssertionError(e2);
        }
    }

    boolean installCaCertsToKeyChain(IKeyChainService keyChainService) {
        for (X509Certificate caCert : this.mCaCerts) {
            try {
                byte[] bytes = caCert.getEncoded();
                if (bytes != null) {
                    try {
                        keyChainService.installCaCertificate(bytes);
                    } catch (RemoteException e) {
                        Log.w("CredentialHelper", "installCaCertsToKeyChain(): " + e);
                        return false;
                    }
                }
            } catch (CertificateEncodingException e2) {
                throw new AssertionError(e2);
            }
        }
        return true;
    }

    boolean extractPkcs12(String password) {
        try {
            return extractPkcs12Internal(password);
        } catch (Exception e) {
            Log.w("CredentialHelper", "extractPkcs12(): " + e, e);
            return false;
        }
    }

    private boolean extractPkcs12Internal(String password) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(password.toCharArray());
        keystore.load(new ByteArrayInputStream(getData("PKCS12")), passwordProtection.getPassword());
        Enumeration<String> aliases = keystore.aliases();
        if (!aliases.hasMoreElements()) {
            return false;
        }
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            KeyStore.Entry entry = keystore.getEntry(alias, passwordProtection);
            Log.d("CredentialHelper", "extracted alias = " + alias + ", entry=" + entry.getClass());
            if (entry instanceof KeyStore.PrivateKeyEntry) {
                if (TextUtils.isEmpty(this.mName)) {
                    this.mName = alias;
                }
                return installFrom((KeyStore.PrivateKeyEntry) entry);
            }
        }
        return true;
    }

    private synchronized boolean installFrom(KeyStore.PrivateKeyEntry entry) {
        this.mUserKey = entry.getPrivateKey();
        this.mUserCert = (X509Certificate) entry.getCertificate();
        Certificate[] certs = entry.getCertificateChain();
        Log.d("CredentialHelper", "# certs extracted = " + certs.length);
        this.mCaCerts = new ArrayList(certs.length);
        for (Certificate c : certs) {
            X509Certificate cert = (X509Certificate) c;
            if (isCa(cert)) {
                this.mCaCerts.add(cert);
            }
        }
        Log.d("CredentialHelper", "# ca certs extracted = " + this.mCaCerts.size());
        return true;
    }
}
