package com.android.server.wifi.hotspot2;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class CertificateVerifier {
    public void verifyCaCert(X509Certificate x509Certificate) throws GeneralSecurityException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        CertPathValidator certPathValidator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath certPathGenerateCertPath = certificateFactory.generateCertPath(Arrays.asList(x509Certificate));
        KeyStore keyStore = KeyStore.getInstance("AndroidCAStore");
        keyStore.load(null, null);
        PKIXParameters pKIXParameters = new PKIXParameters(keyStore);
        pKIXParameters.setRevocationEnabled(false);
        certPathValidator.validate(certPathGenerateCertPath, pKIXParameters);
    }
}
