package org.apache.xml.dtm.ref;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.dom2dtm.DOM2DTM;
import org.apache.xml.dtm.ref.dom2dtm.DOM2DTMdefaultNamespaceDeclarationNode;
import org.apache.xml.dtm.ref.sax2dtm.SAX2DTM;
import org.apache.xml.dtm.ref.sax2dtm.SAX2RTFDTM;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLReaderManager;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class DTMManagerDefault extends DTMManager {
    private static final boolean DEBUG = false;
    private static final boolean DUMPTREE = false;
    protected DTM[] m_dtms = new DTM[DTMFilter.SHOW_DOCUMENT];
    int[] m_dtm_offsets = new int[DTMFilter.SHOW_DOCUMENT];
    protected XMLReaderManager m_readerManager = null;
    protected DefaultHandler m_defaultHandler = new DefaultHandler();
    private ExpandedNameTable m_expandedNameTable = new ExpandedNameTable();

    public synchronized void addDTM(DTM dtm, int i) {
        addDTM(dtm, i, 0);
    }

    public synchronized void addDTM(DTM dtm, int i, int i2) {
        if (i >= 65536) {
            throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
        }
        int length = this.m_dtms.length;
        if (length <= i) {
            int iMin = Math.min(i + DTMFilter.SHOW_DOCUMENT, 65536);
            DTM[] dtmArr = new DTM[iMin];
            System.arraycopy(this.m_dtms, 0, dtmArr, 0, length);
            this.m_dtms = dtmArr;
            int[] iArr = new int[iMin];
            System.arraycopy(this.m_dtm_offsets, 0, iArr, 0, length);
            this.m_dtm_offsets = iArr;
        }
        this.m_dtms[i] = dtm;
        this.m_dtm_offsets[i] = i2;
        dtm.documentRegistration();
    }

    public synchronized int getFirstFreeDTMID() {
        int length = this.m_dtms.length;
        for (int i = 1; i < length; i++) {
            if (this.m_dtms[i] == null) {
                return i;
            }
        }
        return length;
    }

    @Override
    public synchronized DTM getDTM(Source source, boolean z, DTMWSFilter dTMWSFilter, boolean z2, boolean z3) {
        boolean z4;
        Throwable th;
        ?? r13;
        ?? r132;
        String absoluteURI;
        InputSource inputSource;
        ?? incrementalSAXSource_Filter;
        XMLStringFactory xMLStringFactory = this.m_xsf;
        int firstFreeDTMID = getFirstFreeDTMID();
        int i = firstFreeDTMID << 16;
        boolean z5 = false;
        if (source != null && (source instanceof DOMSource)) {
            DOM2DTM dom2dtm = new DOM2DTM(this, (DOMSource) source, i, dTMWSFilter, xMLStringFactory, z3);
            addDTM(dom2dtm, firstFreeDTMID, 0);
            return dom2dtm;
        }
        boolean z6 = source != null ? source instanceof SAXSource : true;
        boolean z7 = source != null ? source instanceof StreamSource : false;
        if (!z6 && !z7) {
            throw new DTMException(XMLMessages.createXMLMessage("ER_NOT_SUPPORTED", new Object[]{source}));
        }
        if (source == null) {
            r132 = 0;
            inputSource = null;
        } else {
            try {
                XMLReader xMLReader = getXMLReader(source);
                try {
                    InputSource inputSourceSourceToInputSource = SAXSource.sourceToInputSource(source);
                    String systemId = inputSourceSourceToInputSource.getSystemId();
                    if (systemId != null) {
                        try {
                            absoluteURI = SystemIDResolver.getAbsoluteURI(systemId);
                        } catch (Exception e) {
                            System.err.println("Can not absolutize URL: " + systemId);
                            absoluteURI = systemId;
                        }
                        inputSourceSourceToInputSource.setSystemId(absoluteURI);
                    }
                    r132 = xMLReader;
                    inputSource = inputSourceSourceToInputSource;
                } catch (Throwable th2) {
                    th = th2;
                    z4 = z2;
                    r132 = xMLReader;
                    th = th;
                    r13 = r132;
                    if (r13 != 0) {
                        r13.setContentHandler(this.m_defaultHandler);
                        r13.setDTDHandler(this.m_defaultHandler);
                        r13.setErrorHandler(this.m_defaultHandler);
                        try {
                            r13.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                        } catch (Exception e2) {
                        }
                    }
                    releaseXMLReader(r13);
                    throw th;
                }
            } catch (Throwable th3) {
                z4 = z2;
                th = th3;
                r13 = 0;
                if (r13 != 0) {
                }
                releaseXMLReader(r13);
                throw th;
            }
        }
        try {
            ?? sax2dtm = (source != null || !z || z2 || z3) ? new SAX2DTM(this, source, i, dTMWSFilter, xMLStringFactory, z3) : new SAX2RTFDTM(this, source, i, dTMWSFilter, xMLStringFactory, z3);
            addDTM(sax2dtm, firstFreeDTMID, 0);
            if (r132 != 0) {
                if (r132.getClass().getName().equals("org.apache.xerces.parsers.SAXParser")) {
                    z5 = true;
                }
            }
            z4 = z5 ? true : z2;
            try {
                if (this.m_incremental && z4) {
                    if (z5) {
                        try {
                            incrementalSAXSource_Filter = (IncrementalSAXSource) Class.forName("org.apache.xml.dtm.ref.IncrementalSAXSource_Xerces").newInstance();
                        } catch (Exception e3) {
                            e3.printStackTrace();
                            incrementalSAXSource_Filter = 0;
                        }
                        if (incrementalSAXSource_Filter == 0) {
                            if (r132 == 0) {
                                incrementalSAXSource_Filter = new IncrementalSAXSource_Filter();
                            } else {
                                incrementalSAXSource_Filter = new IncrementalSAXSource_Filter();
                                incrementalSAXSource_Filter.setXMLReader(r132);
                            }
                        }
                        sax2dtm.setIncrementalSAXSource(incrementalSAXSource_Filter);
                        if (inputSource != null) {
                            if (r132 != 0 && (!this.m_incremental || !z4)) {
                                r132.setContentHandler(this.m_defaultHandler);
                                r132.setDTDHandler(this.m_defaultHandler);
                                r132.setErrorHandler(this.m_defaultHandler);
                                try {
                                    r132.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                                } catch (Exception e4) {
                                }
                            }
                            releaseXMLReader(r132);
                            return sax2dtm;
                        }
                        if (r132.getErrorHandler() == null) {
                            r132.setErrorHandler(sax2dtm);
                        }
                        r132.setDTDHandler(sax2dtm);
                        try {
                            incrementalSAXSource_Filter.startParse(inputSource);
                        } catch (RuntimeException e5) {
                            sax2dtm.clearCoRoutine();
                            throw e5;
                        } catch (Exception e6) {
                            sax2dtm.clearCoRoutine();
                            throw new WrappedRuntimeException(e6);
                        }
                    } else {
                        incrementalSAXSource_Filter = 0;
                        if (incrementalSAXSource_Filter == 0) {
                        }
                        sax2dtm.setIncrementalSAXSource(incrementalSAXSource_Filter);
                        if (inputSource != null) {
                        }
                    }
                } else {
                    if (r132 == 0) {
                        if (r132 != 0 && (!this.m_incremental || !z4)) {
                            r132.setContentHandler(this.m_defaultHandler);
                            r132.setDTDHandler(this.m_defaultHandler);
                            r132.setErrorHandler(this.m_defaultHandler);
                            try {
                                r132.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                            } catch (Exception e7) {
                            }
                        }
                        releaseXMLReader(r132);
                        return sax2dtm;
                    }
                    r132.setContentHandler(sax2dtm);
                    r132.setDTDHandler(sax2dtm);
                    if (r132.getErrorHandler() == null) {
                        r132.setErrorHandler(sax2dtm);
                    }
                    try {
                        r132.setProperty("http://xml.org/sax/properties/lexical-handler", sax2dtm);
                    } catch (SAXNotRecognizedException e8) {
                    } catch (SAXNotSupportedException e9) {
                    }
                    try {
                        r132.parse(inputSource);
                    } catch (RuntimeException e10) {
                        sax2dtm.clearCoRoutine();
                        throw e10;
                    } catch (Exception e11) {
                        sax2dtm.clearCoRoutine();
                        throw new WrappedRuntimeException(e11);
                    }
                }
                if (r132 != 0 && (!this.m_incremental || !z4)) {
                    r132.setContentHandler(this.m_defaultHandler);
                    r132.setDTDHandler(this.m_defaultHandler);
                    r132.setErrorHandler(this.m_defaultHandler);
                    try {
                        r132.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                    } catch (Exception e12) {
                    }
                }
                releaseXMLReader(r132);
                return sax2dtm;
            } catch (Throwable th4) {
                th = th4;
                th = th;
                r13 = r132;
                if (r13 != 0 && (!this.m_incremental || !z4)) {
                    r13.setContentHandler(this.m_defaultHandler);
                    r13.setDTDHandler(this.m_defaultHandler);
                    r13.setErrorHandler(this.m_defaultHandler);
                    r13.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                }
                releaseXMLReader(r13);
                throw th;
            }
        } catch (Throwable th5) {
            th = th5;
            z4 = z2;
        }
    }

    @Override
    public synchronized int getDTMHandleFromNode(Node node) {
        int handleOfNode;
        int handleOfNode2;
        if (node == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NODE_NON_NULL, null));
        }
        if (node instanceof DTMNodeProxy) {
            return ((DTMNodeProxy) node).getDTMNodeNumber();
        }
        int length = this.m_dtms.length;
        for (int i = 0; i < length; i++) {
            DTM dtm = this.m_dtms[i];
            if (dtm != null && (dtm instanceof DOM2DTM) && (handleOfNode2 = ((DOM2DTM) dtm).getHandleOfNode(node)) != -1) {
                return handleOfNode2;
            }
        }
        Node node2 = node;
        for (Node ownerElement = node.getNodeType() == 2 ? ((Attr) node).getOwnerElement() : node.getParentNode(); ownerElement != null; ownerElement = ownerElement.getParentNode()) {
            node2 = ownerElement;
        }
        DOM2DTM dom2dtm = (DOM2DTM) getDTM(new DOMSource(node2), false, null, true, true);
        if (node instanceof DOM2DTMdefaultNamespaceDeclarationNode) {
            handleOfNode = dom2dtm.getAttributeNode(dom2dtm.getHandleOfNode(((Attr) node).getOwnerElement()), node.getNamespaceURI(), node.getLocalName());
        } else {
            handleOfNode = dom2dtm.getHandleOfNode(node);
        }
        if (-1 != handleOfNode) {
            return handleOfNode;
        }
        throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COULD_NOT_RESOLVE_NODE, null));
    }

    public synchronized XMLReader getXMLReader(Source source) {
        XMLReader xMLReader;
        try {
            xMLReader = source instanceof SAXSource ? ((SAXSource) source).getXMLReader() : null;
            if (xMLReader == null) {
                if (this.m_readerManager == null) {
                    this.m_readerManager = XMLReaderManager.getInstance();
                }
                xMLReader = this.m_readerManager.getXMLReader();
            }
        } catch (SAXException e) {
            throw new DTMException(e.getMessage(), e);
        }
        return xMLReader;
    }

    public synchronized void releaseXMLReader(XMLReader xMLReader) {
        if (this.m_readerManager != null) {
            this.m_readerManager.releaseXMLReader(xMLReader);
        }
    }

    @Override
    public synchronized DTM getDTM(int i) {
        try {
        } catch (ArrayIndexOutOfBoundsException e) {
            if (i == -1) {
                return null;
            }
            throw e;
        }
        return this.m_dtms[i >>> 16];
    }

    @Override
    public synchronized int getDTMIdentity(DTM dtm) {
        if (dtm instanceof DTMDefaultBase) {
            DTMDefaultBase dTMDefaultBase = (DTMDefaultBase) dtm;
            if (dTMDefaultBase.getManager() != this) {
                return -1;
            }
            return dTMDefaultBase.getDTMIDs().elementAt(0);
        }
        int length = this.m_dtms.length;
        for (int i = 0; i < length; i++) {
            if (this.m_dtms[i] == dtm && this.m_dtm_offsets[i] == 0) {
                return i << 16;
            }
        }
        return -1;
    }

    @Override
    public synchronized boolean release(DTM dtm, boolean z) {
        if (dtm instanceof SAX2DTM) {
            ((SAX2DTM) dtm).clearCoRoutine();
        }
        if (dtm instanceof DTMDefaultBase) {
            SuballocatedIntVector dTMIDs = ((DTMDefaultBase) dtm).getDTMIDs();
            for (int size = dTMIDs.size() - 1; size >= 0; size--) {
                this.m_dtms[dTMIDs.elementAt(size) >>> 16] = null;
            }
        } else {
            int dTMIdentity = getDTMIdentity(dtm);
            if (dTMIdentity >= 0) {
                this.m_dtms[dTMIdentity >>> 16] = null;
            }
        }
        dtm.documentRelease();
        return true;
    }

    @Override
    public synchronized DTM createDocumentFragment() {
        DocumentBuilderFactory documentBuilderFactoryNewInstance;
        try {
            documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
            documentBuilderFactoryNewInstance.setNamespaceAware(true);
        } catch (Exception e) {
            throw new DTMException(e);
        }
        return getDTM(new DOMSource(documentBuilderFactoryNewInstance.newDocumentBuilder().newDocument().createDocumentFragment()), true, null, false, false);
    }

    @Override
    public synchronized DTMIterator createDTMIterator(int i, DTMFilter dTMFilter, boolean z) {
        return null;
    }

    @Override
    public synchronized DTMIterator createDTMIterator(String str, PrefixResolver prefixResolver) {
        return null;
    }

    @Override
    public synchronized DTMIterator createDTMIterator(int i) {
        return null;
    }

    @Override
    public synchronized DTMIterator createDTMIterator(Object obj, int i) {
        return null;
    }

    public ExpandedNameTable getExpandedNameTable(DTM dtm) {
        return this.m_expandedNameTable;
    }
}
