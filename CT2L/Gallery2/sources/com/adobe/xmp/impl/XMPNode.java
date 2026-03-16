package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.options.PropertyOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class XMPNode implements Comparable {
    static final boolean $assertionsDisabled;
    private List children;
    private boolean implicit;
    private String name;
    private PropertyOptions options;
    private XMPNode parent;
    private List qualifier;
    private String value;

    static {
        $assertionsDisabled = !XMPNode.class.desiredAssertionStatus();
    }

    public XMPNode(String name, String value, PropertyOptions options) {
        this.children = null;
        this.qualifier = null;
        this.options = null;
        this.name = name;
        this.value = value;
        this.options = options;
    }

    public XMPNode(String name, PropertyOptions options) {
        this(name, null, options);
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

    public XMPNode getChild(int index) {
        return (XMPNode) getChildren().get(index - 1);
    }

    public void addChild(XMPNode node) throws XMPException {
        assertChildNotExisting(node.getName());
        node.setParent(this);
        getChildren().add(node);
    }

    public void addChild(int index, XMPNode node) throws XMPException {
        assertChildNotExisting(node.getName());
        node.setParent(this);
        getChildren().add(index - 1, node);
    }

    public void removeChild(XMPNode node) {
        getChildren().remove(node);
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

    public XMPNode findChildByName(String expr) {
        return find(getChildren(), expr);
    }

    public XMPNode getQualifier(int index) {
        return (XMPNode) getQualifier().get(index - 1);
    }

    public void addQualifier(XMPNode qualNode) throws XMPException {
        assertQualifierNotExisting(qualNode.getName());
        qualNode.setParent(this);
        qualNode.getOptions().setQualifier(true);
        getOptions().setHasQualifiers(true);
        if (qualNode.isLanguageNode()) {
            this.options.setHasLanguage(true);
            getQualifier().add(0, qualNode);
        } else if (qualNode.isTypeNode()) {
            this.options.setHasType(true);
            getQualifier().add(this.options.getHasLanguage() ? 1 : 0, qualNode);
        } else {
            getQualifier().add(qualNode);
        }
    }

    public void removeQualifier(XMPNode qualNode) {
        PropertyOptions opts = getOptions();
        if (qualNode.isLanguageNode()) {
            opts.setHasLanguage(false);
        } else if (qualNode.isTypeNode()) {
            opts.setHasType(false);
        }
        getQualifier().remove(qualNode);
        if (this.qualifier.isEmpty()) {
            opts.setHasQualifiers(false);
            this.qualifier = null;
        }
    }

    public XMPNode findQualifierByName(String expr) {
        return find(this.qualifier, expr);
    }

    public boolean hasChildren() {
        return this.children != null && this.children.size() > 0;
    }

    public Iterator iterateChildren() {
        return this.children != null ? getChildren().iterator() : Collections.EMPTY_LIST.listIterator();
    }

    public boolean hasQualifier() {
        return this.qualifier != null && this.qualifier.size() > 0;
    }

    public Iterator iterateQualifier() {
        if (this.qualifier == null) {
            return Collections.EMPTY_LIST.iterator();
        }
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

    public Object clone() {
        PropertyOptions newOptions;
        try {
            newOptions = new PropertyOptions(getOptions().getOptions());
        } catch (XMPException e) {
            newOptions = new PropertyOptions();
        }
        XMPNode newNode = new XMPNode(this.name, this.value, newOptions);
        cloneSubtree(newNode);
        return newNode;
    }

    public void cloneSubtree(XMPNode destination) {
        try {
            Iterator it = iterateChildren();
            while (it.hasNext()) {
                XMPNode child = (XMPNode) it.next();
                destination.addChild((XMPNode) child.clone());
            }
            Iterator it2 = iterateQualifier();
            while (it2.hasNext()) {
                XMPNode qualifier = (XMPNode) it2.next();
                destination.addQualifier((XMPNode) qualifier.clone());
            }
        } catch (XMPException e) {
            if (!$assertionsDisabled) {
                throw new AssertionError();
            }
        }
    }

    @Override
    public int compareTo(Object xmpNode) {
        return getOptions().isSchemaNode() ? this.value.compareTo(((XMPNode) xmpNode).getValue()) : this.name.compareTo(((XMPNode) xmpNode).getName());
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public PropertyOptions getOptions() {
        if (this.options == null) {
            this.options = new PropertyOptions();
        }
        return this.options;
    }

    public void setOptions(PropertyOptions options) {
        this.options = options;
    }

    public boolean isImplicit() {
        return this.implicit;
    }

    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
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

    protected void setParent(XMPNode parent) {
        this.parent = parent;
    }

    private XMPNode find(List list, String expr) {
        if (list != null) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                XMPNode child = (XMPNode) it.next();
                if (child.getName().equals(expr)) {
                    return child;
                }
            }
        }
        return null;
    }

    private void assertChildNotExisting(String childName) throws XMPException {
        if (!"[]".equals(childName) && findChildByName(childName) != null) {
            throw new XMPException("Duplicate property or field node '" + childName + "'", 203);
        }
    }

    private void assertQualifierNotExisting(String qualifierName) throws XMPException {
        if (!"[]".equals(qualifierName) && findQualifierByName(qualifierName) != null) {
            throw new XMPException("Duplicate '" + qualifierName + "' qualifier", 203);
        }
    }
}
