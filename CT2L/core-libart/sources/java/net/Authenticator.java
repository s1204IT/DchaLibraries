package java.net;

public abstract class Authenticator {
    private static Authenticator thisAuthenticator;
    private InetAddress addr;
    private String host;
    private int port;
    private String prompt;
    private String protocol;
    private RequestorType rt;
    private String scheme;
    private URL url;

    public enum RequestorType {
        PROXY,
        SERVER
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return null;
    }

    protected final int getRequestingPort() {
        return this.port;
    }

    protected final InetAddress getRequestingSite() {
        return this.addr;
    }

    protected final String getRequestingPrompt() {
        return this.prompt;
    }

    protected final String getRequestingProtocol() {
        return this.protocol;
    }

    protected final String getRequestingScheme() {
        return this.scheme;
    }

    public static synchronized PasswordAuthentication requestPasswordAuthentication(InetAddress rAddr, int rPort, String rProtocol, String rPrompt, String rScheme) {
        PasswordAuthentication passwordAuthentication;
        if (thisAuthenticator == null) {
            passwordAuthentication = null;
        } else {
            thisAuthenticator.addr = rAddr;
            thisAuthenticator.port = rPort;
            thisAuthenticator.protocol = rProtocol;
            thisAuthenticator.prompt = rPrompt;
            thisAuthenticator.scheme = rScheme;
            thisAuthenticator.rt = RequestorType.SERVER;
            passwordAuthentication = thisAuthenticator.getPasswordAuthentication();
        }
        return passwordAuthentication;
    }

    public static void setDefault(Authenticator a) {
        thisAuthenticator = a;
    }

    public static synchronized PasswordAuthentication requestPasswordAuthentication(String rHost, InetAddress rAddr, int rPort, String rProtocol, String rPrompt, String rScheme) {
        PasswordAuthentication passwordAuthentication;
        if (thisAuthenticator == null) {
            passwordAuthentication = null;
        } else {
            thisAuthenticator.host = rHost;
            thisAuthenticator.addr = rAddr;
            thisAuthenticator.port = rPort;
            thisAuthenticator.protocol = rProtocol;
            thisAuthenticator.prompt = rPrompt;
            thisAuthenticator.scheme = rScheme;
            thisAuthenticator.rt = RequestorType.SERVER;
            passwordAuthentication = thisAuthenticator.getPasswordAuthentication();
        }
        return passwordAuthentication;
    }

    protected final String getRequestingHost() {
        return this.host;
    }

    public static PasswordAuthentication requestPasswordAuthentication(String rHost, InetAddress rAddr, int rPort, String rProtocol, String rPrompt, String rScheme, URL rURL, RequestorType reqType) {
        if (thisAuthenticator == null) {
            return null;
        }
        thisAuthenticator.host = rHost;
        thisAuthenticator.addr = rAddr;
        thisAuthenticator.port = rPort;
        thisAuthenticator.protocol = rProtocol;
        thisAuthenticator.prompt = rPrompt;
        thisAuthenticator.scheme = rScheme;
        thisAuthenticator.url = rURL;
        thisAuthenticator.rt = reqType;
        return thisAuthenticator.getPasswordAuthentication();
    }

    protected URL getRequestingURL() {
        return this.url;
    }

    protected RequestorType getRequestorType() {
        return this.rt;
    }
}
