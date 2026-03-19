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

    public NodeSet(int blocksize) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_last = 0;
        this.m_firstFree = 0;
        this.m_blocksize = blocksize;
        this.m_mapSize = 0;
    }

    public NodeSet(NodeList nodelist) {
        this(32);
        addNodes(nodelist);
    }

    public NodeSet(NodeSet nodelist) {
        this(32);
        addNodes((NodeIterator) nodelist);
    }

    public NodeSet(NodeIterator ni) {
        this(32);
        addNodes(ni);
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
        NodeSet clone = (NodeSet) clone();
        clone.reset();
        return clone;
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
            Node next = elementAt(this.m_next);
            this.m_next++;
            return next;
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
    public void runTo(int index) {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null));
        }
        if (index >= 0 && this.m_next < this.m_firstFree) {
            this.m_next = index;
        } else {
            this.m_next = this.m_firstFree - 1;
        }
    }

    @Override
    public Node item(int index) {
        runTo(index);
        return elementAt(index);
    }

    @Override
    public int getLength() {
        runTo(-1);
        return size();
    }

    public void addNode(Node n) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        addElement(n);
    }

    public void insertNode(Node n, int pos) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        insertElementAt(n, pos);
    }

    public void removeNode(Node n) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        removeElement(n);
    }

    public void addNodes(NodeList nodelist) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (nodelist == null) {
            return;
        }
        int nChildren = nodelist.getLength();
        for (int i = 0; i < nChildren; i++) {
            Node obj = nodelist.item(i);
            if (obj != null) {
                addElement(obj);
            }
        }
    }

    public void addNodes(NodeSet ns) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        addNodes((NodeIterator) ns);
    }

    public void addNodes(NodeIterator iterator) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (iterator == null) {
            return;
        }
        while (true) {
            Node obj = iterator.nextNode();
            if (obj == null) {
                return;
            } else {
                addElement(obj);
            }
        }
    }

    public void addNodesInDocOrder(NodeList nodelist, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        int nChildren = nodelist.getLength();
        for (int i = 0; i < nChildren; i++) {
            Node node = nodelist.item(i);
            if (node != null) {
                addNodeInDocOrder(node, support);
            }
        }
    }

    public void addNodesInDocOrder(NodeIterator iterator, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        while (true) {
            Node node = iterator.nextNode();
            if (node == null) {
                return;
            } else {
                addNodeInDocOrder(node, support);
            }
        }
    }

    private boolean addNodesInDocOrder(int start, int end, int testIndex, NodeList nodelist, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        Node node = nodelist.item(testIndex);
        int i = end;
        while (true) {
            if (i < start) {
                break;
            }
            Node child = elementAt(i);
            if (child == node) {
                i = -2;
                break;
            }
            if (DOM2Helper.isNodeAfter(node, child)) {
                i--;
            } else {
                insertElementAt(node, i + 1);
                int testIndex2 = testIndex - 1;
                if (testIndex2 > 0) {
                    boolean foundPrev = addNodesInDocOrder(0, i, testIndex2, nodelist, support);
                    if (!foundPrev) {
                        addNodesInDocOrder(i, size() - 1, testIndex2, nodelist, support);
                    }
                }
            }
        }
        if (i == -1) {
            insertElementAt(node, 0);
        }
        return false;
    }

    public int addNodeInDocOrder(Node node, boolean test, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (test) {
            int size = size();
            int i = size - 1;
            while (true) {
                if (i >= 0) {
                    Node child = elementAt(i);
                    if (child == node) {
                        i = -2;
                        break;
                    }
                    if (!DOM2Helper.isNodeAfter(node, child)) {
                        break;
                    }
                    i--;
                } else {
                    break;
                }
            }
            if (i == -2) {
                return -1;
            }
            int insertIndex = i + 1;
            insertElementAt(node, insertIndex);
            return insertIndex;
        }
        int insertIndex2 = size();
        boolean foundit = false;
        int i2 = 0;
        while (true) {
            if (i2 >= insertIndex2) {
                break;
            }
            if (!item(i2).equals(node)) {
                i2++;
            } else {
                foundit = true;
                break;
            }
        }
        if (!foundit) {
            addElement(node);
            return insertIndex2;
        }
        return insertIndex2;
    }

    public int addNodeInDocOrder(Node node, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        return addNodeInDocOrder(node, true, support);
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
        int saved = this.m_next;
        Node nodeElementAt = this.m_next < this.m_firstFree ? elementAt(this.m_next) : null;
        this.m_next = saved;
        return nodeElementAt;
    }

    public boolean getShouldCacheNodes() {
        return this.m_cacheNodes;
    }

    @Override
    public void setShouldCacheNodes(boolean b) {
        if (!isFresh()) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_CALL_SETSHOULDCACHENODE, null));
        }
        this.m_cacheNodes = b;
        this.m_mutable = true;
    }

    @Override
    public int getLast() {
        return this.m_last;
    }

    @Override
    public void setLast(int last) {
        this.m_last = last;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        NodeSet clone = (NodeSet) super.clone();
        if (this.m_map != null && this.m_map == clone.m_map) {
            clone.m_map = new Node[this.m_map.length];
            System.arraycopy(this.m_map, 0, clone.m_map, 0, this.m_map.length);
        }
        return clone;
    }

    @Override
    public int size() {
        return this.m_firstFree;
    }

    public void addElement(Node value) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            if (this.m_map == null) {
                this.m_map = new Node[this.m_blocksize];
                this.m_mapSize = this.m_blocksize;
            } else {
                this.m_mapSize += this.m_blocksize;
                Node[] newMap = new Node[this.m_mapSize];
                System.arraycopy(this.m_map, 0, newMap, 0, this.m_firstFree + 1);
                this.m_map = newMap;
            }
        }
        this.m_map[this.m_firstFree] = value;
        this.m_firstFree++;
    }

    public final void push(Node value) {
        int ff = this.m_firstFree;
        if (ff + 1 >= this.m_mapSize) {
            if (this.m_map == null) {
                this.m_map = new Node[this.m_blocksize];
                this.m_mapSize = this.m_blocksize;
            } else {
                this.m_mapSize += this.m_blocksize;
                Node[] newMap = new Node[this.m_mapSize];
                System.arraycopy(this.m_map, 0, newMap, 0, ff + 1);
                this.m_map = newMap;
            }
        }
        this.m_map[ff] = value;
        this.m_firstFree = ff + 1;
    }

    public final Node pop() {
        this.m_firstFree--;
        Node n = this.m_map[this.m_firstFree];
        this.m_map[this.m_firstFree] = null;
        return n;
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

    public final void pushPair(Node v1, Node v2) {
        if (this.m_map == null) {
            this.m_map = new Node[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        } else if (this.m_firstFree + 2 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Node[] newMap = new Node[this.m_mapSize];
            System.arraycopy(this.m_map, 0, newMap, 0, this.m_firstFree);
            this.m_map = newMap;
        }
        this.m_map[this.m_firstFree] = v1;
        this.m_map[this.m_firstFree + 1] = v2;
        this.m_firstFree += 2;
    }

    public final void popPair() {
        this.m_firstFree -= 2;
        this.m_map[this.m_firstFree] = null;
        this.m_map[this.m_firstFree + 1] = null;
    }

    public final void setTail(Node n) {
        this.m_map[this.m_firstFree - 1] = n;
    }

    public final void setTailSub1(Node n) {
        this.m_map[this.m_firstFree - 2] = n;
    }

    public final Node peepTail() {
        return this.m_map[this.m_firstFree - 1];
    }

    public final Node peepTailSub1() {
        return this.m_map[this.m_firstFree - 2];
    }

    public void insertElementAt(Node value, int at) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_map == null) {
            this.m_map = new Node[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        } else if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Node[] newMap = new Node[this.m_mapSize];
            System.arraycopy(this.m_map, 0, newMap, 0, this.m_firstFree + 1);
            this.m_map = newMap;
        }
        if (at <= this.m_firstFree - 1) {
            System.arraycopy(this.m_map, at, this.m_map, at + 1, this.m_firstFree - at);
        }
        this.m_map[at] = value;
        this.m_firstFree++;
    }

    public void appendNodes(NodeSet nodes) {
        int nNodes = nodes.size();
        if (this.m_map == null) {
            this.m_mapSize = this.m_blocksize + nNodes;
            this.m_map = new Node[this.m_mapSize];
        } else if (this.m_firstFree + nNodes >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + nNodes;
            Node[] newMap = new Node[this.m_mapSize];
            System.arraycopy(this.m_map, 0, newMap, 0, this.m_firstFree + nNodes);
            this.m_map = newMap;
        }
        System.arraycopy(nodes.m_map, 0, this.m_map, this.m_firstFree, nNodes);
        this.m_firstFree += nNodes;
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

    public boolean removeElement(Node s) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_map == null) {
            return false;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            Node node = this.m_map[i];
            if (node != null && node.equals(s)) {
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

    public void setElementAt(Node node, int index) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null));
        }
        if (this.m_map == null) {
            this.m_map = new Node[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        }
        this.m_map[index] = node;
    }

    public Node elementAt(int i) {
        if (this.m_map == null) {
            return null;
        }
        return this.m_map[i];
    }

    public boolean contains(Node s) {
        runTo(-1);
        if (this.m_map == null) {
            return false;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            Node node = this.m_map[i];
            if (node != null && node.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(Node elem, int index) {
        runTo(-1);
        if (this.m_map == null) {
            return -1;
        }
        for (int i = index; i < this.m_firstFree; i++) {
            Node node = this.m_map[i];
            if (node != null && node.equals(elem)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(Node elem) {
        runTo(-1);
        if (this.m_map == null) {
            return -1;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            Node node = this.m_map[i];
            if (node != null && node.equals(elem)) {
                return i;
            }
        }
        return -1;
    }
}
