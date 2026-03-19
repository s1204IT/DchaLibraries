package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.RAckHeader;

public class RAck extends SIPHeader implements RAckHeader {
    private static final long serialVersionUID = 743999286077404118L;
    protected long cSeqNumber;
    protected String method;
    protected long rSeqNumber;

    public RAck() {
        super("RAck");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.rSeqNumber);
        stringBuffer.append(Separators.SP);
        stringBuffer.append(this.cSeqNumber);
        stringBuffer.append(Separators.SP);
        stringBuffer.append(this.method);
        return stringBuffer.toString();
    }

    @Override
    public int getCSeqNumber() {
        return (int) this.cSeqNumber;
    }

    public long getCSeqNumberLong() {
        return this.cSeqNumber;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public int getRSeqNumber() {
        return (int) this.rSeqNumber;
    }

    @Override
    public void setCSeqNumber(int i) throws InvalidArgumentException {
        setCSequenceNumber(i);
    }

    @Override
    public void setMethod(String str) throws ParseException {
        this.method = str;
    }

    @Override
    public long getCSequenceNumber() {
        return this.cSeqNumber;
    }

    @Override
    public long getRSequenceNumber() {
        return this.rSeqNumber;
    }

    @Override
    public void setCSequenceNumber(long j) throws InvalidArgumentException {
        if (j <= 0 || j > 2147483648L) {
            throw new InvalidArgumentException("Bad CSeq # " + j);
        }
        this.cSeqNumber = j;
    }

    @Override
    public void setRSeqNumber(int i) throws InvalidArgumentException {
        setRSequenceNumber(i);
    }

    @Override
    public void setRSequenceNumber(long j) throws InvalidArgumentException {
        if (j <= 0 || this.cSeqNumber > 2147483648L) {
            throw new InvalidArgumentException("Bad rSeq # " + j);
        }
        this.rSeqNumber = j;
    }
}
