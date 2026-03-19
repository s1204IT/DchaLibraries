package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.InReplyToHeader;

public class InReplyTo extends SIPHeader implements InReplyToHeader {
    private static final long serialVersionUID = 1682602905733508890L;
    protected CallIdentifier callId;

    public InReplyTo() {
        super("In-Reply-To");
    }

    public InReplyTo(CallIdentifier callIdentifier) {
        super("In-Reply-To");
        this.callId = callIdentifier;
    }

    @Override
    public void setCallId(String str) throws ParseException {
        try {
            this.callId = new CallIdentifier(str);
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public String getCallId() {
        if (this.callId == null) {
            return null;
        }
        return this.callId.encode();
    }

    @Override
    public String encodeBody() {
        return this.callId.encode();
    }

    @Override
    public Object clone() {
        InReplyTo inReplyTo = (InReplyTo) super.clone();
        if (this.callId != null) {
            inReplyTo.callId = (CallIdentifier) this.callId.clone();
        }
        return inReplyTo;
    }
}
