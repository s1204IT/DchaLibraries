package gov.nist.javax.sip.header;

import gov.nist.javax.sip.address.AddressImpl;
import javax.sip.address.Address;
import javax.sip.header.HeaderAddress;
import javax.sip.header.Parameters;

public abstract class AddressParametersHeader extends ParametersHeader implements Parameters {
    protected AddressImpl address;

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = (AddressImpl) address;
    }

    protected AddressParametersHeader(String str) {
        super(str);
    }

    protected AddressParametersHeader(String str, boolean z) {
        super(str, z);
    }

    @Override
    public Object clone() {
        AddressParametersHeader addressParametersHeader = (AddressParametersHeader) super.clone();
        if (this.address != null) {
            addressParametersHeader.address = (AddressImpl) this.address.clone();
        }
        return addressParametersHeader;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HeaderAddress) || !(obj instanceof Parameters)) {
            return false;
        }
        HeaderAddress headerAddress = (HeaderAddress) obj;
        return getAddress().equals(headerAddress.getAddress()) && equalParameters((Parameters) headerAddress);
    }
}
