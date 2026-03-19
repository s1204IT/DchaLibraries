package gov.nist.javax.sip.header.ims;

import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.ExtensionHeader;

public class PMediaAuthorization extends SIPHeader implements PMediaAuthorizationHeader, SIPHeaderNamesIms, ExtensionHeader {
    private static final long serialVersionUID = -6463630258703731133L;
    private String token;

    public PMediaAuthorization() {
        super("P-Media-Authorization");
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public void setMediaAuthorizationToken(String str) throws InvalidArgumentException {
        if (str == null || str.length() == 0) {
            throw new InvalidArgumentException(" the Media-Authorization-Token parameter is null or empty");
        }
        this.token = str;
    }

    @Override
    protected String encodeBody() {
        return this.token;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PMediaAuthorizationHeader) {
            return getToken().equals(((PMediaAuthorizationHeader) obj).getToken());
        }
        return false;
    }

    @Override
    public Object clone() {
        PMediaAuthorization pMediaAuthorization = (PMediaAuthorization) super.clone();
        if (this.token != null) {
            pMediaAuthorization.token = this.token;
        }
        return pMediaAuthorization;
    }
}
