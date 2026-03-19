package gov.nist.javax.sip.header;

public class AuthenticationInfoList extends SIPHeaderList<AuthenticationInfo> {
    private static final long serialVersionUID = 1;

    @Override
    public Object clone() {
        AuthenticationInfoList authenticationInfoList = new AuthenticationInfoList();
        authenticationInfoList.clonehlist(this.hlist);
        return authenticationInfoList;
    }

    public AuthenticationInfoList() {
        super(AuthenticationInfo.class, "Authentication-Info");
    }
}
