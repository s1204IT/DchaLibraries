package mf.org.apache.xerces.util;

import mf.org.apache.xerces.xni.XMLLocator;
import org.xml.sax.Locator;
import org.xml.sax.ext.Locator2;

public final class SAXLocatorWrapper implements XMLLocator {
    private Locator fLocator = null;
    private Locator2 fLocator2 = null;

    public void setLocator(Locator locator) {
        this.fLocator = locator;
        if ((locator instanceof Locator2) || locator == null) {
            this.fLocator2 = (Locator2) locator;
        }
    }

    public Locator getLocator() {
        return this.fLocator;
    }

    @Override
    public String getPublicId() {
        if (this.fLocator != null) {
            return this.fLocator.getPublicId();
        }
        return null;
    }

    @Override
    public String getLiteralSystemId() {
        if (this.fLocator != null) {
            return this.fLocator.getSystemId();
        }
        return null;
    }

    @Override
    public String getBaseSystemId() {
        return null;
    }

    @Override
    public String getExpandedSystemId() {
        return getLiteralSystemId();
    }

    @Override
    public int getLineNumber() {
        if (this.fLocator != null) {
            return this.fLocator.getLineNumber();
        }
        return -1;
    }

    @Override
    public int getColumnNumber() {
        if (this.fLocator != null) {
            return this.fLocator.getColumnNumber();
        }
        return -1;
    }

    @Override
    public int getCharacterOffset() {
        return -1;
    }

    @Override
    public String getEncoding() {
        if (this.fLocator2 != null) {
            return this.fLocator2.getEncoding();
        }
        return null;
    }

    @Override
    public String getXMLVersion() {
        if (this.fLocator2 != null) {
            return this.fLocator2.getXMLVersion();
        }
        return null;
    }
}
