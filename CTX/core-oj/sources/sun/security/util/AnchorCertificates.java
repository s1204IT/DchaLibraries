package sun.security.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import sun.security.x509.X509CertImpl;

public class AnchorCertificates {
    private static final String HASH = "SHA-256";
    private static HashSet<String> certs;
    private static final Debug debug = Debug.getInstance("certpath");

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() throws Throwable {
                KeyStore keyStore;
                FileInputStream fileInputStream;
                Throwable th;
                Throwable th2;
                File file = new File(System.getProperty("java.home"), "lib/security/cacerts");
                try {
                    keyStore = KeyStore.getInstance("JKS");
                    fileInputStream = new FileInputStream(file);
                } catch (Exception e) {
                    if (AnchorCertificates.debug != null) {
                        AnchorCertificates.debug.println("Error parsing cacerts");
                    }
                    e.printStackTrace();
                }
                try {
                    keyStore.load(fileInputStream, null);
                    HashSet unused = AnchorCertificates.certs = new HashSet();
                    Enumeration<String> enumerationAliases = keyStore.aliases();
                    while (enumerationAliases.hasMoreElements()) {
                        String strNextElement = enumerationAliases.nextElement();
                        if (strNextElement.contains(" [jdk")) {
                            AnchorCertificates.certs.add(X509CertImpl.getFingerprint(AnchorCertificates.HASH, (X509Certificate) keyStore.getCertificate(strNextElement)));
                        }
                    }
                    fileInputStream.close();
                    return null;
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        if (th != null) {
                            fileInputStream.close();
                            throw th2;
                        }
                        try {
                            fileInputStream.close();
                            throw th2;
                        } catch (Throwable th5) {
                            th.addSuppressed(th5);
                            throw th2;
                        }
                        return null;
                    }
                }
            }
        });
    }

    public static boolean contains(X509Certificate x509Certificate) {
        boolean zContains = certs.contains(X509CertImpl.getFingerprint(HASH, x509Certificate));
        if (zContains && debug != null) {
            debug.println("AnchorCertificate.contains: matched " + ((Object) x509Certificate.getSubjectDN()));
        }
        return zContains;
    }

    private AnchorCertificates() {
    }
}
