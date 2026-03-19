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
    public boolean match(Object obj) {
        if (obj == null) {
            return true;
        }
        if (!(obj instanceof Address)) {
            return false;
        }
        AddressImpl addressImpl = (AddressImpl) obj;
        if (addressImpl.getMatcher() != null) {
            return addressImpl.getMatcher().match(encode());
        }
        if (addressImpl.displayName != null && this.displayName == null) {
            return false;
        }
        if (addressImpl.displayName == null) {
            return this.address.match(addressImpl.address);
        }
        return this.displayName.equalsIgnoreCase(addressImpl.displayName) && this.address.match(addressImpl.address);
    }

    public HostPort getHostPort() {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        return ((SipUri) this.address).getHostPort();
    }

    @Override
    public int getPort() {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        return ((SipUri) this.address).getHostPort().getPort();
    }

    @Override
    public String getUserAtHostPort() {
        if (this.address instanceof SipUri) {
            return ((SipUri) this.address).getUserAtHostPort();
        }
        return this.address.toString();
    }

    @Override
    public String getHost() {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        return ((SipUri) this.address).getHostPort().getHost().getHostname();
    }

    public void removeParameter(String str) {
        if (!(this.address instanceof SipUri)) {
            throw new RuntimeException("address is not a SipUri");
        }
        ((SipUri) this.address).removeParameter(str);
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.addressType == 3) {
            stringBuffer.append('*');
        } else {
            if (this.displayName != null) {
                stringBuffer.append(Separators.DOUBLE_QUOTE);
                stringBuffer.append(this.displayName);
                stringBuffer.append(Separators.DOUBLE_QUOTE);
                stringBuffer.append(Separators.SP);
            }
            if (this.address != null) {
                if (this.addressType == 1 || this.displayName != null) {
                    stringBuffer.append(Separators.LESS_THAN);
                }
                this.address.encode(stringBuffer);
                if (this.addressType == 1 || this.displayName != null) {
                    stringBuffer.append(Separators.GREATER_THAN);
                }
            }
        }
        return stringBuffer;
    }

    public int getAddressType() {
        return this.addressType;
    }

    public void setAddressType(int i) {
        this.addressType = i;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(String str) {
        this.displayName = str;
        this.addressType = 1;
    }

    public void setAddess(URI uri) {
        this.address = (GenericURI) uri;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Address) {
            return getURI().equals(((Address) obj).getURI());
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
    public void setURI(URI uri) {
        this.address = (GenericURI) uri;
    }

    public void setUser(String str) {
        ((SipUri) this.address).setUser(str);
    }

    @Override
    public void setWildCardFlag() {
        this.addressType = 3;
        this.address = new SipUri();
        ((SipUri) this.address).setUser(Separators.STAR);
    }

    @Override
    public Object clone() {
        AddressImpl addressImpl = (AddressImpl) super.clone();
        if (this.address != null) {
            addressImpl.address = (GenericURI) this.address.clone();
        }
        return addressImpl;
    }
}
