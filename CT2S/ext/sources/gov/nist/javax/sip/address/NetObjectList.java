package gov.nist.javax.sip.address;

import gov.nist.core.GenericObject;
import gov.nist.core.GenericObjectList;
import java.util.ListIterator;

public class NetObjectList extends GenericObjectList {
    private static final long serialVersionUID = -1551780600806959023L;

    public NetObjectList(String lname) {
        super(lname);
    }

    public NetObjectList(String lname, Class<?> cname) {
        super(lname, cname);
    }

    public NetObjectList() {
    }

    public void add(NetObject obj) {
        super.add(obj);
    }

    public void concatenate(NetObjectList net_obj_list) {
        super.concatenate((GenericObjectList) net_obj_list);
    }

    @Override
    public GenericObject first() {
        return (NetObject) super.first();
    }

    @Override
    public GenericObject next() {
        return (NetObject) super.next();
    }

    @Override
    public GenericObject next(ListIterator li) {
        return (NetObject) super.next(li);
    }

    @Override
    public void setMyClass(Class cl) {
        super.setMyClass(cl);
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent);
    }

    @Override
    public String toString() {
        return encode();
    }
}
