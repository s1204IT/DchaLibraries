package mf.org.apache.xerces.impl.xs.opti;

import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;

public class NamedNodeMapImpl implements NamedNodeMap {
    Attr[] attrs;

    public NamedNodeMapImpl(Attr[] attrs) {
        this.attrs = attrs;
    }

    @Override
    public Node getNamedItem(String name) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(name)) {
                return this.attrs[i];
            }
        }
        return null;
    }

    @Override
    public Node item(int index) {
        if (index < 0 && index > getLength()) {
            return null;
        }
        return this.attrs[index];
    }

    @Override
    public int getLength() {
        return this.attrs.length;
    }

    @Override
    public Node getNamedItemNS(String namespaceURI, String localName) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(localName) && this.attrs[i].getNamespaceURI().equals(namespaceURI)) {
                return this.attrs[i];
            }
        }
        return null;
    }

    @Override
    public Node setNamedItemNS(Node arg) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Node setNamedItem(Node arg) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public Node removeNamedItem(String name) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }
}
