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

    public CSeq(long j, String str) {
        this();
        this.seqno = Long.valueOf(j);
        this.method = SIPRequest.getCannonicalName(str);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CSeqHeader)) {
            return false;
        }
        CSeqHeader cSeqHeader = (CSeqHeader) obj;
        return getSeqNumber() == cSeqHeader.getSeqNumber() && getMethod().equals(cSeqHeader.getMethod());
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
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        stringBuffer.append(this.seqno);
        stringBuffer.append(Separators.SP);
        stringBuffer.append(this.method.toUpperCase());
        return stringBuffer;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public void setSeqNumber(long j) throws InvalidArgumentException {
        if (j < 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, CSeq, setSequenceNumber(), the sequence number parameter is < 0 : " + j);
        }
        if (j > 2147483648L) {
            throw new InvalidArgumentException("JAIN-SIP Exception, CSeq, setSequenceNumber(), the sequence number parameter is too large : " + j);
        }
        this.seqno = Long.valueOf(j);
    }

    @Override
    public void setSequenceNumber(int i) throws InvalidArgumentException {
        setSeqNumber(i);
    }

    @Override
    public void setMethod(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, CSeq, setMethod(), the meth parameter is null");
        }
        this.method = SIPRequest.getCannonicalName(str);
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
