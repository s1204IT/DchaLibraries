package gov.nist.javax.sip.header;

public class ProxyAuthenticateList extends SIPHeaderList<ProxyAuthenticate> {
    private static final long serialVersionUID = 1;

    @Override
    public Object clone() {
        ProxyAuthenticateList proxyAuthenticateList = new ProxyAuthenticateList();
        proxyAuthenticateList.clonehlist(this.hlist);
        return proxyAuthenticateList;
    }

    public ProxyAuthenticateList() {
        super(ProxyAuthenticate.class, "Proxy-Authenticate");
    }
}
