package gov.nist.javax.sip.header;

public class ProxyAuthorizationList extends SIPHeaderList<ProxyAuthorization> {
    private static final long serialVersionUID = -1;

    @Override
    public Object clone() {
        ProxyAuthorizationList proxyAuthorizationList = new ProxyAuthorizationList();
        proxyAuthorizationList.clonehlist(this.hlist);
        return proxyAuthorizationList;
    }

    public ProxyAuthorizationList() {
        super(ProxyAuthorization.class, "Proxy-Authorization");
    }
}
