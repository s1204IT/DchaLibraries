package java.security.cert;

public class LDAPCertStoreParameters implements CertStoreParameters {
    private static final int LDAP_DEFAULT_PORT = 389;
    private int port;
    private String serverName;

    public LDAPCertStoreParameters(String str, int i) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.serverName = str;
        this.port = i;
    }

    public LDAPCertStoreParameters(String str) {
        this(str, LDAP_DEFAULT_PORT);
    }

    public LDAPCertStoreParameters() {
        this("localhost", LDAP_DEFAULT_PORT);
    }

    public String getServerName() {
        return this.serverName;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("LDAPCertStoreParameters: [\n");
        stringBuffer.append("  serverName: " + this.serverName + "\n");
        stringBuffer.append("  port: " + this.port + "\n");
        stringBuffer.append("]");
        return stringBuffer.toString();
    }
}
