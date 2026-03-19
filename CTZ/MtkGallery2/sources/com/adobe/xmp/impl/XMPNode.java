package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.options.PropertyOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class XMPNode implements Comparable {
    static final boolean $assertionsDisabled = false;
    private List children;
    private boolean implicit;
    private String name;
    private PropertyOptions options;
    private XMPNode parent;
    private List qualifier;
    private String value;

    public XMPNode(String str, String str2, PropertyOptions propertyOptions) {
        this.children = null;
        this.qualifier = null;
        this.options = null;
        this.name = str;
        this.value = str2;
        this.options = propertyOptions;
    }

    public XMPNode(String str, PropertyOptions propertyOptions) {
        this(str, null, propertyOptions);
    }

    public void clear() {
        this.options = null;
        this.name = null;
        this.value = null;
        this.children = null;
        this.qualifier = null;
    }

    public XMPNode getParent() {
        return this.parent;
    }

    public XMPNode getChild(int i) {
        return (XMPNode) getChildren().get(i - 1);
    }

    public void addChild(XMPNode xMPNode) throws XMPException {
        assertChildNotExisting(xMPNode.getName());
        xMPNode.setParent(this);
        getChildren().add(xMPNode);
    }

    public void addChild(int i, XMPNode xMPNode) throws XMPException {
        assertChildNotExisting(xMPNode.getName());
        xMPNode.setParent(this);
        getChildren().add(i - 1, xMPNode);
    }

    public void removeChild(XMPNode xMPNode) {
        getChildren().remove(xMPNode);
        cleanupChildren();
    }

    protected void cleanupChildren() {
        if (this.children.isEmpty()) {
            this.children = null;
        }
    }

    public void removeChildren() {
        this.children = null;
    }

    public int getChildrenLength() {
        if (this.children != null) {
            return this.children.size();
        }
        return 0;
    }

    public XMPNode findChildByName(String str) {
        return find(getChildren(), str);
    }

    public XMPNode getQualifier(int i) {
        return (XMPNode) getQualifier().get(i - 1);
    }

    public void addQualifier(XMPNode xMPNode) throws XMPException {
        assertQualifierNotExisting(xMPNode.getName());
        xMPNode.setParent(this);
        xMPNode.getOptions().setQualifier(true);
        getOptions().setHasQualifiers(true);
        if (xMPNode.isLanguageNode()) {
            this.options.setHasLanguage(true);
            getQualifier().add(0, xMPNode);
        } else if (xMPNode.isTypeNode()) {
            this.options.setHasType(true);
            getQualifier().add(this.options.getHasLanguage() ? 1 : 0, xMPNode);
        } else {
            getQualifier().add(xMPNode);
        }
    }

    public void removeQualifier(XMPNode xMPNode) {
        PropertyOptions options = getOptions();
        if (xMPNode.isLanguageNode()) {
            options.setHasLanguage(false);
        } else if (xMPNode.isTypeNode()) {
            options.setHasType(false);
        }
        getQualifier().remove(xMPNode);
        if (this.qualifier.isEmpty()) {
            options.setHasQualifiers(false);
            this.qualifier = null;
        }
    }

    public XMPNode findQualifierByName(String str) {
        return find(this.qualifier, str);
    }

    public boolean hasChildren() {
        return this.children != null && this.children.size() > 0;
    }

    public Iterator iterateChildren() {
        if (this.children != null) {
            return getChildren().iterator();
        }
        return Collections.EMPTY_LIST.listIterator();
    }

    public boolean hasQualifier() {
        return this.qualifier != null && this.qualifier.size() > 0;
    }

    public Iterator iterateQualifier() {
        if (this.qualifier != null) {
            final Iterator it = getQualifier().iterator();
            return new Iterator() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Object next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove() is not allowed due to the internal contraints");
                }
            };
        }
        return Collections.EMPTY_LIST.iterator();
    }

    public Object clone() {
        PropertyOptions propertyOptions;
        try {
            propertyOptions = new PropertyOptions(getOptions().getOptions());
        } catch (XMPException e) {
            propertyOptions = new PropertyOptions();
        }
        XMPNode xMPNode = new XMPNode(this.name, this.value, propertyOptions);
        cloneSubtree(xMPNode);
        return xMPNode;
    }

    public void cloneSubtree(XMPNode xMPNode) {
        try {
            Iterator itIterateChildren = iterateChildren();
            while (itIterateChildren.hasNext()) {
                xMPNode.addChild((XMPNode) ((XMPNode) itIterateChildren.next()).clone());
            }
            Iterator itIterateQualifier = iterateQualifier();
            while (itIterateQualifier.hasNext()) {
                xMPNode.addQualifier((XMPNode) ((XMPNode) itIterateQualifier.next()).clone());
            }
        } catch (XMPException e) {
        }
    }

    @Override
    public int compareTo(Object obj) {
        if (getOptions().isSchemaNode()) {
            return this.value.compareTo(((XMPNode) obj).getValue());
        }
        return this.name.compareTo(((XMPNode) obj).getName());
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String str) {
        this.value = str;
    }

    public PropertyOptions getOptions() {
        if (this.options == null) {
            this.options = new PropertyOptions();
        }
        return this.options;
    }

    public void setOptions(PropertyOptions propertyOptions) {
        this.options = propertyOptions;
    }

    public boolean isImplicit() {
        return this.implicit;
    }

    public void setImplicit(boolean z) {
        this.implicit = z;
    }

    private boolean isLanguageNode() {
        return "xml:lang".equals(this.name);
    }

    private boolean isTypeNode() {
        return "rdf:type".equals(this.name);
    }

    private List getChildren() {
        if (this.children == null) {
            this.children = new ArrayList(0);
        }
        return this.children;
    }

    private List getQualifier() {
        if (this.qualifier == null) {
            this.qualifier = new ArrayList(0);
        }
        return this.qualifier;
    }

    protected void setParent(XMPNode xMPNode) {
        this.parent = xMPNode;
    }

    private XMPNode find(List list, String str) {
        if (list != null) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                XMPNode xMPNode = (XMPNode) it.next();
                if (xMPNode.getName().equals(str)) {
                    return xMPNode;
                }
            }
            return null;
        }
        return null;
    }

    private void assertChildNotExisting(String str) throws XMPException {
        if (!"[]".equals(str) && findChildByName(str) != null) {
            throw new XMPException("Duplicate property or field node '" + str + "'", 203);
        }
    }

    private void assertQualifierNotExisting(String str) throws XMPException {
        if (!"[]".equals(str) && findQualifierByName(str) != null) {
            throw new XMPException("Duplicate '" + str + "' qualifier", 203);
        }
    }
}
