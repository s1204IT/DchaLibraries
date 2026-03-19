package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.AllowHeader;

public final class Allow extends SIPHeader implements AllowHeader {
    private static final long serialVersionUID = -3105079479020693930L;
    protected String method;

    public Allow() {
        super("Allow");
    }

    public Allow(String str) {
        super("Allow");
        this.method = str;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public void setMethod(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Allow, setMethod(), the method parameter is null.");
        }
        this.method = str;
    }

    @Override
    protected String encodeBody() {
        return this.method;
    }
}
