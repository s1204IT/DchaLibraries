package gov.nist.javax.sip.header;

public class AcceptList extends SIPHeaderList<Accept> {
    private static final long serialVersionUID = -1800813338560484831L;

    @Override
    public Object clone() {
        AcceptList acceptList = new AcceptList();
        acceptList.clonehlist(this.hlist);
        return acceptList;
    }

    public AcceptList() {
        super(Accept.class, "Accept");
    }
}
