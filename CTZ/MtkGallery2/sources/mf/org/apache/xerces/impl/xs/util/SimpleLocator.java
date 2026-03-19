package mf.org.apache.xerces.impl.xs.util;

import mf.org.apache.xerces.xni.XMLLocator;

public final class SimpleLocator implements XMLLocator {
    private int charOffset;
    private int column;
    private String esid;
    private int line;
    private String lsid;

    public SimpleLocator() {
    }

    public SimpleLocator(String lsid, String esid, int line, int column) {
        this(lsid, esid, line, column, -1);
    }

    public void setValues(String lsid, String esid, int line, int column) {
        setValues(lsid, esid, line, column, -1);
    }

    public SimpleLocator(String lsid, String esid, int line, int column, int offset) {
        this.line = line;
        this.column = column;
        this.lsid = lsid;
        this.esid = esid;
        this.charOffset = offset;
    }

    public void setValues(String lsid, String esid, int line, int column, int offset) {
        this.line = line;
        this.column = column;
        this.lsid = lsid;
        this.esid = esid;
        this.charOffset = offset;
    }

    @Override
    public int getLineNumber() {
        return this.line;
    }

    @Override
    public int getColumnNumber() {
        return this.column;
    }

    @Override
    public int getCharacterOffset() {
        return this.charOffset;
    }

    @Override
    public String getPublicId() {
        return null;
    }

    @Override
    public String getExpandedSystemId() {
        return this.esid;
    }

    @Override
    public String getLiteralSystemId() {
        return this.lsid;
    }

    @Override
    public String getBaseSystemId() {
        return null;
    }

    public void setColumnNumber(int col) {
        this.column = col;
    }

    public void setLineNumber(int line) {
        this.line = line;
    }

    public void setCharacterOffset(int offset) {
        this.charOffset = offset;
    }

    public void setBaseSystemId(String systemId) {
    }

    public void setExpandedSystemId(String systemId) {
        this.esid = systemId;
    }

    public void setLiteralSystemId(String systemId) {
        this.lsid = systemId;
    }

    public void setPublicId(String publicId) {
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public String getXMLVersion() {
        return null;
    }
}
