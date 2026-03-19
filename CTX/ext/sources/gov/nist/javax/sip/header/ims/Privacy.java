package gov.nist.javax.sip.header.ims;

import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class Privacy extends SIPHeader implements PrivacyHeader, SIPHeaderNamesIms, ExtensionHeader {
    private String privacy;

    public Privacy() {
        super("Privacy");
    }

    public Privacy(String str) {
        this();
        this.privacy = str;
    }

    @Override
    public String encodeBody() {
        return this.privacy;
    }

    @Override
    public String getPrivacy() {
        return this.privacy;
    }

    @Override
    public void setPrivacy(String str) throws ParseException {
        if (str == null || str == "") {
            throw new NullPointerException("JAIN-SIP Exception,  Privacy, setPrivacy(), privacy value is null or empty");
        }
        this.privacy = str;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PrivacyHeader) {
            return getPrivacy().equals(((PrivacyHeader) obj).getPrivacy());
        }
        return false;
    }

    @Override
    public Object clone() {
        Privacy privacy = (Privacy) super.clone();
        if (this.privacy != null) {
            privacy.privacy = this.privacy;
        }
        return privacy;
    }
}
