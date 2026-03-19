package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PPreferredIdentity extends AddressParametersHeader implements PPreferredIdentityHeader, SIPHeaderNamesIms, ExtensionHeader {
    public PPreferredIdentity(AddressImpl addressImpl) {
        super("P-Preferred-Identity");
        this.address = addressImpl;
    }

    public PPreferredIdentity() {
        super("P-Preferred-Identity");
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
        return stringBuffer.toString();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}
