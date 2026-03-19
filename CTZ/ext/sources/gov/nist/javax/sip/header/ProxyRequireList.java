package gov.nist.javax.sip.header;

public class ProxyRequireList extends SIPHeaderList<ProxyRequire> {
    private static final long serialVersionUID = 5648630649476486042L;

    @Override
    public Object clone() {
        ProxyRequireList proxyRequireList = new ProxyRequireList();
        proxyRequireList.clonehlist(this.hlist);
        return proxyRequireList;
    }

    public ProxyRequireList() {
        super(ProxyRequire.class, "Proxy-Require");
    }
}
