package org.apache.xpath;

import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.DOM2Helper;
import org.apache.xpath.axes.ContextNodeList;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

public class NodeSet implements NodeList, NodeIterator, Cloneable, ContextNodeList {
    private int m_blocksize;
    protected transient boolean m_cacheNodes;
    protected int m_firstFree;
    private transient int m_last;
    Node[] m_map;
    private int m_mapSize;
    protected transient boolean m_mutable;
    protected transient int m_next;

    public NodeSet() {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_last = 0;
        this.m_firstFree = 0;
        this.m_blocksize = 32;
        this.m_mapSize = 0;
    }

    public NodeSet(int i) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_last = 0;
        this.m_firstFree = 0;
        this.m_blocksize = i;
        this.m_mapSize = 0;
    }

    public NodeSet(NodeList nodeList) {
        this(32);
        addNodes(nodeList);
    }

    public NodeSet(NodeSet nodeSet) {
        this(32);
        addNodes((NodeIterator) nodeSet);
    }

    public NodeSet(NodeIterator nodeIterator) {
        this(32);
        addNodes(nodeIterator);
    }

    public NodeSet(Node node) {
        this(32);
        addNode(node);
    }

    public Node getRoot() {
        return null;
    }

    @Override
    public NodeIterator cloneWithReset() throws CloneNotSupportedException {
        NodeSet nodeSet = (NodeSet) clone();
        nodeSet.reset();
        return nodeSet;
    }

    @Override
    public void reset() {
        this.m_next = 0;
    }

    public int getWhatToShow() {
        return -17;
    }

    public NodeFilter getFilter() {
        return null;
    }

    public boolean getExpandEntityReferences() {
        return true;
    }

    public Node nextNode() throws DOMException {
        if (this.m_next < size()) {
            Node nodeElementAt = elementAt(this.m_next);
            this.m_next++;
            return nodeElementAt;
        }
        return null;
    }

    public Node previousNode() throws DOMException {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_ITERATE, null));
        }
        if (this.m_next - 1 <= 0) {
            return null;
        }
        this.m_next--;
        return elementAt(this.m_next);
    }

    public void detach() {
    }

    @Override
    public boolean isFresh() {
        return this.m_next == 0;
    }

    @Override
    public void runTo(int i) {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null));
        }
        if (i >= 0 && this.m_next < this.m_firstFree) {
            this.m_next = i;
        } else {
            this.m_next = this.m_firstFree - 1;
        }
    }

    @Override
    public Node item(int i) {
        runTo(i);
        return elementAt(i);
    }

    @Override
    public int getLength() {
        runTo(-1);
        return size();
    }

    public void addNode(Node node) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        addElement(node);
    }

    public void insertNode(Node node, int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        insertElementAt(node, i);
    }

    public void removeNode(Node node) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        removeElement(node);
    }

    public void addNodes(NodeList nodeList) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (nodeList != null) {
            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Node nodeItem = nodeList.item(i);
                if (nodeItem != null) {
                    addElement(nodeItem);
                }
            }
        }
    }

    public void addNodes(NodeSet nodeSet) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        addNodes((NodeIterator) nodeSet);
    }

    public void addNodes(NodeIterator nodeIterator) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (nodeIterator == null) {
            return;
        }
        while (true) {
            Node nodeNextNode = nodeIterator.nextNode();
            if (nodeNextNode != null) {
                addElement(nodeNextNode);
            } else {
                return;
            }
        }
    }

    public void addNodesInDocOrder(NodeList nodeList, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            Node nodeItem = nodeList.item(i);
            if (nodeItem != null) {
                addNodeInDocOrder(nodeItem, xPathContext);
            }
        }
    }

    public void addNodesInDocOrder(NodeIterator nodeIterator, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        while (true) {
            Node nodeNextNode = nodeIterator.nextNode();
            if (nodeNextNode != null) {
                addNodeInDocOrder(nodeNextNode, xPathContext);
            } else {
                return;
            }
        }
    }

    private boolean addNodesInDocOrder(int i, int i2, int i3, NodeList nodeList, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        Node nodeItem = nodeList.item(i3);
        while (true) {
            if (i2 < i) {
                break;
            }
            Node nodeElementAt = elementAt(i2);
            if (nodeElementAt == nodeItem) {
                i2 = -2;
                break;
            }
            if (DOM2Helper.isNodeAfter(nodeItem, nodeElementAt)) {
                i2--;
            } else {
                insertElementAt(nodeItem, i2 + 1);
                int i4 = i3 - 1;
                if (i4 > 0 && !addNodesInDocOrder(0, i2, i4, nodeList, xPathContext)) {
                    addNodesInDocOrder(i2, size() - 1, i4, nodeList, xPathContext);
                }
            }
        }
        if (i2 == -1) {
            insertElementAt(nodeItem, 0);
        }
        return false;
    }

    public int addNodeInDocOrder(Node node, boolean z, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        boolean z2 = true;
        if (z) {
            int size = size() - 1;
            while (true) {
                if (size < 0) {
                    break;
                }
                Node nodeElementAt = elementAt(size);
                if (nodeElementAt != node) {
                    if (!DOM2Helper.isNodeAfter(node, nodeElementAt)) {
                        break;
                    }
                    size--;
                } else {
                    size = -2;
                    break;
                }
            }
            if (size != -2) {
                int i = size + 1;
                insertElementAt(node, i);
                return i;
            }
            return -1;
        }
        int size2 = size();
        int i2 = 0;
        while (true) {
            if (i2 < size2) {
                if (item(i2).equals(node)) {
                    break;
                }
                i2++;
            } else {
                z2 = false;
                break;
            }
        }
        if (!z2) {
            addElement(node);
            return size2;
        }
        return size2;
    }

    public int addNodeInDocOrder(Node node, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        return addNodeInDocOrder(node, true, xPathContext);
    }

    @Override
    public int getCurrentPos() {
        return this.m_next;
    }

    @Override
    public void setCurrentPos(int i) {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null));
        }
        this.m_next = i;
    }

    @Override
    public Node getCurrentNode() {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null));
        }
        int i = this.m_next;
        Node nodeElementAt = this.m_next < this.m_firstFree ? elementAt(this.m_next) : null;
        this.m_next = i;
        return nodeElementAt;
    }

    public boolean getShouldCacheNodes() {
        return this.m_cacheNodes;
    }

    @Override
    public void setShouldCacheNodes(boolean z) {
        if (!isFresh()) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_CALL_SETSHOULDCACHENODE, null));
        }
        this.m_cacheNodes = z;
        this.m_mutable = true;
    }

    @Override
    public int getLast() {
        return this.m_last;
    }

    @Override
    public void setLast(int i) {
        this.m_last = i;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        NodeSet nodeSet = (NodeSet) super.clone();
        if (this.m_map != null && this.m_map == nodeSet.m_map) {
            nodeSet.m_map = new Node[this.m_map.length];
            System.arraycopy(this.m_map, 0, nodeSet.m_map, 0, this.m_map.length);
        }
        return nodeSet;
    }

    @Override
    public int size() {
        return this.m_firstFree;
    }

    public void addElement(Node node) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            if (this.m_map == null) {
                this.m_map = new Node[this.m_blocksize];
                this.m_mapSize = this.m_blocksize;
            } else {
                this.m_mapSize += this.m_blocksize;
                Node[] nodeArr = new Node[this.m_mapSize];
                System.arraycopy(this.m_map, 0, nodeArr, 0, this.m_firstFree + 1);
                this.m_map = nodeArr;
            }
        }
        this.m_map[this.m_firstFree] = node;
        this.m_firstFree++;
    }

    public final void push(Node node) {
        int i = this.m_firstFree;
        int i2 = i + 1;
        if (i2 >= this.m_mapSize) {
            if (this.m_map == null) {
                this.m_map = new Node[this.m_blocksize];
                this.m_mapSize = this.m_blocksize;
            } else {
                this.m_mapSize += this.m_blocksize;
                Node[] nodeArr = new Node[this.m_mapSize];
                System.arraycopy(this.m_map, 0, nodeArr, 0, i2);
                this.m_map = nodeArr;
            }
        }
        this.m_map[i] = node;
        this.m_firstFree = i2;
    }

    public final Node pop() {
        this.m_firstFree--;
        Node node = this.m_map[this.m_firstFree];
        this.m_map[this.m_firstFree] = null;
        return node;
    }

    public final Node popAndTop() {
        this.m_firstFree--;
        this.m_map[this.m_firstFree] = null;
        if (this.m_firstFree == 0) {
            return null;
        }
        return this.m_map[this.m_firstFree - 1];
    }

    public final void popQuick() {
        this.m_firstFree--;
        this.m_map[this.m_firstFree] = null;
    }

    public final Node peepOrNull() {
        if (this.m_map == null || this.m_firstFree <= 0) {
            return null;
        }
        return this.m_map[this.m_firstFree - 1];
    }

    public final void pushPair(Node node, Node node2) {
        if (this.m_map == null) {
            this.m_map = new Node[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        } else if (this.m_firstFree + 2 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Node[] nodeArr = new Node[this.m_mapSize];
            System.arraycopy(this.m_map, 0, nodeArr, 0, this.m_firstFree);
            this.m_map = nodeArr;
        }
        this.m_map[this.m_firstFree] = node;
        this.m_map[this.m_firstFree + 1] = node2;
        this.m_firstFree += 2;
    }

    public final void popPair() {
        this.m_firstFree -= 2;
        this.m_map[this.m_firstFree] = null;
        this.m_map[this.m_firstFree + 1] = null;
    }

    public final void setTail(Node node) {
        this.m_map[this.m_firstFree - 1] = node;
    }

    public final void setTailSub1(Node node) {
        this.m_map[this.m_firstFree - 2] = node;
    }

    public final Node peepTail() {
        return this.m_map[this.m_firstFree - 1];
    }

    public final Node peepTailSub1() {
        return this.m_map[this.m_firstFree - 2];
    }

    public void insertElementAt(Node node, int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_map == null) {
            this.m_map = new Node[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        } else if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Node[] nodeArr = new Node[this.m_mapSize];
            System.arraycopy(this.m_map, 0, nodeArr, 0, this.m_firstFree + 1);
            this.m_map = nodeArr;
        }
        if (i <= this.m_firstFree - 1) {
            System.arraycopy(this.m_map, i, this.m_map, i + 1, this.m_firstFree - i);
        }
        this.m_map[i] = node;
        this.m_firstFree++;
    }

    public void appendNodes(NodeSet nodeSet) {
        int size = nodeSet.size();
        if (this.m_map == null) {
            this.m_mapSize = this.m_blocksize + size;
            this.m_map = new Node[this.m_mapSize];
        } else if (this.m_firstFree + size >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + size;
            Node[] nodeArr = new Node[this.m_mapSize];
            System.arraycopy(this.m_map, 0, nodeArr, 0, this.m_firstFree + size);
            this.m_map = nodeArr;
        }
        System.arraycopy(nodeSet.m_map, 0, this.m_map, this.m_firstFree, size);
        this.m_firstFree += size;
    }

    public void removeAllElements() {
        if (this.m_map == null) {
            return;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            this.m_map[i] = null;
        }
        this.m_firstFree = 0;
    }

    public boolean removeElement(Node node) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_map == null) {
            return false;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            Node node2 = this.m_map[i];
            if (node2 != null && node2.equals(node)) {
                if (i < this.m_firstFree - 1) {
                    System.arraycopy(this.m_map, i + 1, this.m_map, i, (this.m_firstFree - i) - 1);
                }
                this.m_firstFree--;
                this.m_map[this.m_firstFree] = null;
                return true;
            }
        }
        return false;
    }

    public void removeElementAt(int i) {
        if (this.m_map == null) {
            return;
        }
        if (i >= this.m_firstFree) {
            throw new ArrayIndexOutOfBoundsException(i + " >= " + this.m_firstFree);
        }
        if (i < 0) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        if (i < this.m_firstFree - 1) {
            System.arraycopy(this.m_map, i + 1, this.m_map, i, (this.m_firstFree - i) - 1);
        }
        this.m_firstFree--;
        this.m_map[this.m_firstFree] = null;
    }

    public void setElementAt(Node node, int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_map == null) {
            this.m_map = new Node[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        }
        this.m_map[i] = node;
    }

    public Node elementAt(int i) {
        if (this.m_map == null) {
            return null;
        }
        return this.m_map[i];
    }

    public boolean contains(Node node) {
        runTo(-1);
        if (this.m_map == null) {
            return false;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            Node node2 = this.m_map[i];
            if (node2 != null && node2.equals(node)) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(Node node, int i) {
        runTo(-1);
        if (this.m_map == null) {
            return -1;
        }
        while (i < this.m_firstFree) {
            Node node2 = this.m_map[i];
            if (node2 == null || !node2.equals(node)) {
                i++;
            } else {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(Node node) {
        runTo(-1);
        if (this.m_map == null) {
            return -1;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            Node node2 = this.m_map[i];
            if (node2 != null && node2.equals(node)) {
                return i;
            }
        }
        return -1;
    }
}
