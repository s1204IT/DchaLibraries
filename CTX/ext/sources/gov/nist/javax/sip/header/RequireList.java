package gov.nist.javax.sip.header;

public final class RequireList extends SIPHeaderList<Require> {
    private static final long serialVersionUID = -1760629092046963213L;

    @Override
    public Object clone() {
        RequireList requireList = new RequireList();
        requireList.clonehlist(this.hlist);
        return requireList;
    }

    public RequireList() {
        super(Require.class, "Require");
    }
}
