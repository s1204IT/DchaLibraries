package gov.nist.javax.sip.header;

import gov.nist.core.HostPort;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import javax.sip.header.ReplyToHeader;

public final class ReplyTo extends AddressParametersHeader implements ReplyToHeader {
    private static final long serialVersionUID = -9103698729465531373L;

    public ReplyTo() {
        super("Reply-To");
    }

    public ReplyTo(AddressImpl address) {
        super("Reply-To");
        this.address = address;
    }

    @Override
    public String encode() {
        return this.headerName + Separators.COLON + Separators.SP + encodeBody() + Separators.NEWLINE;
    }

    @Override
    public String encodeBody() {
        String retval = (this.address.getAddressType() == 2 ? "" + Separators.LESS_THAN : "") + this.address.encode();
        if (this.address.getAddressType() == 2) {
            retval = retval + Separators.GREATER_THAN;
        }
        if (!this.parameters.isEmpty()) {
            return retval + Separators.SEMICOLON + this.parameters.encode();
        }
        return retval;
    }

    public HostPort getHostPort() {
        return this.address.getHostPort();
    }

    @Override
    public String getDisplayName() {
        return this.address.getDisplayName();
    }
}
