package gov.nist.javax.sip.message;

import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class SIPDuplicateHeaderException extends ParseException {
    private static final long serialVersionUID = 8241107266407879291L;
    protected SIPHeader sipHeader;
    protected SIPMessage sipMessage;

    public SIPDuplicateHeaderException(String str) {
        super(str, 0);
    }

    public SIPMessage getSIPMessage() {
        return this.sipMessage;
    }

    public SIPHeader getSIPHeader() {
        return this.sipHeader;
    }

    public void setSIPHeader(SIPHeader sIPHeader) {
        this.sipHeader = sIPHeader;
    }

    public void setSIPMessage(SIPMessage sIPMessage) {
        this.sipMessage = sIPMessage;
    }
}
