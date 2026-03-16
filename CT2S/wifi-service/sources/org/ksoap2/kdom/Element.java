package org.ksoap2.kdom;

import java.io.IOException;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class Element extends Node {
    protected Vector attributes;
    protected String name;
    protected String namespace;
    protected Node parent;
    protected Vector prefixes;

    public void init() {
    }

    public void clear() {
        this.attributes = null;
        this.children = null;
    }

    @Override
    public Element createElement(String namespace, String name) {
        return this.parent == null ? super.createElement(namespace, name) : this.parent.createElement(namespace, name);
    }

    public int getAttributeCount() {
        if (this.attributes == null) {
            return 0;
        }
        return this.attributes.size();
    }

    public String getAttributeNamespace(int index) {
        return ((String[]) this.attributes.elementAt(index))[0];
    }

    public String getAttributeName(int index) {
        return ((String[]) this.attributes.elementAt(index))[1];
    }

    public String getAttributeValue(int index) {
        return ((String[]) this.attributes.elementAt(index))[2];
    }

    public String getAttributeValue(String namespace, String name) {
        for (int i = 0; i < getAttributeCount(); i++) {
            if (name.equals(getAttributeName(i)) && (namespace == null || namespace.equals(getAttributeNamespace(i)))) {
                return getAttributeValue(i);
            }
        }
        return null;
    }

    public Node getRoot() {
        Element current = this;
        while (current.parent != null) {
            if (!(current.parent instanceof Element)) {
                return current.parent;
            }
            current = (Element) current.parent;
        }
        return current;
    }

    public String getName() {
        return this.name;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getNamespaceUri(String prefix) {
        int cnt = getNamespaceCount();
        for (int i = 0; i < cnt; i++) {
            if (prefix == getNamespacePrefix(i) || (prefix != null && prefix.equals(getNamespacePrefix(i)))) {
                return getNamespaceUri(i);
            }
        }
        if (this.parent instanceof Element) {
            return ((Element) this.parent).getNamespaceUri(prefix);
        }
        return null;
    }

    public int getNamespaceCount() {
        if (this.prefixes == null) {
            return 0;
        }
        return this.prefixes.size();
    }

    public String getNamespacePrefix(int i) {
        return ((String[]) this.prefixes.elementAt(i))[0];
    }

    public String getNamespaceUri(int i) {
        return ((String[]) this.prefixes.elementAt(i))[1];
    }

    public Node getParent() {
        return this.parent;
    }

    @Override
    public void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        for (int i = parser.getNamespaceCount(parser.getDepth() - 1); i < parser.getNamespaceCount(parser.getDepth()); i++) {
            setPrefix(parser.getNamespacePrefix(i), parser.getNamespaceUri(i));
        }
        for (int i2 = 0; i2 < parser.getAttributeCount(); i2++) {
            setAttribute(parser.getAttributeNamespace(i2), parser.getAttributeName(i2), parser.getAttributeValue(i2));
        }
        init();
        if (parser.isEmptyElementTag()) {
            parser.nextToken();
        } else {
            parser.nextToken();
            super.parse(parser);
            if (getChildCount() == 0) {
                addChild(7, "");
            }
        }
        parser.require(3, getNamespace(), getName());
        parser.nextToken();
    }

    public void setAttribute(String namespace, String name, String value) {
        if (this.attributes == null) {
            this.attributes = new Vector();
        }
        if (namespace == null) {
            namespace = "";
        }
        for (int i = this.attributes.size() - 1; i >= 0; i--) {
            String[] attribut = (String[]) this.attributes.elementAt(i);
            if (attribut[0].equals(namespace) && attribut[1].equals(name)) {
                if (value == null) {
                    this.attributes.removeElementAt(i);
                    return;
                } else {
                    attribut[2] = value;
                    return;
                }
            }
        }
        this.attributes.addElement(new String[]{namespace, name, value});
    }

    public void setPrefix(String prefix, String namespace) {
        if (this.prefixes == null) {
            this.prefixes = new Vector();
        }
        this.prefixes.addElement(new String[]{prefix, namespace});
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespace(String namespace) {
        if (namespace == null) {
            throw new NullPointerException("Use \"\" for empty namespace");
        }
        this.namespace = namespace;
    }

    protected void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public void write(XmlSerializer writer) throws IOException {
        if (this.prefixes != null) {
            for (int i = 0; i < this.prefixes.size(); i++) {
                writer.setPrefix(getNamespacePrefix(i), getNamespaceUri(i));
            }
        }
        writer.startTag(getNamespace(), getName());
        int len = getAttributeCount();
        for (int i2 = 0; i2 < len; i2++) {
            writer.attribute(getAttributeNamespace(i2), getAttributeName(i2), getAttributeValue(i2));
        }
        writeChildren(writer);
        writer.endTag(getNamespace(), getName());
    }
}
