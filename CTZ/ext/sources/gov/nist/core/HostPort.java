package gov.nist.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class HostPort extends GenericObject {
    private static final long serialVersionUID = -7103412227431884523L;
    protected Host host = null;
    protected int port = -1;

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        this.host.encode(stringBuffer);
        if (this.port != -1) {
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(this.port);
        }
        return stringBuffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HostPort hostPort = (HostPort) obj;
        return this.port == hostPort.port && this.host.equals(hostPort.host);
    }

    public Host getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public boolean hasPort() {
        return this.port != -1;
    }

    public void removePort() {
        this.port = -1;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public void setPort(int i) {
        this.port = i;
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        if (this.host == null) {
            return null;
        }
        return this.host.getInetAddress();
    }

    @Override
    public void merge(Object obj) {
        super.merge(obj);
        if (this.port == -1) {
            this.port = ((HostPort) obj).port;
        }
    }

    @Override
    public Object clone() {
        HostPort hostPort = (HostPort) super.clone();
        if (this.host != null) {
            hostPort.host = (Host) this.host.clone();
        }
        return hostPort;
    }

    public String toString() {
        return encode();
    }

    public int hashCode() {
        return this.host.hashCode() + this.port;
    }
}
