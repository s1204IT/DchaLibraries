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
        boolean temp = this._isRestartable;
        this._isRestartable = true;
        setStartNode(this._startNode);
        this._isRestartable = temp;
        return this;
    }

    public DTMAxisIterator includeSelf() {
        this._includeSelf = true;
        return this;
    }

    @Override
    public int getLast() {
        if (this._last == -1) {
            int temp = this._position;
            setMark();
            reset();
            do {
                this._last++;
            } while (next() != -1);
            gotoMark();
            this._position = temp;
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
            DTMAxisIteratorBase clone = (DTMAxisIteratorBase) super.clone();
            clone._isRestartable = false;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected final int returnNode(int node) {
        this._position++;
        return node;
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
    public void setRestartable(boolean isRestartable) {
        this._isRestartable = isRestartable;
    }

    @Override
    public int getNodeByPosition(int position) {
        int node;
        if (position > 0) {
            int pos = isReverse() ? (getLast() - position) + 1 : position;
            do {
                node = next();
                if (node != -1) {
                }
            } while (pos != getPosition());
            return node;
        }
        return -1;
    }
}
