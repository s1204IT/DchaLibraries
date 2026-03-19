package java.security;

import java.io.Serializable;
import java.net.URL;

public class CodeSource implements Serializable {
    private URL location;

    public CodeSource(URL url, java.security.cert.Certificate[] certificateArr) {
        this.location = url;
    }

    public CodeSource(URL url, CodeSigner[] codeSignerArr) {
        this.location = url;
    }

    public final URL getLocation() {
        return this.location;
    }

    public final java.security.cert.Certificate[] getCertificates() {
        return null;
    }

    public final CodeSigner[] getCodeSigners() {
        return null;
    }

    public boolean implies(CodeSource codeSource) {
        return true;
    }
}
