package org.apache.xpath.objects;

import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;

public class XNull extends XNodeSet {
    static final long serialVersionUID = -6841683711458983005L;

    @Override
    public int getType() {
        return -1;
    }

    @Override
    public String getTypeString() {
        return "#CLASS_NULL";
    }

    @Override
    public double num() {
        return XPath.MATCH_SCORE_QNAME;
    }

    @Override
    public boolean bool() {
        return false;
    }

    @Override
    public String str() {
        return "";
    }

    @Override
    public int rtf(XPathContext xPathContext) {
        return -1;
    }

    @Override
    public boolean equals(XObject xObject) {
        return xObject.getType() == -1;
    }
}
