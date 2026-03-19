package android.net.http;

import android.net.http.DelegatingSSLSession;
import android.util.Log;
import com.android.org.conscrypt.Conscrypt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class CertificateChainValidator {
    private static final String TAG = "CertificateChainValidator";
    private X509TrustManager mTrustManager;

    private static class NoPreloadHolder {
        private static final CertificateChainValidator sInstance = new CertificateChainValidator();
        private static final HostnameVerifier sVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        private NoPreloadHolder() {
        }
    }

    public static CertificateChainValidator getInstance() {
        return NoPreloadHolder.sInstance;
    }

    private CertificateChainValidator() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X.509");
            trustManagerFactory.init((KeyStore) null);
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    this.mTrustManager = (X509TrustManager) trustManager;
                }
            }
            if (this.mTrustManager == null) {
                throw new RuntimeException("None of the X.509 TrustManagers are X509TrustManager");
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException("X.509 TrustManagerFactory cannot be initialized", e);
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException("X.509 TrustManagerFactory must be available", e2);
        }
    }

    public SslError doHandshakeAndValidateServerCertificates(HttpsConnection httpsConnection, SSLSocket sSLSocket, String str) throws IOException {
        if (!sSLSocket.getSession().isValid()) {
            closeSocketThrowException(sSLSocket, "failed to perform SSL handshake");
        }
        Certificate[] peerCertificates = sSLSocket.getSession().getPeerCertificates();
        if (peerCertificates == null || peerCertificates.length == 0) {
            closeSocketThrowException(sSLSocket, "failed to retrieve peer certificates");
        } else if (httpsConnection != null && peerCertificates[0] != null) {
            httpsConnection.setCertificate(new SslCertificate((X509Certificate) peerCertificates[0]));
        }
        return verifyServerDomainAndCertificates((X509Certificate[]) peerCertificates, str, "RSA");
    }

    public static SslError verifyServerCertificates(byte[][] bArr, String str, String str2) throws IOException {
        if (bArr == null || bArr.length == 0) {
            throw new IllegalArgumentException("bad certificate chain");
        }
        X509Certificate[] x509CertificateArr = new X509Certificate[bArr.length];
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            for (int i = 0; i < bArr.length; i++) {
                x509CertificateArr[i] = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(bArr[i]));
            }
            return verifyServerDomainAndCertificates(x509CertificateArr, str, str2);
        } catch (CertificateException e) {
            throw new IOException("can't read certificate", e);
        }
    }

    public static void handleTrustStorageUpdate() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X.509");
            trustManagerFactory.init((KeyStore) null);
            boolean z = false;
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                try {
                    Method declaredMethod = trustManager.getClass().getDeclaredMethod("handleTrustStorageUpdate", new Class[0]);
                    declaredMethod.setAccessible(true);
                    declaredMethod.invoke(trustManager, new Object[0]);
                    z = true;
                } catch (Exception e) {
                }
            }
            if (!z) {
                Log.w(TAG, "Didn't find a TrustManager to handle CA list update");
            }
        } catch (KeyStoreException e2) {
            Log.w(TAG, "Couldn't initialize default X.509 TrustManagerFactory", e2);
        } catch (NoSuchAlgorithmException e3) {
            Log.w(TAG, "Couldn't find default X.509 TrustManagerFactory");
        }
    }

    private static SslError verifyServerDomainAndCertificates(X509Certificate[] x509CertificateArr, String str, String str2) throws IOException {
        X509Certificate x509Certificate = x509CertificateArr[0];
        if (x509Certificate == null) {
            throw new IllegalArgumentException("certificate for this site is null");
        }
        if (!((str == null || str.isEmpty() || !NoPreloadHolder.sVerifier.verify(str, new DelegatingSSLSession.CertificateWrap(x509Certificate))) ? false : true)) {
            return new SslError(2, x509Certificate);
        }
        try {
            X509TrustManager defaultX509TrustManager = Conscrypt.getDefaultX509TrustManager();
            try {
                defaultX509TrustManager.getClass().getMethod("checkServerTrusted", X509Certificate[].class, String.class, String.class).invoke(defaultX509TrustManager, x509CertificateArr, str2, str);
                return null;
            } catch (IllegalAccessException | NoSuchMethodException e) {
                defaultX509TrustManager.checkServerTrusted(x509CertificateArr, str2);
                return null;
            } catch (InvocationTargetException e2) {
                if (e2.getCause() instanceof CertificateException) {
                    throw ((CertificateException) e2.getCause());
                }
                throw new RuntimeException(e2.getCause());
            }
        } catch (GeneralSecurityException e3) {
            return new SslError(3, x509Certificate);
        }
    }

    private X509TrustManager getTrustManager() {
        return this.mTrustManager;
    }

    private void closeSocketThrowException(SSLSocket sSLSocket, String str, String str2) throws IOException {
        if (str == null) {
            str = str2;
        }
        closeSocketThrowException(sSLSocket, str);
    }

    private void closeSocketThrowException(SSLSocket sSLSocket, String str) throws IOException {
        if (sSLSocket != null) {
            SSLSession session = sSLSocket.getSession();
            if (session != null) {
                session.invalidate();
            }
            sSLSocket.close();
        }
        throw new SSLHandshakeException(str);
    }
}
