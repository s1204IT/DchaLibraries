package gov.nist.javax.sip.header;

import gov.nist.core.GenericObject;
import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
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

    protected SIPHeaderList(Class<HDR> cls, String str) {
        this();
        this.headerName = str;
        this.myClass = cls;
    }

    @Override
    public boolean add(HDR hdr) {
        this.hlist.add(hdr);
        return true;
    }

    public void addFirst(HDR hdr) {
        this.hlist.add(0, hdr);
    }

    public void add(HDR hdr, boolean z) {
        if (z) {
            addFirst(hdr);
        } else {
            add((SIPHeader) hdr);
        }
    }

    public void concatenate(SIPHeaderList<HDR> sIPHeaderList, boolean z) throws IllegalArgumentException {
        if (!z) {
            addAll(sIPHeaderList);
        } else {
            addAll(0, sIPHeaderList);
        }
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.hlist.isEmpty()) {
            stringBuffer.append(this.headerName);
            stringBuffer.append(':');
            stringBuffer.append(Separators.NEWLINE);
        } else if (this.headerName.equals("WWW-Authenticate") || this.headerName.equals("Proxy-Authenticate") || this.headerName.equals("Authorization") || this.headerName.equals("Proxy-Authorization") || ((prettyEncode && (this.headerName.equals("Via") || this.headerName.equals("Route") || this.headerName.equals("Record-Route"))) || getClass().equals(ExtensionHeaderList.class))) {
            ListIterator<HDR> listIterator = this.hlist.listIterator();
            while (listIterator.hasNext()) {
                listIterator.next().encode(stringBuffer);
            }
        } else {
            stringBuffer.append(this.headerName);
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(Separators.SP);
            encodeBody(stringBuffer);
            stringBuffer.append(Separators.NEWLINE);
        }
        return stringBuffer;
    }

    public List<String> getHeadersAsEncodedStrings() {
        LinkedList linkedList = new LinkedList();
        ListIterator<HDR> listIterator = this.hlist.listIterator();
        while (listIterator.hasNext()) {
            linkedList.add(listIterator.next().toString());
        }
        return linkedList;
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
    public ListIterator<HDR> listIterator(int i) {
        return this.hlist.listIterator(i);
    }

    public void removeFirst() {
        if (this.hlist.size() != 0) {
            this.hlist.remove(0);
        }
    }

    public void removeLast() {
        if (this.hlist.size() != 0) {
            this.hlist.remove(this.hlist.size() - 1);
        }
    }

    public boolean remove(HDR hdr) {
        if (this.hlist.size() == 0) {
            return false;
        }
        return this.hlist.remove(hdr);
    }

    protected void setMyClass(Class<HDR> cls) {
        this.myClass = cls;
    }

    @Override
    public String debugDump(int i) {
        this.stringRepresentation = "";
        String indentation = new Indentation(i).getIndentation();
        sprint(indentation + getClass().getName());
        sprint(indentation + "{");
        Iterator<HDR> it = this.hlist.iterator();
        while (it.hasNext()) {
            sprint(indentation + it.next().debugDump());
        }
        sprint(indentation + "}");
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

    public int indexOf(GenericObject genericObject) {
        return this.hlist.indexOf(genericObject);
    }

    @Override
    public void add(int i, HDR hdr) throws IndexOutOfBoundsException {
        this.hlist.add(i, hdr);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SIPHeaderList)) {
            return false;
        }
        SIPHeaderList sIPHeaderList = (SIPHeaderList) obj;
        if (this.hlist == sIPHeaderList.hlist) {
            return true;
        }
        if (this.hlist == null) {
            return sIPHeaderList.hlist == null || sIPHeaderList.hlist.size() == 0;
        }
        return this.hlist.equals(sIPHeaderList.hlist);
    }

    public boolean match(SIPHeaderList<?> sIPHeaderList) {
        if (sIPHeaderList == null) {
            return true;
        }
        if (!getClass().equals(sIPHeaderList.getClass())) {
            return false;
        }
        if (this.hlist == sIPHeaderList.hlist) {
            return true;
        }
        if (this.hlist == null) {
            return false;
        }
        for (HDR hdr : sIPHeaderList.hlist) {
            Iterator<HDR> it = this.hlist.iterator();
            boolean zMatch = false;
            while (it.hasNext() && !zMatch) {
                zMatch = it.next().match(hdr);
            }
            if (!zMatch) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object clone() {
        try {
            SIPHeaderList sIPHeaderList = (SIPHeaderList) getClass().getConstructor((Class[]) null).newInstance((Object[]) null);
            sIPHeaderList.headerName = this.headerName;
            sIPHeaderList.myClass = this.myClass;
            return sIPHeaderList.clonehlist(this.hlist);
        } catch (Exception e) {
            throw new RuntimeException("Could not clone!", e);
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
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        ListIterator<HDR> listIterator = listIterator();
        while (true) {
            HDR next = listIterator.next();
            if (next == this) {
                throw new RuntimeException("Unexpected circularity in SipHeaderList");
            }
            next.encodeBody(stringBuffer);
            if (listIterator.hasNext()) {
                if (!this.headerName.equals("Privacy")) {
                    stringBuffer.append(Separators.COMMA);
                } else {
                    stringBuffer.append(Separators.SEMICOLON);
                }
            } else {
                return stringBuffer;
            }
        }
    }

    @Override
    public boolean addAll(Collection<? extends HDR> collection) {
        return this.hlist.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends HDR> collection) {
        return this.hlist.addAll(i, collection);
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
    public boolean contains(Object obj) {
        return this.hlist.contains(obj);
    }

    @Override
    public HDR get(int i) {
        return this.hlist.get(i);
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
    public HDR remove(int i) {
        return this.hlist.remove(i);
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
    public List<HDR> subList(int i, int i2) {
        return this.hlist.subList(i, i2);
    }

    @Override
    public int hashCode() {
        return this.headerName.hashCode();
    }

    @Override
    public HDR set(int i, HDR hdr) {
        return this.hlist.set(i, hdr);
    }

    public static void setPrettyEncode(boolean z) {
        prettyEncode = z;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        return (T[]) this.hlist.toArray(tArr);
    }
}
