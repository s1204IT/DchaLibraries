package gov.nist.javax.sip.header.ims;

import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class SecurityVerify extends SecurityAgree implements SecurityVerifyHeader, ExtensionHeader {
    public SecurityVerify() {
        super("Security-Verify");
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}
