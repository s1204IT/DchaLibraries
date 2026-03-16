package gov.nist.javax.sip.address;

import gov.nist.core.HostPort;
import gov.nist.core.Separators;
import javax.sip.address.Address;
import javax.sip.address.URI;

public final class AddressImpl extends NetObject implements Address {
    public static final int ADDRESS_SPEC = 2;
    public static final int NAME_ADDR = 1;
    public static final int WILD_CARD = 3;
    private static final long serialVersionUID = 429592779568617259L;
    protected GenericURI address;
    protected int addressType = 1;
    protected String displayName;

    @Override
    public boolean match(Object other) {
        if (other == null) {
            return true;
        }
        if (!(other instanceof Address)) {
            return false;
        }
        AddressImpl that = (AddressImpl) other;
        if (that.getMatcher() != null) {
            return that.getMatcher().match(encode());
        }
        if (that.displayName != null && this.displayName == null) {
            return false;
        }
        if (that.displayName == null) {
            return this.address.match(that.address);
        }
        return this.displayName.equalsIgnoreCase(that.displayName) && this.address.match(that.address);
    }

    public HostPort getHostPort() {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        SipUri uri = (SipUri) this.address;
        return uri.getHostPort();
    }

    @Override
    public int getPort() {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        SipUri uri = (SipUri) this.address;
        return uri.getHostPort().getPort();
    }

    @Override
    public String getUserAtHostPort() {
        if (!(this.address instanceof SipUri)) {
            return this.address.toString();
        }
        SipUri uri = (SipUri) this.address;
        return uri.getUserAtHostPort();
    }

    @Override
    public String getHost() {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        SipUri uri = (SipUri) this.address;
        return uri.getHostPort().getHost().getHostname();
    }

    public void removeParameter(String parameterName) {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        SipUri uri = (SipUri) this.address;
        uri.removeParameter(parameterName);
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        if (this.addressType == 3) {
            buffer.append('*');
        } else {
            if (this.displayName != null) {
                buffer.append(Separators.DOUBLE_QUOTE).append(this.displayName).append(Separators.DOUBLE_QUOTE).append(Separators.SP);
            }
            if (this.address != null) {
                if (this.addressType == 1 || this.displayName != null) {
                    buffer.append(Separators.LESS_THAN);
                }
                this.address.encode(buffer);
                if (this.addressType == 1 || this.displayName != null) {
                    buffer.append(Separators.GREATER_THAN);
                }
            }
        }
        return buffer;
    }

    public int getAddressType() {
        return this.addressType;
    }

    public void setAddressType(int atype) {
        this.addressType = atype;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.addressType = 1;
    }

    public void setAddess(URI address) {
        this.address = (GenericURI) address;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Address) {
            Address o = (Address) other;
            return getURI().equals(o.getURI());
        }
        return false;
    }

    @Override
    public boolean hasDisplayName() {
        return this.displayName != null;
    }

    public void removeDisplayName() {
        this.displayName = null;
    }

    @Override
    public boolean isSIPAddress() {
        return this.address instanceof SipUri;
    }

    @Override
    public URI getURI() {
        return this.address;
    }

    @Override
    public boolean isWildcard() {
        return this.addressType == 3;
    }

    @Override
    public void setURI(URI address) {
        this.address = (GenericURI) address;
    }

    public void setUser(String user) {
        ((SipUri) this.address).setUser(user);
    }

    @Override
    public void setWildCardFlag() {
        this.addressType = 3;
        this.address = new SipUri();
        ((SipUri) this.address).setUser(Separators.STAR);
    }

    @Override
    public Object clone() {
        AddressImpl retval = (AddressImpl) super.clone();
        if (this.address != null) {
            retval.address = (GenericURI) this.address.clone();
        }
        return retval;
    }
}
