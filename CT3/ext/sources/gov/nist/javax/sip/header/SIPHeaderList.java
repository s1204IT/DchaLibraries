package gov.nist.javax.sip.header;

import gov.nist.core.GenericObject;
import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.sip.header.Header;

public abstract class SIPHeaderList<HDR extends SIPHeader> extends SIPHeader implements List<HDR>, Header {
    private static boolean prettyEncode = false;
    protected List<HDR> hlist;
    private Class<HDR> myClass;

    @Override
    public String getName() {
        return this.headerName;
    }

    private SIPHeaderList() {
        this.hlist = new LinkedList();
    }

    protected SIPHeaderList(Class<HDR> objclass, String hname) {
        this();
        this.headerName = hname;
        this.myClass = objclass;
    }

    @Override
    public boolean add(HDR objectToAdd) {
        this.hlist.add(objectToAdd);
        return true;
    }

    public void addFirst(HDR obj) {
        this.hlist.add(0, obj);
    }

    public void add(HDR sipheader, boolean top) {
        if (top) {
            addFirst(sipheader);
        } else {
            add((SIPHeader) sipheader);
        }
    }

    public void concatenate(SIPHeaderList<HDR> other, boolean topFlag) throws IllegalArgumentException {
        if (!topFlag) {
            addAll(other);
        } else {
            addAll(0, other);
        }
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        if (this.hlist.isEmpty()) {
            buffer.append(this.headerName).append(':').append(Separators.NEWLINE);
        } else if (this.headerName.equals("WWW-Authenticate") || this.headerName.equals("Proxy-Authenticate") || this.headerName.equals("Authorization") || this.headerName.equals("Proxy-Authorization") || ((prettyEncode && (this.headerName.equals("Via") || this.headerName.equals("Route") || this.headerName.equals("Record-Route"))) || getClass().equals(ExtensionHeaderList.class))) {
            ListIterator<HDR> li = this.hlist.listIterator();
            while (li.hasNext()) {
                HDR sipheader = li.next();
                sipheader.encode(buffer);
            }
        } else {
            buffer.append(this.headerName).append(Separators.COLON).append(Separators.SP);
            encodeBody(buffer);
            buffer.append(Separators.NEWLINE);
        }
        return buffer;
    }

    public List<String> getHeadersAsEncodedStrings() {
        List<String> retval = new LinkedList<>();
        ListIterator<HDR> li = this.hlist.listIterator();
        while (li.hasNext()) {
            Header sipheader = li.next();
            retval.add(sipheader.toString());
        }
        return retval;
    }

    public Header getFirst() {
        if (this.hlist == null || this.hlist.isEmpty()) {
            return null;
        }
        return this.hlist.get(0);
    }

    public Header getLast() {
        if (this.hlist == null || this.hlist.isEmpty()) {
            return null;
        }
        return this.hlist.get(this.hlist.size() - 1);
    }

    public Class<HDR> getMyClass() {
        return this.myClass;
    }

    @Override
    public boolean isEmpty() {
        return this.hlist.isEmpty();
    }

    @Override
    public ListIterator<HDR> listIterator() {
        return this.hlist.listIterator(0);
    }

    public List<HDR> getHeaderList() {
        return this.hlist;
    }

    @Override
    public ListIterator<HDR> listIterator(int position) {
        return this.hlist.listIterator(position);
    }

    public void removeFirst() {
        if (this.hlist.size() == 0) {
            return;
        }
        this.hlist.remove(0);
    }

    public void removeLast() {
        if (this.hlist.size() == 0) {
            return;
        }
        this.hlist.remove(this.hlist.size() - 1);
    }

    public boolean remove(HDR obj) {
        if (this.hlist.size() == 0) {
            return false;
        }
        return this.hlist.remove(obj);
    }

    protected void setMyClass(Class<HDR> cl) {
        this.myClass = cl;
    }

    @Override
    public String debugDump(int indentation) {
        this.stringRepresentation = "";
        String indent = new Indentation(indentation).getIndentation();
        String className = getClass().getName();
        sprint(indent + className);
        sprint(indent + "{");
        for (HDR sipHeader : this.hlist) {
            sprint(indent + sipHeader.debugDump());
        }
        sprint(indent + "}");
        return this.stringRepresentation;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public Object[] toArray() {
        return this.hlist.toArray();
    }

    public int indexOf(GenericObject gobj) {
        return this.hlist.indexOf(gobj);
    }

    @Override
    public void add(int index, HDR sipHeader) throws IndexOutOfBoundsException {
        this.hlist.add(index, sipHeader);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SIPHeaderList)) {
            return false;
        }
        if (this.hlist == obj.hlist) {
            return true;
        }
        if (this.hlist == null) {
            return obj.hlist == null || obj.hlist.size() == 0;
        }
        return this.hlist.equals(obj.hlist);
    }

    public boolean match(SIPHeaderList<?> template) {
        if (template == null) {
            return true;
        }
        if (!getClass().equals(template.getClass())) {
            return false;
        }
        if (this.hlist == template.hlist) {
            return true;
        }
        if (this.hlist == null) {
            return false;
        }
        for (HDR sipHeader : template.hlist) {
            boolean found = false;
            Iterator<HDR> it1 = this.hlist.iterator();
            while (it1.hasNext() && !found) {
                SIPHeader sipHeader1 = it1.next();
                found = sipHeader1.match(sipHeader);
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object clone() {
        try {
            Class<?> clazz = getClass();
            Constructor<?> cons = clazz.getConstructor(null);
            SIPHeaderList<HDR> retval = (SIPHeaderList) cons.newInstance(null);
            retval.headerName = this.headerName;
            retval.myClass = this.myClass;
            return retval.clonehlist(this.hlist);
        } catch (Exception ex) {
            throw new RuntimeException("Could not clone!", ex);
        }
    }

    protected final SIPHeaderList<HDR> clonehlist(List<HDR> list) {
        if (list != null) {
            Iterator<HDR> it = list.iterator();
            while (it.hasNext()) {
                this.hlist.add((HDR) ((SIPHeader) it.next().clone()));
            }
        }
        return this;
    }

    @Override
    public int size() {
        return this.hlist.size();
    }

    @Override
    public boolean isHeaderList() {
        return true;
    }

    @Override
    protected String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer buffer) {
        ListIterator<HDR> iterator = listIterator();
        while (true) {
            SIPHeader sipHeader = iterator.next();
            if (sipHeader == this) {
                throw new RuntimeException("Unexpected circularity in SipHeaderList");
            }
            sipHeader.encodeBody(buffer);
            if (iterator.hasNext()) {
                if (!this.headerName.equals("Privacy")) {
                    buffer.append(Separators.COMMA);
                } else {
                    buffer.append(Separators.SEMICOLON);
                }
            } else {
                return buffer;
            }
        }
    }

    @Override
    public boolean addAll(Collection<? extends HDR> collection) {
        return this.hlist.addAll(collection);
    }

    @Override
    public boolean addAll(int index, Collection<? extends HDR> collection) {
        return this.hlist.addAll(index, collection);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return this.hlist.containsAll(collection);
    }

    @Override
    public void clear() {
        this.hlist.clear();
    }

    @Override
    public boolean contains(Object header) {
        return this.hlist.contains(header);
    }

    @Override
    public HDR get(int index) {
        return this.hlist.get(index);
    }

    @Override
    public int indexOf(Object obj) {
        return this.hlist.indexOf(obj);
    }

    @Override
    public Iterator<HDR> iterator() {
        return this.hlist.listIterator();
    }

    @Override
    public int lastIndexOf(Object obj) {
        return this.hlist.lastIndexOf(obj);
    }

    @Override
    public boolean remove(Object obj) {
        return this.hlist.remove(obj);
    }

    @Override
    public HDR remove(int index) {
        return this.hlist.remove(index);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return this.hlist.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return this.hlist.retainAll(collection);
    }

    @Override
    public List<HDR> subList(int index1, int index2) {
        return this.hlist.subList(index1, index2);
    }

    @Override
    public int hashCode() {
        return this.headerName.hashCode();
    }

    @Override
    public HDR set(int position, HDR sipHeader) {
        return this.hlist.set(position, sipHeader);
    }

    public static void setPrettyEncode(boolean flag) {
        prettyEncode = flag;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        return (T[]) this.hlist.toArray(tArr);
    }
}
