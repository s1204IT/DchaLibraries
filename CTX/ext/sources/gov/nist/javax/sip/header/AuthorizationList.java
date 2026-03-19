package gov.nist.javax.sip.header;

public class AuthorizationList extends SIPHeaderList<Authorization> {
    private static final long serialVersionUID = 1;

    @Override
    public Object clone() {
        AuthorizationList authorizationList = new AuthorizationList();
        authorizationList.clonehlist(this.hlist);
        return authorizationList;
    }

    public AuthorizationList() {
        super(Authorization.class, "Authorization");
    }
}
