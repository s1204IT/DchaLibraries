package com.android.server.wifi.hotspot2;

import android.content.Context;
import android.net.wifi.hotspot2.PasspointConfiguration;
import com.android.org.conscrypt.TrustManagerImpl;
import com.android.server.wifi.Clock;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.PasspointConfigStoreData;
import com.android.server.wifi.hotspot2.PasspointEventHandler;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;

public class PasspointObjectFactory {
    public PasspointEventHandler makePasspointEventHandler(WifiNative wifiNative, PasspointEventHandler.Callbacks callbacks) {
        return new PasspointEventHandler(wifiNative, callbacks);
    }

    public PasspointProvider makePasspointProvider(PasspointConfiguration passpointConfiguration, WifiKeyStore wifiKeyStore, SIMAccessor sIMAccessor, long j, int i) {
        return new PasspointProvider(passpointConfiguration, wifiKeyStore, sIMAccessor, j, i);
    }

    public PasspointConfigStoreData makePasspointConfigStoreData(WifiKeyStore wifiKeyStore, SIMAccessor sIMAccessor, PasspointConfigStoreData.DataSource dataSource) {
        return new PasspointConfigStoreData(wifiKeyStore, sIMAccessor, dataSource);
    }

    public AnqpCache makeAnqpCache(Clock clock) {
        return new AnqpCache(clock);
    }

    public ANQPRequestManager makeANQPRequestManager(PasspointEventHandler passpointEventHandler, Clock clock) {
        return new ANQPRequestManager(passpointEventHandler, clock);
    }

    public CertificateVerifier makeCertificateVerifier() {
        return new CertificateVerifier();
    }

    public PasspointProvisioner makePasspointProvisioner(Context context) {
        return new PasspointProvisioner(context, this);
    }

    public OsuNetworkConnection makeOsuNetworkConnection(Context context) {
        return new OsuNetworkConnection(context);
    }

    public OsuServerConnection makeOsuServerConnection() {
        return new OsuServerConnection();
    }

    public WfaKeyStore makeWfaKeyStore() {
        return new WfaKeyStore();
    }

    public SSLContext getSSLContext(String str) {
        try {
            return SSLContext.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public TrustManagerImpl getTrustManagerImpl(KeyStore keyStore) {
        return new TrustManagerImpl(keyStore);
    }
}
