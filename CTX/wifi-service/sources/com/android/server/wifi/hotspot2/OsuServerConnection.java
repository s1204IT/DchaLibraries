package com.android.server.wifi.hotspot2;

import android.net.Network;
import android.util.Log;
import com.android.org.conscrypt.TrustManagerImpl;
import com.android.server.wifi.hotspot2.PasspointProvisioner;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class OsuServerConnection {
    private static final int DNS_NAME = 2;
    private static final String TAG = "OsuServerConnection";
    private Network mNetwork;
    private PasspointProvisioner.OsuServerCallbacks mOsuServerCallbacks;
    private SSLSocketFactory mSocketFactory;
    private WFATrustManager mTrustManager;
    private URL mUrl;
    private HttpsURLConnection mUrlConnection = null;
    private boolean mSetupComplete = false;
    private boolean mVerboseLoggingEnabled = false;

    public void setEventCallback(PasspointProvisioner.OsuServerCallbacks osuServerCallbacks) {
        this.mOsuServerCallbacks = osuServerCallbacks;
    }

    public void init(SSLContext sSLContext, TrustManagerImpl trustManagerImpl) {
        if (sSLContext == null) {
            return;
        }
        try {
            this.mTrustManager = new WFATrustManager(trustManagerImpl);
            sSLContext.init(null, new TrustManager[]{this.mTrustManager}, null);
            this.mSocketFactory = sSLContext.getSocketFactory();
            this.mSetupComplete = true;
        } catch (KeyManagementException e) {
            Log.w(TAG, "Initialization failed");
            e.printStackTrace();
        }
    }

    public boolean canValidateServer() {
        return this.mSetupComplete;
    }

    public void enableVerboseLogging(int i) {
        this.mVerboseLoggingEnabled = i > 0;
    }

    public boolean connect(URL url, Network network) {
        this.mNetwork = network;
        this.mUrl = url;
        try {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.mNetwork.openConnection(this.mUrl);
            httpsURLConnection.setSSLSocketFactory(this.mSocketFactory);
            httpsURLConnection.connect();
            this.mUrlConnection = httpsURLConnection;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to establish a URL connection");
            e.printStackTrace();
            return false;
        }
    }

    public boolean validateProvider(String str) {
        if (this.mTrustManager.getProviderCert() == null) {
            Log.e(TAG, "Provider doesn't have valid certs");
            return false;
        }
        return true;
    }

    public void cleanup() {
        this.mUrlConnection.disconnect();
    }

    private class WFATrustManager implements X509TrustManager {
        private TrustManagerImpl mDelegate;
        private List<X509Certificate> mServerCerts;

        WFATrustManager(TrustManagerImpl trustManagerImpl) {
            this.mDelegate = trustManagerImpl;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
            if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuServerConnection.TAG, "checkClientTrusted " + str);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
            if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuServerConnection.TAG, "checkServerTrusted " + str);
            }
            boolean z = false;
            try {
                this.mServerCerts = this.mDelegate.getTrustedChainForServer(x509CertificateArr, str, (SSLSocket) null);
                z = true;
            } catch (CertificateException e) {
                Log.e(OsuServerConnection.TAG, "Unable to validate certs " + e);
                if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                    e.printStackTrace();
                }
            }
            if (OsuServerConnection.this.mOsuServerCallbacks != null) {
                OsuServerConnection.this.mOsuServerCallbacks.onServerValidationStatus(OsuServerConnection.this.mOsuServerCallbacks.getSessionId(), z);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuServerConnection.TAG, "getAcceptedIssuers ");
                return null;
            }
            return null;
        }

        public X509Certificate getProviderCert() {
            X509Certificate x509Certificate = null;
            if (this.mServerCerts == null || this.mServerCerts.size() <= 0) {
                return null;
            }
            String host = OsuServerConnection.this.mUrl.getHost();
            try {
                Iterator<X509Certificate> it = this.mServerCerts.iterator();
                while (it.hasNext()) {
                    X509Certificate next = it.next();
                    Collection<List<?>> subjectAlternativeNames = next.getSubjectAlternativeNames();
                    if (subjectAlternativeNames != null) {
                        Iterator<List<?>> it2 = subjectAlternativeNames.iterator();
                        while (true) {
                            if (!it2.hasNext()) {
                                break;
                            }
                            List<?> next2 = it2.next();
                            if (next2 != null && next2.size() >= 2 && next2.get(0).getClass() == Integer.class && next2.get(1).toString().equals(host)) {
                                try {
                                    break;
                                } catch (CertificateParsingException e) {
                                    e = e;
                                    x509Certificate = next;
                                    Log.e(OsuServerConnection.TAG, "Unable to match certificate to " + host);
                                    if (OsuServerConnection.this.mVerboseLoggingEnabled) {
                                        e.printStackTrace();
                                    }
                                    return x509Certificate;
                                }
                            }
                        }
                    }
                }
            } catch (CertificateParsingException e2) {
                e = e2;
            }
            return x509Certificate;
        }
    }
}
