package org.apache.xml.utils;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

public class AttList implements Attributes {
    NamedNodeMap m_attrs;
    DOMHelper m_dh;
    int m_lastIndex;

    public AttList(NamedNodeMap namedNodeMap, DOMHelper dOMHelper) {
        this.m_attrs = namedNodeMap;
        this.m_lastIndex = this.m_attrs.getLength() - 1;
        this.m_dh = dOMHelper;
    }

    @Override
    public int getLength() {
        return this.m_attrs.getLength();
    }

    @Override
    public String getURI(int i) {
        String namespaceOfNode = this.m_dh.getNamespaceOfNode((Attr) this.m_attrs.item(i));
        if (namespaceOfNode == null) {
            return "";
        }
        return namespaceOfNode;
    }

    @Override
    public String getLocalName(int i) {
        return this.m_dh.getLocalNameOfNode((Attr) this.m_attrs.item(i));
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
    public String getType(String str) {
        return "CDATA";
    }

    @Override
    public String getType(String str, String str2) {
        return "CDATA";
    }

    @Override
    public String getValue(String str) {
        Attr attr = (Attr) this.m_attrs.getNamedItem(str);
        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    @Override
    public String getValue(String str, String str2) {
        Node namedItemNS = this.m_attrs.getNamedItemNS(str, str2);
        if (namedItemNS == null) {
            return null;
        }
        return namedItemNS.getNodeValue();
    }

    @Override
    public int getIndex(String str, String str2) {
        for (int length = this.m_attrs.getLength() - 1; length >= 0; length--) {
            Node nodeItem = this.m_attrs.item(length);
            String namespaceURI = nodeItem.getNamespaceURI();
            if (namespaceURI == null) {
                if (str != null) {
                    continue;
                } else if (nodeItem.getLocalName().equals(str2)) {
                    return length;
                }
            } else if (!namespaceURI.equals(str)) {
                continue;
            }
        }
        return -1;
    }

    @Override
    public int getIndex(String str) {
        for (int length = this.m_attrs.getLength() - 1; length >= 0; length--) {
            if (this.m_attrs.item(length).getNodeName().equals(str)) {
                return length;
            }
        }
        return -1;
    }
}
