package java.security;

import java.io.Serializable;
import java.net.URL;

public class CodeSource implements Serializable {
    public CodeSource(URL location, java.security.cert.Certificate[] certs) {
    }

    public CodeSource(URL location, CodeSigner[] signers) {
    }

    public final java.security.cert.Certificate[] getCertificates() {
        return null;
    }

    public final CodeSigner[] getCodeSigners() {
        return null;
    }

    public final URL getLocation() {
        return null;
    }

    public boolean implies(CodeSource cs) {
        return true;
    }
}
