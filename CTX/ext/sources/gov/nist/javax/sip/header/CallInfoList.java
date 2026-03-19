package gov.nist.javax.sip.header;

public class CallInfoList extends SIPHeaderList<CallInfo> {
    private static final long serialVersionUID = -4949850334388806423L;

    @Override
    public Object clone() {
        CallInfoList callInfoList = new CallInfoList();
        callInfoList.clonehlist(this.hlist);
        return callInfoList;
    }

    public CallInfoList() {
        super(CallInfo.class, "Call-Info");
    }
}
