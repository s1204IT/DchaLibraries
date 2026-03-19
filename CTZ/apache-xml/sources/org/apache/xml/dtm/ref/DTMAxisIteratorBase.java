package org.apache.xml.dtm.ref;

import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.utils.WrappedRuntimeException;

public abstract class DTMAxisIteratorBase implements DTMAxisIterator {
    protected int _markedNode;
    protected int _last = -1;
    protected int _position = 0;
    protected int _startNode = -1;
    protected boolean _includeSelf = false;
    protected boolean _isRestartable = true;

    @Override
    public int getStartNode() {
        return this._startNode;
    }

    @Override
    public DTMAxisIterator reset() {
        boolean z = this._isRestartable;
        this._isRestartable = true;
        setStartNode(this._startNode);
        this._isRestartable = z;
        return this;
    }

    public DTMAxisIterator includeSelf() {
        this._includeSelf = true;
        return this;
    }

    @Override
    public int getLast() {
        if (this._last == -1) {
            int i = this._position;
            setMark();
            reset();
            do {
                this._last++;
            } while (next() != -1);
            gotoMark();
            this._position = i;
        }
        return this._last;
    }

    @Override
    public int getPosition() {
        if (this._position == 0) {
            return 1;
        }
        return this._position;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    @Override
    public DTMAxisIterator cloneIterator() {
        try {
            DTMAxisIteratorBase dTMAxisIteratorBase = (DTMAxisIteratorBase) super.clone();
            dTMAxisIteratorBase._isRestartable = false;
            return dTMAxisIteratorBase;
        } catch (CloneNotSupportedException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected final int returnNode(int i) {
        this._position++;
        return i;
    }

    protected final DTMAxisIterator resetPosition() {
        this._position = 0;
        return this;
    }

    public boolean isDocOrdered() {
        return true;
    }

    public int getAxis() {
        return -1;
    }

    @Override
    public void setRestartable(boolean z) {
        this._isRestartable = z;
    }

    @Override
    public int getNodeByPosition(int i) {
        int next;
        if (i > 0) {
            if (isReverse()) {
                i = (getLast() - i) + 1;
            }
            do {
                next = next();
                if (next != -1) {
                }
            } while (i != getPosition());
            return next;
        }
        return -1;
    }
}
