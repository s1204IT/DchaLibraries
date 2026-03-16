package org.apache.xml.dtm.ref.sax2dtm;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.DTMDefaultBaseIterators;
import org.apache.xml.dtm.ref.DTMManagerDefault;
import org.apache.xml.dtm.ref.DTMStringPool;
import org.apache.xml.dtm.ref.DTMTreeWalker;
import org.apache.xml.dtm.ref.IncrementalSAXSource;
import org.apache.xml.dtm.ref.IncrementalSAXSource_Filter;
import org.apache.xml.dtm.ref.NodeLocator;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.IntStack;
import org.apache.xml.utils.IntVector;
import org.apache.xml.utils.StringVector;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.apache.xpath.compiler.PsuedoNames;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class SAX2DTM extends DTMDefaultBaseIterators implements EntityResolver, DTDHandler, ContentHandler, ErrorHandler, DeclHandler, LexicalHandler {
    private static final boolean DEBUG = false;
    private static final int ENTITY_FIELDS_PER = 4;
    private static final int ENTITY_FIELD_NAME = 3;
    private static final int ENTITY_FIELD_NOTATIONNAME = 2;
    private static final int ENTITY_FIELD_PUBLICID = 0;
    private static final int ENTITY_FIELD_SYSTEMID = 1;
    private static final String[] m_fixednames = {null, null, null, PsuedoNames.PSEUDONAME_TEXT, "#cdata_section", null, null, null, PsuedoNames.PSEUDONAME_COMMENT, "#document", null, "#document-fragment", null};
    protected FastStringBuffer m_chars;
    protected transient int m_coalescedTextType;
    protected transient IntStack m_contextIndexes;
    protected SuballocatedIntVector m_data;
    protected SuballocatedIntVector m_dataOrQName;
    protected boolean m_endDocumentOccured;
    private Vector m_entities;
    protected Hashtable m_idAttributes;
    private IncrementalSAXSource m_incrementalSAXSource;
    protected transient boolean m_insideDTD;
    protected transient Locator m_locator;
    protected transient IntStack m_parents;
    boolean m_pastFirstElement;
    protected transient Vector m_prefixMappings;
    protected transient int m_previous;
    protected IntVector m_sourceColumn;
    protected IntVector m_sourceLine;
    protected StringVector m_sourceSystemId;
    private transient String m_systemId;
    protected int m_textPendingStart;
    protected transient int m_textType;
    protected boolean m_useSourceLocationProperty;
    protected DTMStringPool m_valuesOrPrefixes;
    protected DTMTreeWalker m_walker;

    public SAX2DTM(DTMManager mgr, Source source, int dtmIdentity, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory, boolean doIndexing) {
        this(mgr, source, dtmIdentity, whiteSpaceFilter, xstringfactory, doIndexing, 512, true, false);
    }

    public SAX2DTM(DTMManager mgr, Source source, int dtmIdentity, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory, boolean doIndexing, int blocksize, boolean usePrevsib, boolean newNameTable) {
        super(mgr, source, dtmIdentity, whiteSpaceFilter, xstringfactory, doIndexing, blocksize, usePrevsib, newNameTable);
        this.m_incrementalSAXSource = null;
        this.m_previous = 0;
        this.m_prefixMappings = new Vector();
        this.m_textType = 3;
        this.m_coalescedTextType = 3;
        this.m_locator = null;
        this.m_systemId = null;
        this.m_insideDTD = false;
        this.m_walker = new DTMTreeWalker();
        this.m_endDocumentOccured = false;
        this.m_idAttributes = new Hashtable();
        this.m_entities = null;
        this.m_textPendingStart = -1;
        this.m_useSourceLocationProperty = false;
        this.m_pastFirstElement = false;
        if (blocksize <= 64) {
            this.m_data = new SuballocatedIntVector(blocksize, 4);
            this.m_dataOrQName = new SuballocatedIntVector(blocksize, 4);
            this.m_valuesOrPrefixes = new DTMStringPool(16);
            this.m_chars = new FastStringBuffer(7, 10);
            this.m_contextIndexes = new IntStack(4);
            this.m_parents = new IntStack(4);
        } else {
            this.m_data = new SuballocatedIntVector(blocksize, 32);
            this.m_dataOrQName = new SuballocatedIntVector(blocksize, 32);
            this.m_valuesOrPrefixes = new DTMStringPool();
            this.m_chars = new FastStringBuffer(10, 13);
            this.m_contextIndexes = new IntStack();
            this.m_parents = new IntStack();
        }
        this.m_data.addElement(0);
        this.m_useSourceLocationProperty = mgr.getSource_location();
        this.m_sourceSystemId = this.m_useSourceLocationProperty ? new StringVector() : null;
        this.m_sourceLine = this.m_useSourceLocationProperty ? new IntVector() : null;
        this.m_sourceColumn = this.m_useSourceLocationProperty ? new IntVector() : null;
    }

    public void setUseSourceLocation(boolean useSourceLocation) {
        this.m_useSourceLocationProperty = useSourceLocation;
    }

    protected int _dataOrQName(int identity) {
        if (identity < this.m_size) {
            return this.m_dataOrQName.elementAt(identity);
        }
        while (isMore) {
            if (identity < this.m_size) {
                return this.m_dataOrQName.elementAt(identity);
            }
        }
        return -1;
    }

    public void clearCoRoutine() {
        clearCoRoutine(true);
    }

    public void clearCoRoutine(boolean callDoTerminate) {
        if (this.m_incrementalSAXSource != null) {
            if (callDoTerminate) {
                this.m_incrementalSAXSource.deliverMoreNodes(false);
            }
            this.m_incrementalSAXSource = null;
        }
    }

    public void setIncrementalSAXSource(IncrementalSAXSource incrementalSAXSource) {
        this.m_incrementalSAXSource = incrementalSAXSource;
        incrementalSAXSource.setContentHandler(this);
        incrementalSAXSource.setLexicalHandler(this);
        incrementalSAXSource.setDTDHandler(this);
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.m_incrementalSAXSource instanceof IncrementalSAXSource_Filter ? (ContentHandler) this.m_incrementalSAXSource : this;
    }

    @Override
    public LexicalHandler getLexicalHandler() {
        return this.m_incrementalSAXSource instanceof IncrementalSAXSource_Filter ? (LexicalHandler) this.m_incrementalSAXSource : this;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return this;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return this;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this;
    }

    @Override
    public DeclHandler getDeclHandler() {
        return this;
    }

    @Override
    public boolean needsTwoThreads() {
        return this.m_incrementalSAXSource != null;
    }

    @Override
    public void dispatchCharactersEvents(int nodeHandle, ContentHandler ch, boolean normalize) throws SAXException {
        int identity = makeNodeIdentity(nodeHandle);
        if (identity != -1) {
            int type = _type(identity);
            if (isTextType(type)) {
                int dataIndex = this.m_dataOrQName.elementAt(identity);
                int offset = this.m_data.elementAt(dataIndex);
                int length = this.m_data.elementAt(dataIndex + 1);
                if (normalize) {
                    this.m_chars.sendNormalizedSAXcharacters(ch, offset, length);
                    return;
                } else {
                    this.m_chars.sendSAXcharacters(ch, offset, length);
                    return;
                }
            }
            int firstChild = _firstch(identity);
            if (-1 != firstChild) {
                int offset2 = -1;
                int length2 = 0;
                int identity2 = firstChild;
                do {
                    if (isTextType(_type(identity2))) {
                        int dataIndex2 = _dataOrQName(identity2);
                        if (-1 == offset2) {
                            offset2 = this.m_data.elementAt(dataIndex2);
                        }
                        length2 += this.m_data.elementAt(dataIndex2 + 1);
                    }
                    identity2 = getNextNodeIdentity(identity2);
                    if (-1 == identity2) {
                        break;
                    }
                } while (_parent(identity2) >= identity);
                if (length2 > 0) {
                    if (normalize) {
                        this.m_chars.sendNormalizedSAXcharacters(ch, offset2, length2);
                        return;
                    } else {
                        this.m_chars.sendSAXcharacters(ch, offset2, length2);
                        return;
                    }
                }
                return;
            }
            if (type != 1) {
                int dataIndex3 = _dataOrQName(identity);
                if (dataIndex3 < 0) {
                    dataIndex3 = this.m_data.elementAt((-dataIndex3) + 1);
                }
                String str = this.m_valuesOrPrefixes.indexToString(dataIndex3);
                if (normalize) {
                    FastStringBuffer.sendNormalizedSAXcharacters(str.toCharArray(), 0, str.length(), ch);
                } else {
                    ch.characters(str.toCharArray(), 0, str.length());
                }
            }
        }
    }

    @Override
    public String getNodeName(int nodeHandle) {
        int expandedTypeID = getExpandedTypeID(nodeHandle);
        int namespaceID = this.m_expandedNameTable.getNamespaceID(expandedTypeID);
        if (namespaceID == 0) {
            int type = getNodeType(nodeHandle);
            if (type == 13) {
                if (this.m_expandedNameTable.getLocalName(expandedTypeID) == null) {
                    return "xmlns";
                }
                return Constants.ATTRNAME_XMLNS + this.m_expandedNameTable.getLocalName(expandedTypeID);
            }
            if (this.m_expandedNameTable.getLocalNameID(expandedTypeID) == 0) {
                return m_fixednames[type];
            }
            return this.m_expandedNameTable.getLocalName(expandedTypeID);
        }
        int qnameIndex = this.m_dataOrQName.elementAt(makeNodeIdentity(nodeHandle));
        if (qnameIndex < 0) {
            qnameIndex = this.m_data.elementAt(-qnameIndex);
        }
        return this.m_valuesOrPrefixes.indexToString(qnameIndex);
    }

    @Override
    public String getNodeNameX(int nodeHandle) {
        int expandedTypeID = getExpandedTypeID(nodeHandle);
        int namespaceID = this.m_expandedNameTable.getNamespaceID(expandedTypeID);
        if (namespaceID == 0) {
            String name = this.m_expandedNameTable.getLocalName(expandedTypeID);
            if (name == null) {
                return "";
            }
            return name;
        }
        int qnameIndex = this.m_dataOrQName.elementAt(makeNodeIdentity(nodeHandle));
        if (qnameIndex < 0) {
            qnameIndex = this.m_data.elementAt(-qnameIndex);
        }
        return this.m_valuesOrPrefixes.indexToString(qnameIndex);
    }

    @Override
    public boolean isAttributeSpecified(int attributeHandle) {
        return true;
    }

    @Override
    public String getDocumentTypeDeclarationSystemIdentifier() {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
        return null;
    }

    @Override
    protected int getNextNodeIdentity(int identity) {
        int identity2 = identity + 1;
        while (identity2 >= this.m_size) {
            if (this.m_incrementalSAXSource == null) {
                return -1;
            }
            nextNode();
        }
        return identity2;
    }

    @Override
    public void dispatchToEvents(int nodeHandle, ContentHandler ch) throws SAXException {
        DTMTreeWalker treeWalker = this.m_walker;
        ContentHandler prevCH = treeWalker.getcontentHandler();
        if (prevCH != null) {
            treeWalker = new DTMTreeWalker();
        }
        treeWalker.setcontentHandler(ch);
        treeWalker.setDTM(this);
        try {
            treeWalker.traverse(nodeHandle);
        } finally {
            treeWalker.setcontentHandler(null);
        }
    }

    @Override
    public int getNumberOfNodes() {
        return this.m_size;
    }

    @Override
    protected boolean nextNode() {
        if (this.m_incrementalSAXSource == null) {
            return false;
        }
        if (this.m_endDocumentOccured) {
            clearCoRoutine();
            return false;
        }
        Object gotMore = this.m_incrementalSAXSource.deliverMoreNodes(true);
        if (!(gotMore instanceof Boolean)) {
            if (gotMore instanceof RuntimeException) {
                throw ((RuntimeException) gotMore);
            }
            if (gotMore instanceof Exception) {
                throw new WrappedRuntimeException((Exception) gotMore);
            }
            clearCoRoutine();
            return false;
        }
        if (gotMore != Boolean.TRUE) {
            clearCoRoutine();
        }
        return true;
    }

    private final boolean isTextType(int type) {
        return 3 == type || 4 == type;
    }

    protected int addNode(int type, int expandedTypeID, int parentIndex, int previousSibling, int dataOrPrefix, boolean canHaveFirstChild) {
        int nodeIndex = this.m_size;
        this.m_size = nodeIndex + 1;
        if (this.m_dtmIdent.size() == (nodeIndex >>> 16)) {
            addNewDTMID(nodeIndex);
        }
        this.m_firstch.addElement(canHaveFirstChild ? -2 : -1);
        this.m_nextsib.addElement(-2);
        this.m_parent.addElement(parentIndex);
        this.m_exptype.addElement(expandedTypeID);
        this.m_dataOrQName.addElement(dataOrPrefix);
        if (this.m_prevsib != null) {
            this.m_prevsib.addElement(previousSibling);
        }
        if (-1 != previousSibling) {
            this.m_nextsib.setElementAt(nodeIndex, previousSibling);
        }
        if (this.m_locator != null && this.m_useSourceLocationProperty) {
            setSourceLocation();
        }
        switch (type) {
            case 2:
                return nodeIndex;
            case 13:
                declareNamespaceInContext(parentIndex, nodeIndex);
                return nodeIndex;
            default:
                if (-1 == previousSibling && -1 != parentIndex) {
                    this.m_firstch.setElementAt(nodeIndex, parentIndex);
                }
                return nodeIndex;
        }
    }

    protected void addNewDTMID(int nodeIndex) {
        try {
            if (this.m_mgr == null) {
                throw new ClassCastException();
            }
            DTMManagerDefault mgrD = (DTMManagerDefault) this.m_mgr;
            int id = mgrD.getFirstFreeDTMID();
            mgrD.addDTM(this, id, nodeIndex);
            this.m_dtmIdent.addElement(id << 16);
        } catch (ClassCastException e) {
            error(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
        }
    }

    @Override
    public void migrateTo(DTMManager manager) {
        super.migrateTo(manager);
        int numDTMs = this.m_dtmIdent.size();
        int dtmId = this.m_mgrDefault.getFirstFreeDTMID();
        int nodeIndex = 0;
        for (int i = 0; i < numDTMs; i++) {
            this.m_dtmIdent.setElementAt(dtmId << 16, i);
            this.m_mgrDefault.addDTM(this, dtmId, nodeIndex);
            dtmId++;
            nodeIndex += 65536;
        }
    }

    protected void setSourceLocation() {
        this.m_sourceSystemId.addElement(this.m_locator.getSystemId());
        this.m_sourceLine.addElement(this.m_locator.getLineNumber());
        this.m_sourceColumn.addElement(this.m_locator.getColumnNumber());
        if (this.m_sourceSystemId.size() != this.m_size) {
            String msg = "CODING ERROR in Source Location: " + this.m_size + " != " + this.m_sourceSystemId.size();
            System.err.println(msg);
            throw new RuntimeException(msg);
        }
    }

    @Override
    public String getNodeValue(int nodeHandle) {
        int identity = makeNodeIdentity(nodeHandle);
        int type = _type(identity);
        if (isTextType(type)) {
            int dataIndex = _dataOrQName(identity);
            int offset = this.m_data.elementAt(dataIndex);
            int length = this.m_data.elementAt(dataIndex + 1);
            return this.m_chars.getString(offset, length);
        }
        if (1 == type || 11 == type || 9 == type) {
            return null;
        }
        int dataIndex2 = _dataOrQName(identity);
        if (dataIndex2 < 0) {
            dataIndex2 = this.m_data.elementAt((-dataIndex2) + 1);
        }
        return this.m_valuesOrPrefixes.indexToString(dataIndex2);
    }

    @Override
    public String getLocalName(int nodeHandle) {
        return this.m_expandedNameTable.getLocalName(_exptype(makeNodeIdentity(nodeHandle)));
    }

    @Override
    public String getUnparsedEntityURI(String name) {
        String url = "";
        if (this.m_entities == null) {
            return "";
        }
        int n = this.m_entities.size();
        int i = 0;
        while (true) {
            if (i >= n) {
                break;
            }
            String ename = (String) this.m_entities.elementAt(i + 3);
            if (ename == null || !ename.equals(name)) {
                i += 4;
            } else {
                String nname = (String) this.m_entities.elementAt(i + 2);
                if (nname != null && (url = (String) this.m_entities.elementAt(i + 1)) == null) {
                    url = (String) this.m_entities.elementAt(i + 0);
                }
            }
        }
        return url;
    }

    @Override
    public String getPrefix(int nodeHandle) {
        int prefixIndex;
        int identity = makeNodeIdentity(nodeHandle);
        int type = _type(identity);
        if (1 == type) {
            int prefixIndex2 = _dataOrQName(identity);
            if (prefixIndex2 == 0) {
                return "";
            }
            String qname = this.m_valuesOrPrefixes.indexToString(prefixIndex2);
            return getPrefix(qname, null);
        }
        if (2 == type && (prefixIndex = _dataOrQName(identity)) < 0) {
            String qname2 = this.m_valuesOrPrefixes.indexToString(this.m_data.elementAt(-prefixIndex));
            return getPrefix(qname2, null);
        }
        return "";
    }

    @Override
    public int getAttributeNode(int nodeHandle, String namespaceURI, String name) {
        int attrH = getFirstAttribute(nodeHandle);
        while (-1 != attrH) {
            String attrNS = getNamespaceURI(attrH);
            String attrName = getLocalName(attrH);
            boolean nsMatch = namespaceURI == attrNS || (namespaceURI != null && namespaceURI.equals(attrNS));
            if (!nsMatch || !name.equals(attrName)) {
                attrH = getNextAttribute(attrH);
            } else {
                return attrH;
            }
        }
        return -1;
    }

    @Override
    public String getDocumentTypeDeclarationPublicIdentifier() {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
        return null;
    }

    @Override
    public String getNamespaceURI(int nodeHandle) {
        return this.m_expandedNameTable.getNamespace(_exptype(makeNodeIdentity(nodeHandle)));
    }

    @Override
    public XMLString getStringValue(int nodeHandle) {
        int type;
        int identity = makeNodeIdentity(nodeHandle);
        if (identity == -1) {
            type = -1;
        } else {
            type = _type(identity);
        }
        if (isTextType(type)) {
            int dataIndex = _dataOrQName(identity);
            int offset = this.m_data.elementAt(dataIndex);
            int length = this.m_data.elementAt(dataIndex + 1);
            return this.m_xstrf.newstr(this.m_chars, offset, length);
        }
        int firstChild = _firstch(identity);
        if (-1 != firstChild) {
            int offset2 = -1;
            int length2 = 0;
            int identity2 = firstChild;
            do {
                int type2 = _type(identity2);
                if (isTextType(type2)) {
                    int dataIndex2 = _dataOrQName(identity2);
                    if (-1 == offset2) {
                        offset2 = this.m_data.elementAt(dataIndex2);
                    }
                    length2 += this.m_data.elementAt(dataIndex2 + 1);
                }
                identity2 = getNextNodeIdentity(identity2);
                if (-1 == identity2) {
                    break;
                }
            } while (_parent(identity2) >= identity);
            if (length2 > 0) {
                return this.m_xstrf.newstr(this.m_chars, offset2, length2);
            }
        } else if (type != 1) {
            int dataIndex3 = _dataOrQName(identity);
            if (dataIndex3 < 0) {
                dataIndex3 = this.m_data.elementAt((-dataIndex3) + 1);
            }
            return this.m_xstrf.newstr(this.m_valuesOrPrefixes.indexToString(dataIndex3));
        }
        return this.m_xstrf.emptystr();
    }

    public boolean isWhitespace(int nodeHandle) {
        int type;
        int identity = makeNodeIdentity(nodeHandle);
        if (identity == -1) {
            type = -1;
        } else {
            type = _type(identity);
        }
        if (isTextType(type)) {
            int dataIndex = _dataOrQName(identity);
            int offset = this.m_data.elementAt(dataIndex);
            int length = this.m_data.elementAt(dataIndex + 1);
            return this.m_chars.isWhitespace(offset, length);
        }
        return false;
    }

    @Override
    public int getElementById(String elementId) {
        Integer intObj;
        boolean isMore = true;
        do {
            intObj = (Integer) this.m_idAttributes.get(elementId);
            if (intObj != null) {
                return makeNodeHandle(intObj.intValue());
            }
            if (!isMore || this.m_endDocumentOccured) {
                break;
            }
            isMore = nextNode();
        } while (intObj == null);
        return -1;
    }

    public String getPrefix(String qname, String uri) {
        int uriIndex = -1;
        if (uri != null && uri.length() > 0) {
            do {
                uriIndex = this.m_prefixMappings.indexOf(uri, uriIndex + 1);
            } while ((uriIndex & 1) == 0);
            if (uriIndex >= 0) {
                String prefix = (String) this.m_prefixMappings.elementAt(uriIndex - 1);
                return prefix;
            }
            if (qname != null) {
                int indexOfNSSep = qname.indexOf(58);
                if (qname.equals("xmlns")) {
                    return "";
                }
                if (qname.startsWith(Constants.ATTRNAME_XMLNS)) {
                    String prefix2 = qname.substring(indexOfNSSep + 1);
                    return prefix2;
                }
                if (indexOfNSSep <= 0) {
                    return null;
                }
                String prefix3 = qname.substring(0, indexOfNSSep);
                return prefix3;
            }
            return null;
        }
        if (qname != null) {
            int indexOfNSSep2 = qname.indexOf(58);
            if (indexOfNSSep2 > 0) {
                if (qname.startsWith(Constants.ATTRNAME_XMLNS)) {
                    String prefix4 = qname.substring(indexOfNSSep2 + 1);
                    return prefix4;
                }
                String prefix5 = qname.substring(0, indexOfNSSep2);
                return prefix5;
            }
            if (qname.equals("xmlns")) {
                return "";
            }
            return null;
        }
        return null;
    }

    public int getIdForNamespace(String uri) {
        return this.m_valuesOrPrefixes.stringToIndex(uri);
    }

    public String getNamespaceURI(String prefix) {
        int prefixIndex = this.m_contextIndexes.peek() - 1;
        if (prefix == null) {
            prefix = "";
        }
        do {
            prefixIndex = this.m_prefixMappings.indexOf(prefix, prefixIndex + 1);
            if (prefixIndex < 0) {
                break;
            }
        } while ((prefixIndex & 1) == 1);
        if (prefixIndex <= -1) {
            return "";
        }
        String uri = (String) this.m_prefixMappings.elementAt(prefixIndex + 1);
        return uri;
    }

    public void setIDAttribute(String id, int elem) {
        this.m_idAttributes.put(id, new Integer(elem));
    }

    protected void charactersFlush() {
        if (this.m_textPendingStart >= 0) {
            int length = this.m_chars.size() - this.m_textPendingStart;
            boolean doStrip = false;
            if (getShouldStripWhitespace()) {
                doStrip = this.m_chars.isWhitespace(this.m_textPendingStart, length);
            }
            if (doStrip) {
                this.m_chars.setLength(this.m_textPendingStart);
            } else if (length > 0) {
                int exName = this.m_expandedNameTable.getExpandedTypeID(3);
                int dataIndex = this.m_data.size();
                this.m_previous = addNode(this.m_coalescedTextType, exName, this.m_parents.peek(), this.m_previous, dataIndex, false);
                this.m_data.addElement(this.m_textPendingStart);
                this.m_data.addElement(length);
            }
            this.m_textPendingStart = -1;
            this.m_coalescedTextType = 3;
            this.m_textType = 3;
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        return null;
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        if (this.m_entities == null) {
            this.m_entities = new Vector();
        }
        try {
            String systemId2 = SystemIDResolver.getAbsoluteURI(systemId, getDocumentBaseURI());
            this.m_entities.addElement(publicId);
            this.m_entities.addElement(systemId2);
            this.m_entities.addElement(notationName);
            this.m_entities.addElement(name);
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.m_locator = locator;
        this.m_systemId = locator.getSystemId();
    }

    @Override
    public void startDocument() throws SAXException {
        int doc = addNode(9, this.m_expandedNameTable.getExpandedTypeID(9), -1, -1, 0, true);
        this.m_parents.push(doc);
        this.m_previous = -1;
        this.m_contextIndexes.push(this.m_prefixMappings.size());
    }

    @Override
    public void endDocument() throws SAXException {
        charactersFlush();
        this.m_nextsib.setElementAt(-1, 0);
        if (this.m_firstch.elementAt(0) == -2) {
            this.m_firstch.setElementAt(-1, 0);
        }
        if (-1 != this.m_previous) {
            this.m_nextsib.setElementAt(-1, this.m_previous);
        }
        this.m_parents = null;
        this.m_prefixMappings = null;
        this.m_contextIndexes = null;
        this.m_endDocumentOccured = true;
        this.m_locator = null;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (prefix == null) {
            prefix = "";
        }
        this.m_prefixMappings.addElement(prefix);
        this.m_prefixMappings.addElement(uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (prefix == null) {
            prefix = "";
        }
        int index = this.m_contextIndexes.peek() - 1;
        do {
            index = this.m_prefixMappings.indexOf(prefix, index + 1);
            if (index < 0) {
                break;
            }
        } while ((index & 1) == 1);
        if (index > -1) {
            this.m_prefixMappings.setElementAt("%@$#^@#", index);
            this.m_prefixMappings.setElementAt("%@$#^@#", index + 1);
        }
    }

    protected boolean declAlreadyDeclared(String prefix) {
        int startDecls = this.m_contextIndexes.peek();
        Vector prefixMappings = this.m_prefixMappings;
        int nDecls = prefixMappings.size();
        for (int i = startDecls; i < nDecls; i += 2) {
            String prefixDecl = (String) prefixMappings.elementAt(i);
            if (prefixDecl != null && prefixDecl.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        boolean shouldStrip;
        int nodeType;
        charactersFlush();
        int exName = this.m_expandedNameTable.getExpandedTypeID(uri, localName, 1);
        int prefixIndex = getPrefix(qName, uri) != null ? this.m_valuesOrPrefixes.stringToIndex(qName) : 0;
        int elemNode = addNode(1, exName, this.m_parents.peek(), this.m_previous, prefixIndex, true);
        if (this.m_indexing) {
            indexNode(exName, elemNode);
        }
        this.m_parents.push(elemNode);
        int startDecls = this.m_contextIndexes.peek();
        int nDecls = this.m_prefixMappings.size();
        int prev = -1;
        if (!this.m_pastFirstElement) {
            prev = addNode(13, this.m_expandedNameTable.getExpandedTypeID(null, "xml", 13), elemNode, -1, this.m_valuesOrPrefixes.stringToIndex("http://www.w3.org/XML/1998/namespace"), false);
            this.m_pastFirstElement = true;
        }
        for (int i = startDecls; i < nDecls; i += 2) {
            String prefix = (String) this.m_prefixMappings.elementAt(i);
            if (prefix != null) {
                String declURL = (String) this.m_prefixMappings.elementAt(i + 1);
                prev = addNode(13, this.m_expandedNameTable.getExpandedTypeID(null, prefix, 13), elemNode, prev, this.m_valuesOrPrefixes.stringToIndex(declURL), false);
            }
        }
        int n = attributes.getLength();
        for (int i2 = 0; i2 < n; i2++) {
            String attrUri = attributes.getURI(i2);
            String attrQName = attributes.getQName(i2);
            String valString = attributes.getValue(i2);
            String prefix2 = getPrefix(attrQName, attrUri);
            String attrLocalName = attributes.getLocalName(i2);
            if (attrQName != null && (attrQName.equals("xmlns") || attrQName.startsWith(Constants.ATTRNAME_XMLNS))) {
                if (!declAlreadyDeclared(prefix2)) {
                    nodeType = 13;
                }
            } else {
                nodeType = 2;
                if (attributes.getType(i2).equalsIgnoreCase("ID")) {
                    setIDAttribute(valString, elemNode);
                }
            }
            if (valString == null) {
                valString = "";
            }
            int val = this.m_valuesOrPrefixes.stringToIndex(valString);
            if (prefix2 != null) {
                int prefixIndex2 = this.m_valuesOrPrefixes.stringToIndex(attrQName);
                int dataIndex = this.m_data.size();
                this.m_data.addElement(prefixIndex2);
                this.m_data.addElement(val);
                val = -dataIndex;
            }
            prev = addNode(nodeType, this.m_expandedNameTable.getExpandedTypeID(attrUri, attrLocalName, nodeType), elemNode, prev, val, false);
        }
        if (-1 != prev) {
            this.m_nextsib.setElementAt(-1, prev);
        }
        if (this.m_wsfilter != null) {
            short wsv = this.m_wsfilter.getShouldStripSpace(makeNodeHandle(elemNode), this);
            if (3 == wsv) {
                shouldStrip = getShouldStripWhitespace();
            } else {
                shouldStrip = 2 == wsv;
            }
            pushShouldStripWhitespace(shouldStrip);
        }
        this.m_previous = -1;
        this.m_contextIndexes.push(this.m_prefixMappings.size());
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        charactersFlush();
        this.m_contextIndexes.quickPop(1);
        int topContextIndex = this.m_contextIndexes.peek();
        if (topContextIndex != this.m_prefixMappings.size()) {
            this.m_prefixMappings.setSize(topContextIndex);
        }
        int lastNode = this.m_previous;
        this.m_previous = this.m_parents.pop();
        if (-1 == lastNode) {
            this.m_firstch.setElementAt(-1, this.m_previous);
        } else {
            this.m_nextsib.setElementAt(-1, lastNode);
        }
        popShouldStripWhitespace();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.m_textPendingStart == -1) {
            this.m_textPendingStart = this.m_chars.size();
            this.m_coalescedTextType = this.m_textType;
        } else if (this.m_textType == 3) {
            this.m_coalescedTextType = 3;
        }
        this.m_chars.append(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        characters(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        charactersFlush();
        int exName = this.m_expandedNameTable.getExpandedTypeID(null, target, 7);
        int dataIndex = this.m_valuesOrPrefixes.stringToIndex(data);
        this.m_previous = addNode(7, exName, this.m_parents.peek(), this.m_previous, dataIndex, false);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        System.err.println(e.getMessage());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }

    @Override
    public void elementDecl(String name, String model) throws SAXException {
    }

    @Override
    public void attributeDecl(String eName, String aName, String type, String valueDefault, String value) throws SAXException {
    }

    @Override
    public void internalEntityDecl(String name, String value) throws SAXException {
    }

    @Override
    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        this.m_insideDTD = true;
    }

    @Override
    public void endDTD() throws SAXException {
        this.m_insideDTD = false;
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
        this.m_textType = 4;
    }

    @Override
    public void endCDATA() throws SAXException {
        this.m_textType = 3;
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (!this.m_insideDTD) {
            charactersFlush();
            int exName = this.m_expandedNameTable.getExpandedTypeID(8);
            int dataIndex = this.m_valuesOrPrefixes.stringToIndex(new String(ch, start, length));
            this.m_previous = addNode(8, exName, this.m_parents.peek(), this.m_previous, dataIndex, false);
        }
    }

    @Override
    public void setProperty(String property, Object value) {
    }

    @Override
    public SourceLocator getSourceLocatorFor(int node) {
        if (this.m_useSourceLocationProperty) {
            int node2 = makeNodeIdentity(node);
            return new NodeLocator(null, this.m_sourceSystemId.elementAt(node2), this.m_sourceLine.elementAt(node2), this.m_sourceColumn.elementAt(node2));
        }
        if (this.m_locator != null) {
            return new NodeLocator(null, this.m_locator.getSystemId(), -1, -1);
        }
        if (this.m_systemId != null) {
            return new NodeLocator(null, this.m_systemId, -1, -1);
        }
        return null;
    }

    public String getFixedNames(int type) {
        return m_fixednames[type];
    }
}
