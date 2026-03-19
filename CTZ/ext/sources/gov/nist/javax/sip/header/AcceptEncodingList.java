package gov.nist.javax.sip.header;

public class AcceptEncodingList extends SIPHeaderList<AcceptEncoding> {
    @Override
    public Object clone() {
        AcceptEncodingList acceptEncodingList = new AcceptEncodingList();
        acceptEncodingList.clonehlist(this.hlist);
        return acceptEncodingList;
    }

    public AcceptEncodingList() {
        super(AcceptEncoding.class, "Accept-Encoding");
    }
}
