package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import java.util.Iterator;
import javax.sip.header.ExtensionHeader;

public class References extends ParametersHeader implements ReferencesHeader, ExtensionHeader {
    private static final long serialVersionUID = 8536961681006637622L;
    private String callId;

    public References() {
        super(ReferencesHeader.NAME);
    }

    @Override
    public String getCallId() {
        return this.callId;
    }

    @Override
    public String getRel() {
        return getParameter(ReferencesHeader.REL);
    }

    @Override
    public void setCallId(String str) {
        this.callId = str;
    }

    @Override
    public void setRel(String str) throws ParseException {
        if (str != null) {
            setParameter(ReferencesHeader.REL, str);
        }
    }

    @Override
    public String getParameter(String str) {
        return super.getParameter(str);
    }

    @Override
    public Iterator getParameterNames() {
        return super.getParameterNames();
    }

    @Override
    public void removeParameter(String str) {
        super.removeParameter(str);
    }

    @Override
    public void setParameter(String str, String str2) throws ParseException {
        super.setParameter(str, str2);
    }

    @Override
    public String getName() {
        return ReferencesHeader.NAME;
    }

    @Override
    protected String encodeBody() {
        if (this.parameters.isEmpty()) {
            return this.callId;
        }
        return this.callId + Separators.SEMICOLON + this.parameters.encode();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new UnsupportedOperationException("operation not supported");
    }
}
