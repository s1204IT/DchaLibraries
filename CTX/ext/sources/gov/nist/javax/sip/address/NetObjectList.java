package gov.nist.javax.sip.address;

import gov.nist.core.GenericObject;
import gov.nist.core.GenericObjectList;
import java.util.ListIterator;

public class NetObjectList extends GenericObjectList {
    private static final long serialVersionUID = -1551780600806959023L;

    public NetObjectList(String str) {
        super(str);
    }

    public NetObjectList(String str, Class<?> cls) {
        super(str, cls);
    }

    public NetObjectList() {
    }

    public void add(NetObject netObject) {
        super.add(netObject);
    }

    public void concatenate(NetObjectList netObjectList) {
        super.concatenate((GenericObjectList) netObjectList);
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
    public GenericObject next(ListIterator listIterator) {
        return (NetObject) super.next(listIterator);
    }

    @Override
    public void setMyClass(Class cls) {
        super.setMyClass(cls);
    }

    @Override
    public String debugDump(int i) {
        return super.debugDump(i);
    }

    @Override
    public String toString() {
        return encode();
    }
}
