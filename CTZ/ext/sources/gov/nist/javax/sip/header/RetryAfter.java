package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.RetryAfterHeader;

public class RetryAfter extends ParametersHeader implements RetryAfterHeader {
    public static final String DURATION = "duration";
    private static final long serialVersionUID = -1029458515616146140L;
    protected String comment;
    protected Integer retryAfter;

    public RetryAfter() {
        super("Retry-After");
        this.retryAfter = new Integer(0);
    }

    @Override
    public String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.retryAfter != null) {
            stringBuffer.append(this.retryAfter);
        }
        if (this.comment != null) {
            stringBuffer.append(" (" + this.comment + Separators.RPAREN);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON + this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public boolean hasComment() {
        return this.comment != null;
    }

    @Override
    public void removeComment() {
        this.comment = null;
    }

    @Override
    public void removeDuration() {
        super.removeParameter("duration");
    }

    @Override
    public void setRetryAfter(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("invalid parameter " + i);
        }
        this.retryAfter = Integer.valueOf(i);
    }

    @Override
    public int getRetryAfter() {
        return this.retryAfter.intValue();
    }

    @Override
    public String getComment() {
        return this.comment;
    }

    @Override
    public void setComment(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("the comment parameter is null");
        }
        this.comment = str;
    }

    @Override
    public void setDuration(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("the duration parameter is <0");
        }
        setParameter("duration", i);
    }

    @Override
    public int getDuration() {
        if (getParameter("duration") == null) {
            return -1;
        }
        return super.getParameterAsInt("duration");
    }
}
