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
    public void setEventType(String eventType) throws ParseException {
        if (eventType == null) {
            throw new NullPointerException(" the eventType is null");
        }
        this.eventType = eventType;
    }

    @Override
    public String getEventType() {
        return this.eventType;
    }

    @Override
    public void setEventId(String eventId) throws ParseException {
        if (eventId == null) {
            throw new NullPointerException(" the eventId parameter is null");
        }
        setParameter(ParameterNames.ID, eventId);
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
    protected StringBuffer encodeBody(StringBuffer buffer) {
        if (this.eventType != null) {
            buffer.append(this.eventType);
        }
        if (!this.parameters.isEmpty()) {
            buffer.append(Separators.SEMICOLON);
            this.parameters.encode(buffer);
        }
        return buffer;
    }

    public boolean match(Event matchTarget) {
        if (matchTarget.eventType == null && this.eventType != null) {
            return false;
        }
        if (matchTarget.eventType != null && this.eventType == null) {
            return false;
        }
        if (this.eventType == null && matchTarget.eventType == null) {
            return false;
        }
        if (getEventId() == null && matchTarget.getEventId() != null) {
            return false;
        }
        if ((getEventId() != null && matchTarget.getEventId() == null) || !matchTarget.eventType.equalsIgnoreCase(this.eventType)) {
            return false;
        }
        if (getEventId() == matchTarget.getEventId()) {
            return true;
        }
        return getEventId().equalsIgnoreCase(matchTarget.getEventId());
    }
}
