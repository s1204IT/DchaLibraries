package gov.nist.javax.sip.header;

public class SupportedList extends SIPHeaderList<Supported> {
    private static final long serialVersionUID = -4539299544895602367L;

    @Override
    public Object clone() {
        SupportedList supportedList = new SupportedList();
        supportedList.clonehlist(this.hlist);
        return supportedList;
    }

    public SupportedList() {
        super(Supported.class, "Supported");
    }
}
