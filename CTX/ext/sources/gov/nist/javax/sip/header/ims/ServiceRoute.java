package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class ServiceRoute extends AddressParametersHeader implements ServiceRouteHeader, SIPHeaderNamesIms, ExtensionHeader {
    public ServiceRoute(AddressImpl addressImpl) {
        super("Service-Route");
        this.address = addressImpl;
    }

    public ServiceRoute() {
        super("Service-Route");
    }

    @Override
    public String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.address.getAddressType() == 2) {
            stringBuffer.append(Separators.LESS_THAN);
        }
        stringBuffer.append(this.address.encode());
        if (this.address.getAddressType() == 2) {
            stringBuffer.append(Separators.GREATER_THAN);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON + this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}
