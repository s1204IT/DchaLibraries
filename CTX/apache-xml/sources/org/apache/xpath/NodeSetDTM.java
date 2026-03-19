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

    public NodeSetDTM(DTMManager dTMManager) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dTMManager;
    }

    public NodeSetDTM(int i, int i2, DTMManager dTMManager) {
        super(i);
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dTMManager;
    }

    public NodeSetDTM(NodeSetDTM nodeSetDTM) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = nodeSetDTM.getDTMManager();
        this.m_root = nodeSetDTM.getRoot();
        addNodes(nodeSetDTM);
    }

    public NodeSetDTM(DTMIterator dTMIterator) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dTMIterator.getDTMManager();
        this.m_root = dTMIterator.getRoot();
        addNodes(dTMIterator);
    }

    public NodeSetDTM(NodeIterator nodeIterator, XPathContext xPathContext) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = xPathContext.getDTMManager();
        while (true) {
            Node nodeNextNode = nodeIterator.nextNode();
            if (nodeNextNode != null) {
                addNodeInDocOrder(xPathContext.getDTMHandleFromNode(nodeNextNode), xPathContext);
            } else {
                return;
            }
        }
    }

    public NodeSetDTM(NodeList nodeList, XPathContext xPathContext) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = xPathContext.getDTMManager();
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            addNode(xPathContext.getDTMHandleFromNode(nodeList.item(i)));
        }
    }

    public NodeSetDTM(int i, DTMManager dTMManager) {
        this.m_next = 0;
        this.m_mutable = true;
        this.m_cacheNodes = true;
        this.m_root = -1;
        this.m_last = 0;
        this.m_manager = dTMManager;
        addNode(i);
    }

    public void setEnvironment(Object obj) {
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
    public void setRoot(int i, Object obj) {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NodeSetDTM) super.clone();
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        NodeSetDTM nodeSetDTM = (NodeSetDTM) clone();
        nodeSetDTM.reset();
        return nodeSetDTM;
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
    public DTM getDTM(int i) {
        return this.m_manager.getDTM(i);
    }

    @Override
    public DTMManager getDTMManager() {
        return this.m_manager;
    }

    @Override
    public int nextNode() {
        if (this.m_next < size()) {
            int iElementAt = elementAt(this.m_next);
            this.m_next++;
            return iElementAt;
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
    public void allowDetachToRelease(boolean z) {
    }

    @Override
    public boolean isFresh() {
        return this.m_next == 0;
    }

    @Override
    public void runTo(int i) {
        if (!this.m_cacheNodes) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_CANNOT_INDEX, null));
        }
        if (i >= 0 && this.m_next < this.m_firstFree) {
            this.m_next = i;
        } else {
            this.m_next = this.m_firstFree - 1;
        }
    }

    @Override
    public int item(int i) {
        runTo(i);
        return elementAt(i);
    }

    @Override
    public int getLength() {
        runTo(-1);
        return size();
    }

    public void addNode(int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        addElement(i);
    }

    public void insertNode(int i, int i2) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        insertElementAt(i, i2);
    }

    public void removeNode(int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        removeElement(i);
    }

    public void addNodes(DTMIterator dTMIterator) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        if (dTMIterator == null) {
            return;
        }
        while (true) {
            int iNextNode = dTMIterator.nextNode();
            if (-1 != iNextNode) {
                addElement(iNextNode);
            } else {
                return;
            }
        }
    }

    public void addNodesInDocOrder(DTMIterator dTMIterator, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        while (true) {
            int iNextNode = dTMIterator.nextNode();
            if (-1 != iNextNode) {
                addNodeInDocOrder(iNextNode, xPathContext);
            } else {
                return;
            }
        }
    }

    public int addNodeInDocOrder(int i, boolean z, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        if (z) {
            int size = size() - 1;
            while (true) {
                if (size < 0) {
                    break;
                }
                int iElementAt = elementAt(size);
                if (iElementAt != i) {
                    if (!xPathContext.getDTM(i).isNodeAfter(i, iElementAt)) {
                        break;
                    }
                    size--;
                } else {
                    size = -2;
                    break;
                }
            }
            if (size != -2) {
                int i2 = size + 1;
                insertElementAt(i, i2);
                return i2;
            }
            return -1;
        }
        int size2 = size();
        boolean z2 = false;
        int i3 = 0;
        while (true) {
            if (i3 >= size2) {
                break;
            }
            if (i3 != i) {
                i3++;
            } else {
                z2 = true;
                break;
            }
        }
        if (!z2) {
            addElement(i);
            return size2;
        }
        return size2;
    }

    public int addNodeInDocOrder(int i, XPathContext xPathContext) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        return addNodeInDocOrder(i, true, xPathContext);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public void addElement(int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.addElement(i);
    }

    @Override
    public void insertElementAt(int i, int i2) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.insertElementAt(i, i2);
    }

    @Override
    public void appendNodes(NodeVector nodeVector) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.appendNodes(nodeVector);
    }

    @Override
    public void removeAllElements() {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.removeAllElements();
    }

    @Override
    public boolean removeElement(int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        return super.removeElement(i);
    }

    @Override
    public void removeElementAt(int i) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.removeElementAt(i);
    }

    @Override
    public void setElementAt(int i, int i2) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.setElementAt(i, i2);
    }

    @Override
    public void setItem(int i, int i2) {
        if (!this.m_mutable) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_NOT_MUTABLE, null));
        }
        super.setElementAt(i, i2);
    }

    @Override
    public int elementAt(int i) {
        runTo(i);
        return super.elementAt(i);
    }

    @Override
    public boolean contains(int i) {
        runTo(-1);
        return super.contains(i);
    }

    @Override
    public int indexOf(int i, int i2) {
        runTo(-1);
        return super.indexOf(i, i2);
    }

    @Override
    public int indexOf(int i) {
        runTo(-1);
        return super.indexOf(i);
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
        int i = this.m_next;
        int i2 = this.m_next > 0 ? this.m_next - 1 : this.m_next;
        int iElementAt = i2 < this.m_firstFree ? elementAt(i2) : -1;
        this.m_next = i;
        return iElementAt;
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
    public boolean isMutable() {
        return this.m_mutable;
    }

    public int getLast() {
        return this.m_last;
    }

    public void setLast(int i) {
        this.m_last = i;
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
