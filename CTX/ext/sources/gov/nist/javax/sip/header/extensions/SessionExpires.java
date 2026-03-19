package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.ExtensionHeader;

public final class SessionExpires extends ParametersHeader implements ExtensionHeader, SessionExpiresHeader {
    public static final String NAME = "Session-Expires";
    public static final String REFRESHER = "refresher";
    private static final long serialVersionUID = 8765762413224043300L;
    public int expires;

    public SessionExpires() {
        super("Session-Expires");
    }

    @Override
    public int getExpires() {
        return this.expires;
    }

    @Override
    public void setExpires(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad argument " + i);
        }
        this.expires = i;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    protected String encodeBody() {
        String string = Integer.toString(this.expires);
        if (!this.parameters.isEmpty()) {
            return string + Separators.SEMICOLON + this.parameters.encode();
        }
        return string;
    }

    @Override
    public String getRefresher() {
        return this.parameters.getParameter(REFRESHER);
    }

    @Override
    public void setRefresher(String str) {
        this.parameters.set(REFRESHER, str);
    }
}
