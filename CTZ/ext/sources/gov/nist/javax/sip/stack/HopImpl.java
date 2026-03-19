package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import java.io.Serializable;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;

public final class HopImpl implements Hop, Serializable {
    protected boolean defaultRoute;
    protected String host;
    protected int port;
    protected String transport;
    protected boolean uriRoute;

    @Override
    public String toString() {
        return this.host + Separators.COLON + this.port + Separators.SLASH + this.transport;
    }

    public HopImpl(String str, int i, String str2) {
        this.host = str;
        if (this.host.indexOf(Separators.COLON) >= 0 && this.host.indexOf("[") < 0) {
            this.host = "[" + this.host + "]";
        }
        this.port = i;
        this.transport = str2;
    }

    HopImpl(String str) throws IllegalArgumentException {
        String strSubstring;
        if (str == null) {
            throw new IllegalArgumentException("Null arg!");
        }
        int iIndexOf = str.indexOf(93);
        int iIndexOf2 = str.indexOf(58, iIndexOf);
        int iIndexOf3 = str.indexOf(47, iIndexOf2);
        if (iIndexOf2 > 0) {
            this.host = str.substring(0, iIndexOf2);
            if (iIndexOf3 > 0) {
                strSubstring = str.substring(iIndexOf2 + 1, iIndexOf3);
                this.transport = str.substring(iIndexOf3 + 1);
            } else {
                strSubstring = str.substring(iIndexOf2 + 1);
                this.transport = ListeningPoint.UDP;
            }
            try {
                this.port = Integer.parseInt(strSubstring);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad port spec");
            }
        } else {
            if (iIndexOf3 > 0) {
                this.host = str.substring(0, iIndexOf3);
                this.transport = str.substring(iIndexOf3 + 1);
                this.port = this.transport.equalsIgnoreCase(ListeningPoint.TLS) ? 5061 : 5060;
            } else {
                this.host = str;
                this.transport = ListeningPoint.UDP;
                this.port = 5060;
            }
        }
        if (this.host == null || this.host.length() == 0) {
            throw new IllegalArgumentException("no host!");
        }
        this.host = this.host.trim();
        this.transport = this.transport.trim();
        if (iIndexOf > 0 && this.host.charAt(0) != '[') {
            throw new IllegalArgumentException("Bad IPv6 reference spec");
        }
        if (this.transport.compareToIgnoreCase(ListeningPoint.UDP) != 0 && this.transport.compareToIgnoreCase(ListeningPoint.TLS) != 0 && this.transport.compareToIgnoreCase(ListeningPoint.TCP) != 0) {
            System.err.println("Bad transport string " + this.transport);
            throw new IllegalArgumentException(str);
        }
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public String getTransport() {
        return this.transport;
    }

    @Override
    public boolean isURIRoute() {
        return this.uriRoute;
    }

    @Override
    public void setURIRouteFlag() {
        this.uriRoute = true;
    }
}
