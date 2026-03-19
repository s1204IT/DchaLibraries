package com.android.keychain;

import android.app.BroadcastOptions;
import android.app.IntentService;
import android.app.admin.SecurityLog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.StringParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyStore;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.AttestationUtils;
import android.security.keystore.DeviceIdAttestationException;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keychain.internal.GrantsDatabase;
import com.android.org.conscrypt.TrustedCertificateStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyChainService extends IntentService {
    private GrantsDatabase mGrantsDb;
    private final IKeyChainService.Stub mIKeyChainService;
    private Injector mInjector;

    public KeyChainService() {
        super(KeyChainService.class.getSimpleName());
        this.mIKeyChainService = new IKeyChainService.Stub() {
            private final Context mContext;
            private final KeyStore mKeyStore = KeyStore.getInstance();
            private final TrustedCertificateStore mTrustedCertificateStore = new TrustedCertificateStore();

            {
                this.mContext = KeyChainService.this;
            }

            public String requestPrivateKey(String str) {
                checkArgs(str);
                return this.mKeyStore.grant("USRPKEY_" + str, KeyChainService.this.mInjector.getCallingUid());
            }

            public byte[] getCertificate(String str) {
                checkArgs(str);
                return this.mKeyStore.get("USRCERT_" + str);
            }

            public byte[] getCaCertificates(String str) {
                checkArgs(str);
                return this.mKeyStore.get("CACERT_" + str);
            }

            public boolean isUserSelectable(String str) {
                validateAlias(str);
                return KeyChainService.this.mGrantsDb.isUserSelectable(str);
            }

            public void setUserSelectable(String str, boolean z) {
                validateAlias(str);
                checkSystemCaller();
                KeyChainService.this.mGrantsDb.setIsUserSelectable(str, z);
            }

            public int generateKeyPair(String str, ParcelableKeyGenParameterSpec parcelableKeyGenParameterSpec) {
                checkSystemCaller();
                KeyGenParameterSpec spec = parcelableKeyGenParameterSpec.getSpec();
                if (TextUtils.isEmpty(spec.getKeystoreAlias()) || spec.getUid() != -1) {
                    Log.e("KeyChain", "Cannot generate key pair with empty alias or specified uid.");
                    return 1;
                }
                if (spec.getAttestationChallenge() != null) {
                    Log.e("KeyChain", "Key generation request should not include an Attestation challenge.");
                    return 2;
                }
                try {
                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(str, "AndroidKeyStore");
                    keyPairGenerator.initialize(spec);
                    if (keyPairGenerator.generateKeyPair() == null) {
                        Log.e("KeyChain", "Key generation failed.");
                        return 6;
                    }
                    return 0;
                } catch (InvalidAlgorithmParameterException e) {
                    Log.e("KeyChain", "Invalid algorithm params", e);
                    return 4;
                } catch (NoSuchAlgorithmException e2) {
                    Log.e("KeyChain", "Invalid algorithm requested", e2);
                    return 3;
                } catch (NoSuchProviderException e3) {
                    Log.e("KeyChain", "Could not find Keystore.", e3);
                    return 5;
                }
            }

            public int attestKey(String str, byte[] bArr, int[] iArr, KeymasterCertificateChain keymasterCertificateChain) {
                checkSystemCaller();
                validateAlias(str);
                if (bArr == null) {
                    Log.e("KeyChain", String.format("Missing attestation challenge for alias %s", str));
                    return 1;
                }
                try {
                    int iAttestKey = this.mKeyStore.attestKey("USRPKEY_" + str, AttestationUtils.prepareAttestationArguments(this.mContext, iArr, bArr), keymasterCertificateChain);
                    if (iAttestKey == 1) {
                        return 0;
                    }
                    Log.e("KeyChain", String.format("Failure attesting for key %s: %d", str, Integer.valueOf(iAttestKey)));
                    if (iAttestKey == -66) {
                        return 3;
                    }
                    return 4;
                } catch (DeviceIdAttestationException e) {
                    Log.e("KeyChain", "Failed collecting attestation data", e);
                    return 2;
                }
            }

            public boolean setKeyPairCertificate(String str, byte[] bArr, byte[] bArr2) {
                checkSystemCaller();
                if (!this.mKeyStore.isUnlocked()) {
                    Log.e("KeyChain", "Keystore is " + this.mKeyStore.state().toString() + ". Credentials cannot be installed until device is unlocked");
                    return false;
                }
                if (!this.mKeyStore.put("USRCERT_" + str, bArr, -1, 0)) {
                    Log.e("KeyChain", "Failed to import user certificate " + bArr);
                    return false;
                }
                if (bArr2 != null && bArr2.length > 0) {
                    if (!this.mKeyStore.put("CACERT_" + str, bArr2, -1, 0)) {
                        Log.e("KeyChain", "Failed to import certificate chain" + bArr2);
                        if (!this.mKeyStore.delete("USRCERT_" + str)) {
                            Log.e("KeyChain", "Failed to clean up key chain after certificate chain importing failed");
                        }
                        return false;
                    }
                } else {
                    if (!this.mKeyStore.delete("CACERT_" + str)) {
                        Log.e("KeyChain", "Failed to remove CA certificate chain for alias " + str);
                    }
                }
                KeyChainService.this.broadcastKeychainChange();
                KeyChainService.this.broadcastLegacyStorageChange();
                return true;
            }

            private void validateAlias(String str) {
                if (str == null) {
                    throw new NullPointerException("alias == null");
                }
            }

            private void validateKeyStoreState() {
                if (!this.mKeyStore.isUnlocked()) {
                    throw new IllegalStateException("keystore is " + this.mKeyStore.state().toString());
                }
            }

            private void checkArgs(String str) {
                validateAlias(str);
                validateKeyStoreState();
                int callingUid = KeyChainService.this.mInjector.getCallingUid();
                if (!KeyChainService.this.mGrantsDb.hasGrant(callingUid, str)) {
                    throw new IllegalStateException("uid " + callingUid + " doesn't have permission to access the requested alias");
                }
            }

            public String installCaCertificate(byte[] bArr) {
                String certificateAlias;
                checkCertInstallerOrSystemCaller();
                try {
                    X509Certificate certificate = parseCertificate(bArr);
                    name = KeyChainService.this.mInjector.isSecurityLoggingEnabled() ? certificate.getSubjectX500Principal().getName("CANONICAL") : null;
                    synchronized (this.mTrustedCertificateStore) {
                        this.mTrustedCertificateStore.installCertificate(certificate);
                        certificateAlias = this.mTrustedCertificateStore.getCertificateAlias(certificate);
                    }
                    if (name != null) {
                        KeyChainService.this.mInjector.writeSecurityEvent(210029, 1, name);
                    }
                    KeyChainService.this.broadcastLegacyStorageChange();
                    KeyChainService.this.broadcastTrustStoreChange();
                    return certificateAlias;
                } catch (IOException | CertificateException e) {
                    if (name != null) {
                        KeyChainService.this.mInjector.writeSecurityEvent(210029, 0, name);
                    }
                    throw new IllegalStateException(e);
                }
            }

            public boolean installKeyPair(byte[] bArr, byte[] bArr2, byte[] bArr3, String str) {
                checkCertInstallerOrSystemCaller();
                if (!this.mKeyStore.isUnlocked()) {
                    Log.e("KeyChain", "Keystore is " + this.mKeyStore.state().toString() + ". Credentials cannot be installed until device is unlocked");
                    return false;
                }
                if (!removeKeyPair(str)) {
                    return false;
                }
                if (!this.mKeyStore.importKey("USRPKEY_" + str, bArr, -1, 1)) {
                    Log.e("KeyChain", "Failed to import private key " + str);
                    return false;
                }
                if (!this.mKeyStore.put("USRCERT_" + str, bArr2, -1, 1)) {
                    Log.e("KeyChain", "Failed to import user certificate " + bArr2);
                    if (!this.mKeyStore.delete("USRPKEY_" + str)) {
                        Log.e("KeyChain", "Failed to delete private key after certificate importing failed");
                    }
                    return false;
                }
                if (bArr3 != null && bArr3.length > 0) {
                    if (!this.mKeyStore.put("CACERT_" + str, bArr3, -1, 1)) {
                        Log.e("KeyChain", "Failed to import certificate chain" + bArr3);
                        if (!removeKeyPair(str)) {
                            Log.e("KeyChain", "Failed to clean up key chain after certificate chain importing failed");
                        }
                        return false;
                    }
                }
                KeyChainService.this.broadcastKeychainChange();
                KeyChainService.this.broadcastLegacyStorageChange();
                return true;
            }

            public boolean removeKeyPair(String str) {
                checkCertInstallerOrSystemCaller();
                if (Credentials.deleteAllTypesForAlias(this.mKeyStore, str)) {
                    KeyChainService.this.mGrantsDb.removeAliasInformation(str);
                    KeyChainService.this.broadcastKeychainChange();
                    KeyChainService.this.broadcastLegacyStorageChange();
                    return true;
                }
                return false;
            }

            private X509Certificate parseCertificate(byte[] bArr) throws CertificateException {
                return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
            }

            public boolean reset() {
                boolean z;
                checkSystemCaller();
                KeyChainService.this.mGrantsDb.removeAllAliasesInformation();
                synchronized (this.mTrustedCertificateStore) {
                    z = true;
                    for (String str : this.mTrustedCertificateStore.aliases()) {
                        if (TrustedCertificateStore.isUser(str) && !deleteCertificateEntry(str)) {
                            z = false;
                        }
                    }
                }
                KeyChainService.this.broadcastTrustStoreChange();
                KeyChainService.this.broadcastKeychainChange();
                KeyChainService.this.broadcastLegacyStorageChange();
                return z;
            }

            public boolean deleteCaCertificate(String str) {
                boolean zDeleteCertificateEntry;
                checkSystemCaller();
                synchronized (this.mTrustedCertificateStore) {
                    zDeleteCertificateEntry = deleteCertificateEntry(str);
                }
                KeyChainService.this.broadcastTrustStoreChange();
                KeyChainService.this.broadcastLegacyStorageChange();
                return zDeleteCertificateEntry;
            }

            private boolean deleteCertificateEntry(String str) {
                String name;
                if (KeyChainService.this.mInjector.isSecurityLoggingEnabled()) {
                    Certificate certificate = this.mTrustedCertificateStore.getCertificate(str);
                    if (certificate instanceof X509Certificate) {
                        name = ((X509Certificate) certificate).getSubjectX500Principal().getName("CANONICAL");
                    } else {
                        name = null;
                    }
                }
                try {
                    this.mTrustedCertificateStore.deleteCertificateEntry(str);
                    if (name != null) {
                        KeyChainService.this.mInjector.writeSecurityEvent(210030, 1, name);
                    }
                    return true;
                } catch (IOException | CertificateException e) {
                    Log.w("KeyChain", "Problem removing CA certificate " + str, e);
                    if (name != null) {
                        KeyChainService.this.mInjector.writeSecurityEvent(210030, 0, name);
                    }
                    return false;
                }
            }

            private void checkCertInstallerOrSystemCaller() {
                String strCallingPackage = callingPackage();
                if (!"android.uid.system:1000".equals(strCallingPackage) && !"com.android.certinstaller".equals(strCallingPackage)) {
                    throw new SecurityException("Not system or cert installer package: " + strCallingPackage);
                }
            }

            private void checkSystemCaller() {
                String strCallingPackage = callingPackage();
                if (!"android.uid.system:1000".equals(strCallingPackage)) {
                    throw new SecurityException("Not system package: " + strCallingPackage);
                }
            }

            private String callingPackage() {
                return KeyChainService.this.getPackageManager().getNameForUid(KeyChainService.this.mInjector.getCallingUid());
            }

            public boolean hasGrant(int i, String str) {
                checkSystemCaller();
                return KeyChainService.this.mGrantsDb.hasGrant(i, str);
            }

            public void setGrant(int i, String str, boolean z) {
                checkSystemCaller();
                KeyChainService.this.mGrantsDb.setGrant(i, str, z);
                KeyChainService.this.broadcastPermissionChange(i, str, z);
                KeyChainService.this.broadcastLegacyStorageChange();
            }

            public StringParceledListSlice getUserCaAliases() {
                StringParceledListSlice stringParceledListSlice;
                synchronized (this.mTrustedCertificateStore) {
                    stringParceledListSlice = new StringParceledListSlice(new ArrayList(this.mTrustedCertificateStore.userAliases()));
                }
                return stringParceledListSlice;
            }

            public StringParceledListSlice getSystemCaAliases() {
                StringParceledListSlice stringParceledListSlice;
                synchronized (this.mTrustedCertificateStore) {
                    stringParceledListSlice = new StringParceledListSlice(new ArrayList(this.mTrustedCertificateStore.allSystemAliases()));
                }
                return stringParceledListSlice;
            }

            public boolean containsCaAlias(String str) {
                return this.mTrustedCertificateStore.containsAlias(str);
            }

            public byte[] getEncodedCaCertificate(String str, boolean z) {
                synchronized (this.mTrustedCertificateStore) {
                    X509Certificate x509Certificate = (X509Certificate) this.mTrustedCertificateStore.getCertificate(str, z);
                    if (x509Certificate == null) {
                        Log.w("KeyChain", "Could not find CA certificate " + str);
                        return null;
                    }
                    try {
                        return x509Certificate.getEncoded();
                    } catch (CertificateEncodingException e) {
                        Log.w("KeyChain", "Error while encoding CA certificate " + str);
                        return null;
                    }
                }
            }

            public List<String> getCaCertificateChainAliases(String str, boolean z) {
                ArrayList arrayList;
                synchronized (this.mTrustedCertificateStore) {
                    try {
                        List certificateChain = this.mTrustedCertificateStore.getCertificateChain((X509Certificate) this.mTrustedCertificateStore.getCertificate(str, z));
                        arrayList = new ArrayList(certificateChain.size());
                        int size = certificateChain.size();
                        for (int i = 0; i < size; i++) {
                            String certificateAlias = this.mTrustedCertificateStore.getCertificateAlias((Certificate) certificateChain.get(i), true);
                            if (certificateAlias != null) {
                                arrayList.add(certificateAlias);
                            }
                        }
                    } catch (CertificateException e) {
                        Log.w("KeyChain", "Error retrieving cert chain for root " + str);
                        return Collections.emptyList();
                    }
                }
                return arrayList;
            }
        };
        this.mInjector = new Injector();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mGrantsDb = new GrantsDatabase(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mGrantsDb.destroy();
        this.mGrantsDb = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (IKeyChainService.class.getName().equals(intent.getAction())) {
            return this.mIKeyChainService;
        }
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) throws Exception {
        if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
            this.mGrantsDb.purgeOldGrants(getPackageManager());
        }
    }

    private void broadcastLegacyStorageChange() {
        Intent intent = new Intent("android.security.STORAGE_CHANGED");
        BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
        broadcastOptionsMakeBasic.setMaxManifestReceiverApiLevel(25);
        sendBroadcastAsUser(intent, UserHandle.of(UserHandle.myUserId()), null, broadcastOptionsMakeBasic.toBundle());
    }

    private void broadcastKeychainChange() {
        sendBroadcastAsUser(new Intent("android.security.action.KEYCHAIN_CHANGED"), UserHandle.of(UserHandle.myUserId()));
    }

    private void broadcastTrustStoreChange() {
        sendBroadcastAsUser(new Intent("android.security.action.TRUST_STORE_CHANGED"), UserHandle.of(UserHandle.myUserId()));
    }

    private void broadcastPermissionChange(int i, String str, boolean z) {
        String[] packagesForUid = getPackageManager().getPackagesForUid(i);
        if (packagesForUid == null) {
            return;
        }
        for (String str2 : packagesForUid) {
            Intent intent = new Intent("android.security.action.KEY_ACCESS_CHANGED");
            intent.putExtra("android.security.extra.KEY_ALIAS", str);
            intent.putExtra("android.security.extra.KEY_ACCESSIBLE", z);
            intent.setPackage(str2);
            sendBroadcastAsUser(intent, UserHandle.of(UserHandle.myUserId()));
        }
    }

    @VisibleForTesting
    void setInjector(Injector injector) {
        this.mInjector = injector;
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public boolean isSecurityLoggingEnabled() {
            return SecurityLog.isLoggingEnabled();
        }

        public void writeSecurityEvent(int i, Object... objArr) {
            SecurityLog.writeEvent(i, objArr);
        }

        public int getCallingUid() {
            return Binder.getCallingUid();
        }
    }
}
