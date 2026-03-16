package java.security.cert;

public class LDAPCertStoreParameters implements CertStoreParameters {
    private static final int DEFAULT_LDAP_PORT = 389;
    private static final String DEFAULT_LDAP_SERVER_NAME = "localhost";
    private final int port;
    private final String serverName;

    public LDAPCertStoreParameters(String serverName, int port) {
        if (serverName == null) {
            throw new NullPointerException("serverName == null");
        }
        this.port = port;
        this.serverName = serverName;
    }

    public LDAPCertStoreParameters() {
        this.serverName = DEFAULT_LDAP_SERVER_NAME;
        this.port = DEFAULT_LDAP_PORT;
    }

    public LDAPCertStoreParameters(String serverName) {
        if (serverName == null) {
            throw new NullPointerException("serverName == null");
        }
        this.port = DEFAULT_LDAP_PORT;
        this.serverName = serverName;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public int getPort() {
        return this.port;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String toString() {
        return "LDAPCertStoreParameters: [\n serverName: " + getServerName() + "\n port: " + getPort() + "\n]";
    }
}
