package mf.org.apache.xerces.util;

import mf.org.apache.xerces.xni.XMLLocator;
import org.xml.sax.ext.Locator2;

public class LocatorProxy implements Locator2 {
    private final XMLLocator fLocator;

    public LocatorProxy(XMLLocator locator) {
        this.fLocator = locator;
    }

    @Override
    public String getPublicId() {
        return this.fLocator.getPublicId();
    }

    @Override
    public String getSystemId() {
        return this.fLocator.getExpandedSystemId();
    }

    @Override
    public int getLineNumber() {
        return this.fLocator.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return this.fLocator.getColumnNumber();
    }

    @Override
    public String getXMLVersion() {
        return this.fLocator.getXMLVersion();
    }

    @Override
    public String getEncoding() {
        return this.fLocator.getEncoding();
    }
}
