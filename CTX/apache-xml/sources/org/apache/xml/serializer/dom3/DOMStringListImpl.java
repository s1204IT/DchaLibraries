package org.apache.xml.serializer.dom3;

import java.util.Vector;
import org.w3c.dom.DOMStringList;

final class DOMStringListImpl implements DOMStringList {
    private Vector fStrings;

    DOMStringListImpl() {
        this.fStrings = new Vector();
    }

    DOMStringListImpl(Vector vector) {
        this.fStrings = vector;
    }

    DOMStringListImpl(String[] strArr) {
        this.fStrings = new Vector();
        if (strArr != null) {
            for (String str : strArr) {
                this.fStrings.add(str);
            }
        }
    }

    @Override
    public String item(int i) {
        try {
            return (String) this.fStrings.elementAt(i);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public int getLength() {
        return this.fStrings.size();
    }

    @Override
    public boolean contains(String str) {
        return this.fStrings.contains(str);
    }

    public void add(String str) {
        this.fStrings.add(str);
    }
}
