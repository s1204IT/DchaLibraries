package sun.security.x509;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class URIName implements GeneralNameInterface {
    private String host;
    private DNSName hostDNS;
    private IPAddressName hostIP;
    private URI uri;

    public URIName(DerValue derValue) throws IOException {
        this(derValue.getIA5String());
    }

    public URIName(String str) throws IOException {
        try {
            this.uri = new URI(str);
            if (this.uri.getScheme() == null) {
                throw new IOException("URI name must include scheme:" + str);
            }
            this.host = this.uri.getHost();
            if (this.host != null) {
                if (this.host.charAt(0) == '[') {
                    try {
                        this.hostIP = new IPAddressName(this.host.substring(1, this.host.length() - 1));
                        return;
                    } catch (IOException e) {
                        throw new IOException("invalid URI name (host portion is not a valid IPv6 address):" + str);
                    }
                }
                try {
                    this.hostDNS = new DNSName(this.host);
                } catch (IOException e2) {
                    try {
                        this.hostIP = new IPAddressName(this.host);
                    } catch (Exception e3) {
                        throw new IOException("invalid URI name (host portion is not a valid DNS name, IPv4 address, or IPv6 address):" + str);
                    }
                }
            }
        } catch (URISyntaxException e4) {
            throw new IOException("invalid URI name:" + str, e4);
        }
    }

    public static URIName nameConstraint(DerValue derValue) throws IOException {
        DNSName dNSName;
        String iA5String = derValue.getIA5String();
        try {
            URI uri = new URI(iA5String);
            if (uri.getScheme() == null) {
                String schemeSpecificPart = uri.getSchemeSpecificPart();
                try {
                    if (schemeSpecificPart.startsWith(".")) {
                        dNSName = new DNSName(schemeSpecificPart.substring(1));
                    } else {
                        dNSName = new DNSName(schemeSpecificPart);
                    }
                    return new URIName(uri, schemeSpecificPart, dNSName);
                } catch (IOException e) {
                    throw new IOException("invalid URI name constraint:" + iA5String, e);
                }
            }
            throw new IOException("invalid URI name constraint (should not include scheme):" + iA5String);
        } catch (URISyntaxException e2) {
            throw new IOException("invalid URI name constraint:" + iA5String, e2);
        }
    }

    URIName(URI uri, String str, DNSName dNSName) {
        this.uri = uri;
        this.host = str;
        this.hostDNS = dNSName;
    }

    @Override
    public int getType() {
        return 6;
    }

    @Override
    public void encode(DerOutputStream derOutputStream) throws IOException {
        derOutputStream.putIA5String(this.uri.toASCIIString());
    }

    public String toString() {
        return "URIName: " + this.uri.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof URIName)) {
            return false;
        }
        return this.uri.equals(((URIName) obj).getURI());
    }

    public URI getURI() {
        return this.uri;
    }

    public String getName() {
        return this.uri.toString();
    }

    public String getScheme() {
        return this.uri.getScheme();
    }

    public String getHost() {
        return this.host;
    }

    public Object getHostObject() {
        if (this.hostIP != null) {
            return this.hostIP;
        }
        return this.hostDNS;
    }

    public int hashCode() {
        return this.uri.hashCode();
    }

    @Override
    public int constrains(GeneralNameInterface generalNameInterface) throws UnsupportedOperationException {
        int i = 3;
        if (generalNameInterface == null || generalNameInterface.getType() != 6) {
            return -1;
        }
        URIName uRIName = (URIName) generalNameInterface;
        String host = uRIName.getHost();
        if (host.equalsIgnoreCase(this.host)) {
            return 0;
        }
        Object hostObject = uRIName.getHostObject();
        if (this.hostDNS == null || !(hostObject instanceof DNSName)) {
            return 3;
        }
        boolean z = this.host.charAt(0) == '.';
        boolean z2 = host.charAt(0) == '.';
        int iConstrains = this.hostDNS.constrains((DNSName) hostObject);
        if (z || z2 || (iConstrains != 2 && iConstrains != 1)) {
            i = iConstrains;
        }
        return (z == z2 || i != 0) ? i : z ? 2 : 1;
    }

    @Override
    public int subtreeDepth() throws UnsupportedOperationException {
        try {
            return new DNSName(this.host).subtreeDepth();
        } catch (IOException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
    }
}
