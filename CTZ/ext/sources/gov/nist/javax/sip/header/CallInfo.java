package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;
import javax.sip.address.URI;
import javax.sip.header.CallInfoHeader;

public final class CallInfo extends ParametersHeader implements CallInfoHeader {
    private static final long serialVersionUID = -8179246487696752928L;
    protected GenericURI info;

    public CallInfo() {
        super("Call-Info");
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        stringBuffer.append(Separators.LESS_THAN);
        this.info.encode(stringBuffer);
        stringBuffer.append(Separators.GREATER_THAN);
        if (this.parameters != null && !this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }

    @Override
    public String getPurpose() {
        return getParameter(ParameterNames.PURPOSE);
    }

    @Override
    public URI getInfo() {
        return this.info;
    }

    @Override
    public void setPurpose(String str) {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        try {
            setParameter(ParameterNames.PURPOSE, str);
        } catch (ParseException e) {
        }
    }

    @Override
    public void setInfo(URI uri) {
        this.info = (GenericURI) uri;
    }

    @Override
    public Object clone() {
        CallInfo callInfo = (CallInfo) super.clone();
        if (this.info != null) {
            callInfo.info = (GenericURI) this.info.clone();
        }
        return callInfo;
    }
}
