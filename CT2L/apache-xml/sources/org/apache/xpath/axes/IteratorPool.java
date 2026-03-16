package org.apache.xpath.axes;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;

public final class IteratorPool implements Serializable {
    static final long serialVersionUID = -460927331149566998L;
    private final ArrayList m_freeStack = new ArrayList();
    private final DTMIterator m_orig;

    public IteratorPool(DTMIterator original) {
        this.m_orig = original;
    }

    public synchronized DTMIterator getInstanceOrThrow() throws CloneNotSupportedException {
        DTMIterator dTMIterator;
        if (this.m_freeStack.isEmpty()) {
            dTMIterator = (DTMIterator) this.m_orig.clone();
        } else {
            DTMIterator result = (DTMIterator) this.m_freeStack.remove(this.m_freeStack.size() - 1);
            dTMIterator = result;
        }
        return dTMIterator;
    }

    public synchronized DTMIterator getInstance() {
        DTMIterator dTMIterator;
        if (this.m_freeStack.isEmpty()) {
            try {
                dTMIterator = (DTMIterator) this.m_orig.clone();
            } catch (Exception ex) {
                throw new WrappedRuntimeException(ex);
            }
        } else {
            DTMIterator result = (DTMIterator) this.m_freeStack.remove(this.m_freeStack.size() - 1);
            dTMIterator = result;
        }
        return dTMIterator;
    }

    public synchronized void freeInstance(DTMIterator obj) {
        this.m_freeStack.add(obj);
    }
}
