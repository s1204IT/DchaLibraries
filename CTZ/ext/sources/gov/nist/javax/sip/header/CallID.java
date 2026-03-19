package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.CallIdHeader;

public class CallID extends SIPHeader implements CallIdHeader {
    private static final long serialVersionUID = -6463630258703731156L;
    protected CallIdentifier callIdentifier;

    public CallID() {
        super("Call-ID");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CallIdHeader) {
            return getCallId().equalsIgnoreCase(((CallIdHeader) obj).getCallId());
        }
        return false;
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.callIdentifier != null) {
            this.callIdentifier.encode(stringBuffer);
        }
        return stringBuffer;
    }

    @Override
    public String getCallId() {
        return encodeBody();
    }

    public CallIdentifier getCallIdentifer() {
        return this.callIdentifier;
    }

    @Override
    public void setCallId(String str) throws ParseException {
        try {
            this.callIdentifier = new CallIdentifier(str);
        } catch (IllegalArgumentException e) {
            throw new ParseException(str, 0);
        }
    }

    public void setCallIdentifier(CallIdentifier callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    public CallID(String str) throws IllegalArgumentException {
        super("Call-ID");
        this.callIdentifier = new CallIdentifier(str);
    }

    @Override
    public Object clone() {
        CallID callID = (CallID) super.clone();
        if (this.callIdentifier != null) {
            callID.callIdentifier = (CallIdentifier) this.callIdentifier.clone();
        }
        return callID;
    }
}
