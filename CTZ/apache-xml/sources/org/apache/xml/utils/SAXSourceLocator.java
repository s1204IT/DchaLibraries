package org.apache.xml.utils;

import java.io.Serializable;
import javax.xml.transform.SourceLocator;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

public class SAXSourceLocator extends LocatorImpl implements SourceLocator, Serializable {
    static final long serialVersionUID = 3181680946321164112L;
    Locator m_locator;

    public SAXSourceLocator() {
    }

    public SAXSourceLocator(Locator locator) {
        this.m_locator = locator;
        setColumnNumber(locator.getColumnNumber());
        setLineNumber(locator.getLineNumber());
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
    }

    public SAXSourceLocator(SourceLocator sourceLocator) {
        this.m_locator = null;
        setColumnNumber(sourceLocator.getColumnNumber());
        setLineNumber(sourceLocator.getLineNumber());
        setPublicId(sourceLocator.getPublicId());
        setSystemId(sourceLocator.getSystemId());
    }

    public SAXSourceLocator(SAXParseException sAXParseException) {
        setLineNumber(sAXParseException.getLineNumber());
        setColumnNumber(sAXParseException.getColumnNumber());
        setPublicId(sAXParseException.getPublicId());
        setSystemId(sAXParseException.getSystemId());
    }

    @Override
    public String getPublicId() {
        return this.m_locator == null ? super.getPublicId() : this.m_locator.getPublicId();
    }

    @Override
    public String getSystemId() {
        return this.m_locator == null ? super.getSystemId() : this.m_locator.getSystemId();
    }

    @Override
    public int getLineNumber() {
        return this.m_locator == null ? super.getLineNumber() : this.m_locator.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return this.m_locator == null ? super.getColumnNumber() : this.m_locator.getColumnNumber();
    }
}
