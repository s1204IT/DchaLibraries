package org.apache.xml.dtm;

import javax.xml.transform.Source;
import org.apache.xml.dtm.ObjectFactory;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Node;

public abstract class DTMManager {
    public static final int IDENT_DTM_DEFAULT = -65536;
    public static final int IDENT_DTM_NODE_BITS = 16;
    public static final int IDENT_MAX_DTMS = 65536;
    public static final int IDENT_NODE_DEFAULT = 65535;
    private static boolean debug = false;
    private static String defaultClassName = "org.apache.xml.dtm.ref.DTMManagerDefault";
    private static final String defaultPropName = "org.apache.xml.dtm.DTMManager";
    protected XMLStringFactory m_xsf = null;
    public boolean m_incremental = false;
    public boolean m_source_location = false;

    public abstract DTMIterator createDTMIterator(int i);

    public abstract DTMIterator createDTMIterator(int i, DTMFilter dTMFilter, boolean z);

    public abstract DTMIterator createDTMIterator(Object obj, int i);

    public abstract DTMIterator createDTMIterator(String str, PrefixResolver prefixResolver);

    public abstract DTM createDocumentFragment();

    public abstract DTM getDTM(int i);

    public abstract DTM getDTM(Source source, boolean z, DTMWSFilter dTMWSFilter, boolean z2, boolean z3);

    public abstract int getDTMHandleFromNode(Node node);

    public abstract int getDTMIdentity(DTM dtm);

    public abstract boolean release(DTM dtm, boolean z);

    static {
        try {
            debug = System.getProperty("dtm.debug") != null;
        } catch (SecurityException e) {
        }
    }

    protected DTMManager() {
    }

    public XMLStringFactory getXMLStringFactory() {
        return this.m_xsf;
    }

    public void setXMLStringFactory(XMLStringFactory xMLStringFactory) {
        this.m_xsf = xMLStringFactory;
    }

    public static DTMManager newInstance(XMLStringFactory xMLStringFactory) throws DTMConfigurationException {
        try {
            DTMManager dTMManager = (DTMManager) ObjectFactory.createObject(defaultPropName, defaultClassName);
            if (dTMManager == null) {
                throw new DTMConfigurationException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DEFAULT_IMPL, null));
            }
            dTMManager.setXMLStringFactory(xMLStringFactory);
            return dTMManager;
        } catch (ObjectFactory.ConfigurationError e) {
            throw new DTMConfigurationException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DEFAULT_IMPL, null), e.getException());
        }
    }

    public boolean getIncremental() {
        return this.m_incremental;
    }

    public void setIncremental(boolean z) {
        this.m_incremental = z;
    }

    public boolean getSource_location() {
        return this.m_source_location;
    }

    public void setSource_location(boolean z) {
        this.m_source_location = z;
    }

    public int getDTMIdentityMask() {
        return IDENT_DTM_DEFAULT;
    }

    public int getNodeIdentityMask() {
        return IDENT_NODE_DEFAULT;
    }
}
