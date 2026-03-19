package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.header.ReferToHeader;

public final class ReferTo extends AddressParametersHeader implements ReferToHeader {
    private static final long serialVersionUID = -1666700428440034851L;

    public ReferTo() {
        super(ReferToHeader.NAME);
    }

    @Override
    protected String encodeBody() {
        if (this.address == null) {
            return null;
        }
        String str = "";
        if (this.address.getAddressType() == 2) {
            str = "" + Separators.LESS_THAN;
        }
        String str2 = str + this.address.encode();
        if (this.address.getAddressType() == 2) {
            str2 = str2 + Separators.GREATER_THAN;
        }
        if (!this.parameters.isEmpty()) {
            return str2 + Separators.SEMICOLON + this.parameters.encode();
        }
        return str2;
    }
}
