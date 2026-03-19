package gov.nist.javax.sip.header;

import java.util.ListIterator;

public class ExtensionHeaderList extends SIPHeaderList<ExtensionHeaderImpl> {
    private static final long serialVersionUID = 4681326807149890197L;

    @Override
    public Object clone() {
        ExtensionHeaderList retval = new ExtensionHeaderList(this.headerName);
        retval.clonehlist(this.hlist);
        return retval;
    }

    public ExtensionHeaderList(String hName) {
        super(ExtensionHeaderImpl.class, hName);
    }

    public ExtensionHeaderList() {
        super(ExtensionHeaderImpl.class, null);
    }

    @Override
    public String encode() {
        StringBuffer retval = new StringBuffer();
        ListIterator<ExtensionHeaderImpl> it = listIterator();
        while (it.hasNext()) {
            ExtensionHeaderImpl eh = it.next();
            retval.append(eh.encode());
        }
        return retval.toString();
    }
}
