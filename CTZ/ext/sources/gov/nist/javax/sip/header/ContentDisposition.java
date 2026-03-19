package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.header.ContentDispositionHeader;

public final class ContentDisposition extends ParametersHeader implements ContentDispositionHeader {
    private static final long serialVersionUID = 835596496276127003L;
    protected String dispositionType;

    public ContentDisposition() {
        super("Content-Disposition");
    }

    @Override
    public String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer(this.dispositionType);
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setDispositionType(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, ContentDisposition, setDispositionType(), the dispositionType parameter is null");
        }
        this.dispositionType = str;
    }

    @Override
    public String getDispositionType() {
        return this.dispositionType;
    }

    @Override
    public String getHandling() {
        return getParameter(ParameterNames.HANDLING);
    }

    @Override
    public void setHandling(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, ContentDisposition, setHandling(), the handling parameter is null");
        }
        setParameter(ParameterNames.HANDLING, str);
    }

    public String getContentDisposition() {
        return encodeBody();
    }
}
