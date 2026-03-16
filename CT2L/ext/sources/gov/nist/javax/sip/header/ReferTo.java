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
        String retval = (this.address.getAddressType() == 2 ? "" + Separators.LESS_THAN : "") + this.address.encode();
        if (this.address.getAddressType() == 2) {
            retval = retval + Separators.GREATER_THAN;
        }
        if (!this.parameters.isEmpty()) {
            return retval + Separators.SEMICOLON + this.parameters.encode();
        }
        return retval;
    }
}
