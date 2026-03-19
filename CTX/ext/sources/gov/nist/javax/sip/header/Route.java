package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import javax.sip.header.RouteHeader;

public class Route extends AddressParametersHeader implements RouteHeader {
    private static final long serialVersionUID = 5683577362998368846L;

    public Route() {
        super("Route");
    }

    public Route(AddressImpl addressImpl) {
        super("Route");
        this.address = addressImpl;
    }

    @Override
    public int hashCode() {
        return this.address.getHostPort().encode().toLowerCase().hashCode();
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (!(this.address.getAddressType() == 1)) {
            stringBuffer.append('<');
            this.address.encode(stringBuffer);
            stringBuffer.append('>');
        } else {
            this.address.encode(stringBuffer);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof RouteHeader) && super.equals(obj);
    }
}
