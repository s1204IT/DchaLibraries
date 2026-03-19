package android.security;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.SettingsStringUtil;
import android.security.IKeyChainAliasCallback;
import android.security.IKeyChainService;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.keystore.KeyProperties;
import com.android.org.conscrypt.TrustedCertificateStore;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.security.KeyPair;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public final class KeyChain {
    public static final String ACCOUNT_TYPE = "com.android.keychain";
    private static final String ACTION_CHOOSER = "com.android.keychain.CHOOSER";
    private static final String ACTION_INSTALL = "android.credentials.INSTALL";
    public static final String ACTION_KEYCHAIN_CHANGED = "android.security.action.KEYCHAIN_CHANGED";
    public static final String ACTION_KEY_ACCESS_CHANGED = "android.security.action.KEY_ACCESS_CHANGED";
    public static final String ACTION_STORAGE_CHANGED = "android.security.STORAGE_CHANGED";
    public static final String ACTION_TRUST_STORE_CHANGED = "android.security.action.TRUST_STORE_CHANGED";
    private static final String CERT_INSTALLER_PACKAGE = "com.android.certinstaller";
    public static final String EXTRA_ALIAS = "alias";
    public static final String EXTRA_CERTIFICATE = "CERT";
    public static final String EXTRA_KEY_ACCESSIBLE = "android.security.extra.KEY_ACCESSIBLE";
    public static final String EXTRA_KEY_ALIAS = "android.security.extra.KEY_ALIAS";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PKCS12 = "PKCS12";
    public static final String EXTRA_RESPONSE = "response";
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_URI = "uri";
    private static final String KEYCHAIN_PACKAGE = "com.android.keychain";
    public static final int KEY_ATTESTATION_CANNOT_ATTEST_IDS = 3;
    public static final int KEY_ATTESTATION_CANNOT_COLLECT_DATA = 2;
    public static final int KEY_ATTESTATION_FAILURE = 4;
    public static final int KEY_ATTESTATION_MISSING_CHALLENGE = 1;
    public static final int KEY_ATTESTATION_SUCCESS = 0;
    public static final int KEY_GEN_FAILURE = 6;
    public static final int KEY_GEN_INVALID_ALGORITHM_PARAMETERS = 4;
    public static final int KEY_GEN_MISSING_ALIAS = 1;
    public static final int KEY_GEN_NO_KEYSTORE_PROVIDER = 5;
    public static final int KEY_GEN_NO_SUCH_ALGORITHM = 3;
    public static final int KEY_GEN_SUCCESS = 0;
    public static final int KEY_GEN_SUPERFLUOUS_ATTESTATION_CHALLENGE = 2;

    public static Intent createInstallIntent() {
        Intent intent = new Intent("android.credentials.INSTALL");
        intent.setClassName(CERT_INSTALLER_PACKAGE, "com.android.certinstaller.CertInstallerMain");
        return intent;
    }

    public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback keyChainAliasCallback, String[] strArr, Principal[] principalArr, String str, int i, String str2) {
        Uri uriBuild;
        String str3;
        if (str != null) {
            Uri.Builder builder = new Uri.Builder();
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            if (i != -1) {
                str3 = SettingsStringUtil.DELIMITER + i;
            } else {
                str3 = "";
            }
            sb.append(str3);
            uriBuild = builder.authority(sb.toString()).build();
        } else {
            uriBuild = null;
        }
        choosePrivateKeyAlias(activity, keyChainAliasCallback, strArr, principalArr, uriBuild, str2);
    }

    public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback keyChainAliasCallback, String[] strArr, Principal[] principalArr, Uri uri, String str) {
        if (activity == null) {
            throw new NullPointerException("activity == null");
        }
        if (keyChainAliasCallback == null) {
            throw new NullPointerException("response == null");
        }
        Intent intent = new Intent(ACTION_CHOOSER);
        intent.setPackage("com.android.keychain");
        intent.putExtra("response", new AliasResponse(keyChainAliasCallback));
        intent.putExtra("uri", uri);
        intent.putExtra(EXTRA_ALIAS, str);
        intent.putExtra(EXTRA_SENDER, PendingIntent.getActivity(activity, 0, new Intent(), 0));
        activity.startActivity(intent);
    }

    private static class AliasResponse extends IKeyChainAliasCallback.Stub {
        private final KeyChainAliasCallback keyChainAliasResponse;

        private AliasResponse(KeyChainAliasCallback keyChainAliasCallback) {
            this.keyChainAliasResponse = keyChainAliasCallback;
        }

        @Override
        public void alias(String str) {
            this.keyChainAliasResponse.alias(str);
        }
    }

    public static PrivateKey getPrivateKey(Context context, String str) throws InterruptedException, KeyChainException {
        KeyPair keyPair = getKeyPair(context, str);
        if (keyPair != null) {
            return keyPair.getPrivate();
        }
        return null;
    }

    public static KeyPair getKeyPair(Context context, String str) throws InterruptedException, KeyChainException {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        try {
            KeyChainConnection keyChainConnectionBind = bind(context.getApplicationContext());
            Throwable th = null;
            try {
                try {
                    String strRequestPrivateKey = keyChainConnectionBind.getService().requestPrivateKey(str);
                    if (strRequestPrivateKey == null) {
                        return null;
                    }
                    try {
                        return AndroidKeyStoreProvider.loadAndroidKeyStoreKeyPairFromKeystore(KeyStore.getInstance(), strRequestPrivateKey, -1);
                    } catch (RuntimeException | UnrecoverableKeyException e) {
                        throw new KeyChainException(e);
                    }
                } finally {
                }
            } finally {
                if (keyChainConnectionBind != null) {
                    $closeResource(th, keyChainConnectionBind);
                }
            }
        } catch (RemoteException e2) {
            throw new KeyChainException(e2);
        } catch (RuntimeException e3) {
            throw new KeyChainException(e3);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static X509Certificate[] getCertificateChain(Context context, String str) throws Exception {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        try {
            KeyChainConnection keyChainConnectionBind = bind(context.getApplicationContext());
            Throwable th = null;
            try {
                try {
                    IKeyChainService service = keyChainConnectionBind.getService();
                    byte[] certificate = service.getCertificate(str);
                    if (certificate == null) {
                        return null;
                    }
                    byte[] caCertificates = service.getCaCertificates(str);
                    if (keyChainConnectionBind != null) {
                        $closeResource(null, keyChainConnectionBind);
                    }
                    try {
                        X509Certificate certificate2 = toCertificate(certificate);
                        if (caCertificates == null || caCertificates.length == 0) {
                            List certificateChain = new TrustedCertificateStore().getCertificateChain(certificate2);
                            return (X509Certificate[]) certificateChain.toArray(new X509Certificate[certificateChain.size()]);
                        }
                        Collection<X509Certificate> certificates = toCertificates(caCertificates);
                        ArrayList arrayList = new ArrayList(certificates.size() + 1);
                        arrayList.add(certificate2);
                        arrayList.addAll(certificates);
                        return (X509Certificate[]) arrayList.toArray(new X509Certificate[arrayList.size()]);
                    } catch (RuntimeException | CertificateException e) {
                        throw new KeyChainException(e);
                    }
                } finally {
                }
            } finally {
                if (keyChainConnectionBind != null) {
                }
            }
            if (keyChainConnectionBind != null) {
                $closeResource(th, keyChainConnectionBind);
            }
        } catch (RemoteException e2) {
            throw new KeyChainException(e2);
        } catch (RuntimeException e3) {
            throw new KeyChainException(e3);
        }
    }

    public static boolean isKeyAlgorithmSupported(String str) {
        String upperCase = str.toUpperCase(Locale.US);
        return KeyProperties.KEY_ALGORITHM_EC.equals(upperCase) || KeyProperties.KEY_ALGORITHM_RSA.equals(upperCase);
    }

    @Deprecated
    public static boolean isBoundKeyAlgorithm(String str) {
        if (!isKeyAlgorithmSupported(str)) {
            return false;
        }
        return KeyStore.getInstance().isHardwareBacked(str);
    }

    public static X509Certificate toCertificate(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("bytes == null");
        }
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }

    public static Collection<X509Certificate> toCertificates(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("bytes == null");
        }
        try {
            return CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(bArr));
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }

    public static class KeyChainConnection implements Closeable {
        private final Context context;
        private final IKeyChainService service;
        private final ServiceConnection serviceConnection;

        protected KeyChainConnection(Context context, ServiceConnection serviceConnection, IKeyChainService iKeyChainService) {
            this.context = context;
            this.serviceConnection = serviceConnection;
            this.service = iKeyChainService;
        }

        @Override
        public void close() {
            this.context.unbindService(this.serviceConnection);
        }

        public IKeyChainService getService() {
            return this.service;
        }
    }

    public static KeyChainConnection bind(Context context) throws InterruptedException {
        return bindAsUser(context, Process.myUserHandle());
    }

    public static KeyChainConnection bindAsUser(Context context, UserHandle userHandle) throws InterruptedException {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        ensureNotOnMainThread(context);
        final LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(1);
        ServiceConnection serviceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                if (!this.mConnectedAtLeastOnce) {
                    this.mConnectedAtLeastOnce = true;
                    try {
                        linkedBlockingQueue.put(IKeyChainService.Stub.asInterface(Binder.allowBlocking(iBinder)));
                    } catch (InterruptedException e) {
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        Intent intent = new Intent(IKeyChainService.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(context.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !context.bindServiceAsUser(intent, serviceConnection, 1, userHandle)) {
            throw new AssertionError("could not bind to KeyChainService");
        }
        return new KeyChainConnection(context, serviceConnection, (IKeyChainService) linkedBlockingQueue.take());
    }

    private static void ensureNotOnMainThread(Context context) {
        Looper looperMyLooper = Looper.myLooper();
        if (looperMyLooper != null && looperMyLooper == context.getMainLooper()) {
            throw new IllegalStateException("calling this from your main thread can lead to deadlock");
        }
    }
}
