package mf.org.apache.xerces.impl.xs.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import mf.javax.xml.namespace.QName;
import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.xs.XSNamedMap;
import mf.org.apache.xerces.xs.XSObject;

public class XSNamedMapImpl extends AbstractMap implements XSNamedMap {
    public static final XSNamedMapImpl EMPTY_MAP = new XSNamedMapImpl(new XSObject[0], 0);
    XSObject[] fArray;
    private Set fEntrySet;
    int fLength;
    final SymbolHash[] fMaps;
    final int fNSNum;
    final String[] fNamespaces;

    public XSNamedMapImpl(String namespace, SymbolHash map) {
        this.fArray = null;
        this.fLength = -1;
        this.fEntrySet = null;
        this.fNamespaces = new String[]{namespace};
        this.fMaps = new SymbolHash[]{map};
        this.fNSNum = 1;
    }

    public XSNamedMapImpl(String[] namespaces, SymbolHash[] maps, int num) {
        this.fArray = null;
        this.fLength = -1;
        this.fEntrySet = null;
        this.fNamespaces = namespaces;
        this.fMaps = maps;
        this.fNSNum = num;
    }

    public XSNamedMapImpl(XSObject[] array, int length) {
        this.fArray = null;
        this.fLength = -1;
        this.fEntrySet = null;
        if (length == 0) {
            this.fNamespaces = null;
            this.fMaps = null;
            this.fNSNum = 0;
            this.fArray = array;
            this.fLength = 0;
            return;
        }
        this.fNamespaces = new String[]{array[0].getNamespace()};
        this.fMaps = null;
        this.fNSNum = 1;
        this.fArray = array;
        this.fLength = length;
    }

    public synchronized int getLength() {
        int i;
        if (this.fLength == -1) {
            this.fLength = 0;
            for (int i2 = 0; i2 < this.fNSNum; i2++) {
                this.fLength += this.fMaps[i2].getLength();
            }
        }
        i = this.fLength;
        return i;
    }

    public XSObject itemByName(String namespace, String localName) {
        for (int i = 0; i < this.fNSNum; i++) {
            if (isEqual(namespace, this.fNamespaces[i])) {
                if (this.fMaps != null) {
                    return (XSObject) this.fMaps[i].get(localName);
                }
                for (int j = 0; j < this.fLength; j++) {
                    XSObject ret = this.fArray[j];
                    if (ret.getName().equals(localName)) {
                        return ret;
                    }
                }
                return null;
            }
        }
        return null;
    }

    public synchronized XSObject item(int index) {
        if (this.fArray == null) {
            getLength();
            this.fArray = new XSObject[this.fLength];
            int pos = 0;
            for (int i = 0; i < this.fNSNum; i++) {
                pos += this.fMaps[i].getValues(this.fArray, pos);
            }
        }
        if (index >= 0 && index < this.fLength) {
            return this.fArray[index];
        }
        return null;
    }

    static boolean isEqual(String one, String two) {
        return one != null ? one.equals(two) : two == null;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Object get(Object obj) {
        if (obj instanceof QName) {
            String namespaceURI = obj.getNamespaceURI();
            if ("".equals(namespaceURI)) {
                namespaceURI = null;
            }
            String localPart = obj.getLocalPart();
            return itemByName(namespaceURI, localPart);
        }
        return null;
    }

    @Override
    public int size() {
        return getLength();
    }

    @Override
    public synchronized Set entrySet() {
        if (this.fEntrySet == null) {
            final int length = getLength();
            final XSNamedMapEntry[] entries = new XSNamedMapEntry[length];
            for (int i = 0; i < length; i++) {
                XSObject xso = item(i);
                entries[i] = new XSNamedMapEntry(new QName(xso.getNamespace(), xso.getName()), xso);
            }
            this.fEntrySet = new AbstractSet() {
                @Override
                public Iterator iterator() {
                    final int i2 = length;
                    final XSNamedMapEntry[] xSNamedMapEntryArr = entries;
                    return new Iterator() {
                        private int index = 0;

                        @Override
                        public boolean hasNext() {
                            return this.index < i2;
                        }

                        @Override
                        public Object next() {
                            if (this.index < i2) {
                                XSNamedMapEntry[] xSNamedMapEntryArr2 = xSNamedMapEntryArr;
                                int i3 = this.index;
                                this.index = i3 + 1;
                                return xSNamedMapEntryArr2[i3];
                            }
                            throw new NoSuchElementException();
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public int size() {
                    return length;
                }
            };
        }
        return this.fEntrySet;
    }

    private static final class XSNamedMapEntry implements Map.Entry {
        private final QName key;
        private final XSObject value;

        public XSNamedMapEntry(QName key, XSObject value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey() {
            return this.key;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            Object otherKey = e.getKey();
            Object otherValue = e.getValue();
            if (this.key == null) {
                if (otherKey != null) {
                    return false;
                }
            } else if (!this.key.equals(otherKey)) {
                return false;
            }
            if (this.value == null) {
                if (otherValue != null) {
                    return false;
                }
            } else if (!this.value.equals(otherValue)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int iHashCode;
            if (this.key != null) {
                iHashCode = this.key.hashCode();
            } else {
                iHashCode = 0;
            }
            return iHashCode ^ (this.value != null ? this.value.hashCode() : 0);
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(String.valueOf(this.key));
            buffer.append('=');
            buffer.append(String.valueOf(this.value));
            return buffer.toString();
        }
    }
}
