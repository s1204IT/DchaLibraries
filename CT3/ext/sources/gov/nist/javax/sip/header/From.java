package gov.nist.javax.sip.header;

import gov.nist.core.HostPort;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.parser.Parser;
import java.text.ParseException;
import javax.sip.address.Address;
import javax.sip.header.FromHeader;

public final class From extends AddressParametersHeader implements FromHeader {
    private static final long serialVersionUID = -6312727234330643892L;

    public From() {
        super("From");
    }

    public From(To to) {
        super("From");
        this.address = to.address;
        this.parameters = to.parameters;
    }

    @Override
    protected String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer buffer) {
        if (this.address.getAddressType() == 2) {
            buffer.append(Separators.LESS_THAN);
        }
        this.address.encode(buffer);
        if (this.address.getAddressType() == 2) {
            buffer.append(Separators.GREATER_THAN);
        }
        if (!this.parameters.isEmpty()) {
            buffer.append(Separators.SEMICOLON);
            this.parameters.encode(buffer);
        }
        return buffer;
    }

    public HostPort getHostPort() {
        return this.address.getHostPort();
    }

    @Override
    public String getDisplayName() {
        return this.address.getDisplayName();
    }

    @Override
    public String getTag() {
        if (this.parameters == null) {
            return null;
        }
        return getParameter(ParameterNames.TAG);
    }

    @Override
    public boolean hasTag() {
        return hasParameter(ParameterNames.TAG);
    }

    @Override
    public void removeTag() {
        this.parameters.delete(ParameterNames.TAG);
    }

    @Override
    public void setAddress(Address address) {
        this.address = (AddressImpl) address;
    }

    @Override
    public void setTag(String t) throws ParseException {
        Parser.checkToken(t);
        setParameter(ParameterNames.TAG, t);
    }

    @Override
    public String getUserAtHostPort() {
        return this.address.getUserAtHostPort();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FromHeader) {
            return super.equals(other);
        }
        return false;
    }
}
