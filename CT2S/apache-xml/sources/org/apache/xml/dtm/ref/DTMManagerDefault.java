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
        int i;
        int n = this.m_dtms.length;
        i = 1;
        while (true) {
            if (i >= n) {
                i = n;
                break;
            }
            if (this.m_dtms[i] == null) {
                break;
            }
            i++;
        }
        return i;
    }

    @Override
    public synchronized DTM getDTM(Source source, boolean z, DTMWSFilter dTMWSFilter, boolean z2, boolean z3) {
        InputSource inputSourceSourceToInputSource;
        DTM dtm;
        XMLStringFactory xMLStringFactory = this.m_xsf;
        int firstFreeDTMID = getFirstFreeDTMID();
        int i = firstFreeDTMID << 16;
        if (source == null || !(source instanceof DOMSource)) {
            boolean z4 = source != null ? source instanceof SAXSource : true;
            boolean z5 = source != null ? source instanceof StreamSource : false;
            if (!z4 && !z5) {
                throw new DTMException(XMLMessages.createXMLMessage("ER_NOT_SUPPORTED", new Object[]{source}));
            }
            XMLReader xMLReader = null;
            if (source == null) {
                inputSourceSourceToInputSource = null;
            } else {
                try {
                    xMLReader = getXMLReader(source);
                    inputSourceSourceToInputSource = SAXSource.sourceToInputSource(source);
                    String systemId = inputSourceSourceToInputSource.getSystemId();
                    if (systemId != null) {
                        try {
                            systemId = SystemIDResolver.getAbsoluteURI(systemId);
                        } catch (Exception e) {
                            System.err.println("Can not absolutize URL: " + systemId);
                        }
                        inputSourceSourceToInputSource.setSystemId(systemId);
                    }
                } finally {
                    if (xMLReader != null && (!this.m_incremental || !z2)) {
                        xMLReader.setContentHandler(this.m_defaultHandler);
                        xMLReader.setDTDHandler(this.m_defaultHandler);
                        xMLReader.setErrorHandler(this.m_defaultHandler);
                        try {
                            xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                        } catch (Exception e2) {
                        }
                    }
                    releaseXMLReader(xMLReader);
                }
            }
            SAX2DTM sax2dtm = (source != null || !z || z2 || z3) ? new SAX2DTM(this, source, i, dTMWSFilter, xMLStringFactory, z3) : new SAX2RTFDTM(this, source, i, dTMWSFilter, xMLStringFactory, z3);
            addDTM(sax2dtm, firstFreeDTMID, 0);
            boolean z6 = xMLReader != null && xMLReader.getClass().getName().equals("org.apache.xerces.parsers.SAXParser");
            if (z6) {
                z2 = true;
            }
            if (this.m_incremental && z2) {
                IncrementalSAXSource incrementalSAXSource_Filter = null;
                if (z6) {
                    try {
                        incrementalSAXSource_Filter = (IncrementalSAXSource) Class.forName("org.apache.xml.dtm.ref.IncrementalSAXSource_Xerces").newInstance();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                        incrementalSAXSource_Filter = null;
                    }
                    if (incrementalSAXSource_Filter == null) {
                        if (xMLReader == null) {
                            incrementalSAXSource_Filter = new IncrementalSAXSource_Filter();
                        } else {
                            IncrementalSAXSource_Filter incrementalSAXSource_Filter2 = new IncrementalSAXSource_Filter();
                            incrementalSAXSource_Filter2.setXMLReader(xMLReader);
                            incrementalSAXSource_Filter = incrementalSAXSource_Filter2;
                        }
                    }
                    sax2dtm.setIncrementalSAXSource(incrementalSAXSource_Filter);
                    if (inputSourceSourceToInputSource == null) {
                        if (xMLReader.getErrorHandler() == null) {
                            xMLReader.setErrorHandler(sax2dtm);
                        }
                        xMLReader.setDTDHandler(sax2dtm);
                        try {
                            incrementalSAXSource_Filter.startParse(inputSourceSourceToInputSource);
                            if (xMLReader != null) {
                                xMLReader.setContentHandler(this.m_defaultHandler);
                                xMLReader.setDTDHandler(this.m_defaultHandler);
                                xMLReader.setErrorHandler(this.m_defaultHandler);
                                xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                            }
                            releaseXMLReader(xMLReader);
                            dtm = sax2dtm;
                        } catch (RuntimeException e4) {
                            sax2dtm.clearCoRoutine();
                            throw e4;
                        } catch (Exception e5) {
                            sax2dtm.clearCoRoutine();
                            throw new WrappedRuntimeException(e5);
                        }
                    }
                } else {
                    if (incrementalSAXSource_Filter == null) {
                    }
                    sax2dtm.setIncrementalSAXSource(incrementalSAXSource_Filter);
                    if (inputSourceSourceToInputSource == null) {
                    }
                }
            } else if (xMLReader == null) {
                if (xMLReader != null && (!this.m_incremental || !z2)) {
                    xMLReader.setContentHandler(this.m_defaultHandler);
                    xMLReader.setDTDHandler(this.m_defaultHandler);
                    xMLReader.setErrorHandler(this.m_defaultHandler);
                    try {
                        xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                    } catch (Exception e6) {
                    }
                }
                releaseXMLReader(xMLReader);
                dtm = sax2dtm;
            } else {
                xMLReader.setContentHandler(sax2dtm);
                xMLReader.setDTDHandler(sax2dtm);
                if (xMLReader.getErrorHandler() == null) {
                    xMLReader.setErrorHandler(sax2dtm);
                }
                try {
                    xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", sax2dtm);
                } catch (SAXNotRecognizedException e7) {
                } catch (SAXNotSupportedException e8) {
                }
                try {
                    xMLReader.parse(inputSourceSourceToInputSource);
                    if (xMLReader != null && (!this.m_incremental || !z2)) {
                        xMLReader.setContentHandler(this.m_defaultHandler);
                        xMLReader.setDTDHandler(this.m_defaultHandler);
                        xMLReader.setErrorHandler(this.m_defaultHandler);
                        try {
                            xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", null);
                        } catch (Exception e9) {
                        }
                    }
                    releaseXMLReader(xMLReader);
                    dtm = sax2dtm;
                } catch (RuntimeException e10) {
                    sax2dtm.clearCoRoutine();
                    throw e10;
                } catch (Exception e11) {
                    sax2dtm.clearCoRoutine();
                    throw new WrappedRuntimeException(e11);
                }
            }
        } else {
            DTM dom2dtm = new DOM2DTM(this, (DOMSource) source, i, dTMWSFilter, xMLStringFactory, z3);
            addDTM(dom2dtm, firstFreeDTMID, 0);
            dtm = dom2dtm;
        }
        return dtm;
    }

    @Override
    public synchronized int getDTMHandleFromNode(Node node) {
        int handle;
        if (node == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NODE_NON_NULL, null));
        }
        if (node instanceof DTMNodeProxy) {
            handle = ((DTMNodeProxy) node).getDTMNodeNumber();
        } else {
            int max = this.m_dtms.length;
            int i = 0;
            while (true) {
                if (i < max) {
                    DTM thisDTM = this.m_dtms[i];
                    if (thisDTM != null && (thisDTM instanceof DOM2DTM) && (handle = ((DOM2DTM) thisDTM).getHandleOfNode(node)) != -1) {
                        break;
                    }
                    i++;
                } else {
                    Node root = node;
                    for (Node p = root.getNodeType() == 2 ? ((Attr) root).getOwnerElement() : root.getParentNode(); p != null; p = p.getParentNode()) {
                        root = p;
                    }
                    DOM2DTM dtm = (DOM2DTM) getDTM(new DOMSource(root), false, null, true, true);
                    if (node instanceof DOM2DTMdefaultNamespaceDeclarationNode) {
                        int handle2 = dtm.getHandleOfNode(((Attr) node).getOwnerElement());
                        handle = dtm.getAttributeNode(handle2, node.getNamespaceURI(), node.getLocalName());
                    } else {
                        handle = dtm.getHandleOfNode(node);
                    }
                    if (-1 == handle) {
                        throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_COULD_NOT_RESOLVE_NODE, null));
                    }
                }
            }
        }
        return handle;
    }

    public synchronized XMLReader getXMLReader(Source inputSource) {
        XMLReader reader;
        try {
            reader = inputSource instanceof SAXSource ? ((SAXSource) inputSource).getXMLReader() : null;
            if (reader == null) {
                if (this.m_readerManager == null) {
                    this.m_readerManager = XMLReaderManager.getInstance();
                }
                reader = this.m_readerManager.getXMLReader();
            }
        } catch (SAXException se) {
            throw new DTMException(se.getMessage(), se);
        }
        return reader;
    }

    public synchronized void releaseXMLReader(XMLReader reader) {
        if (this.m_readerManager != null) {
            this.m_readerManager.releaseXMLReader(reader);
        }
    }

    @Override
    public synchronized DTM getDTM(int nodeHandle) {
        DTM dtm;
        try {
            dtm = this.m_dtms[nodeHandle >>> 16];
        } catch (ArrayIndexOutOfBoundsException e) {
            if (nodeHandle == -1) {
                dtm = null;
            } else {
                throw e;
            }
        }
        return dtm;
    }

    @Override
    public synchronized int getDTMIdentity(DTM dtm) {
        int iElementAt = -1;
        synchronized (this) {
            if (dtm instanceof DTMDefaultBase) {
                DTMDefaultBase dtmdb = (DTMDefaultBase) dtm;
                if (dtmdb.getManager() == this) {
                    iElementAt = dtmdb.getDTMIDs().elementAt(0);
                }
            } else {
                int n = this.m_dtms.length;
                int i = 0;
                while (true) {
                    if (i >= n) {
                        break;
                    }
                    DTM tdtm = this.m_dtms[i];
                    if (tdtm != dtm || this.m_dtm_offsets[i] != 0) {
                        i++;
                    } else {
                        iElementAt = i << 16;
                        break;
                    }
                }
            }
        }
        return iElementAt;
    }

    @Override
    public synchronized boolean release(DTM dtm, boolean shouldHardDelete) {
        if (dtm instanceof SAX2DTM) {
            ((SAX2DTM) dtm).clearCoRoutine();
        }
        if (dtm instanceof DTMDefaultBase) {
            SuballocatedIntVector ids = ((DTMDefaultBase) dtm).getDTMIDs();
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
