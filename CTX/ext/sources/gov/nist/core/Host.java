package gov.nist.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public Host(String str) throws IllegalArgumentException {
        this.stripAddressScopeZones = false;
        if (str == null) {
            throw new IllegalArgumentException("null host name");
        }
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
        setHost(str, 2);
    }

    public Host(String str, int i) {
        this.stripAddressScopeZones = false;
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
        setHost(str, i);
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.addressType == 3 && !isIPv6Reference(this.hostname)) {
            stringBuffer.append('[');
            stringBuffer.append(this.hostname);
            stringBuffer.append(']');
        } else {
            stringBuffer.append(this.hostname);
        }
        return stringBuffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        return ((Host) obj).hostname.equals(this.hostname);
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
                return this.inetAddress.getHostAddress();
            } catch (UnknownHostException e) {
                dbgPrint("Could not resolve hostname " + e);
                return null;
            }
        }
        return this.hostname;
    }

    public void setHostname(String str) {
        setHost(str, 1);
    }

    public void setHostAddress(String str) {
        setHost(str, 2);
    }

    private void setHost(String str, int i) {
        int iIndexOf;
        this.inetAddress = null;
        if (isIPv6Address(str)) {
            this.addressType = 3;
        } else {
            this.addressType = i;
        }
        if (str != null) {
            this.hostname = str.trim();
            if (this.addressType == 1) {
                this.hostname = this.hostname.toLowerCase();
            }
            if (this.addressType == 3 && this.stripAddressScopeZones && (iIndexOf = this.hostname.indexOf(37)) != -1) {
                this.hostname = this.hostname.substring(0, iIndexOf);
            }
        }
    }

    public void setAddress(String str) {
        setHostAddress(str);
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
        this.inetAddress = InetAddress.getByName(this.hostname);
        return this.inetAddress;
    }

    private boolean isIPv6Address(String str) {
        return (str == null || str.indexOf(58) == -1) ? false : true;
    }

    public static boolean isIPv6Reference(String str) {
        return str.charAt(0) == '[' && str.charAt(str.length() - 1) == ']';
    }

    public int hashCode() {
        return getHostname().hashCode();
    }
}
