package org.apache.xpath.axes;

import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xpath.XPathContext;

public class ReverseAxesWalker extends AxesWalker {
    static final long serialVersionUID = 2847007647832768941L;
    protected DTMAxisIterator m_iterator;

    ReverseAxesWalker(LocPathIterator locPathIterator, int i) {
        super(locPathIterator, i);
    }

    @Override
    public void setRoot(int i) {
        super.setRoot(i);
        this.m_iterator = getDTM(i).getAxisIterator(this.m_axis);
        this.m_iterator.setStartNode(i);
    }

    @Override
    public void detach() {
        this.m_iterator = null;
        super.detach();
    }

    @Override
    protected int getNextNode() {
        if (this.m_foundLast) {
            return -1;
        }
        int next = this.m_iterator.next();
        if (this.m_isFresh) {
            this.m_isFresh = false;
        }
        if (-1 == next) {
            this.m_foundLast = true;
        }
        return next;
    }

    @Override
    public boolean isReverseAxes() {
        return true;
    }

    @Override
    protected int getProximityPosition(int i) {
        if (i < 0) {
            return -1;
        }
        int i2 = this.m_proximityPositions[i];
        if (i2 <= 0) {
            AxesWalker lastUsedWalker = wi().getLastUsedWalker();
            try {
                ReverseAxesWalker reverseAxesWalker = (ReverseAxesWalker) clone();
                reverseAxesWalker.setRoot(getRoot());
                reverseAxesWalker.setPredicateCount(i);
                reverseAxesWalker.setPrevWalker(null);
                reverseAxesWalker.setNextWalker(null);
                wi().setLastUsedWalker(reverseAxesWalker);
                i2++;
                while (-1 != reverseAxesWalker.nextNode()) {
                    i2++;
                }
                this.m_proximityPositions[i] = i2;
            } catch (CloneNotSupportedException e) {
            } catch (Throwable th) {
                wi().setLastUsedWalker(lastUsedWalker);
                throw th;
            }
            wi().setLastUsedWalker(lastUsedWalker);
        }
        return i2;
    }

    @Override
    protected void countProximityPosition(int i) {
        if (i < this.m_proximityPositions.length) {
            this.m_proximityPositions[i] = r0[i] - 1;
        }
    }

    @Override
    public int getLastPos(XPathContext xPathContext) {
        AxesWalker lastUsedWalker = wi().getLastUsedWalker();
        int i = 0;
        try {
            ReverseAxesWalker reverseAxesWalker = (ReverseAxesWalker) clone();
            reverseAxesWalker.setRoot(getRoot());
            reverseAxesWalker.setPredicateCount(this.m_predicateIndex);
            reverseAxesWalker.setPrevWalker(null);
            reverseAxesWalker.setNextWalker(null);
            wi().setLastUsedWalker(reverseAxesWalker);
            while (-1 != reverseAxesWalker.nextNode()) {
                i++;
            }
        } catch (CloneNotSupportedException e) {
        } catch (Throwable th) {
            wi().setLastUsedWalker(lastUsedWalker);
            throw th;
        }
        wi().setLastUsedWalker(lastUsedWalker);
        return i;
    }

    @Override
    public boolean isDocOrdered() {
        return false;
    }
}
