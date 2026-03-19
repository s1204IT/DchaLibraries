package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.RSeqHeader;

public class RSeq extends SIPHeader implements RSeqHeader {
    private static final long serialVersionUID = 8765762413224043394L;
    protected long sequenceNumber;

    public RSeq() {
        super("RSeq");
    }

    @Override
    public int getSequenceNumber() {
        return (int) this.sequenceNumber;
    }

    @Override
    protected String encodeBody() {
        return Long.toString(this.sequenceNumber);
    }

    @Override
    public long getSeqNumber() {
        return this.sequenceNumber;
    }

    @Override
    public void setSeqNumber(long j) throws InvalidArgumentException {
        if (j <= 0 || j > 2147483648L) {
            throw new InvalidArgumentException("Bad seq number " + j);
        }
        this.sequenceNumber = j;
    }

    @Override
    public void setSequenceNumber(int i) throws InvalidArgumentException {
        setSeqNumber(i);
    }
}
