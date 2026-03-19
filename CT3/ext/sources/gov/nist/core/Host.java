package gov.nist.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class Host extends GenericObject {
    protected static final int HOSTNAME = 1;
    protected static final int IPV4ADDRESS = 2;
    protected static final int IPV6ADDRESS = 3;
    private static final long serialVersionUID = -7233564517978323344L;
    protected int addressType;
    protected String hostname;
    private InetAddress inetAddress;
    private boolean stripAddressScopeZones;

    public Host() {
        this.stripAddressScopeZones = false;
        this.addressType = 1;
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }

    public Host(String hostName) throws IllegalArgumentException {
        this.stripAddressScopeZones = false;
        if (hostName == null) {
            throw new IllegalArgumentException("null host name");
        }
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
        setHost(hostName, 2);
    }

    public Host(String name, int addrType) {
        this.stripAddressScopeZones = false;
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
        setHost(name, addrType);
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        if (this.addressType == 3 && !isIPv6Reference(this.hostname)) {
            buffer.append('[').append(this.hostname).append(']');
        } else {
            buffer.append(this.hostname);
        }
        return buffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        Host otherHost = (Host) obj;
        return otherHost.hostname.equals(this.hostname);
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getAddress() {
        return this.hostname;
    }

    public String getIpAddress() {
        if (this.hostname == null) {
            return null;
        }
        if (this.addressType == 1) {
            try {
                if (this.inetAddress == null) {
                    this.inetAddress = InetAddress.getByName(this.hostname);
                }
                String rawIpAddress = this.inetAddress.getHostAddress();
                return rawIpAddress;
            } catch (UnknownHostException ex) {
                dbgPrint("Could not resolve hostname " + ex);
                return null;
            }
        }
        String rawIpAddress2 = this.hostname;
        return rawIpAddress2;
    }

    public void setHostname(String h) {
        setHost(h, 1);
    }

    public void setHostAddress(String address) {
        setHost(address, 2);
    }

    private void setHost(String host, int type) {
        int zoneStart;
        this.inetAddress = null;
        if (isIPv6Address(host)) {
            this.addressType = 3;
        } else {
            this.addressType = type;
        }
        if (host == null) {
            return;
        }
        this.hostname = host.trim();
        if (this.addressType == 1) {
            this.hostname = this.hostname.toLowerCase();
        }
        if (this.addressType != 3 || !this.stripAddressScopeZones || (zoneStart = this.hostname.indexOf(37)) == -1) {
            return;
        }
        this.hostname = this.hostname.substring(0, zoneStart);
    }

    public void setAddress(String address) {
        setHostAddress(address);
    }

    public boolean isHostname() {
        return this.addressType == 1;
    }

    public boolean isIPAddress() {
        return this.addressType != 1;
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        if (this.hostname == null) {
            return null;
        }
        if (this.inetAddress != null) {
            return this.inetAddress;
        }
        new Properties();
        String val = System.getProperty("sip.dns.timeout");
        if (val != null && "true".equals(val)) {
            this.inetAddress = getByAsyncName(this.hostname);
        } else {
            this.inetAddress = InetAddress.getByName(this.hostname);
        }
        return this.inetAddress;
    }

    private boolean isIPv6Address(String address) {
        return (address == null || address.indexOf(58) == -1) ? false : true;
    }

    public static boolean isIPv6Reference(String address) {
        return address.charAt(0) == '[' && address.charAt(address.length() + (-1)) == ']';
    }

    public int hashCode() {
        return getHostname().hashCode();
    }

    private static InetAddress getByAsyncName(String host) throws UnknownHostException {
        QueryDns dns = new QueryDns(host);
        dns.start();
        return dns.getDnsAddress();
    }

    private static class QueryDns extends Thread {
        private String mHost;
        private boolean mDone = false;
        private InetAddress mHostAddress = null;
        private boolean mIsUnknownHost = false;

        QueryDns(String host) {
            this.mHost = null;
            this.mHost = host;
        }

        @Override
        public void run() {
            synchronized (this) {
                if (this.mHost == null) {
                    return;
                }
                try {
                    try {
                        this.mHostAddress = InetAddress.getByName(this.mHost);
                    } catch (UnknownHostException e) {
                        this.mIsUnknownHost = true;
                        System.out.println("done");
                        this.mDone = true;
                        notifyAll();
                    }
                } finally {
                    System.out.println("done");
                    this.mDone = true;
                    notifyAll();
                }
            }
        }

        synchronized InetAddress getDnsAddress() throws UnknownHostException {
            try {
                wait(8000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (this.mIsUnknownHost) {
                throw new UnknownHostException(this.mHost + ": unknown host");
            }
            return this.mHostAddress;
        }
    }
}
