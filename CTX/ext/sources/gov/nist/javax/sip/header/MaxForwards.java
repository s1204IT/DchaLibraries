package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.TooManyHopsException;

public class MaxForwards extends SIPHeader implements MaxForwardsHeader {
    private static final long serialVersionUID = -3096874323347175943L;
    protected int maxForwards;

    public MaxForwards() {
        super("Max-Forwards");
    }

    public MaxForwards(int i) throws InvalidArgumentException {
        super("Max-Forwards");
        setMaxForwards(i);
    }

    @Override
    public int getMaxForwards() {
        return this.maxForwards;
    }

    @Override
    public void setMaxForwards(int i) throws InvalidArgumentException {
        if (i < 0 || i > 255) {
            throw new InvalidArgumentException("bad max forwards value " + i);
        }
        this.maxForwards = i;
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        stringBuffer.append(this.maxForwards);
        return stringBuffer;
    }

    @Override
    public boolean hasReachedZero() {
        return this.maxForwards == 0;
    }

    @Override
    public void decrementMaxForwards() throws TooManyHopsException {
        if (this.maxForwards > 0) {
            this.maxForwards--;
            return;
        }
        throw new TooManyHopsException("has already reached 0!");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof MaxForwardsHeader) && getMaxForwards() == ((MaxForwardsHeader) obj).getMaxForwards();
    }
}
