package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.SubscriptionStateHeader;

public class SubscriptionState extends ParametersHeader implements SubscriptionStateHeader {
    private static final long serialVersionUID = -6673833053927258745L;
    protected int expires;
    protected String reasonCode;
    protected int retryAfter;
    protected String state;

    public SubscriptionState() {
        super("Subscription-State");
        this.expires = -1;
        this.retryAfter = -1;
    }

    @Override
    public void setExpires(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, SubscriptionState, setExpires(), the expires parameter is  < 0");
        }
        this.expires = i;
    }

    @Override
    public int getExpires() {
        return this.expires;
    }

    @Override
    public void setRetryAfter(int i) throws InvalidArgumentException {
        if (i <= 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, SubscriptionState, setRetryAfter(), the retryAfter parameter is <=0");
        }
        this.retryAfter = i;
    }

    @Override
    public int getRetryAfter() {
        return this.retryAfter;
    }

    @Override
    public String getReasonCode() {
        return this.reasonCode;
    }

    @Override
    public void setReasonCode(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, SubscriptionState, setReasonCode(), the reasonCode parameter is null");
        }
        this.reasonCode = str;
    }

    @Override
    public String getState() {
        return this.state;
    }

    @Override
    public void setState(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, SubscriptionState, setState(), the state parameter is null");
        }
        this.state = str;
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.state != null) {
            stringBuffer.append(this.state);
        }
        if (this.reasonCode != null) {
            stringBuffer.append(";reason=");
            stringBuffer.append(this.reasonCode);
        }
        if (this.expires != -1) {
            stringBuffer.append(";expires=");
            stringBuffer.append(this.expires);
        }
        if (this.retryAfter != -1) {
            stringBuffer.append(";retry-after=");
            stringBuffer.append(this.retryAfter);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
    }
}
