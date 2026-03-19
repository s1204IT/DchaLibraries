package gov.nist.javax.sip.header;

public class ErrorInfoList extends SIPHeaderList<ErrorInfo> {
    private static final long serialVersionUID = 1;

    @Override
    public Object clone() {
        ErrorInfoList errorInfoList = new ErrorInfoList();
        errorInfoList.clonehlist(this.hlist);
        return errorInfoList;
    }

    public ErrorInfoList() {
        super(ErrorInfo.class, "Error-Info");
    }
}
