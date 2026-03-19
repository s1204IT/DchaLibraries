package org.apache.xml.dtm.ref;

import javax.xml.parsers.DocumentBuilder;
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
import org.w3c.dom.Document;
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

    public synchronized void addDTM(DTM dtm, int id) {
        addDTM(dtm, id, 0);
    }

    public synchronized void addDTM(DTM dtm, int id, int offset) {
        if (id >= 65536) {
            throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
        }
        int oldlen = this.m_dtms.length;
        if (oldlen <= id) {
            int newlen = Math.min(id + DTMFilter.SHOW_DOCUMENT, 65536);
            DTM[] new_m_dtms = new DTM[newlen];
            System.arraycopy(this.m_dtms, 0, new_m_dtms, 0, oldlen);
            this.m_dtms = new_m_dtms;
            int[] new_m_dtm_offsets = new int[newlen];
            System.arraycopy(this.m_dtm_offsets, 0, new_m_dtm_offsets, 0, oldlen);
            this.m_dtm_offsets = new_m_dtm_offsets;
        }
        this.m_dtms[id] = dtm;
        this.m_dtm_offsets[id] = offset;
        dtm.documentRegistration();
    }

    public synchronized int getFirstFreeDTMID() {
        int n = this.m_dtms.length;
        for (int i = 1; i < n; i++) {
            if (this.m_dtms[i] == null) {
                return i;
            }
        }
        return n;
    }

    @Override
    public synchronized DTM getDTM(Source source, boolean unique, DTMWSFilter whiteSpaceFilter, boolean incremental, boolean doIndexing) {
        InputSource xmlSource;
        XMLStringFactory xstringFactory = this.m_xsf;
        int dtmPos = getFirstFreeDTMID();
        int documentID = dtmPos << 16;
        if (source != 0 && (source instanceof DOMSource)) {
            DOM2DTM dtm = new DOM2DTM(this, source, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
            addDTM(dtm, dtmPos, 0);
            return dtm;
        }
        boolean z = source != 0 ? source instanceof SAXSource : true;
        boolean z2 = source != 0 ? source instanceof StreamSource : false;
        if (!z && !z2) {
            throw new DTMException(XMLMessages.createXMLMessage("ER_NOT_SUPPORTED", new Object[]{source}));
        }
        XMLReader reader = null;
        if (source != 0) {
            try {
                reader = getXMLReader(source);
                xmlSource = SAXSource.sourceToInputSource(source);
                String urlOfSource = xmlSource.getSystemId();
                if (urlOfSource != null) {
                    try {
                        urlOfSource = SystemIDResolver.getAbsoluteURI(urlOfSource);
                    } catch (Exception e) {
                        System.err.println("Can not absolutize URL: " + urlOfSource);
                    }
                    xmlSource.setSystemId(urlOfSource);
                }
            } finally {
                if (reader != null && (!this.m_incremental || !incremental)) {
                    reader.setContentHandler(this.m_defaultHandler);
                    reader.setDTDHandler(this.m_defaultHandler);
                    reader.setErrorHandler(this.m_defaultHandler);
                    try {
                        reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                    } catch (Exception e2) {
                    }
                }
                releaseXMLReader(reader);
            }
        }
        xmlSource = null;
        SAX2DTM dtm2 = (source != 0 || !unique || incremental || doIndexing) ? new SAX2DTM(this, source, documentID, whiteSpaceFilter, xstringFactory, doIndexing) : new SAX2RTFDTM(this, source, documentID, whiteSpaceFilter, xstringFactory, doIndexing);
        addDTM(dtm2, dtmPos, 0);
        boolean haveXercesParser = reader != null ? reader.getClass().getName().equals("org.apache.xerces.parsers.SAXParser") : false;
        if (haveXercesParser) {
            incremental = true;
        }
        if (!this.m_incremental || !incremental) {
            if (reader == null) {
                if (reader != null && (!this.m_incremental || !incremental)) {
                    reader.setContentHandler(this.m_defaultHandler);
                    reader.setDTDHandler(this.m_defaultHandler);
                    reader.setErrorHandler(this.m_defaultHandler);
                    try {
                        reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                    } catch (Exception e3) {
                    }
                }
                releaseXMLReader(reader);
                return dtm2;
            }
            reader.setContentHandler(dtm2);
            reader.setDTDHandler(dtm2);
            if (reader.getErrorHandler() == null) {
                reader.setErrorHandler(dtm2);
            }
            try {
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", dtm2);
            } catch (SAXNotRecognizedException e4) {
            } catch (SAXNotSupportedException e5) {
            }
            try {
                reader.parse(xmlSource);
                if (reader != null && (!this.m_incremental || !incremental)) {
                    reader.setContentHandler(this.m_defaultHandler);
                    reader.setDTDHandler(this.m_defaultHandler);
                    reader.setErrorHandler(this.m_defaultHandler);
                    try {
                        reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                    } catch (Exception e6) {
                    }
                }
                releaseXMLReader(reader);
                return dtm2;
            } catch (RuntimeException re) {
                dtm2.clearCoRoutine();
                throw re;
            } catch (Exception e7) {
                dtm2.clearCoRoutine();
                throw new WrappedRuntimeException(e7);
            }
        }
        IncrementalSAXSource coParser = null;
        if (haveXercesParser) {
            try {
                coParser = (IncrementalSAXSource) Class.forName("org.apache.xml.dtm.ref.IncrementalSAXSource_Xerces").newInstance();
            } catch (Exception ex) {
                ex.printStackTrace();
                coParser = null;
            }
        }
        if (coParser == null) {
            if (reader == null) {
                coParser = new IncrementalSAXSource_Filter();
            } else {
                IncrementalSAXSource_Filter filter = new IncrementalSAXSource_Filter();
                filter.setXMLReader(reader);
                coParser = filter;
            }
        }
        dtm2.setIncrementalSAXSource(coParser);
        if (xmlSource == null) {
            return dtm2;
        }
        if (reader.getErrorHandler() == null) {
            reader.setErrorHandler(dtm2);
        }
        reader.setDTDHandler(dtm2);
        try {
            coParser.startParse(xmlSource);
            if (reader != null) {
                reader.setContentHandler(this.m_defaultHandler);
                reader.setDTDHandler(this.m_defaultHandler);
                reader.setErrorHandler(this.m_defaultHandler);
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
            }
            releaseXMLReader(reader);
            return dtm2;
        } catch (RuntimeException re2) {
            dtm2.clearCoRoutine();
            throw re2;
        } catch (Exception e8) {
            dtm2.clearCoRoutine();
            throw new WrappedRuntimeException(e8);
        }
    }

    @Override
    public synchronized int getDTMHandleFromNode(Node node) {
        int handle;
        int handle2;
        if (node == 0) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NODE_NON_NULL, null));
        }
        if (node instanceof DTMNodeProxy) {
            return node.getDTMNodeNumber();
        }
        int max = this.m_dtms.length;
        for (int i = 0; i < max; i++) {
            DOM2DTM dom2dtm = this.m_dtms[i];
            if (dom2dtm != 0 && (dom2dtm instanceof DOM2DTM) && (handle2 = dom2dtm.getHandleOfNode(node)) != -1) {
                return handle2;
            }
        }
        Node node2 = node;
        for (Node p = node.getNodeType() == 2 ? ((Attr) node).getOwnerElement() : node.getParentNode(); p != null; p = p.getParentNode()) {
            node2 = p;
        }
        DOM2DTM dtm = (DOM2DTM) getDTM(new DOMSource(node2), false, null, true, true);
        if (node instanceof DOM2DTMdefaultNamespaceDeclarationNode) {
            int handle3 = dtm.getHandleOfNode(node.getOwnerElement());
            handle = dtm.getAttributeNode(handle3, node.getNamespaceURI(), node.getLocalName());
        } else {
            handle = dtm.getHandleOfNode(node);
        }
        if (-1 == handle) {
            throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COULD_NOT_RESOLVE_NODE, null));
        }
        return handle;
    }

    public synchronized XMLReader getXMLReader(Source source) {
        XMLReader xMLReader;
        try {
            xMLReader = source instanceof SAXSource ? source.getXMLReader() : null;
            if (xMLReader == null) {
                if (this.m_readerManager == null) {
                    this.m_readerManager = XMLReaderManager.getInstance();
                }
                xMLReader = this.m_readerManager.getXMLReader();
            }
        } catch (SAXException se) {
            throw new DTMException(se.getMessage(), se);
        }
        return xMLReader;
    }

    public synchronized void releaseXMLReader(XMLReader reader) {
        if (this.m_readerManager != null) {
            this.m_readerManager.releaseXMLReader(reader);
        }
    }

    @Override
    public synchronized DTM getDTM(int nodeHandle) {
        try {
        } catch (ArrayIndexOutOfBoundsException e) {
            if (nodeHandle == -1) {
                return null;
            }
            throw e;
        }
        return this.m_dtms[nodeHandle >>> 16];
    }

    @Override
    public synchronized int getDTMIdentity(DTM dtm) {
        if (dtm instanceof DTMDefaultBase) {
            if (dtm.getManager() != this) {
                return -1;
            }
            return dtm.getDTMIDs().elementAt(0);
        }
        int n = this.m_dtms.length;
        for (int i = 0; i < n; i++) {
            DTM tdtm = this.m_dtms[i];
            if (tdtm == dtm && this.m_dtm_offsets[i] == 0) {
                return i << 16;
            }
        }
        return -1;
    }

    @Override
    public synchronized boolean release(DTM dtm, boolean shouldHardDelete) {
        if (dtm instanceof SAX2DTM) {
            dtm.clearCoRoutine();
        }
        if (dtm instanceof DTMDefaultBase) {
            SuballocatedIntVector ids = dtm.getDTMIDs();
            for (int i = ids.size() - 1; i >= 0; i--) {
                this.m_dtms[ids.elementAt(i) >>> 16] = null;
            }
        } else {
            int i2 = getDTMIdentity(dtm);
            if (i2 >= 0) {
                this.m_dtms[i2 >>> 16] = null;
            }
        }
        dtm.documentRelease();
        return true;
    }

    @Override
    public synchronized DTM createDocumentFragment() {
        Node df;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            df = doc.createDocumentFragment();
        } catch (Exception e) {
            throw new DTMException(e);
        }
        return getDTM(new DOMSource(df), true, null, false, false);
    }

    @Override
    public synchronized DTMIterator createDTMIterator(int whatToShow, DTMFilter filter, boolean entityReferenceExpansion) {
        return null;
    }

    @Override
    public synchronized DTMIterator createDTMIterator(String xpathString, PrefixResolver presolver) {
        return null;
    }

    @Override
    public synchronized DTMIterator createDTMIterator(int node) {
        return null;
    }

    @Override
    public synchronized DTMIterator createDTMIterator(Object xpathCompiler, int pos) {
        return null;
    }

    public ExpandedNameTable getExpandedNameTable(DTM dtm) {
        return this.m_expandedNameTable;
    }
}
