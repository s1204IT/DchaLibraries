package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.header.EventHeader;

public class Event extends ParametersHeader implements EventHeader {
    private static final long serialVersionUID = -6458387810431874841L;
    protected String eventType;

    public Event() {
        super("Event");
    }

    @Override
    public void setEventType(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException(" the eventType is null");
        }
        this.eventType = str;
    }

    @Override
    public String getEventType() {
        return this.eventType;
    }

    @Override
    public void setEventId(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException(" the eventId parameter is null");
        }
        setParameter(ParameterNames.ID, str);
    }

    @Override
    public String getEventId() {
        return getParameter(ParameterNames.ID);
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.eventType != null) {
            stringBuffer.append(this.eventType);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }

    public boolean match(Event event) {
        if (event.eventType == null && this.eventType != null) {
            return false;
        }
        if (event.eventType != null && this.eventType == null) {
            return false;
        }
        if (this.eventType == null && event.eventType == null) {
            return false;
        }
        if (getEventId() == null && event.getEventId() != null) {
            return false;
        }
        if ((getEventId() == null || event.getEventId() != null) && event.eventType.equalsIgnoreCase(this.eventType)) {
            return getEventId() == event.getEventId() || getEventId().equalsIgnoreCase(event.getEventId());
        }
        return false;
    }
}
