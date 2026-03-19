package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.message.SIPRequest;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.CSeqHeader;

public class CSeq extends SIPHeader implements CSeqHeader {
    private static final long serialVersionUID = -5405798080040422910L;
    protected String method;
    protected Long seqno;

    public CSeq() {
        super("CSeq");
    }

    public CSeq(long seqno, String method) {
        this();
        this.seqno = Long.valueOf(seqno);
        this.method = SIPRequest.getCannonicalName(method);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CSeqHeader)) {
            return false;
        }
        CSeqHeader o = (CSeqHeader) other;
        if (getSeqNumber() == o.getSeqNumber()) {
            return getMethod().equals(o.getMethod());
        }
        return false;
    }

    @Override
    public String encode() {
        return this.headerName + Separators.COLON + Separators.SP + encodeBody() + Separators.NEWLINE;
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer buffer) {
        return buffer.append(this.seqno).append(Separators.SP).append(this.method.toUpperCase());
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public void setSeqNumber(long sequenceNumber) throws InvalidArgumentException {
        if (sequenceNumber < 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, CSeq, setSequenceNumber(), the sequence number parameter is < 0 : " + sequenceNumber);
        }
        if (sequenceNumber > 2147483648L) {
            throw new InvalidArgumentException("JAIN-SIP Exception, CSeq, setSequenceNumber(), the sequence number parameter is too large : " + sequenceNumber);
        }
        this.seqno = Long.valueOf(sequenceNumber);
    }

    @Override
    public void setSequenceNumber(int sequenceNumber) throws InvalidArgumentException {
        setSeqNumber(sequenceNumber);
    }

    @Override
    public void setMethod(String meth) throws ParseException {
        if (meth == null) {
            throw new NullPointerException("JAIN-SIP Exception, CSeq, setMethod(), the meth parameter is null");
        }
        this.method = SIPRequest.getCannonicalName(meth);
    }

    @Override
    public int getSequenceNumber() {
        if (this.seqno == null) {
            return 0;
        }
        return this.seqno.intValue();
    }

    @Override
    public long getSeqNumber() {
        return this.seqno.longValue();
    }
}
