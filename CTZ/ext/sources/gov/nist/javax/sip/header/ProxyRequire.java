package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.ProxyRequireHeader;

public class ProxyRequire extends SIPHeader implements ProxyRequireHeader {
    private static final long serialVersionUID = -3269274234851067893L;
    protected String optionTag;

    public ProxyRequire() {
        super("Proxy-Require");
    }

    public ProxyRequire(String str) {
        super("Proxy-Require");
        this.optionTag = str;
    }

    @Override
    public String encodeBody() {
        return this.optionTag;
    }

    @Override
    public void setOptionTag(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, ProxyRequire, setOptionTag(), the optionTag parameter is null");
        }
        this.optionTag = str;
    }

    @Override
    public String getOptionTag() {
        return this.optionTag;
    }
}
