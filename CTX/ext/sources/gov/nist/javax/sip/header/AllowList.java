package gov.nist.javax.sip.header;

import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class AllowList extends SIPHeaderList<Allow> {
    private static final long serialVersionUID = -4699795429662562358L;

    @Override
    public Object clone() {
        AllowList allowList = new AllowList();
        allowList.clonehlist(this.hlist);
        return allowList;
    }

    public AllowList() {
        super(Allow.class, "Allow");
    }

    public ListIterator<String> getMethods() {
        LinkedList linkedList = new LinkedList();
        Iterator it = this.hlist.iterator();
        while (it.hasNext()) {
            linkedList.add(((Allow) it.next()).getMethod());
        }
        return linkedList.listIterator();
    }

    public void setMethods(List<String> list) throws ParseException {
        ListIterator<String> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            Allow allow = new Allow();
            allow.setMethod(listIterator.next());
            add(allow);
        }
    }
}
