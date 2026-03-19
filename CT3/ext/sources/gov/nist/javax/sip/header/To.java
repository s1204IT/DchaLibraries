package gov.nist.javax.sip.header;

import gov.nist.core.HostPort;
import gov.nist.core.Separators;
import gov.nist.javax.sip.parser.Parser;
import java.text.ParseException;
import javax.sip.header.ToHeader;

public final class To extends AddressParametersHeader implements ToHeader {
    private static final long serialVersionUID = -4057413800584586316L;

    public To() {
        super("To", true);
    }

    public To(From from) {
        super("To");
        setAddress(from.address);
        setParameters(from.parameters);
    }

    @Override
    public String encode() {
        return this.headerName + Separators.COLON + Separators.SP + encodeBody() + Separators.NEWLINE;
    }

    @Override
    protected String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer buffer) {
        if (this.address != null) {
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
        }
        return buffer;
    }

    public HostPort getHostPort() {
        if (this.address == null) {
            return null;
        }
        return this.address.getHostPort();
    }

    @Override
    public String getDisplayName() {
        if (this.address == null) {
            return null;
        }
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
        if (this.parameters == null) {
            return false;
        }
        return hasParameter(ParameterNames.TAG);
    }

    @Override
    public void removeTag() {
        if (this.parameters == null) {
            return;
        }
        this.parameters.delete(ParameterNames.TAG);
    }

    @Override
    public void setTag(String t) throws ParseException {
        Parser.checkToken(t);
        setParameter(ParameterNames.TAG, t);
    }

    @Override
    public String getUserAtHostPort() {
        if (this.address == null) {
            return null;
        }
        return this.address.getUserAtHostPort();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ToHeader) {
            return super.equals(other);
        }
        return false;
    }
}
