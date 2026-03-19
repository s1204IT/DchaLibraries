package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.RequireHeader;

public class Require extends SIPHeader implements RequireHeader {
    private static final long serialVersionUID = -3743425404884053281L;
    protected String optionTag;

    public Require() {
        super("Require");
    }

    public Require(String str) {
        super("Require");
        this.optionTag = str;
    }

    @Override
    public String encodeBody() {
        return this.optionTag;
    }

    @Override
    public void setOptionTag(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Require, setOptionTag(), the optionTag parameter is null");
        }
        this.optionTag = str;
    }

    @Override
    public String getOptionTag() {
        return this.optionTag;
    }
}
