package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.x500.X500Name;
import com.android.org.bouncycastle.x509.X509AttributeCertificate;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

class PrincipalUtils {
    PrincipalUtils() {
    }

    static X500Name getSubjectPrincipal(X509Certificate cert) {
        return X500Name.getInstance(cert.getSubjectX500Principal().getEncoded());
    }

    static X500Name getIssuerPrincipal(X509CRL crl) {
        return X500Name.getInstance(crl.getIssuerX500Principal().getEncoded());
    }

    static X500Name getIssuerPrincipal(X509Certificate cert) {
        return X500Name.getInstance(cert.getIssuerX500Principal().getEncoded());
    }

    static X500Name getCA(TrustAnchor trustAnchor) {
        return X500Name.getInstance(trustAnchor.getCA().getEncoded());
    }

    static X500Name getEncodedIssuerPrincipal(Object obj) {
        return obj instanceof X509Certificate ? getIssuerPrincipal((X509Certificate) obj) : X500Name.getInstance(((X500Principal) ((X509AttributeCertificate) obj).getIssuer().getPrincipals()[0]).getEncoded());
    }
}
