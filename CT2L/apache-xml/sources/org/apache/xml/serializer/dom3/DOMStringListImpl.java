package org.apache.xml.serializer.dom3;

import java.util.Vector;
import org.w3c.dom.DOMStringList;

final class DOMStringListImpl implements DOMStringList {
    private Vector fStrings;

    DOMStringListImpl() {
        this.fStrings = new Vector();
    }

    DOMStringListImpl(Vector params) {
        this.fStrings = params;
    }

    DOMStringListImpl(String[] params) {
        this.fStrings = new Vector();
        if (params != null) {
            for (String str : params) {
                this.fStrings.add(str);
            }
        }
    }

    @Override
    public String item(int index) {
        try {
            return (String) this.fStrings.elementAt(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public int getLength() {
        return this.fStrings.size();
    }

    @Override
    public boolean contains(String param) {
        return this.fStrings.contains(param);
    }

    public void add(String param) {
        this.fStrings.add(param);
    }
}
