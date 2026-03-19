package com.android.server.locksettings.recoverablekeystore;

import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.security.keystore.recovery.TrustedRootCertificates;
import android.util.Log;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

public class TestOnlyInsecureCertificateHelper {
    private static final String TAG = "TestCertHelper";

    public X509Certificate getRootCertificate(String str) throws RemoteException, ServiceSpecificException {
        String defaultCertificateAliasIfEmpty = getDefaultCertificateAliasIfEmpty(str);
        if (isTestOnlyCertificateAlias(defaultCertificateAliasIfEmpty)) {
            return TrustedRootCertificates.getTestOnlyInsecureCertificate();
        }
        X509Certificate rootCertificate = TrustedRootCertificates.getRootCertificate(defaultCertificateAliasIfEmpty);
        if (rootCertificate == null) {
            throw new ServiceSpecificException(28, "The provided root certificate alias is invalid");
        }
        return rootCertificate;
    }

    public String getDefaultCertificateAliasIfEmpty(String str) {
        if (str == null || str.isEmpty()) {
            Log.e(TAG, "rootCertificateAlias is null or empty - use secure default value");
            return "GoogleCloudKeyVaultServiceV1";
        }
        return str;
    }

    public boolean isTestOnlyCertificateAlias(String str) {
        return "TEST_ONLY_INSECURE_CERTIFICATE_ALIAS".equals(str);
    }

    public boolean isValidRootCertificateAlias(String str) {
        return TrustedRootCertificates.getRootCertificates().containsKey(str) || isTestOnlyCertificateAlias(str);
    }

    public boolean doesCredentialSupportInsecureMode(int i, String str) {
        return i == 2 && str != null && str.startsWith("INSECURE_PSWD_");
    }

    public Map<String, SecretKey> keepOnlyWhitelistedInsecureKeys(Map<String, SecretKey> map) {
        if (map == null) {
            return null;
        }
        HashMap map2 = new HashMap();
        for (Map.Entry<String, SecretKey> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("INSECURE_KEY_ALIAS_KEY_MATERIAL_IS_NOT_PROTECTED_")) {
                map2.put(entry.getKey(), entry.getValue());
                Log.d(TAG, "adding key with insecure alias " + key + " to the recovery snapshot");
            }
        }
        return map2;
    }
}
