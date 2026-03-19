package org.apache.xpath;

import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.utils.NodeVector;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

public class NodeSetDTM extends NodeVector implements DTMIterator, Cloneable {
    static final long serialVersionUID = 7686480133331317070L;
    protected transient boolean m_cacheNodes;
    private transient int m_last;
    DTMManager m_manager;
    protected transient boolean m_mutable;
    protected transient int m_next;
    protected int m_root;

    public NodeSetDTM(DTMManager dtmManager) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dtmManager;
    }

    public NodeSetDTM(int blocksize, int dummy, DTMManager dtmManager) {
        super(blocksize);
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dtmManager;
    }

    public NodeSetDTM(NodeSetDTM nodelist) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = nodelist.getDTMManager();
        this.m_root = nodelist.getRoot();
        addNodes(nodelist);
    }

    public NodeSetDTM(DTMIterator ni) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = ni.getDTMManager();
        this.m_root = ni.getRoot();
        addNodes(ni);
    }

    public NodeSetDTM(NodeIterator iterator, XPathContext xctxt) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = xctxt.getDTMManager();
        while (true) {
            Node node = iterator.nextNode();
            if (node == null) {
                return;
            }
            int handle = xctxt.getDTMHandleFromNode(node);
            addNodeInDocOrder(handle, xctxt);
        }
    }

    public NodeSetDTM(NodeList nodeList, XPathContext xctxt) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = xctxt.getDTMManager();
        int n = nodeList.getLength();
        for (int i = 0; i < n; i++) {
            Node node = nodeList.item(i);
            int handle = xctxt.getDTMHandleFromNode(node);
            addNode(handle);
        }
    }

    public NodeSetDTM(int node, DTMManager dtmManager) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dtmManager;
        addNode(node);
    }

    public void setEnvironment(Object environment) {
    }

    @Override
    public int getRoot() {
        if (-1 == this.m_root) {
            if (size() > 0) {
                return item(0);
            }
            return -1;
        }
        return this.m_root;
    }

    @Override
    public void setRoot(int context, Object environment) {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        NodeSetDTM clone = (NodeSetDTM) super.clone();
        return clone;
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        NodeSetDTM clone = (NodeSetDTM) clone();
        clone.reset();
        return clone;
    }

    @Override
    public void reset() {
        this.m_next = 0;
    }

    @Override
    public int getWhatToShow() {
        return -17;
    }

    public DTMFilter getFilter() {
        return null;
    }

    @Override
    public boolean getExpandEntityReferences() {
        return true;
    }

    @Override
    public DTM getDTM(int nodeHandle) {
        return this.m_manager.getDTM(nodeHandle);
    }

    @Override
    public DTMManager getDTMManager() {
        return this.m_manager;
    }

    @Override
    public int nextNode() {
        if (this.m_next < size()) {
            int next = elementAt(this.m_next);
            this.m_next++;
            return next;
        }
        return -1;
    }

    @Override
    public int previousNode() {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_CANNOT_ITERATE, null));
        }
        if (this.m_next - 1 > 0) {
            this.m_next--;
            return elementAt(this.m_next);
        }
        return -1;
    }

    @Override
    public void detach() {
    }

    @Override
    public void allowDetachToRelease(boolean allowRelease) {
    }

    @Override
    public boolean isFresh() {
        return this.m_next == 0;
    }

    @Override
    public void runTo(int index) {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_CANNOT_INDEX, null));
        }
        if (index >= 0 && this.m_next < this.m_firstFree) {
            this.m_next = index;
        } else {
            this.m_next = this.m_firstFree - 1;
        }
    }

    @Override
    public int item(int index) {
        runTo(index);
        return elementAt(index);
    }

    @Override
    public int getLength() {
        runTo(-1);
        return size();
    }

    public void addNode(int n) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        addElement(n);
    }

    public void insertNode(int n, int pos) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        insertElementAt(n, pos);
    }

    public void removeNode(int n) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        removeElement(n);
    }

    public void addNodes(DTMIterator iterator) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        if (iterator == null) {
            return;
        }
        while (true) {
            int obj = iterator.nextNode();
            if (-1 == obj) {
                return;
            } else {
                addElement(obj);
            }
        }
    }

    public void addNodesInDocOrder(DTMIterator iterator, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        while (true) {
            int node = iterator.nextNode();
            if (-1 == node) {
                return;
            } else {
                addNodeInDocOrder(node, support);
            }
        }
    }

    public int addNodeInDocOrder(int node, boolean test, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        if (test) {
            int size = size();
            int i = size - 1;
            while (true) {
                if (i >= 0) {
                    int child = elementAt(i);
                    if (child == node) {
                        i = -2;
                        break;
                    }
                    DTM dtm = support.getDTM(node);
                    if (!dtm.isNodeAfter(node, child)) {
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
            if (i2 != node) {
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

    public int addNodeInDocOrder(int node, XPathContext support) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        return addNodeInDocOrder(node, true, support);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public void addElement(int value) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.addElement(value);
    }

    @Override
    public void insertElementAt(int value, int at) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.insertElementAt(value, at);
    }

    @Override
    public void appendNodes(NodeVector nodes) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.appendNodes(nodes);
    }

    @Override
    public void removeAllElements() {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.removeAllElements();
    }

    @Override
    public boolean removeElement(int s) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        return super.removeElement(s);
    }

    @Override
    public void removeElementAt(int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.removeElementAt(i);
    }

    @Override
    public void setElementAt(int node, int index) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.setElementAt(node, index);
    }

    @Override
    public void setItem(int node, int index) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.setElementAt(node, index);
    }

    @Override
    public int elementAt(int i) {
        runTo(i);
        return super.elementAt(i);
    }

    @Override
    public boolean contains(int s) {
        runTo(-1);
        return super.contains(s);
    }

    @Override
    public int indexOf(int elem, int index) {
        runTo(-1);
        return super.indexOf(elem, index);
    }

    @Override
    public int indexOf(int elem) {
        runTo(-1);
        return super.indexOf(elem);
    }

    @Override
    public int getCurrentPos() {
        return this.m_next;
    }

    @Override
    public void setCurrentPos(int i) {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_CANNOT_INDEX, null));
        }
        this.m_next = i;
    }

    @Override
    public int getCurrentNode() {
        if (!this.m_cacheNodes) {
            throw new RuntimeException("This NodeSetDTM can not do indexing or counting functions!");
        }
        int saved = this.m_next;
        int current = this.m_next > 0 ? this.m_next - 1 : this.m_next;
        int n = current < this.m_firstFree ? elementAt(current) : -1;
        this.m_next = saved;
        return n;
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
    public boolean isMutable() {
        return this.m_mutable;
    }

    public int getLast() {
        return this.m_last;
    }

    public void setLast(int last) {
        this.m_last = last;
    }

    @Override
    public boolean isDocOrdered() {
        return true;
    }

    @Override
    public int getAxis() {
        return -1;
    }
}
