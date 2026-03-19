package org.apache.xpath.axes;

import java.util.Vector;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.utils.NodeVector;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;

public class NodeSequence extends XObject implements DTMIterator, Cloneable, PathComponent {
    static final long serialVersionUID = 3866261934726581044L;
    private IteratorCache m_cache;
    protected DTMManager m_dtmMgr;
    protected DTMIterator m_iter;
    protected int m_last;
    protected int m_next;

    protected NodeVector getVector() {
        if (this.m_cache != null) {
            return this.m_cache.getVector();
        }
        return null;
    }

    private IteratorCache getCache() {
        return this.m_cache;
    }

    protected void SetVector(NodeVector nodeVector) {
        setObject(nodeVector);
    }

    public boolean hasCache() {
        return getVector() != null;
    }

    private boolean cacheComplete() {
        if (this.m_cache == null) {
            return false;
        }
        return this.m_cache.isComplete();
    }

    private void markCacheComplete() {
        if (getVector() == null) {
            return;
        }
        this.m_cache.setCacheComplete(true);
    }

    public final void setIter(DTMIterator dTMIterator) {
        this.m_iter = dTMIterator;
    }

    public final DTMIterator getContainedIter() {
        return this.m_iter;
    }

    private NodeSequence(DTMIterator dTMIterator, int i, XPathContext xPathContext, boolean z) {
        this.m_last = -1;
        this.m_next = 0;
        setIter(dTMIterator);
        setRoot(i, xPathContext);
        setShouldCacheNodes(z);
    }

    public NodeSequence(Object obj) {
        super(obj);
        this.m_last = -1;
        this.m_next = 0;
        boolean z = obj instanceof NodeVector;
        if (z) {
            SetVector((NodeVector) obj);
        }
        if (obj != null) {
            assertion(z, "Must have a NodeVector as the object for NodeSequence!");
            if (obj instanceof DTMIterator) {
                DTMIterator dTMIterator = (DTMIterator) obj;
                setIter(dTMIterator);
                this.m_last = dTMIterator.getLength();
            }
        }
    }

    private NodeSequence(DTMManager dTMManager) {
        super(new NodeVector());
        this.m_last = -1;
        this.m_next = 0;
        this.m_last = 0;
        this.m_dtmMgr = dTMManager;
    }

    public NodeSequence() {
        this.m_last = -1;
        this.m_next = 0;
    }

    @Override
    public DTM getDTM(int i) {
        if (getDTMManager() != null) {
            return getDTMManager().getDTM(i);
        }
        assertion(false, "Can not get a DTM Unless a DTMManager has been set!");
        return null;
    }

    @Override
    public DTMManager getDTMManager() {
        return this.m_dtmMgr;
    }

    @Override
    public int getRoot() {
        if (this.m_iter != null) {
            return this.m_iter.getRoot();
        }
        return -1;
    }

    public void setRoot(int i, Object obj) {
        if (this.m_iter != null) {
            this.m_dtmMgr = ((XPathContext) obj).getDTMManager();
            this.m_iter.setRoot(i, obj);
            if (!this.m_iter.isDocOrdered()) {
                if (!hasCache()) {
                    setShouldCacheNodes(true);
                }
                runTo(-1);
                this.m_next = 0;
                return;
            }
            return;
        }
        assertion(false, "Can not setRoot on a non-iterated NodeSequence!");
    }

    @Override
    public void reset() {
        this.m_next = 0;
    }

    @Override
    public int getWhatToShow() {
        if (hasCache()) {
            return -17;
        }
        return this.m_iter.getWhatToShow();
    }

    @Override
    public boolean getExpandEntityReferences() {
        if (this.m_iter != null) {
            return this.m_iter.getExpandEntityReferences();
        }
        return true;
    }

    @Override
    public int nextNode() {
        NodeVector vector = getVector();
        if (vector != null) {
            if (this.m_next < vector.size()) {
                int iElementAt = vector.elementAt(this.m_next);
                this.m_next++;
                return iElementAt;
            }
            if (cacheComplete() || -1 != this.m_last || this.m_iter == null) {
                this.m_next++;
                return -1;
            }
        }
        if (this.m_iter == null) {
            return -1;
        }
        int iNextNode = this.m_iter.nextNode();
        if (-1 != iNextNode) {
            if (hasCache()) {
                if (this.m_iter.isDocOrdered()) {
                    getVector().addElement(iNextNode);
                    this.m_next++;
                } else if (addNodeInDocOrder(iNextNode) >= 0) {
                    this.m_next++;
                }
            } else {
                this.m_next++;
            }
        } else {
            markCacheComplete();
            this.m_last = this.m_next;
            this.m_next++;
        }
        return iNextNode;
    }

    @Override
    public int previousNode() {
        if (hasCache()) {
            if (this.m_next <= 0) {
                return -1;
            }
            this.m_next--;
            return item(this.m_next);
        }
        this.m_iter.previousNode();
        this.m_next = this.m_iter.getCurrentPos();
        return this.m_next;
    }

    @Override
    public void detach() {
        if (this.m_iter != null) {
            this.m_iter.detach();
        }
        super.detach();
    }

    @Override
    public void allowDetachToRelease(boolean z) {
        if (!z && !hasCache()) {
            setShouldCacheNodes(true);
        }
        if (this.m_iter != null) {
            this.m_iter.allowDetachToRelease(z);
        }
        super.allowDetachToRelease(z);
    }

    @Override
    public int getCurrentNode() {
        if (hasCache()) {
            int i = this.m_next - 1;
            NodeVector vector = getVector();
            if (i < 0 || i >= vector.size()) {
                return -1;
            }
            return vector.elementAt(i);
        }
        if (this.m_iter != null) {
            return this.m_iter.getCurrentNode();
        }
        return -1;
    }

    @Override
    public boolean isFresh() {
        return this.m_next == 0;
    }

    @Override
    public void setShouldCacheNodes(boolean z) {
        if (z) {
            if (!hasCache()) {
                SetVector(new NodeVector());
                return;
            }
            return;
        }
        SetVector(null);
    }

    @Override
    public boolean isMutable() {
        return hasCache();
    }

    @Override
    public int getCurrentPos() {
        return this.m_next;
    }

    @Override
    public void runTo(int i) {
        if (-1 == i) {
            int i2 = this.m_next;
            while (-1 != nextNode()) {
            }
            this.m_next = i2;
        } else {
            if (this.m_next == i) {
                return;
            }
            if (hasCache() && this.m_next < getVector().size()) {
                this.m_next = i;
                return;
            }
            if (getVector() == null && i < this.m_next) {
                while (this.m_next >= i && -1 != previousNode()) {
                }
            } else {
                while (this.m_next < i && -1 != nextNode()) {
                }
            }
        }
    }

    @Override
    public void setCurrentPos(int i) {
        runTo(i);
    }

    @Override
    public int item(int i) {
        setCurrentPos(i);
        int iNextNode = nextNode();
        this.m_next = i;
        return iNextNode;
    }

    @Override
    public void setItem(int i, int i2) {
        NodeVector vector = getVector();
        if (vector != null) {
            if (vector.elementAt(i2) != i && this.m_cache.useCount() > 1) {
                IteratorCache iteratorCache = new IteratorCache();
                try {
                    vector = (NodeVector) vector.clone();
                    iteratorCache.setVector(vector);
                    iteratorCache.setCacheComplete(true);
                    this.m_cache = iteratorCache;
                    super.setObject(vector);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
            vector.setElementAt(i, i2);
            this.m_last = vector.size();
            return;
        }
        this.m_iter.setItem(i, i2);
    }

    @Override
    public int getLength() {
        IteratorCache cache = getCache();
        if (cache == null) {
            if (-1 != this.m_last) {
                return this.m_last;
            }
            int length = this.m_iter.getLength();
            this.m_last = length;
            return length;
        }
        if (cache.isComplete()) {
            return cache.getVector().size();
        }
        if (!(this.m_iter instanceof NodeSetDTM)) {
            if (-1 == this.m_last) {
                int i = this.m_next;
                runTo(-1);
                this.m_next = i;
            }
            return this.m_last;
        }
        return this.m_iter.getLength();
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        NodeSequence nodeSequence = (NodeSequence) super.clone();
        nodeSequence.m_next = 0;
        if (this.m_cache != null) {
            this.m_cache.increaseUseCount();
        }
        return nodeSequence;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        NodeSequence nodeSequence = (NodeSequence) super.clone();
        if (this.m_iter != null) {
            nodeSequence.m_iter = (DTMIterator) this.m_iter.clone();
        }
        if (this.m_cache != null) {
            this.m_cache.increaseUseCount();
        }
        return nodeSequence;
    }

    @Override
    public boolean isDocOrdered() {
        if (this.m_iter != null) {
            return this.m_iter.isDocOrdered();
        }
        return true;
    }

    @Override
    public int getAxis() {
        if (this.m_iter != null) {
            return this.m_iter.getAxis();
        }
        assertion(false, "Can not getAxis from a non-iterated node sequence!");
        return 0;
    }

    @Override
    public int getAnalysisBits() {
        if (this.m_iter != null && (this.m_iter instanceof PathComponent)) {
            return ((PathComponent) this.m_iter).getAnalysisBits();
        }
        return 0;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
    }

    protected int addNodeInDocOrder(int i) {
        assertion(hasCache(), "addNodeInDocOrder must be done on a mutable sequence!");
        NodeVector vector = getVector();
        int size = vector.size();
        while (true) {
            size--;
            if (size < 0) {
                break;
            }
            int iElementAt = vector.elementAt(size);
            if (iElementAt != i) {
                if (!this.m_dtmMgr.getDTM(i).isNodeAfter(i, iElementAt)) {
                    break;
                }
            } else {
                size = -2;
                break;
            }
        }
        if (size != -2) {
            int i2 = size + 1;
            vector.insertElementAt(i, i2);
            return i2;
        }
        return -1;
    }

    @Override
    protected void setObject(Object obj) {
        if (!(obj instanceof NodeVector)) {
            if (obj instanceof IteratorCache) {
                IteratorCache iteratorCache = (IteratorCache) obj;
                this.m_cache = iteratorCache;
                this.m_cache.increaseUseCount();
                super.setObject(iteratorCache.getVector());
                return;
            }
            super.setObject(obj);
            return;
        }
        super.setObject(obj);
        NodeVector nodeVector = (NodeVector) obj;
        if (this.m_cache == null) {
            if (nodeVector != null) {
                this.m_cache = new IteratorCache();
                this.m_cache.setVector(nodeVector);
                return;
            }
            return;
        }
        this.m_cache.setVector(nodeVector);
    }

    private static final class IteratorCache {
        private NodeVector m_vec2 = null;
        private boolean m_isComplete2 = false;
        private int m_useCount2 = 1;

        IteratorCache() {
        }

        private int useCount() {
            return this.m_useCount2;
        }

        private void increaseUseCount() {
            if (this.m_vec2 != null) {
                this.m_useCount2++;
            }
        }

        private void setVector(NodeVector nodeVector) {
            this.m_vec2 = nodeVector;
            this.m_useCount2 = 1;
        }

        private NodeVector getVector() {
            return this.m_vec2;
        }

        private void setCacheComplete(boolean z) {
            this.m_isComplete2 = z;
        }

        private boolean isComplete() {
            return this.m_isComplete2;
        }
    }

    protected IteratorCache getIteratorCache() {
        return this.m_cache;
    }
}
