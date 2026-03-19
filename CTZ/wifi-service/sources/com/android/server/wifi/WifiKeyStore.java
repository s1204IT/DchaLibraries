package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import java.io.IOException;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

public class WifiKeyStore {
    private static final String TAG = "WifiKeyStore";
    private final KeyStore mKeyStore;
    private boolean mVerboseLoggingEnabled = false;

    WifiKeyStore(KeyStore keyStore) {
        this.mKeyStore = keyStore;
    }

    void enableVerboseLogging(boolean z) {
        this.mVerboseLoggingEnabled = z;
    }

    private static boolean needsKeyStore(WifiEnterpriseConfig wifiEnterpriseConfig) {
        return (wifiEnterpriseConfig.getClientCertificate() == null && wifiEnterpriseConfig.getCaCertificate() == null) ? false : true;
    }

    private static boolean isHardwareBackedKey(Key key) {
        return KeyChain.isBoundKeyAlgorithm(key.getAlgorithm());
    }

    private static boolean hasHardwareBackedKey(Certificate certificate) {
        return isHardwareBackedKey(certificate.getPublicKey());
    }

    private boolean installKeys(WifiEnterpriseConfig wifiEnterpriseConfig, WifiEnterpriseConfig wifiEnterpriseConfig2, String str) {
        boolean zPutCertsInKeyStore;
        String str2;
        String str3 = "USRPKEY_" + str;
        String str4 = "USRCERT_" + str;
        X509Certificate[] clientCertificateChain = wifiEnterpriseConfig2.getClientCertificateChain();
        if (clientCertificateChain != null && clientCertificateChain.length != 0) {
            byte[] encoded = wifiEnterpriseConfig2.getClientPrivateKey().getEncoded();
            if (this.mVerboseLoggingEnabled) {
                if (isHardwareBackedKey(wifiEnterpriseConfig2.getClientPrivateKey())) {
                    Log.d(TAG, "importing keys " + str + " in hardware backed store");
                } else {
                    Log.d(TAG, "importing keys " + str + " in software backed store");
                }
            }
            boolean zImportKey = this.mKeyStore.importKey(str3, encoded, 1010, 0);
            if (!zImportKey) {
                return zImportKey;
            }
            zPutCertsInKeyStore = putCertsInKeyStore(str4, clientCertificateChain);
            if (!zPutCertsInKeyStore) {
                this.mKeyStore.delete(str3, 1010);
                return zPutCertsInKeyStore;
            }
        } else {
            zPutCertsInKeyStore = true;
        }
        X509Certificate[] caCertificates = wifiEnterpriseConfig2.getCaCertificates();
        ArraySet<String> arraySet = new ArraySet();
        if (wifiEnterpriseConfig != null && wifiEnterpriseConfig.getCaCertificateAliases() != null) {
            arraySet.addAll(Arrays.asList(wifiEnterpriseConfig.getCaCertificateAliases()));
        }
        ArrayList<String> arrayList = null;
        if (caCertificates != null) {
            arrayList = new ArrayList();
            boolean z = zPutCertsInKeyStore;
            int i = 0;
            while (i < caCertificates.length) {
                if (caCertificates.length != 1) {
                    str2 = String.format("%s_%d", str, Integer.valueOf(i));
                } else {
                    str2 = str;
                }
                arraySet.remove(str2);
                boolean zPutCertInKeyStore = putCertInKeyStore("CACERT_" + str2, caCertificates[i]);
                if (!zPutCertInKeyStore) {
                    if (wifiEnterpriseConfig2.getClientCertificate() != null) {
                        this.mKeyStore.delete(str3, 1010);
                        this.mKeyStore.delete(str4, 1010);
                    }
                    for (String str5 : arrayList) {
                        this.mKeyStore.delete("CACERT_" + str5, 1010);
                    }
                    return zPutCertInKeyStore;
                }
                arrayList.add(str2);
                i++;
                z = zPutCertInKeyStore;
            }
            zPutCertsInKeyStore = z;
        }
        for (String str6 : arraySet) {
            this.mKeyStore.delete("CACERT_" + str6, 1010);
        }
        if (wifiEnterpriseConfig2.getClientCertificate() != null) {
            wifiEnterpriseConfig2.setClientCertificateAlias(str);
            wifiEnterpriseConfig2.resetClientKeyEntry();
        }
        if (caCertificates != null) {
            wifiEnterpriseConfig2.setCaCertificateAliases((String[]) arrayList.toArray(new String[arrayList.size()]));
            wifiEnterpriseConfig2.resetCaCertificate();
        }
        return zPutCertsInKeyStore;
    }

    public boolean putCertInKeyStore(String str, Certificate certificate) {
        return putCertsInKeyStore(str, new Certificate[]{certificate});
    }

    public boolean putCertsInKeyStore(String str, Certificate[] certificateArr) {
        try {
            byte[] bArrConvertToPem = Credentials.convertToPem(certificateArr);
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "putting " + certificateArr.length + " certificate(s) " + str + " in keystore");
            }
            return this.mKeyStore.put(str, bArrConvertToPem, 1010, 0);
        } catch (IOException e) {
            return false;
        } catch (CertificateException e2) {
            return false;
        }
    }

    public boolean putKeyInKeyStore(String str, Key key) {
        return this.mKeyStore.importKey(str, key.getEncoded(), 1010, 0);
    }

    public boolean removeEntryFromKeyStore(String str) {
        return this.mKeyStore.delete(str, 1010);
    }

    public void removeKeys(WifiEnterpriseConfig wifiEnterpriseConfig) {
        String clientCertificateAlias = wifiEnterpriseConfig.getClientCertificateAlias();
        if (!TextUtils.isEmpty(clientCertificateAlias)) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "removing client private key and user cert");
            }
            this.mKeyStore.delete("USRPKEY_" + clientCertificateAlias, 1010);
            this.mKeyStore.delete("USRCERT_" + clientCertificateAlias, 1010);
        }
        String[] caCertificateAliases = wifiEnterpriseConfig.getCaCertificateAliases();
        if (caCertificateAliases != null) {
            for (String str : caCertificateAliases) {
                if (!TextUtils.isEmpty(str)) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.d(TAG, "removing CA cert: " + str);
                    }
                    this.mKeyStore.delete("CACERT_" + str, 1010);
                }
            }
        }
    }

    public boolean updateNetworkKeys(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        WifiEnterpriseConfig wifiEnterpriseConfig = wifiConfiguration.enterpriseConfig;
        if (needsKeyStore(wifiEnterpriseConfig)) {
            try {
                if (!installKeys(wifiConfiguration2 != null ? wifiConfiguration2.enterpriseConfig : null, wifiEnterpriseConfig, wifiConfiguration.getKeyIdForCredentials(wifiConfiguration2))) {
                    Log.e(TAG, wifiConfiguration.SSID + ": failed to install keys");
                    return false;
                }
                return true;
            } catch (IllegalStateException e) {
                Log.e(TAG, wifiConfiguration.SSID + " invalid config for key installation: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    public static boolean needsSoftwareBackedKeyStore(WifiEnterpriseConfig wifiEnterpriseConfig) {
        if (!TextUtils.isEmpty(wifiEnterpriseConfig.getClientCertificateAlias())) {
            return true;
        }
        return false;
    }
}
