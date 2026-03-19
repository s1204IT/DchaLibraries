package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;
import javax.sip.address.URI;
import javax.sip.header.ErrorInfoHeader;

public final class ErrorInfo extends ParametersHeader implements ErrorInfoHeader {
    private static final long serialVersionUID = -6347702901964436362L;
    protected GenericURI errorInfo;

    public ErrorInfo() {
        super("Error-Info");
    }

    public ErrorInfo(GenericURI genericURI) {
        this();
        this.errorInfo = genericURI;
    }

    @Override
    public String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer(Separators.LESS_THAN);
        stringBuffer.append(this.errorInfo.toString());
        stringBuffer.append(Separators.GREATER_THAN);
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            stringBuffer.append(this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setErrorInfo(URI uri) {
        this.errorInfo = (GenericURI) uri;
    }

    @Override
    public URI getErrorInfo() {
        return this.errorInfo;
    }

    @Override
    public void setErrorMessage(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception , ErrorInfoHeader, setErrorMessage(), the message parameter is null");
        }
        setParameter("message", str);
    }

    @Override
    public String getErrorMessage() {
        return getParameter("message");
    }

    @Override
    public Object clone() {
        ErrorInfo errorInfo = (ErrorInfo) super.clone();
        if (this.errorInfo != null) {
            errorInfo.errorInfo = (GenericURI) this.errorInfo.clone();
        }
        return errorInfo;
    }
}
