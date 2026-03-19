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
    protected StringBuffer encodeBody(StringBuffer buffer) {
        if (this.contentCoding != null) {
            buffer.append(this.contentCoding);
        }
        if (this.parameters != null && !this.parameters.isEmpty()) {
            buffer.append(Separators.SEMICOLON).append(this.parameters.encode());
        }
        return buffer;
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
    public void setQValue(float q) throws InvalidArgumentException {
        if (q < 0.0d || q > 1.0d) {
            throw new InvalidArgumentException("qvalue out of range!");
        }
        super.setParameter("q", q);
    }

    @Override
    public void setEncoding(String encoding) throws ParseException {
        if (encoding == null) {
            throw new NullPointerException(" encoding parameter is null");
        }
        this.contentCoding = encoding;
    }
}
