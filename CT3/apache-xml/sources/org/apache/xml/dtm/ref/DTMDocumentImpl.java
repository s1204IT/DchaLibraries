package org.apache.xml.dtm.ref;

import javax.xml.transform.SourceLocator;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.serializer.SerializerConstants;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.compiler.PsuedoNames;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class DTMDocumentImpl implements DTM, ContentHandler, LexicalHandler {
    protected static final int DOCHANDLE_MASK = -8388608;
    protected static final byte DOCHANDLE_SHIFT = 22;
    protected static final int NODEHANDLE_MASK = 8388607;
    private static final String[] fixednames = {null, null, null, PsuedoNames.PSEUDONAME_TEXT, "#cdata_section", null, null, null, PsuedoNames.PSEUDONAME_COMMENT, "#document", null, "#document-fragment", null};
    protected String m_documentBaseURI;
    private XMLStringFactory m_xsf;
    int m_docHandle = -1;
    int m_docElement = -1;
    int currentParent = 0;
    int previousSibling = 0;
    protected int m_currentNode = -1;
    private boolean previousSiblingWasParent = false;
    int[] gotslot = new int[4];
    private boolean done = false;
    boolean m_isError = false;
    private final boolean DEBUG = false;
    private IncrementalSAXSource m_incrSAXSource = null;
    ChunkedIntArray nodes = new ChunkedIntArray(4);
    private FastStringBuffer m_char = new FastStringBuffer();
    private int m_char_current_start = 0;
    private DTMStringPool m_localNames = new DTMStringPool();
    private DTMStringPool m_nsNames = new DTMStringPool();
    private DTMStringPool m_prefixNames = new DTMStringPool();
    private ExpandedNameTable m_expandedNames = new ExpandedNameTable();

    public DTMDocumentImpl(DTMManager mgr, int documentNumber, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory) {
        initDocument(documentNumber);
        this.m_xsf = xstringfactory;
    }

    public void setIncrementalSAXSource(IncrementalSAXSource source) {
        this.m_incrSAXSource = source;
        source.setContentHandler(this);
        source.setLexicalHandler(this);
    }

    private final int appendNode(int w0, int w1, int w2, int w3) {
        int slotnumber = this.nodes.appendSlot(w0, w1, w2, w3);
        if (this.previousSiblingWasParent) {
            this.nodes.writeEntry(this.previousSibling, 2, slotnumber);
        }
        this.previousSiblingWasParent = false;
        return slotnumber;
    }

    @Override
    public void setFeature(String featureId, boolean state) {
    }

    public void setLocalNameTable(DTMStringPool poolRef) {
        this.m_localNames = poolRef;
    }

    public DTMStringPool getLocalNameTable() {
        return this.m_localNames;
    }

    public void setNsNameTable(DTMStringPool poolRef) {
        this.m_nsNames = poolRef;
    }

    public DTMStringPool getNsNameTable() {
        return this.m_nsNames;
    }

    public void setPrefixNameTable(DTMStringPool poolRef) {
        this.m_prefixNames = poolRef;
    }

    public DTMStringPool getPrefixNameTable() {
        return this.m_prefixNames;
    }

    void setContentBuffer(FastStringBuffer buffer) {
        this.m_char = buffer;
    }

    FastStringBuffer getContentBuffer() {
        return this.m_char;
    }

    @Override
    public ContentHandler getContentHandler() {
        if (this.m_incrSAXSource instanceof IncrementalSAXSource_Filter) {
            return (ContentHandler) this.m_incrSAXSource;
        }
        return this;
    }

    @Override
    public LexicalHandler getLexicalHandler() {
        if (this.m_incrSAXSource instanceof IncrementalSAXSource_Filter) {
            return (LexicalHandler) this.m_incrSAXSource;
        }
        return this;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return null;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return null;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public DeclHandler getDeclHandler() {
        return null;
    }

    @Override
    public boolean needsTwoThreads() {
        return this.m_incrSAXSource != null;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.m_char.append(ch, start, length);
    }

    private void processAccumulatedText() {
        int len = this.m_char.length();
        if (len == this.m_char_current_start) {
            return;
        }
        appendTextChild(this.m_char_current_start, len - this.m_char_current_start);
        this.m_char_current_start = len;
    }

    @Override
    public void endDocument() throws SAXException {
        appendEndDocument();
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        processAccumulatedText();
        appendEndElement();
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        processAccumulatedText();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        processAccumulatedText();
    }

    @Override
    public void startDocument() throws SAXException {
        appendStartDocument();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        String prefix;
        String localName2;
        String prefix2;
        processAccumulatedText();
        String prefix3 = null;
        int colon = qName.indexOf(58);
        if (colon > 0) {
            prefix3 = qName.substring(0, colon);
        }
        System.out.println("Prefix=" + prefix3 + " index=" + this.m_prefixNames.stringToIndex(prefix3));
        appendStartElement(this.m_nsNames.stringToIndex(namespaceURI), this.m_localNames.stringToIndex(localName), this.m_prefixNames.stringToIndex(prefix3));
        int nAtts = atts == null ? 0 : atts.getLength();
        for (int i = nAtts - 1; i >= 0; i--) {
            String qName2 = atts.getQName(i);
            if (qName2.startsWith(Constants.ATTRNAME_XMLNS) || "xmlns".equals(qName2)) {
                int colon2 = qName2.indexOf(58);
                if (colon2 > 0) {
                    prefix2 = qName2.substring(0, colon2);
                } else {
                    prefix2 = null;
                }
                appendNSDeclaration(this.m_prefixNames.stringToIndex(prefix2), this.m_nsNames.stringToIndex(atts.getValue(i)), atts.getType(i).equalsIgnoreCase("ID"));
            }
        }
        for (int i2 = nAtts - 1; i2 >= 0; i2--) {
            String qName3 = atts.getQName(i2);
            if (!(!qName3.startsWith(Constants.ATTRNAME_XMLNS) ? "xmlns".equals(qName3) : true)) {
                int colon3 = qName3.indexOf(58);
                if (colon3 > 0) {
                    prefix = qName3.substring(0, colon3);
                    localName2 = qName3.substring(colon3 + 1);
                } else {
                    prefix = "";
                    localName2 = qName3;
                }
                this.m_char.append(atts.getValue(i2));
                int contentEnd = this.m_char.length();
                if (!(!"xmlns".equals(prefix) ? "xmlns".equals(qName3) : true)) {
                    appendAttribute(this.m_nsNames.stringToIndex(atts.getURI(i2)), this.m_localNames.stringToIndex(localName2), this.m_prefixNames.stringToIndex(prefix), atts.getType(i2).equalsIgnoreCase("ID"), this.m_char_current_start, contentEnd - this.m_char_current_start);
                }
                this.m_char_current_start = contentEnd;
            }
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        processAccumulatedText();
        this.m_char.append(ch, start, length);
        appendComment(this.m_char_current_start, length);
        this.m_char_current_start += length;
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }

    final void initDocument(int documentNumber) {
        this.m_docHandle = documentNumber << 22;
        this.nodes.writeSlot(0, 9, -1, -1, 0);
        this.done = false;
    }

    @Override
    public boolean hasChildNodes(int nodeHandle) {
        return getFirstChild(nodeHandle) != -1;
    }

    @Override
    public int getFirstChild(int nodeHandle) {
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        this.nodes.readSlot(nodeHandle2, this.gotslot);
        short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (type == 1 || type == 9 || type == 5) {
            int kid = nodeHandle2 + 1;
            this.nodes.readSlot(kid, this.gotslot);
            while (2 == (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT)) {
                kid = this.gotslot[2];
                if (kid == -1) {
                    return -1;
                }
                this.nodes.readSlot(kid, this.gotslot);
            }
            if (this.gotslot[1] == nodeHandle2) {
                int firstChild = kid | this.m_docHandle;
                return firstChild;
            }
        }
        return -1;
    }

    @Override
    public int getLastChild(int nodeHandle) {
        int lastChild = -1;
        int nextkid = getFirstChild(nodeHandle & NODEHANDLE_MASK);
        while (nextkid != -1) {
            lastChild = nextkid;
            nextkid = getNextSibling(nextkid);
        }
        return this.m_docHandle | lastChild;
    }

    @Override
    public int getAttributeNode(int nodeHandle, String namespaceURI, String name) {
        int nsIndex = this.m_nsNames.stringToIndex(namespaceURI);
        int nameIndex = this.m_localNames.stringToIndex(name);
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        this.nodes.readSlot(nodeHandle2, this.gotslot);
        short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (type == 1) {
            nodeHandle2++;
        }
        while (type == 2) {
            if (nsIndex == (this.gotslot[0] << 16) && this.gotslot[3] == nameIndex) {
                return this.m_docHandle | nodeHandle2;
            }
            nodeHandle2 = this.gotslot[2];
            this.nodes.readSlot(nodeHandle2, this.gotslot);
        }
        return -1;
    }

    @Override
    public int getFirstAttribute(int nodeHandle) {
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        if (1 != (this.nodes.readEntry(nodeHandle2, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
            return -1;
        }
        int nodeHandle3 = nodeHandle2 + 1;
        if (2 == (this.nodes.readEntry(nodeHandle3, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
            return this.m_docHandle | nodeHandle3;
        }
        return -1;
    }

    @Override
    public int getFirstNamespaceNode(int nodeHandle, boolean inScope) {
        return -1;
    }

    @Override
    public int getNextSibling(int nodeHandle) {
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        if (nodeHandle2 == 0) {
            return -1;
        }
        short type = (short) (this.nodes.readEntry(nodeHandle2, 0) & DTMManager.IDENT_NODE_DEFAULT);
        if (type == 1 || type == 2 || type == 5) {
            int nextSib = this.nodes.readEntry(nodeHandle2, 2);
            if (nextSib == -1) {
                return -1;
            }
            if (nextSib != 0) {
                return this.m_docHandle | nextSib;
            }
        }
        int thisParent = this.nodes.readEntry(nodeHandle2, 1);
        int nodeHandle3 = nodeHandle2 + 1;
        if (this.nodes.readEntry(nodeHandle3, 1) == thisParent) {
            return this.m_docHandle | nodeHandle3;
        }
        return -1;
    }

    @Override
    public int getPreviousSibling(int nodeHandle) {
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        if (nodeHandle2 == 0) {
            return -1;
        }
        int parent = this.nodes.readEntry(nodeHandle2, 1);
        int kid = -1;
        int nextkid = getFirstChild(parent);
        while (nextkid != nodeHandle2) {
            kid = nextkid;
            nextkid = getNextSibling(nextkid);
        }
        return this.m_docHandle | kid;
    }

    @Override
    public int getNextAttribute(int nodeHandle) {
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        this.nodes.readSlot(nodeHandle2, this.gotslot);
        short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (type == 1) {
            return getFirstAttribute(nodeHandle2);
        }
        if (type != 2 || this.gotslot[2] == -1) {
            return -1;
        }
        return this.m_docHandle | this.gotslot[2];
    }

    @Override
    public int getNextNamespaceNode(int baseHandle, int namespaceHandle, boolean inScope) {
        return -1;
    }

    public int getNextDescendant(int subtreeRootHandle, int nodeHandle) {
        int subtreeRootHandle2 = subtreeRootHandle & NODEHANDLE_MASK;
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        if (nodeHandle2 == 0) {
            return -1;
        }
        while (true) {
            if (this.m_isError || (this.done && nodeHandle2 > this.nodes.slotsUsed())) {
                break;
            }
            if (nodeHandle2 > subtreeRootHandle2) {
                this.nodes.readSlot(nodeHandle2 + 1, this.gotslot);
                if (this.gotslot[2] != 0) {
                    short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
                    if (type == 2) {
                        nodeHandle2 += 2;
                    } else {
                        int nextParentPos = this.gotslot[1];
                        if (nextParentPos >= subtreeRootHandle2) {
                            return this.m_docHandle | (nodeHandle2 + 1);
                        }
                    }
                } else if (this.done) {
                    break;
                }
            } else {
                nodeHandle2++;
            }
        }
        return -1;
    }

    public int getNextFollowing(int axisContextHandle, int nodeHandle) {
        return -1;
    }

    public int getNextPreceding(int axisContextHandle, int nodeHandle) {
        int nodeHandle2 = nodeHandle & NODEHANDLE_MASK;
        while (nodeHandle2 > 1) {
            nodeHandle2--;
            if (2 != (this.nodes.readEntry(nodeHandle2, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
                return this.m_docHandle | this.nodes.specialFind(axisContextHandle, nodeHandle2);
            }
        }
        return -1;
    }

    @Override
    public int getParent(int nodeHandle) {
        return this.m_docHandle | this.nodes.readEntry(nodeHandle, 1);
    }

    public int getDocumentRoot() {
        return this.m_docHandle | this.m_docElement;
    }

    @Override
    public int getDocument() {
        return this.m_docHandle;
    }

    @Override
    public int getOwnerDocument(int nodeHandle) {
        if ((NODEHANDLE_MASK & nodeHandle) == 0) {
            return -1;
        }
        return DOCHANDLE_MASK & nodeHandle;
    }

    @Override
    public int getDocumentRoot(int nodeHandle) {
        if ((NODEHANDLE_MASK & nodeHandle) == 0) {
            return -1;
        }
        return DOCHANDLE_MASK & nodeHandle;
    }

    @Override
    public XMLString getStringValue(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        int nodetype = this.gotslot[0] & WalkerFactory.BITS_COUNT;
        String value = null;
        switch (nodetype) {
            case 3:
            case 4:
            case 8:
                value = this.m_char.getString(this.gotslot[2], this.gotslot[3]);
                break;
        }
        return this.m_xsf.newstr(value);
    }

    @Override
    public int getStringValueChunkCount(int nodeHandle) {
        return 0;
    }

    @Override
    public char[] getStringValueChunk(int nodeHandle, int chunkIndex, int[] startAndLen) {
        return new char[0];
    }

    @Override
    public int getExpandedTypeID(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        String qName = this.m_localNames.indexToString(this.gotslot[3]);
        int colonpos = qName.indexOf(":");
        String localName = qName.substring(colonpos + 1);
        String namespace = this.m_nsNames.indexToString(this.gotslot[0] << 16);
        String expandedName = namespace + ":" + localName;
        int expandedNameID = this.m_nsNames.stringToIndex(expandedName);
        return expandedNameID;
    }

    @Override
    public int getExpandedTypeID(String namespace, String localName, int type) {
        String expandedName = namespace + ":" + localName;
        int expandedNameID = this.m_nsNames.stringToIndex(expandedName);
        return expandedNameID;
    }

    @Override
    public String getLocalNameFromExpandedNameID(int ExpandedNameID) {
        String expandedName = this.m_localNames.indexToString(ExpandedNameID);
        int colonpos = expandedName.indexOf(":");
        String localName = expandedName.substring(colonpos + 1);
        return localName;
    }

    @Override
    public String getNamespaceFromExpandedNameID(int ExpandedNameID) {
        String expandedName = this.m_localNames.indexToString(ExpandedNameID);
        int colonpos = expandedName.indexOf(":");
        String nsName = expandedName.substring(0, colonpos);
        return nsName;
    }

    @Override
    public String getNodeName(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        String name = fixednames[type];
        if (name == null) {
            int i = this.gotslot[3];
            System.out.println("got i=" + i + " " + (i >> 16) + PsuedoNames.PSEUDONAME_ROOT + (i & DTMManager.IDENT_NODE_DEFAULT));
            String name2 = this.m_localNames.indexToString(i & DTMManager.IDENT_NODE_DEFAULT);
            String prefix = this.m_prefixNames.indexToString(i >> 16);
            if (prefix != null && prefix.length() > 0) {
                return prefix + ":" + name2;
            }
            return name2;
        }
        return name;
    }

    @Override
    public String getNodeNameX(int nodeHandle) {
        return null;
    }

    @Override
    public String getLocalName(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (type != 1 && type != 2) {
            return "";
        }
        int i = this.gotslot[3];
        String name = this.m_localNames.indexToString(i & DTMManager.IDENT_NODE_DEFAULT);
        return name == null ? "" : name;
    }

    @Override
    public String getPrefix(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        short type = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (type != 1 && type != 2) {
            return "";
        }
        int i = this.gotslot[3];
        String name = this.m_prefixNames.indexToString(i >> 16);
        return name == null ? "" : name;
    }

    @Override
    public String getNamespaceURI(int nodeHandle) {
        return null;
    }

    @Override
    public String getNodeValue(int nodeHandle) {
        this.nodes.readSlot(nodeHandle, this.gotslot);
        int nodetype = this.gotslot[0] & WalkerFactory.BITS_COUNT;
        switch (nodetype) {
            case 2:
                this.nodes.readSlot(nodeHandle + 1, this.gotslot);
                break;
            case 3:
            case 4:
            case 8:
                break;
            case 5:
            case 6:
            case 7:
            default:
                return null;
        }
        String value = this.m_char.getString(this.gotslot[2], this.gotslot[3]);
        return value;
    }

    @Override
    public short getNodeType(int nodeHandle) {
        return (short) (this.nodes.readEntry(nodeHandle, 0) & DTMManager.IDENT_NODE_DEFAULT);
    }

    @Override
    public short getLevel(int nodeHandle) {
        short count = 0;
        while (nodeHandle != 0) {
            count = (short) (count + 1);
            nodeHandle = this.nodes.readEntry(nodeHandle, 1);
        }
        return count;
    }

    @Override
    public boolean isSupported(String feature, String version) {
        return false;
    }

    @Override
    public String getDocumentBaseURI() {
        return this.m_documentBaseURI;
    }

    @Override
    public void setDocumentBaseURI(String baseURI) {
        this.m_documentBaseURI = baseURI;
    }

    @Override
    public String getDocumentSystemIdentifier(int nodeHandle) {
        return null;
    }

    @Override
    public String getDocumentEncoding(int nodeHandle) {
        return null;
    }

    @Override
    public String getDocumentStandalone(int nodeHandle) {
        return null;
    }

    @Override
    public String getDocumentVersion(int documentHandle) {
        return null;
    }

    @Override
    public boolean getDocumentAllDeclarationsProcessed() {
        return false;
    }

    @Override
    public String getDocumentTypeDeclarationSystemIdentifier() {
        return null;
    }

    @Override
    public String getDocumentTypeDeclarationPublicIdentifier() {
        return null;
    }

    @Override
    public int getElementById(String elementId) {
        return 0;
    }

    @Override
    public String getUnparsedEntityURI(String name) {
        return null;
    }

    @Override
    public boolean supportsPreStripping() {
        return false;
    }

    @Override
    public boolean isNodeAfter(int nodeHandle1, int nodeHandle2) {
        return false;
    }

    @Override
    public boolean isCharacterElementContentWhitespace(int nodeHandle) {
        return false;
    }

    @Override
    public boolean isDocumentAllDeclarationsProcessed(int documentHandle) {
        return false;
    }

    @Override
    public boolean isAttributeSpecified(int attributeHandle) {
        return false;
    }

    @Override
    public void dispatchCharactersEvents(int nodeHandle, ContentHandler ch, boolean normalize) throws SAXException {
    }

    @Override
    public void dispatchToEvents(int nodeHandle, ContentHandler ch) throws SAXException {
    }

    @Override
    public Node getNode(int nodeHandle) {
        return null;
    }

    @Override
    public void appendChild(int newChild, boolean clone, boolean cloneDepth) {
        boolean sameDoc = (DOCHANDLE_MASK & newChild) == this.m_docHandle;
        if (clone || sameDoc) {
        }
    }

    @Override
    public void appendTextChild(String str) {
    }

    void appendTextChild(int m_char_current_start, int contentLength) {
        int w1 = this.currentParent;
        int ourslot = appendNode(3, w1, m_char_current_start, contentLength);
        this.previousSibling = ourslot;
    }

    void appendComment(int m_char_current_start, int contentLength) {
        int w1 = this.currentParent;
        int ourslot = appendNode(8, w1, m_char_current_start, contentLength);
        this.previousSibling = ourslot;
    }

    void appendStartElement(int namespaceIndex, int localNameIndex, int prefixIndex) {
        int w0 = (namespaceIndex << 16) | 1;
        int w1 = this.currentParent;
        int w3 = localNameIndex | (prefixIndex << 16);
        System.out.println("set w3=" + w3 + " " + (w3 >> 16) + PsuedoNames.PSEUDONAME_ROOT + (65535 & w3));
        int ourslot = appendNode(w0, w1, 0, w3);
        this.currentParent = ourslot;
        this.previousSibling = 0;
        if (this.m_docElement != -1) {
            return;
        }
        this.m_docElement = ourslot;
    }

    void appendNSDeclaration(int prefixIndex, int namespaceIndex, boolean isID) {
        this.m_nsNames.stringToIndex(SerializerConstants.XMLNS_URI);
        int w0 = (this.m_nsNames.stringToIndex(SerializerConstants.XMLNS_URI) << 16) | 13;
        int w1 = this.currentParent;
        int ourslot = appendNode(w0, w1, 0, namespaceIndex);
        this.previousSibling = ourslot;
        this.previousSiblingWasParent = false;
    }

    void appendAttribute(int namespaceIndex, int localNameIndex, int prefixIndex, boolean isID, int m_char_current_start, int contentLength) {
        int w0 = (namespaceIndex << 16) | 2;
        int w1 = this.currentParent;
        int w3 = localNameIndex | (prefixIndex << 16);
        System.out.println("set w3=" + w3 + " " + (w3 >> 16) + PsuedoNames.PSEUDONAME_ROOT + (65535 & w3));
        int ourslot = appendNode(w0, w1, 0, w3);
        this.previousSibling = ourslot;
        appendNode(3, ourslot, m_char_current_start, contentLength);
        this.previousSiblingWasParent = true;
    }

    @Override
    public DTMAxisTraverser getAxisTraverser(int axis) {
        return null;
    }

    @Override
    public DTMAxisIterator getAxisIterator(int axis) {
        return null;
    }

    @Override
    public DTMAxisIterator getTypedAxisIterator(int axis, int type) {
        return null;
    }

    void appendEndElement() {
        if (this.previousSiblingWasParent) {
            this.nodes.writeEntry(this.previousSibling, 2, -1);
        }
        this.previousSibling = this.currentParent;
        this.nodes.readSlot(this.currentParent, this.gotslot);
        this.currentParent = this.gotslot[1] & DTMManager.IDENT_NODE_DEFAULT;
        this.previousSiblingWasParent = true;
    }

    void appendStartDocument() {
        this.m_docElement = -1;
        initDocument(0);
    }

    void appendEndDocument() {
        this.done = true;
    }

    @Override
    public void setProperty(String property, Object value) {
    }

    @Override
    public SourceLocator getSourceLocatorFor(int node) {
        return null;
    }

    @Override
    public void documentRegistration() {
    }

    @Override
    public void documentRelease() {
    }

    @Override
    public void migrateTo(DTMManager manager) {
    }
}
