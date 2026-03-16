package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.options.PropertyOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

class XMPNode implements Comparable {
    static final boolean $assertionsDisabled;
    private boolean alias;
    private List children;
    private boolean hasAliases;
    private boolean hasValueChild;
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

    public void replaceChild(int index, XMPNode node) {
        node.setParent(this);
        getChildren().set(index - 1, node);
    }

    public void removeChild(int itemIndex) {
        getChildren().remove(itemIndex - 1);
        cleanupChildren();
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

    public int getQualifierLength() {
        if (this.qualifier != null) {
            return this.qualifier.size();
        }
        return 0;
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

    public void removeQualifiers() {
        PropertyOptions opts = getOptions();
        opts.setHasQualifiers(false);
        opts.setHasLanguage(false);
        opts.setHasType(false);
        this.qualifier = null;
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

    public String dumpNode(boolean recursive) {
        StringBuffer result = new StringBuffer(512);
        dumpNode(result, recursive, 0, 0);
        return result.toString();
    }

    @Override
    public int compareTo(Object xmpNode) {
        return getOptions().isSchemaNode() ? this.value.compareTo(((XMPNode) xmpNode).getValue()) : this.name.compareTo(((XMPNode) xmpNode).getName());
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean getHasAliases() {
        return this.hasAliases;
    }

    public void setHasAliases(boolean hasAliases) {
        this.hasAliases = hasAliases;
    }

    public boolean isAlias() {
        return this.alias;
    }

    public void setAlias(boolean alias) {
        this.alias = alias;
    }

    public boolean getHasValueChild() {
        return this.hasValueChild;
    }

    public void setHasValueChild(boolean hasValueChild) {
        this.hasValueChild = hasValueChild;
    }

    public void sort() {
        if (hasQualifier()) {
            XMPNode[] quals = (XMPNode[]) getQualifier().toArray(new XMPNode[getQualifierLength()]);
            int sortFrom = 0;
            while (quals.length > sortFrom && (XMPConst.XML_LANG.equals(quals[sortFrom].getName()) || XMPConst.RDF_TYPE.equals(quals[sortFrom].getName()))) {
                quals[sortFrom].sort();
                sortFrom++;
            }
            Arrays.sort(quals, sortFrom, quals.length);
            ListIterator it = this.qualifier.listIterator();
            for (int j = 0; j < quals.length; j++) {
                it.next();
                it.set(quals[j]);
                quals[j].sort();
            }
        }
        if (hasChildren()) {
            if (!getOptions().isArray()) {
                Collections.sort(this.children);
            }
            Iterator it2 = iterateChildren();
            while (it2.hasNext()) {
                ((XMPNode) it2.next()).sort();
            }
        }
    }

    private void dumpNode(StringBuffer result, boolean recursive, int indent, int index) {
        for (int i = 0; i < indent; i++) {
            result.append('\t');
        }
        if (this.parent != null) {
            if (getOptions().isQualifier()) {
                result.append('?');
                result.append(this.name);
            } else if (getParent().getOptions().isArray()) {
                result.append('[');
                result.append(index);
                result.append(']');
            } else {
                result.append(this.name);
            }
        } else {
            result.append("ROOT NODE");
            if (this.name != null && this.name.length() > 0) {
                result.append(" (");
                result.append(this.name);
                result.append(')');
            }
        }
        if (this.value != null && this.value.length() > 0) {
            result.append(" = \"");
            result.append(this.value);
            result.append('\"');
        }
        if (getOptions().containsOneOf(-1)) {
            result.append("\t(");
            result.append(getOptions().toString());
            result.append(" : ");
            result.append(getOptions().getOptionsString());
            result.append(')');
        }
        result.append('\n');
        if (recursive && hasQualifier()) {
            XMPNode[] quals = (XMPNode[]) getQualifier().toArray(new XMPNode[getQualifierLength()]);
            int i2 = 0;
            while (quals.length > i2 && (XMPConst.XML_LANG.equals(quals[i2].getName()) || XMPConst.RDF_TYPE.equals(quals[i2].getName()))) {
                i2++;
            }
            Arrays.sort(quals, i2, quals.length);
            for (int i3 = 0; i3 < quals.length; i3++) {
                XMPNode qualifier = quals[i3];
                qualifier.dumpNode(result, recursive, indent + 2, i3 + 1);
            }
        }
        if (recursive && hasChildren()) {
            XMPNode[] children = (XMPNode[]) getChildren().toArray(new XMPNode[getChildrenLength()]);
            if (!getOptions().isArray()) {
                Arrays.sort(children);
            }
            for (int i4 = 0; i4 < children.length; i4++) {
                XMPNode child = children[i4];
                child.dumpNode(result, recursive, indent + 1, i4 + 1);
            }
        }
    }

    private boolean isLanguageNode() {
        return XMPConst.XML_LANG.equals(this.name);
    }

    private boolean isTypeNode() {
        return XMPConst.RDF_TYPE.equals(this.name);
    }

    private List getChildren() {
        if (this.children == null) {
            this.children = new ArrayList(0);
        }
        return this.children;
    }

    public List getUnmodifiableChildren() {
        return Collections.unmodifiableList(new ArrayList(getChildren()));
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
        if (!XMPConst.ARRAY_ITEM_NAME.equals(childName) && findChildByName(childName) != null) {
            throw new XMPException("Duplicate property or field node '" + childName + "'", 203);
        }
    }

    private void assertQualifierNotExisting(String qualifierName) throws XMPException {
        if (!XMPConst.ARRAY_ITEM_NAME.equals(qualifierName) && findQualifierByName(qualifierName) != null) {
            throw new XMPException("Duplicate '" + qualifierName + "' qualifier", 203);
        }
    }
}
