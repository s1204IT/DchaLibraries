package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.InReplyToHeader;

public class InReplyTo extends SIPHeader implements InReplyToHeader {
    private static final long serialVersionUID = 1682602905733508890L;
    protected CallIdentifier callId;

    public InReplyTo() {
        super("In-Reply-To");
    }

    public InReplyTo(CallIdentifier cid) {
        super("In-Reply-To");
        this.callId = cid;
    }

    @Override
    public void setCallId(String callId) throws ParseException {
        try {
            this.callId = new CallIdentifier(callId);
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
        InReplyTo retval = (InReplyTo) super.clone();
        if (this.callId != null) {
            retval.callId = (CallIdentifier) this.callId.clone();
        }
        return retval;
    }
}
