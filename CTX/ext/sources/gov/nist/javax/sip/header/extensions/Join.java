package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.CallIdentifier;
import gov.nist.javax.sip.header.ParameterNames;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class Join extends ParametersHeader implements ExtensionHeader, JoinHeader {
    public static final String NAME = "Join";
    private static final long serialVersionUID = -840116548918120056L;
    public String callId;
    public CallIdentifier callIdentifier;

    public Join() {
        super("Join");
    }

    public Join(String str) throws IllegalArgumentException {
        super("Join");
        this.callIdentifier = new CallIdentifier(str);
    }

    @Override
    public String encodeBody() {
        if (this.callId == null) {
            return null;
        }
        String str = this.callId;
        if (!this.parameters.isEmpty()) {
            return str + Separators.SEMICOLON + this.parameters.encode();
        }
        return str;
    }

    @Override
    public String getCallId() {
        return this.callId;
    }

    public CallIdentifier getCallIdentifer() {
        return this.callIdentifier;
    }

    @Override
    public void setCallId(String str) {
        this.callId = str;
    }

    public void setCallIdentifier(CallIdentifier callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    @Override
    public String getToTag() {
        if (this.parameters == null) {
            return null;
        }
        return getParameter(ParameterNames.TO_TAG);
    }

    @Override
    public void setToTag(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null tag ");
        }
        if (str.trim().equals("")) {
            throw new ParseException("bad tag", 0);
        }
        setParameter(ParameterNames.TO_TAG, str);
    }

    public boolean hasToTag() {
        return hasParameter(ParameterNames.TO_TAG);
    }

    public void removeToTag() {
        this.parameters.delete(ParameterNames.TO_TAG);
    }

    @Override
    public String getFromTag() {
        if (this.parameters == null) {
            return null;
        }
        return getParameter(ParameterNames.FROM_TAG);
    }

    @Override
    public void setFromTag(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null tag ");
        }
        if (str.trim().equals("")) {
            throw new ParseException("bad tag", 0);
        }
        setParameter(ParameterNames.FROM_TAG, str);
    }

    public boolean hasFromTag() {
        return hasParameter(ParameterNames.FROM_TAG);
    }

    public void removeFromTag() {
        this.parameters.delete(ParameterNames.FROM_TAG);
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}
