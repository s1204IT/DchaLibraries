package sun.security.provider.certpath;

import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import sun.security.util.Debug;

class PKIXMasterCertPathValidator {
    private static final Debug debug = Debug.getInstance("certpath");

    PKIXMasterCertPathValidator() {
    }

    static void validate(CertPath certPath, List<X509Certificate> list, List<PKIXCertPathChecker> list2) throws CertPathValidatorException {
        int size = list.size();
        if (debug != null) {
            debug.println("--------------------------------------------------------------");
            debug.println("Executing PKIX certification path validation algorithm.");
        }
        for (int i = 0; i < size; i++) {
            X509Certificate x509Certificate = list.get(i);
            if (debug != null) {
                debug.println("Checking cert" + (i + 1) + " - Subject: " + ((Object) x509Certificate.getSubjectX500Principal()));
            }
            Set<String> criticalExtensionOIDs = x509Certificate.getCriticalExtensionOIDs();
            if (criticalExtensionOIDs == null) {
                criticalExtensionOIDs = Collections.emptySet();
            }
            if (debug != null && !criticalExtensionOIDs.isEmpty()) {
                StringJoiner stringJoiner = new StringJoiner(", ", "{", "}");
                Iterator<String> it = criticalExtensionOIDs.iterator();
                while (it.hasNext()) {
                    stringJoiner.add(it.next());
                }
                debug.println("Set of critical extensions: " + stringJoiner.toString());
            }
            for (int i2 = 0; i2 < list2.size(); i2++) {
                PKIXCertPathChecker pKIXCertPathChecker = list2.get(i2);
                if (debug != null) {
                    debug.println("-Using checker" + (i2 + 1) + " ... [" + pKIXCertPathChecker.getClass().getName() + "]");
                }
                if (i == 0) {
                    pKIXCertPathChecker.init(false);
                }
                try {
                    pKIXCertPathChecker.check(x509Certificate, criticalExtensionOIDs);
                    if (debug != null) {
                        debug.println("-checker" + (i2 + 1) + " validation succeeded");
                    }
                } catch (CertPathValidatorException e) {
                    throw new CertPathValidatorException(e.getMessage(), e.getCause() != null ? e.getCause() : e, certPath, size - (i + 1), e.getReason());
                }
            }
            if (!criticalExtensionOIDs.isEmpty()) {
                throw new CertPathValidatorException("unrecognized critical extension(s)", null, certPath, size - (i + 1), PKIXReason.UNRECOGNIZED_CRIT_EXT);
            }
            if (debug != null) {
                debug.println("\ncert" + (i + 1) + " validation succeeded.\n");
            }
        }
        if (debug != null) {
            debug.println("Cert path validation succeeded. (PKIX validation algorithm)");
            debug.println("--------------------------------------------------------------");
        }
    }
}
