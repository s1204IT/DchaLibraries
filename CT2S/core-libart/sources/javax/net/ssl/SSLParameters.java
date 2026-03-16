package javax.net.ssl;

public class SSLParameters {
    private String[] cipherSuites;
    private String endpointIdentificationAlgorithm;
    private boolean needClientAuth;
    private String[] protocols;
    private boolean wantClientAuth;

    public SSLParameters() {
    }

    public SSLParameters(String[] cipherSuites) {
        setCipherSuites(cipherSuites);
    }

    public SSLParameters(String[] cipherSuites, String[] protocols) {
        setCipherSuites(cipherSuites);
        setProtocols(protocols);
    }

    public String[] getCipherSuites() {
        if (this.cipherSuites == null) {
            return null;
        }
        return (String[]) this.cipherSuites.clone();
    }

    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites == null ? null : (String[]) cipherSuites.clone();
    }

    public String[] getProtocols() {
        if (this.protocols == null) {
            return null;
        }
        return (String[]) this.protocols.clone();
    }

    public void setProtocols(String[] protocols) {
        this.protocols = protocols == null ? null : (String[]) protocols.clone();
    }

    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
        this.wantClientAuth = false;
    }

    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
        this.needClientAuth = false;
    }
}
