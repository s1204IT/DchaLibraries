package gov.nist.javax.sip.header;

import java.util.ListIterator;

public class ExtensionHeaderList extends SIPHeaderList<ExtensionHeaderImpl> {
    private static final long serialVersionUID = 4681326807149890197L;

    @Override
    public Object clone() {
        ExtensionHeaderList extensionHeaderList = new ExtensionHeaderList(this.headerName);
        extensionHeaderList.clonehlist(this.hlist);
        return extensionHeaderList;
    }

    public ExtensionHeaderList(String str) {
        super(ExtensionHeaderImpl.class, str);
    }

    public ExtensionHeaderList() {
        super(ExtensionHeaderImpl.class, null);
    }

    @Override
    public String encode() {
        StringBuffer stringBuffer = new StringBuffer();
        ListIterator<ExtensionHeaderImpl> listIterator = listIterator();
        while (listIterator.hasNext()) {
            stringBuffer.append(listIterator.next().encode());
        }
        return stringBuffer.toString();
    }
}
