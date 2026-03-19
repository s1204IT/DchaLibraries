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

    public SAX2DTM(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z) {
        this(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, 512, true, false);
    }

    public SAX2DTM(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z, int i2, boolean z2, boolean z3) {
        super(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, i2, z2, z3);
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
        if (i2 <= 64) {
            this.m_data = new SuballocatedIntVector(i2, 4);
            this.m_dataOrQName = new SuballocatedIntVector(i2, 4);
            this.m_valuesOrPrefixes = new DTMStringPool(16);
            this.m_chars = new FastStringBuffer(7, 10);
            this.m_contextIndexes = new IntStack(4);
            this.m_parents = new IntStack(4);
        } else {
            this.m_data = new SuballocatedIntVector(i2, 32);
            this.m_dataOrQName = new SuballocatedIntVector(i2, 32);
            this.m_valuesOrPrefixes = new DTMStringPool();
            this.m_chars = new FastStringBuffer(10, 13);
            this.m_contextIndexes = new IntStack();
            this.m_parents = new IntStack();
        }
        this.m_data.addElement(0);
        this.m_useSourceLocationProperty = dTMManager.getSource_location();
        this.m_sourceSystemId = this.m_useSourceLocationProperty ? new StringVector() : null;
        this.m_sourceLine = this.m_useSourceLocationProperty ? new IntVector() : null;
        this.m_sourceColumn = this.m_useSourceLocationProperty ? new IntVector() : null;
    }

    public void setUseSourceLocation(boolean z) {
        this.m_useSourceLocationProperty = z;
    }

    protected int _dataOrQName(int i) {
        if (i < this.m_size) {
            return this.m_dataOrQName.elementAt(i);
        }
        while (nextNode()) {
            if (i < this.m_size) {
                return this.m_dataOrQName.elementAt(i);
            }
        }
        return -1;
    }

    public void clearCoRoutine() {
        clearCoRoutine(true);
    }

    public void clearCoRoutine(boolean z) {
        if (this.m_incrementalSAXSource != null) {
            if (z) {
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
        if (this.m_incrementalSAXSource instanceof IncrementalSAXSource_Filter) {
            return (ContentHandler) this.m_incrementalSAXSource;
        }
        return this;
    }

    @Override
    public LexicalHandler getLexicalHandler() {
        if (this.m_incrementalSAXSource instanceof IncrementalSAXSource_Filter) {
            return (LexicalHandler) this.m_incrementalSAXSource;
        }
        return this;
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
    public void dispatchCharactersEvents(int i, ContentHandler contentHandler, boolean z) throws SAXException {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return;
        }
        short s_type = _type(iMakeNodeIdentity);
        if (isTextType(s_type)) {
            int iElementAt = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
            int iElementAt2 = this.m_data.elementAt(iElementAt);
            int iElementAt3 = this.m_data.elementAt(iElementAt + 1);
            if (z) {
                this.m_chars.sendNormalizedSAXcharacters(contentHandler, iElementAt2, iElementAt3);
                return;
            } else {
                this.m_chars.sendSAXcharacters(contentHandler, iElementAt2, iElementAt3);
                return;
            }
        }
        int i_firstch = _firstch(iMakeNodeIdentity);
        int iElementAt4 = 0;
        if (-1 != i_firstch) {
            int iElementAt5 = -1;
            do {
                if (isTextType(_type(i_firstch))) {
                    int i_dataOrQName = _dataOrQName(i_firstch);
                    if (-1 == iElementAt5) {
                        iElementAt5 = this.m_data.elementAt(i_dataOrQName);
                    }
                    iElementAt4 += this.m_data.elementAt(i_dataOrQName + 1);
                }
                i_firstch = getNextNodeIdentity(i_firstch);
                if (-1 == i_firstch) {
                    break;
                }
            } while (_parent(i_firstch) >= iMakeNodeIdentity);
            if (iElementAt4 > 0) {
                if (z) {
                    this.m_chars.sendNormalizedSAXcharacters(contentHandler, iElementAt5, iElementAt4);
                    return;
                } else {
                    this.m_chars.sendSAXcharacters(contentHandler, iElementAt5, iElementAt4);
                    return;
                }
            }
            return;
        }
        if (s_type != 1) {
            int i_dataOrQName2 = _dataOrQName(iMakeNodeIdentity);
            if (i_dataOrQName2 < 0) {
                i_dataOrQName2 = this.m_data.elementAt((-i_dataOrQName2) + 1);
            }
            String strIndexToString = this.m_valuesOrPrefixes.indexToString(i_dataOrQName2);
            if (!z) {
                contentHandler.characters(strIndexToString.toCharArray(), 0, strIndexToString.length());
            } else {
                FastStringBuffer.sendNormalizedSAXcharacters(strIndexToString.toCharArray(), 0, strIndexToString.length(), contentHandler);
            }
        }
    }

    @Override
    public String getNodeName(int i) {
        int expandedTypeID = getExpandedTypeID(i);
        if (this.m_expandedNameTable.getNamespaceID(expandedTypeID) == 0) {
            short nodeType = getNodeType(i);
            if (nodeType == 13) {
                if (this.m_expandedNameTable.getLocalName(expandedTypeID) == null) {
                    return "xmlns";
                }
                return Constants.ATTRNAME_XMLNS + this.m_expandedNameTable.getLocalName(expandedTypeID);
            }
            if (this.m_expandedNameTable.getLocalNameID(expandedTypeID) == 0) {
                return m_fixednames[nodeType];
            }
            return this.m_expandedNameTable.getLocalName(expandedTypeID);
        }
        int iElementAt = this.m_dataOrQName.elementAt(makeNodeIdentity(i));
        if (iElementAt < 0) {
            iElementAt = this.m_data.elementAt(-iElementAt);
        }
        return this.m_valuesOrPrefixes.indexToString(iElementAt);
    }

    @Override
    public String getNodeNameX(int i) {
        int expandedTypeID = getExpandedTypeID(i);
        if (this.m_expandedNameTable.getNamespaceID(expandedTypeID) == 0) {
            String localName = this.m_expandedNameTable.getLocalName(expandedTypeID);
            if (localName == null) {
                return "";
            }
            return localName;
        }
        int iElementAt = this.m_dataOrQName.elementAt(makeNodeIdentity(i));
        if (iElementAt < 0) {
            iElementAt = this.m_data.elementAt(-iElementAt);
        }
        return this.m_valuesOrPrefixes.indexToString(iElementAt);
    }

    @Override
    public boolean isAttributeSpecified(int i) {
        return true;
    }

    @Override
    public String getDocumentTypeDeclarationSystemIdentifier() {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
        return null;
    }

    @Override
    protected int getNextNodeIdentity(int i) {
        int i2 = i + 1;
        while (i2 >= this.m_size) {
            if (this.m_incrementalSAXSource == null) {
                return -1;
            }
            nextNode();
        }
        return i2;
    }

    @Override
    public void dispatchToEvents(int i, ContentHandler contentHandler) throws SAXException {
        DTMTreeWalker dTMTreeWalker = this.m_walker;
        if (dTMTreeWalker.getcontentHandler() != null) {
            dTMTreeWalker = new DTMTreeWalker();
        }
        dTMTreeWalker.setcontentHandler(contentHandler);
        dTMTreeWalker.setDTM(this);
        try {
            dTMTreeWalker.traverse(i);
        } finally {
            dTMTreeWalker.setcontentHandler(null);
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
        Object objDeliverMoreNodes = this.m_incrementalSAXSource.deliverMoreNodes(true);
        if (!(objDeliverMoreNodes instanceof Boolean)) {
            if (objDeliverMoreNodes instanceof RuntimeException) {
                throw ((RuntimeException) objDeliverMoreNodes);
            }
            if (objDeliverMoreNodes instanceof Exception) {
                throw new WrappedRuntimeException((Exception) objDeliverMoreNodes);
            }
            clearCoRoutine();
            return false;
        }
        if (objDeliverMoreNodes != Boolean.TRUE) {
            clearCoRoutine();
        }
        return true;
    }

    private final boolean isTextType(int i) {
        return 3 == i || 4 == i;
    }

    protected int addNode(int i, int i2, int i3, int i4, int i5, boolean z) {
        int i6 = this.m_size;
        this.m_size = i6 + 1;
        if (this.m_dtmIdent.size() == (i6 >>> 16)) {
            addNewDTMID(i6);
        }
        this.m_firstch.addElement(z ? -2 : -1);
        this.m_nextsib.addElement(-2);
        this.m_parent.addElement(i3);
        this.m_exptype.addElement(i2);
        this.m_dataOrQName.addElement(i5);
        if (this.m_prevsib != null) {
            this.m_prevsib.addElement(i4);
        }
        if (-1 != i4) {
            this.m_nextsib.setElementAt(i6, i4);
        }
        if (this.m_locator != null && this.m_useSourceLocationProperty) {
            setSourceLocation();
        }
        if (i != 2) {
            if (i == 13) {
                declareNamespaceInContext(i3, i6);
            } else if (-1 == i4 && -1 != i3) {
                this.m_firstch.setElementAt(i6, i3);
            }
        }
        return i6;
    }

    protected void addNewDTMID(int i) {
        try {
            if (this.m_mgr == null) {
                throw new ClassCastException();
            }
            DTMManagerDefault dTMManagerDefault = (DTMManagerDefault) this.m_mgr;
            int firstFreeDTMID = dTMManagerDefault.getFirstFreeDTMID();
            dTMManagerDefault.addDTM(this, firstFreeDTMID, i);
            this.m_dtmIdent.addElement(firstFreeDTMID << 16);
        } catch (ClassCastException e) {
            error(XMLMessages.createXMLMessage(XMLErrorResources.ER_NO_DTMIDS_AVAIL, null));
        }
    }

    @Override
    public void migrateTo(DTMManager dTMManager) {
        super.migrateTo(dTMManager);
        int size = this.m_dtmIdent.size();
        int firstFreeDTMID = this.m_mgrDefault.getFirstFreeDTMID();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            this.m_dtmIdent.setElementAt(firstFreeDTMID << 16, i2);
            this.m_mgrDefault.addDTM(this, firstFreeDTMID, i);
            firstFreeDTMID++;
            i += 65536;
        }
    }

    protected void setSourceLocation() {
        this.m_sourceSystemId.addElement(this.m_locator.getSystemId());
        this.m_sourceLine.addElement(this.m_locator.getLineNumber());
        this.m_sourceColumn.addElement(this.m_locator.getColumnNumber());
        if (this.m_sourceSystemId.size() != this.m_size) {
            String str = "CODING ERROR in Source Location: " + this.m_size + " != " + this.m_sourceSystemId.size();
            System.err.println(str);
            throw new RuntimeException(str);
        }
    }

    @Override
    public String getNodeValue(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        short s_type = _type(iMakeNodeIdentity);
        if (isTextType(s_type)) {
            int i_dataOrQName = _dataOrQName(iMakeNodeIdentity);
            return this.m_chars.getString(this.m_data.elementAt(i_dataOrQName), this.m_data.elementAt(i_dataOrQName + 1));
        }
        if (1 == s_type || 11 == s_type || 9 == s_type) {
            return null;
        }
        int i_dataOrQName2 = _dataOrQName(iMakeNodeIdentity);
        if (i_dataOrQName2 < 0) {
            i_dataOrQName2 = this.m_data.elementAt((-i_dataOrQName2) + 1);
        }
        return this.m_valuesOrPrefixes.indexToString(i_dataOrQName2);
    }

    @Override
    public String getLocalName(int i) {
        return this.m_expandedNameTable.getLocalName(_exptype(makeNodeIdentity(i)));
    }

    @Override
    public String getUnparsedEntityURI(String str) {
        if (this.m_entities == null) {
            return "";
        }
        int size = this.m_entities.size();
        for (int i = 0; i < size; i += 4) {
            String str2 = (String) this.m_entities.elementAt(i + 3);
            if (str2 != null && str2.equals(str)) {
                if (((String) this.m_entities.elementAt(i + 2)) == null) {
                    return "";
                }
                String str3 = (String) this.m_entities.elementAt(i + 1);
                if (str3 == null) {
                    return (String) this.m_entities.elementAt(i + 0);
                }
                return str3;
            }
        }
        return "";
    }

    @Override
    public String getPrefix(int i) {
        int i_dataOrQName;
        int iMakeNodeIdentity = makeNodeIdentity(i);
        short s_type = _type(iMakeNodeIdentity);
        if (1 == s_type) {
            int i_dataOrQName2 = _dataOrQName(iMakeNodeIdentity);
            if (i_dataOrQName2 == 0) {
                return "";
            }
            return getPrefix(this.m_valuesOrPrefixes.indexToString(i_dataOrQName2), null);
        }
        if (2 == s_type && (i_dataOrQName = _dataOrQName(iMakeNodeIdentity)) < 0) {
            return getPrefix(this.m_valuesOrPrefixes.indexToString(this.m_data.elementAt(-i_dataOrQName)), null);
        }
        return "";
    }

    @Override
    public int getAttributeNode(int i, String str, String str2) {
        int firstAttribute = getFirstAttribute(i);
        while (-1 != firstAttribute) {
            String namespaceURI = getNamespaceURI(firstAttribute);
            String localName = getLocalName(firstAttribute);
            if (!(str == namespaceURI || (str != null && str.equals(namespaceURI))) || !str2.equals(localName)) {
                firstAttribute = getNextAttribute(firstAttribute);
            } else {
                return firstAttribute;
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
    public String getNamespaceURI(int i) {
        return this.m_expandedNameTable.getNamespace(_exptype(makeNodeIdentity(i)));
    }

    @Override
    public XMLString getStringValue(int i) {
        short s_type;
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity != -1) {
            s_type = _type(iMakeNodeIdentity);
        } else {
            s_type = -1;
        }
        if (isTextType(s_type)) {
            int i_dataOrQName = _dataOrQName(iMakeNodeIdentity);
            return this.m_xstrf.newstr(this.m_chars, this.m_data.elementAt(i_dataOrQName), this.m_data.elementAt(i_dataOrQName + 1));
        }
        int i_firstch = _firstch(iMakeNodeIdentity);
        if (-1 != i_firstch) {
            int iElementAt = 0;
            int iElementAt2 = -1;
            do {
                if (isTextType(_type(i_firstch))) {
                    int i_dataOrQName2 = _dataOrQName(i_firstch);
                    if (-1 == iElementAt2) {
                        iElementAt2 = this.m_data.elementAt(i_dataOrQName2);
                    }
                    iElementAt += this.m_data.elementAt(i_dataOrQName2 + 1);
                }
                i_firstch = getNextNodeIdentity(i_firstch);
                if (-1 == i_firstch) {
                    break;
                }
            } while (_parent(i_firstch) >= iMakeNodeIdentity);
            if (iElementAt > 0) {
                return this.m_xstrf.newstr(this.m_chars, iElementAt2, iElementAt);
            }
        } else if (s_type != 1) {
            int i_dataOrQName3 = _dataOrQName(iMakeNodeIdentity);
            if (i_dataOrQName3 < 0) {
                i_dataOrQName3 = this.m_data.elementAt((-i_dataOrQName3) + 1);
            }
            return this.m_xstrf.newstr(this.m_valuesOrPrefixes.indexToString(i_dataOrQName3));
        }
        return this.m_xstrf.emptystr();
    }

    public boolean isWhitespace(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        short s_type = -1;
        if (iMakeNodeIdentity != -1) {
            s_type = _type(iMakeNodeIdentity);
        }
        if (isTextType(s_type)) {
            int i_dataOrQName = _dataOrQName(iMakeNodeIdentity);
            return this.m_chars.isWhitespace(this.m_data.elementAt(i_dataOrQName), this.m_data.elementAt(i_dataOrQName + 1));
        }
        return false;
    }

    @Override
    public int getElementById(String str) {
        Integer num;
        boolean zNextNode = true;
        do {
            num = (Integer) this.m_idAttributes.get(str);
            if (num != null) {
                return makeNodeHandle(num.intValue());
            }
            if (zNextNode && !this.m_endDocumentOccured) {
                zNextNode = nextNode();
            } else {
                return -1;
            }
        } while (num == null);
        return -1;
    }

    public String getPrefix(String str, String str2) {
        String strSubstring;
        String strSubstring2;
        if (str2 != null && str2.length() > 0) {
            int iIndexOf = -1;
            do {
                iIndexOf = this.m_prefixMappings.indexOf(str2, iIndexOf + 1);
            } while ((iIndexOf & 1) == 0);
            if (iIndexOf >= 0) {
                return (String) this.m_prefixMappings.elementAt(iIndexOf - 1);
            }
            if (str == null) {
                return null;
            }
            int iIndexOf2 = str.indexOf(58);
            if (str.equals("xmlns")) {
                strSubstring2 = "";
            } else if (str.startsWith(Constants.ATTRNAME_XMLNS)) {
                strSubstring2 = str.substring(iIndexOf2 + 1);
            } else {
                if (iIndexOf2 > 0) {
                    return str.substring(0, iIndexOf2);
                }
                return null;
            }
            return strSubstring2;
        }
        if (str == null) {
            return null;
        }
        int iIndexOf3 = str.indexOf(58);
        if (iIndexOf3 > 0) {
            if (str.startsWith(Constants.ATTRNAME_XMLNS)) {
                strSubstring = str.substring(iIndexOf3 + 1);
            } else {
                strSubstring = str.substring(0, iIndexOf3);
            }
        } else {
            if (!str.equals("xmlns")) {
                return null;
            }
            strSubstring = "";
        }
        return strSubstring;
    }

    public int getIdForNamespace(String str) {
        return this.m_valuesOrPrefixes.stringToIndex(str);
    }

    public String getNamespaceURI(String str) {
        int iPeek = this.m_contextIndexes.peek() - 1;
        if (str == null) {
            str = "";
        }
        do {
            iPeek = this.m_prefixMappings.indexOf(str, iPeek + 1);
            if (iPeek < 0) {
                break;
            }
        } while ((iPeek & 1) == 1);
        if (iPeek <= -1) {
            return "";
        }
        return (String) this.m_prefixMappings.elementAt(iPeek + 1);
    }

    public void setIDAttribute(String str, int i) {
        this.m_idAttributes.put(str, new Integer(i));
    }

    protected void charactersFlush() {
        if (this.m_textPendingStart >= 0) {
            int size = this.m_chars.size() - this.m_textPendingStart;
            boolean zIsWhitespace = false;
            if (getShouldStripWhitespace()) {
                zIsWhitespace = this.m_chars.isWhitespace(this.m_textPendingStart, size);
            }
            if (zIsWhitespace) {
                this.m_chars.setLength(this.m_textPendingStart);
            } else if (size > 0) {
                this.m_previous = addNode(this.m_coalescedTextType, this.m_expandedNameTable.getExpandedTypeID(3), this.m_parents.peek(), this.m_previous, this.m_data.size(), false);
                this.m_data.addElement(this.m_textPendingStart);
                this.m_data.addElement(size);
            }
            this.m_textPendingStart = -1;
            this.m_coalescedTextType = 3;
            this.m_textType = 3;
        }
    }

    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException {
        return null;
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        if (this.m_entities == null) {
            this.m_entities = new Vector();
        }
        try {
            String absoluteURI = SystemIDResolver.getAbsoluteURI(str3, getDocumentBaseURI());
            this.m_entities.addElement(str2);
            this.m_entities.addElement(absoluteURI);
            this.m_entities.addElement(str4);
            this.m_entities.addElement(str);
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
        this.m_parents.push(addNode(9, this.m_expandedNameTable.getExpandedTypeID(9), -1, -1, 0, true));
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
    public void startPrefixMapping(String str, String str2) throws SAXException {
        if (str == null) {
            str = "";
        }
        this.m_prefixMappings.addElement(str);
        this.m_prefixMappings.addElement(str2);
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
        if (str == null) {
            str = "";
        }
        int iPeek = this.m_contextIndexes.peek() - 1;
        do {
            iPeek = this.m_prefixMappings.indexOf(str, iPeek + 1);
            if (iPeek < 0) {
                break;
            }
        } while ((iPeek & 1) == 1);
        if (iPeek > -1) {
            this.m_prefixMappings.setElementAt("%@$#^@#", iPeek);
            this.m_prefixMappings.setElementAt("%@$#^@#", iPeek + 1);
        }
    }

    protected boolean declAlreadyDeclared(String str) {
        Vector vector = this.m_prefixMappings;
        int size = vector.size();
        for (int iPeek = this.m_contextIndexes.peek(); iPeek < size; iPeek += 2) {
            String str2 = (String) vector.elementAt(iPeek);
            if (str2 != null && str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        int iStringToIndex;
        boolean shouldStripWhitespace;
        int i;
        charactersFlush();
        int expandedTypeID = this.m_expandedNameTable.getExpandedTypeID(str, str2, 1);
        if (getPrefix(str3, str) != null) {
            iStringToIndex = this.m_valuesOrPrefixes.stringToIndex(str3);
        } else {
            iStringToIndex = 0;
        }
        int iAddNode = addNode(1, expandedTypeID, this.m_parents.peek(), this.m_previous, iStringToIndex, true);
        if (this.m_indexing) {
            indexNode(expandedTypeID, iAddNode);
        }
        this.m_parents.push(iAddNode);
        int size = this.m_prefixMappings.size();
        int iAddNode2 = -1;
        if (!this.m_pastFirstElement) {
            iAddNode2 = addNode(13, this.m_expandedNameTable.getExpandedTypeID(null, "xml", 13), iAddNode, -1, this.m_valuesOrPrefixes.stringToIndex("http://www.w3.org/XML/1998/namespace"), false);
            this.m_pastFirstElement = true;
        }
        for (int iPeek = this.m_contextIndexes.peek(); iPeek < size; iPeek += 2) {
            String str4 = (String) this.m_prefixMappings.elementAt(iPeek);
            if (str4 != null) {
                iAddNode2 = addNode(13, this.m_expandedNameTable.getExpandedTypeID(null, str4, 13), iAddNode, iAddNode2, this.m_valuesOrPrefixes.stringToIndex((String) this.m_prefixMappings.elementAt(iPeek + 1)), false);
            }
        }
        int length = attributes.getLength();
        for (int i2 = 0; i2 < length; i2++) {
            String uri = attributes.getURI(i2);
            String qName = attributes.getQName(i2);
            String value = attributes.getValue(i2);
            String prefix = getPrefix(qName, uri);
            String localName = attributes.getLocalName(i2);
            if (qName != null && (qName.equals("xmlns") || qName.startsWith(Constants.ATTRNAME_XMLNS))) {
                if (!declAlreadyDeclared(prefix)) {
                    i = 13;
                }
            } else {
                if (attributes.getType(i2).equalsIgnoreCase("ID")) {
                    setIDAttribute(value, iAddNode);
                }
                i = 2;
            }
            if (value == null) {
                value = "";
            }
            int iStringToIndex2 = this.m_valuesOrPrefixes.stringToIndex(value);
            if (prefix != null) {
                int iStringToIndex3 = this.m_valuesOrPrefixes.stringToIndex(qName);
                int size2 = this.m_data.size();
                this.m_data.addElement(iStringToIndex3);
                this.m_data.addElement(iStringToIndex2);
                iStringToIndex2 = -size2;
            }
            iAddNode2 = addNode(i, this.m_expandedNameTable.getExpandedTypeID(uri, localName, i), iAddNode, iAddNode2, iStringToIndex2, false);
        }
        if (-1 != iAddNode2) {
            this.m_nextsib.setElementAt(-1, iAddNode2);
        }
        if (this.m_wsfilter != null) {
            short shouldStripSpace = this.m_wsfilter.getShouldStripSpace(makeNodeHandle(iAddNode), this);
            if (3 == shouldStripSpace) {
                shouldStripWhitespace = getShouldStripWhitespace();
            } else {
                shouldStripWhitespace = 2 == shouldStripSpace;
            }
            pushShouldStripWhitespace(shouldStripWhitespace);
        }
        this.m_previous = -1;
        this.m_contextIndexes.push(this.m_prefixMappings.size());
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        charactersFlush();
        this.m_contextIndexes.quickPop(1);
        int iPeek = this.m_contextIndexes.peek();
        if (iPeek != this.m_prefixMappings.size()) {
            this.m_prefixMappings.setSize(iPeek);
        }
        int i = this.m_previous;
        this.m_previous = this.m_parents.pop();
        if (-1 == i) {
            this.m_firstch.setElementAt(-1, this.m_previous);
        } else {
            this.m_nextsib.setElementAt(-1, i);
        }
        popShouldStripWhitespace();
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_textPendingStart == -1) {
            this.m_textPendingStart = this.m_chars.size();
            this.m_coalescedTextType = this.m_textType;
        } else if (this.m_textType == 3) {
            this.m_coalescedTextType = 3;
        }
        this.m_chars.append(cArr, i, i2);
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        characters(cArr, i, i2);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        charactersFlush();
        this.m_previous = addNode(7, this.m_expandedNameTable.getExpandedTypeID(null, str, 7), this.m_parents.peek(), this.m_previous, this.m_valuesOrPrefixes.stringToIndex(str2), false);
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        System.err.println(sAXParseException.getMessage());
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        throw sAXParseException;
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        throw sAXParseException;
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
        this.m_insideDTD = true;
    }

    @Override
    public void endDTD() throws SAXException {
        this.m_insideDTD = false;
    }

    @Override
    public void startEntity(String str) throws SAXException {
    }

    @Override
    public void endEntity(String str) throws SAXException {
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
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_insideDTD) {
            return;
        }
        charactersFlush();
        this.m_previous = addNode(8, this.m_expandedNameTable.getExpandedTypeID(8), this.m_parents.peek(), this.m_previous, this.m_valuesOrPrefixes.stringToIndex(new String(cArr, i, i2)), false);
    }

    @Override
    public void setProperty(String str, Object obj) {
    }

    @Override
    public SourceLocator getSourceLocatorFor(int i) {
        if (this.m_useSourceLocationProperty) {
            int iMakeNodeIdentity = makeNodeIdentity(i);
            return new NodeLocator(null, this.m_sourceSystemId.elementAt(iMakeNodeIdentity), this.m_sourceLine.elementAt(iMakeNodeIdentity), this.m_sourceColumn.elementAt(iMakeNodeIdentity));
        }
        if (this.m_locator != null) {
            return new NodeLocator(null, this.m_locator.getSystemId(), -1, -1);
        }
        if (this.m_systemId != null) {
            return new NodeLocator(null, this.m_systemId, -1, -1);
        }
        return null;
    }

    public String getFixedNames(int i) {
        return m_fixednames[i];
    }
}
