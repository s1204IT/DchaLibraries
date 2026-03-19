package org.apache.xml.dtm.ref;

import javax.xml.transform.SourceLocator;

public class NodeLocator implements SourceLocator {
    protected int m_columnNumber;
    protected int m_lineNumber;
    protected String m_publicId;
    protected String m_systemId;

    public NodeLocator(String str, String str2, int i, int i2) {
        this.m_publicId = str;
        this.m_systemId = str2;
        this.m_lineNumber = i;
        this.m_columnNumber = i2;
    }

    @Override
    public String getPublicId() {
        return this.m_publicId;
    }

    @Override
    public String getSystemId() {
        return this.m_systemId;
    }

    @Override
    public int getLineNumber() {
        return this.m_lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return this.m_columnNumber;
    }

    public String toString() {
        return "file '" + this.m_systemId + "', line #" + this.m_lineNumber + ", column #" + this.m_columnNumber;
    }
}
