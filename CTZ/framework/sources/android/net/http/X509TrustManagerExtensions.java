package android.net.http;

import android.annotation.SystemApi;
import android.security.net.config.UserCertificateSource;
import com.android.org.conscrypt.TrustManagerImpl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.X509TrustManager;

public class X509TrustManagerExtensions {
    private final Method mCheckServerTrusted;
    private final TrustManagerImpl mDelegate;
    private final Method mIsSameTrustConfiguration;
    private final X509TrustManager mTrustManager;

    public X509TrustManagerExtensions(X509TrustManager x509TrustManager) throws IllegalArgumentException {
        Method method;
        if (x509TrustManager instanceof TrustManagerImpl) {
            this.mDelegate = (TrustManagerImpl) x509TrustManager;
            this.mTrustManager = null;
            this.mCheckServerTrusted = null;
            this.mIsSameTrustConfiguration = null;
            return;
        }
        this.mDelegate = null;
        this.mTrustManager = x509TrustManager;
        try {
            this.mCheckServerTrusted = x509TrustManager.getClass().getMethod("checkServerTrusted", X509Certificate[].class, String.class, String.class);
            try {
                method = x509TrustManager.getClass().getMethod("isSameTrustConfiguration", String.class, String.class);
            } catch (ReflectiveOperationException e) {
                method = null;
            }
            this.mIsSameTrustConfiguration = method;
        } catch (NoSuchMethodException e2) {
            throw new IllegalArgumentException("Required method checkServerTrusted(X509Certificate[], String, String, String) missing");
        }
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] x509CertificateArr, String str, String str2) throws CertificateException {
        if (this.mDelegate != null) {
            return this.mDelegate.checkServerTrusted(x509CertificateArr, str, str2);
        }
        try {
            return (List) this.mCheckServerTrusted.invoke(this.mTrustManager, x509CertificateArr, str, str2);
        } catch (IllegalAccessException e) {
            throw new CertificateException("Failed to call checkServerTrusted", e);
        } catch (InvocationTargetException e2) {
            if (e2.getCause() instanceof CertificateException) {
                throw ((CertificateException) e2.getCause());
            }
            if (e2.getCause() instanceof RuntimeException) {
                throw ((RuntimeException) e2.getCause());
            }
            throw new CertificateException("checkServerTrusted failed", e2.getCause());
        }
    }

    public boolean isUserAddedCertificate(X509Certificate x509Certificate) {
        return UserCertificateSource.getInstance().findBySubjectAndPublicKey(x509Certificate) != null;
    }

    @SystemApi
    public boolean isSameTrustConfiguration(String str, String str2) {
        if (this.mIsSameTrustConfiguration == null) {
            return true;
        }
        try {
            return ((Boolean) this.mIsSameTrustConfiguration.invoke(this.mTrustManager, str, str2)).booleanValue();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to call isSameTrustConfiguration", e);
        } catch (InvocationTargetException e2) {
            if (e2.getCause() instanceof RuntimeException) {
                throw ((RuntimeException) e2.getCause());
            }
            throw new RuntimeException("isSameTrustConfiguration failed", e2.getCause());
        }
    }
}
