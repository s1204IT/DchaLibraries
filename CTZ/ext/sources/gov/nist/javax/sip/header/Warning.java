package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.WarningHeader;

public class Warning extends SIPHeader implements WarningHeader {
    private static final long serialVersionUID = -3433328864230783899L;
    protected String agent;
    protected int code;
    protected String text;

    public Warning() {
        super("Warning");
    }

    @Override
    public String encodeBody() {
        if (this.text != null) {
            return Integer.toString(this.code) + Separators.SP + this.agent + Separators.SP + Separators.DOUBLE_QUOTE + this.text + Separators.DOUBLE_QUOTE;
        }
        return Integer.toString(this.code) + Separators.SP + this.agent;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getAgent() {
        return this.agent;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public void setCode(int i) throws InvalidArgumentException {
        if (i > 99 && i < 1000) {
            this.code = i;
            return;
        }
        throw new InvalidArgumentException("Code parameter in the Warning header is invalid: code=" + i);
    }

    @Override
    public void setAgent(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("the host parameter in the Warning header is null");
        }
        this.agent = str;
    }

    @Override
    public void setText(String str) throws ParseException {
        if (str == null) {
            throw new ParseException("The text parameter in the Warning header is null", 0);
        }
        this.text = str;
    }
}
