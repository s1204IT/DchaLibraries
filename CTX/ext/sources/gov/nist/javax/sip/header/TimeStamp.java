package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.InvalidArgumentException;
import javax.sip.header.TimeStampHeader;

public class TimeStamp extends SIPHeader implements TimeStampHeader {
    private static final long serialVersionUID = -3711322366481232720L;
    protected int delay;
    protected float delayFloat;
    protected long timeStamp;
    private float timeStampFloat;

    public TimeStamp() {
        super("Timestamp");
        this.timeStamp = -1L;
        this.delay = -1;
        this.delayFloat = -1.0f;
        this.timeStampFloat = -1.0f;
        this.delay = -1;
    }

    private String getTimeStampAsString() {
        if (this.timeStamp == -1 && this.timeStampFloat == -1.0f) {
            return "";
        }
        if (this.timeStamp != -1) {
            return Long.toString(this.timeStamp);
        }
        return Float.toString(this.timeStampFloat);
    }

    private String getDelayAsString() {
        if (this.delay == -1 && this.delayFloat == -1.0f) {
            return "";
        }
        if (this.delay != -1) {
            return Integer.toString(this.delay);
        }
        return Float.toString(this.delayFloat);
    }

    @Override
    public String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        String timeStampAsString = getTimeStampAsString();
        String delayAsString = getDelayAsString();
        if (timeStampAsString.equals("") && delayAsString.equals("")) {
            return "";
        }
        if (!timeStampAsString.equals("")) {
            stringBuffer.append(timeStampAsString);
        }
        if (!delayAsString.equals("")) {
            stringBuffer.append(Separators.SP);
            stringBuffer.append(delayAsString);
        }
        return stringBuffer.toString();
    }

    @Override
    public boolean hasDelay() {
        return this.delay != -1;
    }

    @Override
    public void removeDelay() {
        this.delay = -1;
    }

    @Override
    public void setTimeStamp(float f) throws InvalidArgumentException {
        if (f < 0.0f) {
            throw new InvalidArgumentException("JAIN-SIP Exception, TimeStamp, setTimeStamp(), the timeStamp parameter is <0");
        }
        this.timeStamp = -1L;
        this.timeStampFloat = f;
    }

    @Override
    public float getTimeStamp() {
        return this.timeStampFloat == -1.0f ? Float.valueOf(this.timeStamp).floatValue() : this.timeStampFloat;
    }

    @Override
    public float getDelay() {
        return this.delayFloat == -1.0f ? Float.valueOf(this.delay).floatValue() : this.delayFloat;
    }

    @Override
    public void setDelay(float f) throws InvalidArgumentException {
        if (f < 0.0f && f != -1.0f) {
            throw new InvalidArgumentException("JAIN-SIP Exception, TimeStamp, setDelay(), the delay parameter is <0");
        }
        this.delayFloat = f;
        this.delay = -1;
    }

    @Override
    public long getTime() {
        return this.timeStamp == -1 ? (long) this.timeStampFloat : this.timeStamp;
    }

    @Override
    public int getTimeDelay() {
        return this.delay == -1 ? (int) this.delayFloat : this.delay;
    }

    @Override
    public void setTime(long j) throws InvalidArgumentException {
        if (j < -1) {
            throw new InvalidArgumentException("Illegal timestamp");
        }
        this.timeStamp = j;
        this.timeStampFloat = -1.0f;
    }

    @Override
    public void setTimeDelay(int i) throws InvalidArgumentException {
        if (i < -1) {
            throw new InvalidArgumentException("Value out of range " + i);
        }
        this.delay = i;
        this.delayFloat = -1.0f;
    }
}
