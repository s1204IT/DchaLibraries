package gov.nist.javax.sip.header.ims;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.SIPHeader;
import javax.sip.address.Address;

public abstract class AddressHeaderIms extends SIPHeader {
    protected AddressImpl address;

    @Override
    public abstract String encodeBody();

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = (AddressImpl) address;
    }

    public AddressHeaderIms(String str) {
        super(str);
    }

    @Override
    public Object clone() {
        AddressHeaderIms addressHeaderIms = (AddressHeaderIms) super.clone();
        if (this.address != null) {
            addressHeaderIms.address = (AddressImpl) this.address.clone();
        }
        return addressHeaderIms;
    }
}
