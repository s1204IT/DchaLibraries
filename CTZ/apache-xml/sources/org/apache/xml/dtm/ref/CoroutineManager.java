package org.apache.xml.dtm.ref;

import java.util.BitSet;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;

public class CoroutineManager {
    static final int ANYBODY = -1;
    static final int NOBODY = -1;
    static final int m_unreasonableId = 1024;
    BitSet m_activeIDs = new BitSet();
    Object m_yield = null;
    int m_nextCoroutine = -1;

    public synchronized int co_joinCoroutineSet(int i) {
        if (i >= 0) {
            if (i >= 1024 || this.m_activeIDs.get(i)) {
                return -1;
            }
        } else {
            i = 0;
            while (i < 1024 && this.m_activeIDs.get(i)) {
                i++;
            }
            if (i >= 1024) {
                return -1;
            }
        }
        this.m_activeIDs.set(i);
        return i;
    }

    public synchronized Object co_entry_pause(int i) throws NoSuchMethodException {
        if (!this.m_activeIDs.get(i)) {
            throw new NoSuchMethodException();
        }
        while (this.m_nextCoroutine != i) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        return this.m_yield;
    }

    public synchronized Object co_resume(Object obj, int i, int i2) throws NoSuchMethodException {
        if (!this.m_activeIDs.get(i2)) {
            throw new NoSuchMethodException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COROUTINE_NOT_AVAIL, new Object[]{Integer.toString(i2)}));
        }
        this.m_yield = obj;
        this.m_nextCoroutine = i2;
        notify();
        while (true) {
            if (this.m_nextCoroutine == i && this.m_nextCoroutine != -1 && this.m_nextCoroutine != -1) {
                break;
            }
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        if (this.m_nextCoroutine == -1) {
            co_exit(i);
            throw new NoSuchMethodException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COROUTINE_CO_EXIT, null));
        }
        return this.m_yield;
    }

    public synchronized void co_exit(int i) {
        this.m_activeIDs.clear(i);
        this.m_nextCoroutine = -1;
        notify();
    }

    public synchronized void co_exit_to(Object obj, int i, int i2) throws NoSuchMethodException {
        if (!this.m_activeIDs.get(i2)) {
            throw new NoSuchMethodException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COROUTINE_NOT_AVAIL, new Object[]{Integer.toString(i2)}));
        }
        this.m_yield = obj;
        this.m_nextCoroutine = i2;
        this.m_activeIDs.clear(i);
        notify();
    }
}
