package gov.nist.javax.sip.header;

public final class InReplyToList extends SIPHeaderList<InReplyTo> {
    private static final long serialVersionUID = -7993498496830999237L;

    @Override
    public Object clone() {
        InReplyToList inReplyToList = new InReplyToList();
        inReplyToList.clonehlist(this.hlist);
        return inReplyToList;
    }

    public InReplyToList() {
        super(InReplyTo.class, "In-Reply-To");
    }
}
