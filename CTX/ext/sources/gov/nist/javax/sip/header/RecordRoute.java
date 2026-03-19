package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import javax.sip.header.RecordRouteHeader;

public class RecordRoute extends AddressParametersHeader implements RecordRouteHeader {
    private static final long serialVersionUID = 2388023364181727205L;

    public RecordRoute(AddressImpl addressImpl) {
        super("Record-Route");
        this.address = addressImpl;
    }

    public RecordRoute() {
        super("Record-Route");
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.address.getAddressType() == 2) {
            stringBuffer.append(Separators.LESS_THAN);
        }
        this.address.encode(stringBuffer);
        if (this.address.getAddressType() == 2) {
            stringBuffer.append(Separators.GREATER_THAN);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }
}
