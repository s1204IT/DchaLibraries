package org.apache.xpath.objects;

import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;

public final class DTMXRTreeFrag {
    private DTM m_dtm;
    private int m_dtmIdentity;
    private XPathContext m_xctxt;

    public DTMXRTreeFrag(int i, XPathContext xPathContext) {
        this.m_dtmIdentity = -1;
        this.m_xctxt = xPathContext;
        this.m_dtmIdentity = i;
        this.m_dtm = xPathContext.getDTM(i);
    }

    public final void destruct() {
        this.m_dtm = null;
        this.m_xctxt = null;
    }

    final DTM getDTM() {
        return this.m_dtm;
    }

    public final int getDTMIdentity() {
        return this.m_dtmIdentity;
    }

    final XPathContext getXPathContext() {
        return this.m_xctxt;
    }

    public final int hashCode() {
        return this.m_dtmIdentity;
    }

    public final boolean equals(Object obj) {
        return (obj instanceof DTMXRTreeFrag) && this.m_dtmIdentity == ((DTMXRTreeFrag) obj).getDTMIdentity();
    }
}
