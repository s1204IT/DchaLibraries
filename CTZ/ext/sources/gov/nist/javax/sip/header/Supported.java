package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.header.SupportedHeader;

public class Supported extends SIPHeader implements SupportedHeader {
    private static final long serialVersionUID = -7679667592702854542L;
    protected String optionTag;

    public Supported() {
        super("Supported");
        this.optionTag = null;
    }

    public Supported(String str) {
        super("Supported");
        this.optionTag = str;
    }

    @Override
    public String encode() {
        String str = this.headerName + Separators.COLON;
        if (this.optionTag != null) {
            str = str + Separators.SP + this.optionTag;
        }
        return str + Separators.NEWLINE;
    }

    @Override
    public String encodeBody() {
        return this.optionTag != null ? this.optionTag : "";
    }

    @Override
    public void setOptionTag(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Supported, setOptionTag(), the optionTag parameter is null");
        }
        this.optionTag = str;
    }

    @Override
    public String getOptionTag() {
        return this.optionTag;
    }
}
