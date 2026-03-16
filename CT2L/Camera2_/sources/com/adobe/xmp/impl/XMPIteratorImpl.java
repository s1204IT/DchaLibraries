package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.IteratorOptions;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.properties.XMPPropertyInfo;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class XMPIteratorImpl implements XMPIterator {
    private String baseNS;
    private Iterator nodeIterator;
    private IteratorOptions options;
    protected boolean skipSiblings = false;
    protected boolean skipSubtree = false;

    public XMPIteratorImpl(XMPMetaImpl xmp, String schemaNS, String propPath, IteratorOptions options) throws XMPException {
        XMPNode startNode;
        this.baseNS = null;
        this.nodeIterator = null;
        this.options = options == null ? new IteratorOptions() : options;
        String initialPath = null;
        boolean baseSchema = schemaNS != null && schemaNS.length() > 0;
        boolean baseProperty = propPath != null && propPath.length() > 0;
        if (!baseSchema && !baseProperty) {
            startNode = xmp.getRoot();
        } else if (baseSchema && baseProperty) {
            XMPPath path = XMPPathParser.expandXPath(schemaNS, propPath);
            XMPPath basePath = new XMPPath();
            for (int i = 0; i < path.size() - 1; i++) {
                basePath.add(path.getSegment(i));
            }
            startNode = XMPNodeUtils.findNode(xmp.getRoot(), path, false, null);
            this.baseNS = schemaNS;
            initialPath = basePath.toString();
        } else if (baseSchema && !baseProperty) {
            startNode = XMPNodeUtils.findSchemaNode(xmp.getRoot(), schemaNS, false);
        } else {
            throw new XMPException("Schema namespace URI is required", 101);
        }
        if (startNode != null) {
            if (!this.options.isJustChildren()) {
                this.nodeIterator = new NodeIterator(startNode, initialPath, 1);
                return;
            } else {
                this.nodeIterator = new NodeIteratorChildren(startNode, initialPath);
                return;
            }
        }
        this.nodeIterator = Collections.EMPTY_LIST.iterator();
    }

    @Override
    public void skipSubtree() {
        this.skipSubtree = true;
    }

    @Override
    public void skipSiblings() {
        skipSubtree();
        this.skipSiblings = true;
    }

    @Override
    public boolean hasNext() {
        return this.nodeIterator.hasNext();
    }

    @Override
    public Object next() {
        return this.nodeIterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("The XMPIterator does not support remove().");
    }

    protected IteratorOptions getOptions() {
        return this.options;
    }

    protected String getBaseNS() {
        return this.baseNS;
    }

    protected void setBaseNS(String baseNS) {
        this.baseNS = baseNS;
    }

    private class NodeIterator implements Iterator {
        protected static final int ITERATE_CHILDREN = 1;
        protected static final int ITERATE_NODE = 0;
        protected static final int ITERATE_QUALIFIER = 2;
        private Iterator childrenIterator;
        private int index;
        private String path;
        private XMPPropertyInfo returnProperty;
        private int state;
        private Iterator subIterator;
        private XMPNode visitedNode;

        public NodeIterator() {
            this.state = 0;
            this.childrenIterator = null;
            this.index = 0;
            this.subIterator = Collections.EMPTY_LIST.iterator();
            this.returnProperty = null;
        }

        public NodeIterator(XMPNode visitedNode, String parentPath, int index) {
            this.state = 0;
            this.childrenIterator = null;
            this.index = 0;
            this.subIterator = Collections.EMPTY_LIST.iterator();
            this.returnProperty = null;
            this.visitedNode = visitedNode;
            this.state = 0;
            if (visitedNode.getOptions().isSchemaNode()) {
                XMPIteratorImpl.this.setBaseNS(visitedNode.getName());
            }
            this.path = accumulatePath(visitedNode, parentPath, index);
        }

        @Override
        public boolean hasNext() {
            if (this.returnProperty != null) {
                return true;
            }
            if (this.state == 0) {
                return reportNode();
            }
            if (this.state == 1) {
                if (this.childrenIterator == null) {
                    this.childrenIterator = this.visitedNode.iterateChildren();
                }
                boolean hasNext = iterateChildren(this.childrenIterator);
                if (!hasNext && this.visitedNode.hasQualifier() && !XMPIteratorImpl.this.getOptions().isOmitQualifiers()) {
                    this.state = 2;
                    this.childrenIterator = null;
                    return hasNext();
                }
                return hasNext;
            }
            if (this.childrenIterator == null) {
                this.childrenIterator = this.visitedNode.iterateQualifier();
            }
            return iterateChildren(this.childrenIterator);
        }

        protected boolean reportNode() {
            this.state = 1;
            if (this.visitedNode.getParent() == null || (XMPIteratorImpl.this.getOptions().isJustLeafnodes() && this.visitedNode.hasChildren())) {
                return hasNext();
            }
            this.returnProperty = createPropertyInfo(this.visitedNode, XMPIteratorImpl.this.getBaseNS(), this.path);
            return true;
        }

        private boolean iterateChildren(Iterator iterator) {
            if (XMPIteratorImpl.this.skipSiblings) {
                XMPIteratorImpl.this.skipSiblings = false;
                this.subIterator = Collections.EMPTY_LIST.iterator();
            }
            if (!this.subIterator.hasNext() && iterator.hasNext()) {
                XMPNode child = (XMPNode) iterator.next();
                this.index++;
                this.subIterator = XMPIteratorImpl.this.new NodeIterator(child, this.path, this.index);
            }
            if (!this.subIterator.hasNext()) {
                return false;
            }
            this.returnProperty = (XMPPropertyInfo) this.subIterator.next();
            return true;
        }

        @Override
        public Object next() {
            if (hasNext()) {
                XMPPropertyInfo result = this.returnProperty;
                this.returnProperty = null;
                return result;
            }
            throw new NoSuchElementException("There are no more nodes to return");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        protected String accumulatePath(XMPNode currNode, String parentPath, int currentIndex) {
            String separator;
            String segmentName;
            if (currNode.getParent() == null || currNode.getOptions().isSchemaNode()) {
                return null;
            }
            if (currNode.getParent().getOptions().isArray()) {
                separator = "";
                segmentName = "[" + String.valueOf(currentIndex) + "]";
            } else {
                separator = "/";
                segmentName = currNode.getName();
            }
            if (parentPath != null && parentPath.length() != 0) {
                if (XMPIteratorImpl.this.getOptions().isJustLeafname()) {
                    return segmentName.startsWith("?") ? segmentName.substring(1) : segmentName;
                }
                return parentPath + separator + segmentName;
            }
            return segmentName;
        }

        protected XMPPropertyInfo createPropertyInfo(final XMPNode node, final String baseNS, final String path) {
            final String value = node.getOptions().isSchemaNode() ? null : node.getValue();
            return new XMPPropertyInfo() {
                @Override
                public String getNamespace() {
                    return baseNS;
                }

                @Override
                public String getPath() {
                    return path;
                }

                @Override
                public Object getValue() {
                    return value;
                }

                @Override
                public PropertyOptions getOptions() {
                    return node.getOptions();
                }

                @Override
                public String getLanguage() {
                    return null;
                }
            };
        }

        protected Iterator getChildrenIterator() {
            return this.childrenIterator;
        }

        protected void setChildrenIterator(Iterator childrenIterator) {
            this.childrenIterator = childrenIterator;
        }

        protected XMPPropertyInfo getReturnProperty() {
            return this.returnProperty;
        }

        protected void setReturnProperty(XMPPropertyInfo returnProperty) {
            this.returnProperty = returnProperty;
        }
    }

    private class NodeIteratorChildren extends NodeIterator {
        private Iterator childrenIterator;
        private int index;
        private String parentPath;

        public NodeIteratorChildren(XMPNode parentNode, String parentPath) {
            super();
            this.index = 0;
            if (parentNode.getOptions().isSchemaNode()) {
                XMPIteratorImpl.this.setBaseNS(parentNode.getName());
            }
            this.parentPath = accumulatePath(parentNode, parentPath, 1);
            this.childrenIterator = parentNode.iterateChildren();
        }

        @Override
        public boolean hasNext() {
            if (getReturnProperty() != null) {
                return true;
            }
            if (!XMPIteratorImpl.this.skipSiblings && this.childrenIterator.hasNext()) {
                XMPNode child = (XMPNode) this.childrenIterator.next();
                this.index++;
                String path = null;
                if (child.getOptions().isSchemaNode()) {
                    XMPIteratorImpl.this.setBaseNS(child.getName());
                } else if (child.getParent() != null) {
                    path = accumulatePath(child, this.parentPath, this.index);
                }
                if (!XMPIteratorImpl.this.getOptions().isJustLeafnodes() || !child.hasChildren()) {
                    setReturnProperty(createPropertyInfo(child, XMPIteratorImpl.this.getBaseNS(), path));
                    return true;
                }
                return hasNext();
            }
            return false;
        }
    }
}
