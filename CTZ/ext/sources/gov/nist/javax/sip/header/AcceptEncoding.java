package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.AcceptEncodingHeader;

public final class AcceptEncoding extends ParametersHeader implements AcceptEncodingHeader {
    private static final long serialVersionUID = -1476807565552873525L;
    protected String contentCoding;

    public AcceptEncoding() {
        super("Accept-Encoding");
    }

    @Override
    protected String encodeBody() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.contentCoding != null) {
            stringBuffer.append(this.contentCoding);
        }
        if (this.parameters != null && !this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(this.parameters.encode());
        }
        return stringBuffer;
    }

    @Override
    public float getQValue() {
        return getParameterAsFloat("q");
    }

    @Override
    public String getEncoding() {
        return this.contentCoding;
    }

    @Override
    public void setQValue(float f) throws InvalidArgumentException {
        double d = f;
        if (d < 0.0d || d > 1.0d) {
            throw new InvalidArgumentException("qvalue out of range!");
        }
        super.setParameter("q", f);
    }

    @Override
    public void setEncoding(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException(" encoding parameter is null");
        }
        this.contentCoding = str;
    }
}
