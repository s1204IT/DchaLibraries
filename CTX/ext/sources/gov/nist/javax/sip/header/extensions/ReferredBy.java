package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public final class ReferredBy extends AddressParametersHeader implements ExtensionHeader, ReferredByHeader {
    public static final String NAME = "Referred-By";
    private static final long serialVersionUID = 3134344915465784267L;

    public ReferredBy() {
        super("Referred-By");
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
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
