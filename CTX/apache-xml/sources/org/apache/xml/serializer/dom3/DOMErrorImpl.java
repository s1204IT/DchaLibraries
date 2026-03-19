package org.apache.xml.serializer.dom3;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMLocator;

public final class DOMErrorImpl implements DOMError {
    private Exception fException;
    private DOMLocatorImpl fLocation;
    private String fMessage;
    private Object fRelatedData;
    private short fSeverity;
    private String fType;

    DOMErrorImpl() {
        this.fSeverity = (short) 1;
        this.fMessage = null;
        this.fException = null;
        this.fLocation = new DOMLocatorImpl();
    }

    public DOMErrorImpl(short s, String str, String str2) {
        this.fSeverity = (short) 1;
        this.fMessage = null;
        this.fException = null;
        this.fLocation = new DOMLocatorImpl();
        this.fSeverity = s;
        this.fMessage = str;
        this.fType = str2;
    }

    public DOMErrorImpl(short s, String str, String str2, Exception exc) {
        this.fSeverity = (short) 1;
        this.fMessage = null;
        this.fException = null;
        this.fLocation = new DOMLocatorImpl();
        this.fSeverity = s;
        this.fMessage = str;
        this.fType = str2;
        this.fException = exc;
    }

    public DOMErrorImpl(short s, String str, String str2, Exception exc, Object obj, DOMLocatorImpl dOMLocatorImpl) {
        this.fSeverity = (short) 1;
        this.fMessage = null;
        this.fException = null;
        this.fLocation = new DOMLocatorImpl();
        this.fSeverity = s;
        this.fMessage = str;
        this.fType = str2;
        this.fException = exc;
        this.fRelatedData = obj;
        this.fLocation = dOMLocatorImpl;
    }

    @Override
    public short getSeverity() {
        return this.fSeverity;
    }

    @Override
    public String getMessage() {
        return this.fMessage;
    }

    @Override
    public DOMLocator getLocation() {
        return this.fLocation;
    }

    @Override
    public Object getRelatedException() {
        return this.fException;
    }

    @Override
    public String getType() {
        return this.fType;
    }

    @Override
    public Object getRelatedData() {
        return this.fRelatedData;
    }

    public void reset() {
        this.fSeverity = (short) 1;
        this.fException = null;
        this.fMessage = null;
        this.fType = null;
        this.fRelatedData = null;
        this.fLocation = null;
    }
}
