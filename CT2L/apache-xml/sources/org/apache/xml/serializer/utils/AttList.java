package org.apache.xml.serializer.utils;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

public final class AttList implements Attributes {
    NamedNodeMap m_attrs;
    DOM2Helper m_dh;
    int m_lastIndex;

    public AttList(NamedNodeMap attrs, DOM2Helper dh) {
        this.m_attrs = attrs;
        this.m_lastIndex = this.m_attrs.getLength() - 1;
        this.m_dh = dh;
    }

    @Override
    public int getLength() {
        return this.m_attrs.getLength();
    }

    @Override
    public String getURI(int index) {
        String ns = this.m_dh.getNamespaceOfNode((Attr) this.m_attrs.item(index));
        if (ns == null) {
            return "";
        }
        return ns;
    }

    @Override
    public String getLocalName(int index) {
        return this.m_dh.getLocalNameOfNode((Attr) this.m_attrs.item(index));
    }

    @Override
    public String getQName(int i) {
        return ((Attr) this.m_attrs.item(i)).getName();
    }

    @Override
    public String getType(int i) {
        return "CDATA";
    }

    @Override
    public String getValue(int i) {
        return ((Attr) this.m_attrs.item(i)).getValue();
    }

    @Override
    public String getType(String name) {
        return "CDATA";
    }

    @Override
    public String getType(String uri, String localName) {
        return "CDATA";
    }

    @Override
    public String getValue(String name) {
        Attr attr = (Attr) this.m_attrs.getNamedItem(name);
        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    @Override
    public String getValue(String uri, String localName) {
        Node a = this.m_attrs.getNamedItemNS(uri, localName);
        if (a == null) {
            return null;
        }
        return a.getNodeValue();
    }

    @Override
    public int getIndex(String uri, String localPart) {
        for (int i = this.m_attrs.getLength() - 1; i >= 0; i--) {
            Node a = this.m_attrs.item(i);
            String u = a.getNamespaceURI();
            if (u == null) {
                if (uri != null) {
                    continue;
                } else if (a.getLocalName().equals(localPart)) {
                    return i;
                }
            } else if (!u.equals(uri)) {
                continue;
            }
        }
        return -1;
    }

    @Override
    public int getIndex(String qName) {
        for (int i = this.m_attrs.getLength() - 1; i >= 0; i--) {
            Node a = this.m_attrs.item(i);
            if (a.getNodeName().equals(qName)) {
                return i;
            }
        }
        return -1;
    }
}
