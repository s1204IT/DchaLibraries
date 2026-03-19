package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.AllowEventsHeader;

public final class AllowEvents extends SIPHeader implements AllowEventsHeader {
    private static final long serialVersionUID = 617962431813193114L;
    protected String eventType;

    public AllowEvents() {
        super("Allow-Events");
    }

    public AllowEvents(String str) {
        super("Allow-Events");
        this.eventType = str;
    }

    @Override
    public void setEventType(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,AllowEvents, setEventType(), the eventType parameter is null");
        }
        this.eventType = str;
    }

    @Override
    public String getEventType() {
        return this.eventType;
    }

    @Override
    protected String encodeBody() {
        return this.eventType;
    }
}
