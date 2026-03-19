package com.android.certinstaller;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.x509.BasicConstraints;
import com.android.org.conscrypt.TrustedCertificateStore;
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
import java.util.Iterator;
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
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        String string = extras.getString("name");
        extras.remove("name");
        if (string != null) {
            this.mName = string;
        }
        this.mUid = extras.getInt("install_as_uid", -1);
        extras.remove("install_as_uid");
        Log.d("CredentialHelper", "# extras: " + extras.size());
        for (String str : extras.keySet()) {
            byte[] byteArray = extras.getByteArray(str);
            StringBuilder sb = new StringBuilder();
            sb.append("   ");
            sb.append(str);
            sb.append(": ");
            sb.append(byteArray == null ? -1 : byteArray.length);
            Log.d("CredentialHelper", sb.toString());
            this.mBundle.put(str, byteArray);
        }
        parseCert(getData("CERT"));
    }

    synchronized void onSaveStates(Bundle bundle) {
        try {
            bundle.putSerializable("data", this.mBundle);
            bundle.putString("name", this.mName);
            bundle.putInt("install_as_uid", this.mUid);
            if (this.mUserKey != null) {
                bundle.putByteArray("USRPKEY_", this.mUserKey.getEncoded());
            }
            ArrayList arrayList = new ArrayList(this.mCaCerts.size() + 1);
            if (this.mUserCert != null) {
                arrayList.add(this.mUserCert.getEncoded());
            }
            Iterator<X509Certificate> it = this.mCaCerts.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().getEncoded());
            }
            bundle.putByteArray("crts", Util.toBytes(arrayList));
        } catch (CertificateEncodingException e) {
            throw new AssertionError(e);
        }
    }

    void onRestoreStates(Bundle bundle) {
        this.mBundle = (HashMap) bundle.getSerializable("data");
        this.mName = bundle.getString("name");
        this.mUid = bundle.getInt("install_as_uid", -1);
        byte[] byteArray = bundle.getByteArray("USRPKEY_");
        if (byteArray != null) {
            setPrivateKey(byteArray);
        }
        Iterator it = ((ArrayList) Util.fromBytes(bundle.getByteArray("crts"))).iterator();
        while (it.hasNext()) {
            parseCert((byte[]) it.next());
        }
    }

    X509Certificate getUserCertificate() {
        return this.mUserCert;
    }

    private void parseCert(byte[] bArr) {
        if (bArr == null) {
            return;
        }
        try {
            X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
            if (isCa(x509Certificate)) {
                Log.d("CredentialHelper", "got a CA cert");
                this.mCaCerts.add(x509Certificate);
            } else {
                Log.d("CredentialHelper", "got a user cert");
                this.mUserCert = x509Certificate;
            }
        } catch (CertificateException e) {
            Log.w("CredentialHelper", "parseCert(): " + e);
        }
    }

    private boolean isCa(X509Certificate x509Certificate) {
        try {
            byte[] extensionValue = x509Certificate.getExtensionValue("2.5.29.19");
            if (extensionValue == null) {
                return false;
            }
            return BasicConstraints.getInstance(new ASN1InputStream(new ASN1InputStream(extensionValue).readObject().getOctets()).readObject()).isCA();
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

    void setPrivateKey(byte[] bArr) {
        try {
            this.mUserKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bArr));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (InvalidKeySpecException e2) {
            throw new AssertionError(e2);
        }
    }

    boolean containsAnyRawData() {
        return !this.mBundle.isEmpty();
    }

    byte[] getData(String str) {
        return this.mBundle.get(str);
    }

    CharSequence getDescription(Context context) {
        StringBuilder sb = new StringBuilder();
        if (this.mUserKey != null) {
            sb.append(context.getString(R.string.one_userkey));
            sb.append("<br>");
        }
        if (this.mUserCert != null) {
            sb.append(context.getString(R.string.one_usercrt));
            sb.append("<br>");
        }
        int size = this.mCaCerts.size();
        if (size > 0) {
            if (size == 1) {
                sb.append(context.getString(R.string.one_cacrt));
            } else {
                sb.append(context.getString(R.string.n_cacrts, Integer.valueOf(size)));
            }
        }
        return Html.fromHtml(sb.toString());
    }

    void setName(String str) {
        this.mName = str;
    }

    String getName() {
        return this.mName;
    }

    void setInstallAsUid(int i) {
        this.mUid = i;
    }

    boolean isInstallAsUidSet() {
        return this.mUid != -1;
    }

    int getInstallAsUid() {
        return this.mUid;
    }

    Intent createSystemInstallIntent(Context context) {
        Intent intent = new Intent("com.android.credentials.INSTALL");
        if (!isWear(context)) {
            intent.setClassName("com.android.settings", "com.android.settings.CredentialStorage");
        } else {
            intent.setClassName("com.google.android.apps.wearable.settings", "com.google.android.clockwork.settings.CredentialStorage");
        }
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
                intent.putExtra("ca_certificates_data", Credentials.convertToPem((X509Certificate[]) this.mCaCerts.toArray(new X509Certificate[this.mCaCerts.size()])));
            }
            return intent;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (CertificateEncodingException e2) {
            throw new AssertionError(e2);
        }
    }

    boolean installVpnAndAppsTrustAnchors(Context context, IKeyChainService iKeyChainService) {
        TrustedCertificateStore trustedCertificateStore = new TrustedCertificateStore();
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        for (X509Certificate x509Certificate : this.mCaCerts) {
            try {
                byte[] encoded = x509Certificate.getEncoded();
                if (encoded != null) {
                    try {
                        iKeyChainService.installCaCertificate(encoded);
                        String certificateAlias = trustedCertificateStore.getCertificateAlias(x509Certificate);
                        if (certificateAlias == null) {
                            Log.e("CredentialHelper", "alias is null");
                            return false;
                        }
                        if (context.getResources().getBoolean(R.bool.config_auto_cert_approval)) {
                            devicePolicyManager.approveCaCert(certificateAlias, UserHandle.myUserId(), true);
                        }
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

    boolean hasPassword() {
        if (!hasPkcs12KeyStore()) {
            return false;
        }
        try {
            return loadPkcs12Internal(new KeyStore.PasswordProtection(new char[0])) == null;
        } catch (Exception e) {
            return true;
        }
    }

    boolean extractPkcs12(String str) {
        try {
            return extractPkcs12Internal(new KeyStore.PasswordProtection(str.toCharArray()));
        } catch (Exception e) {
            Log.w("CredentialHelper", "extractPkcs12(): " + e, e);
            return false;
        }
    }

    private boolean extractPkcs12Internal(KeyStore.PasswordProtection passwordProtection) throws Exception {
        KeyStore keyStoreLoadPkcs12Internal = loadPkcs12Internal(passwordProtection);
        Enumeration<String> enumerationAliases = keyStoreLoadPkcs12Internal.aliases();
        if (!enumerationAliases.hasMoreElements()) {
            Log.e("CredentialHelper", "PKCS12 file has no elements");
            return false;
        }
        while (enumerationAliases.hasMoreElements()) {
            String strNextElement = enumerationAliases.nextElement();
            if (keyStoreLoadPkcs12Internal.isKeyEntry(strNextElement)) {
                ?? entry = keyStoreLoadPkcs12Internal.getEntry(strNextElement, passwordProtection);
                Log.d("CredentialHelper", "extracted alias = " + strNextElement + ", entry=" + entry.getClass());
                if (entry instanceof KeyStore.PrivateKeyEntry) {
                    if (TextUtils.isEmpty(this.mName)) {
                        this.mName = strNextElement;
                    }
                    return installFrom(entry);
                }
            } else {
                Log.d("CredentialHelper", "Skip non-key entry, alias = " + strNextElement);
            }
        }
        return true;
    }

    private KeyStore loadPkcs12Internal(KeyStore.PasswordProtection passwordProtection) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(getData("PKCS12")), passwordProtection.getPassword());
        return keyStore;
    }

    private synchronized boolean installFrom(KeyStore.PrivateKeyEntry privateKeyEntry) {
        this.mUserKey = privateKeyEntry.getPrivateKey();
        this.mUserCert = (X509Certificate) privateKeyEntry.getCertificate();
        Certificate[] certificateChain = privateKeyEntry.getCertificateChain();
        Log.d("CredentialHelper", "# certs extracted = " + certificateChain.length);
        this.mCaCerts = new ArrayList(certificateChain.length);
        for (Certificate certificate : certificateChain) {
            X509Certificate x509Certificate = (X509Certificate) certificate;
            if (isCa(x509Certificate)) {
                this.mCaCerts.add(x509Certificate);
            }
        }
        Log.d("CredentialHelper", "# ca certs extracted = " + this.mCaCerts.size());
        return true;
    }

    private static boolean isWear(Context context) {
        return context.getPackageManager().hasSystemFeature("android.hardware.type.watch");
    }

    public boolean includesVpnAndAppsTrustAnchors() {
        return hasCaCerts() && getInstallAsUid() == -1 && this.mUserKey == null;
    }
}
