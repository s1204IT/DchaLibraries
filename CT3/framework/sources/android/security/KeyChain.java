package android.security;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.IKeyChainAliasCallback;
import android.security.IKeyChainService;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.keystore.KeyProperties;
import com.android.org.conscrypt.TrustedCertificateStore;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class KeyChain {
    public static final String ACCOUNT_TYPE = "com.android.keychain";
    private static final String ACTION_CHOOSER = "com.android.keychain.CHOOSER";
    private static final String ACTION_INSTALL = "android.credentials.INSTALL";
    public static final String ACTION_STORAGE_CHANGED = "android.security.STORAGE_CHANGED";
    private static final String CERT_INSTALLER_PACKAGE = "com.android.certinstaller";
    public static final String EXTRA_ALIAS = "alias";
    public static final String EXTRA_CERTIFICATE = "CERT";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PKCS12 = "PKCS12";
    public static final String EXTRA_RESPONSE = "response";
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_URI = "uri";
    private static final String KEYCHAIN_PACKAGE = "com.android.keychain";

    public static Intent createInstallIntent() {
        Intent intent = new Intent("android.credentials.INSTALL");
        intent.setClassName(CERT_INSTALLER_PACKAGE, "com.android.certinstaller.CertInstallerMain");
        return intent;
    }

    public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback response, String[] keyTypes, Principal[] issuers, String host, int port, String alias) {
        Uri uri = null;
        if (host != null) {
            uri = new Uri.Builder().authority(host + (port != -1 ? ":" + port : ProxyInfo.LOCAL_EXCL_LIST)).build();
        }
        choosePrivateKeyAlias(activity, response, keyTypes, issuers, uri, alias);
    }

    public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback response, String[] keyTypes, Principal[] issuers, Uri uri, String alias) {
        AliasResponse aliasResponse = null;
        if (activity == null) {
            throw new NullPointerException("activity == null");
        }
        if (response == null) {
            throw new NullPointerException("response == null");
        }
        Intent intent = new Intent(ACTION_CHOOSER);
        intent.setPackage("com.android.keychain");
        intent.putExtra("response", new AliasResponse(response, aliasResponse));
        intent.putExtra("uri", uri);
        intent.putExtra(EXTRA_ALIAS, alias);
        intent.putExtra(EXTRA_SENDER, PendingIntent.getActivity(activity, 0, new Intent(), 0));
        activity.startActivity(intent);
    }

    private static class AliasResponse extends IKeyChainAliasCallback.Stub {
        private final KeyChainAliasCallback keyChainAliasResponse;

        AliasResponse(KeyChainAliasCallback keyChainAliasResponse, AliasResponse aliasResponse) {
            this(keyChainAliasResponse);
        }

        private AliasResponse(KeyChainAliasCallback keyChainAliasResponse) {
            this.keyChainAliasResponse = keyChainAliasResponse;
        }

        @Override
        public void alias(String alias) {
            this.keyChainAliasResponse.alias(alias);
        }
    }

    public static PrivateKey getPrivateKey(Context context, String alias) throws InterruptedException, KeyChainException {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        KeyChainConnection keyChainConnection = bind(context);
        try {
            try {
                try {
                    IKeyChainService keyChainService = keyChainConnection.getService();
                    String keyId = keyChainService.requestPrivateKey(alias);
                    if (keyId == null) {
                        return null;
                    }
                    return AndroidKeyStoreProvider.loadAndroidKeyStorePrivateKeyFromKeystore(KeyStore.getInstance(), keyId, -1);
                } catch (RemoteException e) {
                    throw new KeyChainException(e);
                }
            } catch (RuntimeException e2) {
                throw new KeyChainException(e2);
            } catch (UnrecoverableKeyException e3) {
                throw new KeyChainException(e3);
            }
        } finally {
            keyChainConnection.close();
        }
    }

    public static X509Certificate[] getCertificateChain(Context context, String alias) throws InterruptedException, KeyChainException {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        }
        KeyChainConnection keyChainConnection = bind(context);
        try {
            try {
                try {
                    try {
                        IKeyChainService keyChainService = keyChainConnection.getService();
                        byte[] certificateBytes = keyChainService.getCertificate(alias);
                        if (certificateBytes == null) {
                            return null;
                        }
                        X509Certificate leafCert = toCertificate(certificateBytes);
                        byte[] certChainBytes = keyChainService.getCaCertificates(alias);
                        if (certChainBytes == null || certChainBytes.length == 0) {
                            TrustedCertificateStore store = new TrustedCertificateStore();
                            List<X509Certificate> chain = store.getCertificateChain(leafCert);
                            return (X509Certificate[]) chain.toArray(new X509Certificate[chain.size()]);
                        }
                        Collection<? extends X509Certificate> chain2 = toCertificates(certChainBytes);
                        ArrayList<X509Certificate> fullChain = new ArrayList<>(chain2.size() + 1);
                        fullChain.add(leafCert);
                        fullChain.addAll(chain2);
                        return (X509Certificate[]) fullChain.toArray(new X509Certificate[fullChain.size()]);
                    } catch (CertificateException e) {
                        throw new KeyChainException(e);
                    }
                } catch (RemoteException e2) {
                    throw new KeyChainException(e2);
                }
            } catch (RuntimeException e3) {
                throw new KeyChainException(e3);
            }
        } finally {
            keyChainConnection.close();
        }
    }

    public static boolean isKeyAlgorithmSupported(String algorithm) {
        String algUpper = algorithm.toUpperCase(Locale.US);
        if (KeyProperties.KEY_ALGORITHM_EC.equals(algUpper)) {
            return true;
        }
        return KeyProperties.KEY_ALGORITHM_RSA.equals(algUpper);
    }

    @Deprecated
    public static boolean isBoundKeyAlgorithm(String algorithm) {
        if (!isKeyAlgorithmSupported(algorithm)) {
            return false;
        }
        return KeyStore.getInstance().isHardwareBacked(algorithm);
    }

    public static X509Certificate toCertificate(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes == null");
        }
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(bytes));
            return (X509Certificate) cert;
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }

    public static Collection<X509Certificate> toCertificates(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes == null");
        }
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return certFactory.generateCertificates(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            throw new AssertionError(e);
        }
    }

    public static final class KeyChainConnection implements Closeable {
        private final Context context;
        private final IKeyChainService service;
        private final ServiceConnection serviceConnection;

        KeyChainConnection(Context context, ServiceConnection serviceConnection, IKeyChainService service, KeyChainConnection keyChainConnection) {
            this(context, serviceConnection, service);
        }

        private KeyChainConnection(Context context, ServiceConnection serviceConnection, IKeyChainService service) {
            this.context = context;
            this.serviceConnection = serviceConnection;
            this.service = service;
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

    public static KeyChainConnection bindAsUser(Context context, UserHandle user) throws InterruptedException {
        KeyChainConnection keyChainConnection = null;
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        ensureNotOnMainThread(context);
        final BlockingQueue<IKeyChainService> q = new LinkedBlockingQueue<>(1);
        ServiceConnection keyChainServiceConnection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (this.mConnectedAtLeastOnce) {
                    return;
                }
                this.mConnectedAtLeastOnce = true;
                try {
                    q.put(IKeyChainService.Stub.asInterface(service));
                } catch (InterruptedException e) {
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent intent = new Intent(IKeyChainService.class.getName());
        ComponentName comp = intent.resolveSystemService(context.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !context.bindServiceAsUser(intent, keyChainServiceConnection, 1, user)) {
            throw new AssertionError("could not bind to KeyChainService");
        }
        return new KeyChainConnection(context, keyChainServiceConnection, q.take(), keyChainConnection);
    }

    private static void ensureNotOnMainThread(Context context) {
        Looper looper = Looper.myLooper();
        if (looper == null || looper != context.getMainLooper()) {
        } else {
            throw new IllegalStateException("calling this from your main thread can lead to deadlock");
        }
    }
}
