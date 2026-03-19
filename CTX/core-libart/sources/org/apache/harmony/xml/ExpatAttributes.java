package org.apache.harmony.xml;

import org.xml.sax.Attributes;

abstract class ExpatAttributes implements Attributes {
    private static final String CDATA = "CDATA";

    private static native int getIndex(long j, String str, String str2);

    private static native int getIndexForQName(long j, String str);

    private static native String getLocalName(long j, long j2, int i);

    private static native String getQName(long j, long j2, int i);

    private static native String getURI(long j, long j2, int i);

    private static native String getValue(long j, String str, String str2);

    private static native String getValueByIndex(long j, int i);

    private static native String getValueForQName(long j, String str);

    protected native void freeAttributes(long j);

    @Override
    public abstract int getLength();

    abstract long getParserPointer();

    public abstract long getPointer();

    ExpatAttributes() {
    }

    @Override
    public String getURI(int i) {
        if (i < 0 || i >= getLength()) {
            return null;
        }
        return getURI(getParserPointer(), getPointer(), i);
    }

    @Override
    public String getLocalName(int i) {
        if (i < 0 || i >= getLength()) {
            return null;
        }
        return getLocalName(getParserPointer(), getPointer(), i);
    }

    @Override
    public String getQName(int i) {
        if (i < 0 || i >= getLength()) {
            return null;
        }
        return getQName(getParserPointer(), getPointer(), i);
    }

    @Override
    public String getType(int i) {
        if (i < 0 || i >= getLength()) {
            return null;
        }
        return CDATA;
    }

    @Override
    public String getValue(int i) {
        if (i < 0 || i >= getLength()) {
            return null;
        }
        return getValueByIndex(getPointer(), i);
    }

    @Override
    public int getIndex(String str, String str2) {
        if (str == null) {
            throw new NullPointerException("uri == null");
        }
        if (str2 == null) {
            throw new NullPointerException("localName == null");
        }
        long pointer = getPointer();
        if (pointer == 0) {
            return -1;
        }
        return getIndex(pointer, str, str2);
    }

    @Override
    public int getIndex(String str) {
        if (str == null) {
            throw new NullPointerException("qName == null");
        }
        long pointer = getPointer();
        if (pointer == 0) {
            return -1;
        }
        return getIndexForQName(pointer, str);
    }

    @Override
    public String getType(String str, String str2) {
        if (str == null) {
            throw new NullPointerException("uri == null");
        }
        if (str2 == null) {
            throw new NullPointerException("localName == null");
        }
        if (getIndex(str, str2) == -1) {
            return null;
        }
        return CDATA;
    }

    @Override
    public String getType(String str) {
        if (getIndex(str) == -1) {
            return null;
        }
        return CDATA;
    }

    @Override
    public String getValue(String str, String str2) {
        if (str == null) {
            throw new NullPointerException("uri == null");
        }
        if (str2 == null) {
            throw new NullPointerException("localName == null");
        }
        long pointer = getPointer();
        if (pointer == 0) {
            return null;
        }
        return getValue(pointer, str, str2);
    }

    @Override
    public String getValue(String str) {
        if (str == null) {
            throw new NullPointerException("qName == null");
        }
        long pointer = getPointer();
        if (pointer == 0) {
            return null;
        }
        return getValueForQName(pointer, str);
    }
}
