package gov.nist.javax.sip.header;

public final class ReasonList extends SIPHeaderList<Reason> {
    private static final long serialVersionUID = 7459989997463160670L;

    @Override
    public Object clone() {
        ReasonList reasonList = new ReasonList();
        reasonList.clonehlist(this.hlist);
        return reasonList;
    }

    public ReasonList() {
        super(Reason.class, "Reason");
    }
}
