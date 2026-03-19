package gov.nist.javax.sip.message;

import gov.nist.javax.sip.header.SIPHeader;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class HeaderIterator implements ListIterator {
    private int index;
    private SIPHeader sipHeader;
    private SIPMessage sipMessage;
    private boolean toRemove;

    protected HeaderIterator(SIPMessage sIPMessage, SIPHeader sIPHeader) {
        this.sipMessage = sIPMessage;
        this.sipHeader = sIPHeader;
    }

    @Override
    public Object next() throws NoSuchElementException {
        if (this.sipHeader == null || this.index == 1) {
            throw new NoSuchElementException();
        }
        this.toRemove = true;
        this.index = 1;
        return this.sipHeader;
    }

    @Override
    public Object previous() throws NoSuchElementException {
        if (this.sipHeader == null || this.index == 0) {
            throw new NoSuchElementException();
        }
        this.toRemove = true;
        this.index = 0;
        return this.sipHeader;
    }

    @Override
    public int nextIndex() {
        return 1;
    }

    @Override
    public int previousIndex() {
        return this.index == 0 ? -1 : 0;
    }

    @Override
    public void set(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Object obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() throws IllegalStateException {
        if (this.sipHeader == null) {
            throw new IllegalStateException();
        }
        if (this.toRemove) {
            this.sipHeader = null;
            this.sipMessage.removeHeader(this.sipHeader.getName());
            return;
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean hasNext() {
        return this.index == 0;
    }

    @Override
    public boolean hasPrevious() {
        return this.index == 1;
    }
}
