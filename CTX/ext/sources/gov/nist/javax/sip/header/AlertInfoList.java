package gov.nist.javax.sip.header;

public class AlertInfoList extends SIPHeaderList<AlertInfo> {
    private static final long serialVersionUID = 1;

    @Override
    public Object clone() {
        AlertInfoList alertInfoList = new AlertInfoList();
        alertInfoList.clonehlist(this.hlist);
        return alertInfoList;
    }

    public AlertInfoList() {
        super(AlertInfo.class, "Alert-Info");
    }
}
