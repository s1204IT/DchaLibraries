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
    public void setCallId(String callId) {
        this.callId = callId;
    }

    @Override
    public void setRel(String rel) throws ParseException {
        if (rel == null) {
            return;
        }
        setParameter(ReferencesHeader.REL, rel);
    }

    @Override
    public String getParameter(String name) {
        return super.getParameter(name);
    }

    @Override
    public Iterator getParameterNames() {
        return super.getParameterNames();
    }

    @Override
    public void removeParameter(String name) {
        super.removeParameter(name);
    }

    @Override
    public void setParameter(String name, String value) throws ParseException {
        super.setParameter(name, value);
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
    public void setValue(String value) throws ParseException {
        throw new UnsupportedOperationException("operation not supported");
    }
}
