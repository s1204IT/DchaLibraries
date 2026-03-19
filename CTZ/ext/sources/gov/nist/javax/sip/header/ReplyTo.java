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

    public ReplyTo(AddressImpl addressImpl) {
        super("Reply-To");
        this.address = addressImpl;
    }

    @Override
    public String encode() {
        return this.headerName + Separators.COLON + Separators.SP + encodeBody() + Separators.NEWLINE;
    }

    @Override
    public String encodeBody() {
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

    public HostPort getHostPort() {
        return this.address.getHostPort();
    }

    @Override
    public String getDisplayName() {
        return this.address.getDisplayName();
    }
}
