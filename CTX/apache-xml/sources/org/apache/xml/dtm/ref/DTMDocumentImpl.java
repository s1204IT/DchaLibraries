package org.apache.xml.dtm.ref;

import java.io.PrintStream;
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

    public DTMDocumentImpl(DTMManager dTMManager, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory) {
        initDocument(i);
        this.m_xsf = xMLStringFactory;
    }

    public void setIncrementalSAXSource(IncrementalSAXSource incrementalSAXSource) {
        this.m_incrSAXSource = incrementalSAXSource;
        incrementalSAXSource.setContentHandler(this);
        incrementalSAXSource.setLexicalHandler(this);
    }

    private final int appendNode(int i, int i2, int i3, int i4) {
        int iAppendSlot = this.nodes.appendSlot(i, i2, i3, i4);
        if (this.previousSiblingWasParent) {
            this.nodes.writeEntry(this.previousSibling, 2, iAppendSlot);
        }
        this.previousSiblingWasParent = false;
        return iAppendSlot;
    }

    @Override
    public void setFeature(String str, boolean z) {
    }

    public void setLocalNameTable(DTMStringPool dTMStringPool) {
        this.m_localNames = dTMStringPool;
    }

    public DTMStringPool getLocalNameTable() {
        return this.m_localNames;
    }

    public void setNsNameTable(DTMStringPool dTMStringPool) {
        this.m_nsNames = dTMStringPool;
    }

    public DTMStringPool getNsNameTable() {
        return this.m_nsNames;
    }

    public void setPrefixNameTable(DTMStringPool dTMStringPool) {
        this.m_prefixNames = dTMStringPool;
    }

    public DTMStringPool getPrefixNameTable() {
        return this.m_prefixNames;
    }

    void setContentBuffer(FastStringBuffer fastStringBuffer) {
        this.m_char = fastStringBuffer;
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
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        this.m_char.append(cArr, i, i2);
    }

    private void processAccumulatedText() {
        int length = this.m_char.length();
        if (length != this.m_char_current_start) {
            appendTextChild(this.m_char_current_start, length - this.m_char_current_start);
            this.m_char_current_start = length;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        appendEndDocument();
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        processAccumulatedText();
        appendEndElement();
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        processAccumulatedText();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        processAccumulatedText();
    }

    @Override
    public void startDocument() throws SAXException {
        appendStartDocument();
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        String strSubstring;
        int length;
        String strSubstring2;
        String strSubstring3;
        String strSubstring4;
        processAccumulatedText();
        int iIndexOf = str3.indexOf(58);
        if (iIndexOf > 0) {
            strSubstring = str3.substring(0, iIndexOf);
        } else {
            strSubstring = null;
        }
        System.out.println("Prefix=" + strSubstring + " index=" + this.m_prefixNames.stringToIndex(strSubstring));
        appendStartElement(this.m_nsNames.stringToIndex(str), this.m_localNames.stringToIndex(str2), this.m_prefixNames.stringToIndex(strSubstring));
        if (attributes != null) {
            length = attributes.getLength();
        } else {
            length = 0;
        }
        int i = length - 1;
        for (int i2 = i; i2 >= 0; i2--) {
            String qName = attributes.getQName(i2);
            if (qName.startsWith(Constants.ATTRNAME_XMLNS) || "xmlns".equals(qName)) {
                int iIndexOf2 = qName.indexOf(58);
                if (iIndexOf2 > 0) {
                    strSubstring4 = qName.substring(0, iIndexOf2);
                } else {
                    strSubstring4 = null;
                }
                appendNSDeclaration(this.m_prefixNames.stringToIndex(strSubstring4), this.m_nsNames.stringToIndex(attributes.getValue(i2)), attributes.getType(i2).equalsIgnoreCase("ID"));
            }
        }
        for (int i3 = i; i3 >= 0; i3--) {
            String qName2 = attributes.getQName(i3);
            if (!qName2.startsWith(Constants.ATTRNAME_XMLNS) && !"xmlns".equals(qName2)) {
                int iIndexOf3 = qName2.indexOf(58);
                if (iIndexOf3 > 0) {
                    strSubstring2 = qName2.substring(0, iIndexOf3);
                    strSubstring3 = qName2.substring(iIndexOf3 + 1);
                } else {
                    strSubstring2 = "";
                    strSubstring3 = qName2;
                }
                this.m_char.append(attributes.getValue(i3));
                int length2 = this.m_char.length();
                if (!"xmlns".equals(strSubstring2) && !"xmlns".equals(qName2)) {
                    appendAttribute(this.m_nsNames.stringToIndex(attributes.getURI(i3)), this.m_localNames.stringToIndex(strSubstring3), this.m_prefixNames.stringToIndex(strSubstring2), attributes.getType(i3).equalsIgnoreCase("ID"), this.m_char_current_start, length2 - this.m_char_current_start);
                }
                this.m_char_current_start = length2;
            }
        }
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        processAccumulatedText();
        this.m_char.append(cArr, i, i2);
        appendComment(this.m_char_current_start, i2);
        this.m_char_current_start += i2;
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void endEntity(String str) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void startEntity(String str) throws SAXException {
    }

    final void initDocument(int i) {
        this.m_docHandle = i << 22;
        this.nodes.writeSlot(0, 9, -1, -1, 0);
        this.done = false;
    }

    @Override
    public boolean hasChildNodes(int i) {
        return getFirstChild(i) != -1;
    }

    @Override
    public int getFirstChild(int i) {
        int i2 = i & NODEHANDLE_MASK;
        this.nodes.readSlot(i2, this.gotslot);
        short s = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (s == 1 || s == 9 || s == 5) {
            int i3 = i2 + 1;
            this.nodes.readSlot(i3, this.gotslot);
            while (2 == (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT)) {
                i3 = this.gotslot[2];
                if (i3 == -1) {
                    return -1;
                }
                this.nodes.readSlot(i3, this.gotslot);
            }
            if (this.gotslot[1] == i2) {
                return this.m_docHandle | i3;
            }
        }
        return -1;
    }

    @Override
    public int getLastChild(int i) {
        int firstChild = getFirstChild(i & NODEHANDLE_MASK);
        int i2 = -1;
        while (firstChild != -1) {
            i2 = firstChild;
            firstChild = getNextSibling(firstChild);
        }
        return this.m_docHandle | i2;
    }

    @Override
    public int getAttributeNode(int i, String str, String str2) {
        int iStringToIndex = this.m_nsNames.stringToIndex(str);
        int iStringToIndex2 = this.m_localNames.stringToIndex(str2);
        int i2 = i & NODEHANDLE_MASK;
        this.nodes.readSlot(i2, this.gotslot);
        short s = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (s == 1) {
            i2++;
        }
        while (s == 2) {
            if (iStringToIndex != (this.gotslot[0] << 16) || this.gotslot[3] != iStringToIndex2) {
                i2 = this.gotslot[2];
                this.nodes.readSlot(i2, this.gotslot);
            } else {
                return i2 | this.m_docHandle;
            }
        }
        return -1;
    }

    @Override
    public int getFirstAttribute(int i) {
        int i2 = i & NODEHANDLE_MASK;
        if (1 != (this.nodes.readEntry(i2, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
            return -1;
        }
        int i3 = i2 + 1;
        if (2 == (this.nodes.readEntry(i3, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
            return i3 | this.m_docHandle;
        }
        return -1;
    }

    @Override
    public int getFirstNamespaceNode(int i, boolean z) {
        return -1;
    }

    @Override
    public int getNextSibling(int i) {
        int i2 = i & NODEHANDLE_MASK;
        if (i2 == 0) {
            return -1;
        }
        short entry = (short) (this.nodes.readEntry(i2, 0) & DTMManager.IDENT_NODE_DEFAULT);
        if (entry == 1 || entry == 2 || entry == 5) {
            int entry2 = this.nodes.readEntry(i2, 2);
            if (entry2 == -1) {
                return -1;
            }
            if (entry2 != 0) {
                return this.m_docHandle | entry2;
            }
        }
        int entry3 = this.nodes.readEntry(i2, 1);
        int i3 = i2 + 1;
        if (this.nodes.readEntry(i3, 1) != entry3) {
            return -1;
        }
        return i3 | this.m_docHandle;
    }

    @Override
    public int getPreviousSibling(int i) {
        int i2 = i & NODEHANDLE_MASK;
        int i3 = -1;
        if (i2 == 0) {
            return -1;
        }
        int firstChild = getFirstChild(this.nodes.readEntry(i2, 1));
        while (true) {
            int i4 = firstChild;
            int i5 = i3;
            i3 = i4;
            if (i3 != i2) {
                firstChild = getNextSibling(i3);
            } else {
                return this.m_docHandle | i5;
            }
        }
    }

    @Override
    public int getNextAttribute(int i) {
        int i2 = i & NODEHANDLE_MASK;
        this.nodes.readSlot(i2, this.gotslot);
        short s = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (s == 1) {
            return getFirstAttribute(i2);
        }
        if (s != 2 || this.gotslot[2] == -1) {
            return -1;
        }
        return this.m_docHandle | this.gotslot[2];
    }

    @Override
    public int getNextNamespaceNode(int i, int i2, boolean z) {
        return -1;
    }

    public int getNextDescendant(int i, int i2) {
        int i3 = i & NODEHANDLE_MASK;
        int i4 = i2 & NODEHANDLE_MASK;
        if (i4 == 0) {
            return -1;
        }
        while (true) {
            if (this.m_isError || (this.done && i4 > this.nodes.slotsUsed())) {
                break;
            }
            if (i4 > i3) {
                int i5 = i4 + 1;
                this.nodes.readSlot(i5, this.gotslot);
                if (this.gotslot[2] != 0) {
                    if (((short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT)) == 2) {
                        i4 += 2;
                    } else if (this.gotslot[1] >= i3) {
                        return this.m_docHandle | i5;
                    }
                } else if (this.done) {
                    break;
                }
            } else {
                i4++;
            }
        }
        return -1;
    }

    public int getNextFollowing(int i, int i2) {
        return -1;
    }

    public int getNextPreceding(int i, int i2) {
        int i3 = i2 & NODEHANDLE_MASK;
        while (i3 > 1) {
            i3--;
            if (2 != (this.nodes.readEntry(i3, 0) & DTMManager.IDENT_NODE_DEFAULT)) {
                return this.nodes.specialFind(i, i3) | this.m_docHandle;
            }
        }
        return -1;
    }

    @Override
    public int getParent(int i) {
        return this.nodes.readEntry(i, 1) | this.m_docHandle;
    }

    public int getDocumentRoot() {
        return this.m_docHandle | this.m_docElement;
    }

    @Override
    public int getDocument() {
        return this.m_docHandle;
    }

    @Override
    public int getOwnerDocument(int i) {
        if ((NODEHANDLE_MASK & i) == 0) {
            return -1;
        }
        return i & DOCHANDLE_MASK;
    }

    @Override
    public int getDocumentRoot(int i) {
        if ((NODEHANDLE_MASK & i) == 0) {
            return -1;
        }
        return i & DOCHANDLE_MASK;
    }

    @Override
    public XMLString getStringValue(int i) {
        String string;
        this.nodes.readSlot(i, this.gotslot);
        int i2 = this.gotslot[0] & WalkerFactory.BITS_COUNT;
        if (i2 != 8) {
            switch (i2) {
                case 3:
                case 4:
                    string = this.m_char.getString(this.gotslot[2], this.gotslot[3]);
                    break;
                default:
                    string = null;
                    break;
            }
        }
        return this.m_xsf.newstr(string);
    }

    @Override
    public int getStringValueChunkCount(int i) {
        return 0;
    }

    @Override
    public char[] getStringValueChunk(int i, int i2, int[] iArr) {
        return new char[0];
    }

    @Override
    public int getExpandedTypeID(int i) {
        this.nodes.readSlot(i, this.gotslot);
        String strIndexToString = this.m_localNames.indexToString(this.gotslot[3]);
        String strSubstring = strIndexToString.substring(strIndexToString.indexOf(":") + 1);
        return this.m_nsNames.stringToIndex(this.m_nsNames.indexToString(this.gotslot[0] << 16) + ":" + strSubstring);
    }

    @Override
    public int getExpandedTypeID(String str, String str2, int i) {
        return this.m_nsNames.stringToIndex(str + ":" + str2);
    }

    @Override
    public String getLocalNameFromExpandedNameID(int i) {
        String strIndexToString = this.m_localNames.indexToString(i);
        return strIndexToString.substring(strIndexToString.indexOf(":") + 1);
    }

    @Override
    public String getNamespaceFromExpandedNameID(int i) {
        String strIndexToString = this.m_localNames.indexToString(i);
        return strIndexToString.substring(0, strIndexToString.indexOf(":"));
    }

    @Override
    public String getNodeName(int i) {
        this.nodes.readSlot(i, this.gotslot);
        String str = fixednames[(short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT)];
        if (str == null) {
            int i2 = this.gotslot[3];
            PrintStream printStream = System.out;
            StringBuilder sb = new StringBuilder();
            sb.append("got i=");
            sb.append(i2);
            sb.append(" ");
            int i3 = i2 >> 16;
            sb.append(i3);
            sb.append(PsuedoNames.PSEUDONAME_ROOT);
            int i4 = i2 & DTMManager.IDENT_NODE_DEFAULT;
            sb.append(i4);
            printStream.println(sb.toString());
            String strIndexToString = this.m_localNames.indexToString(i4);
            String strIndexToString2 = this.m_prefixNames.indexToString(i3);
            if (strIndexToString2 != null && strIndexToString2.length() > 0) {
                return strIndexToString2 + ":" + strIndexToString;
            }
            return strIndexToString;
        }
        return str;
    }

    @Override
    public String getNodeNameX(int i) {
        return null;
    }

    @Override
    public String getLocalName(int i) {
        this.nodes.readSlot(i, this.gotslot);
        short s = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (s != 1 && s != 2) {
            return "";
        }
        String strIndexToString = this.m_localNames.indexToString(this.gotslot[3] & DTMManager.IDENT_NODE_DEFAULT);
        return strIndexToString == null ? "" : strIndexToString;
    }

    @Override
    public String getPrefix(int i) {
        this.nodes.readSlot(i, this.gotslot);
        short s = (short) (this.gotslot[0] & DTMManager.IDENT_NODE_DEFAULT);
        if (s != 1 && s != 2) {
            return "";
        }
        String strIndexToString = this.m_prefixNames.indexToString(this.gotslot[3] >> 16);
        return strIndexToString == null ? "" : strIndexToString;
    }

    @Override
    public String getNamespaceURI(int i) {
        return null;
    }

    @Override
    public String getNodeValue(int i) {
        this.nodes.readSlot(i, this.gotslot);
        int i2 = this.gotslot[0] & WalkerFactory.BITS_COUNT;
        if (i2 != 8) {
            switch (i2) {
                case 2:
                    this.nodes.readSlot(i + 1, this.gotslot);
                    break;
                case 3:
                case 4:
                    break;
                default:
                    return null;
            }
        }
        return this.m_char.getString(this.gotslot[2], this.gotslot[3]);
    }

    @Override
    public short getNodeType(int i) {
        return (short) (this.nodes.readEntry(i, 0) & DTMManager.IDENT_NODE_DEFAULT);
    }

    @Override
    public short getLevel(int i) {
        short s = 0;
        while (i != 0) {
            s = (short) (s + 1);
            i = this.nodes.readEntry(i, 1);
        }
        return s;
    }

    @Override
    public boolean isSupported(String str, String str2) {
        return false;
    }

    @Override
    public String getDocumentBaseURI() {
        return this.m_documentBaseURI;
    }

    @Override
    public void setDocumentBaseURI(String str) {
        this.m_documentBaseURI = str;
    }

    @Override
    public String getDocumentSystemIdentifier(int i) {
        return null;
    }

    @Override
    public String getDocumentEncoding(int i) {
        return null;
    }

    @Override
    public String getDocumentStandalone(int i) {
        return null;
    }

    @Override
    public String getDocumentVersion(int i) {
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
    public int getElementById(String str) {
        return 0;
    }

    @Override
    public String getUnparsedEntityURI(String str) {
        return null;
    }

    @Override
    public boolean supportsPreStripping() {
        return false;
    }

    @Override
    public boolean isNodeAfter(int i, int i2) {
        return false;
    }

    @Override
    public boolean isCharacterElementContentWhitespace(int i) {
        return false;
    }

    @Override
    public boolean isDocumentAllDeclarationsProcessed(int i) {
        return false;
    }

    @Override
    public boolean isAttributeSpecified(int i) {
        return false;
    }

    @Override
    public void dispatchCharactersEvents(int i, ContentHandler contentHandler, boolean z) throws SAXException {
    }

    @Override
    public void dispatchToEvents(int i, ContentHandler contentHandler) throws SAXException {
    }

    @Override
    public Node getNode(int i) {
        return null;
    }

    @Override
    public void appendChild(int i, boolean z, boolean z2) {
        int i2 = i & DOCHANDLE_MASK;
        int i3 = this.m_docHandle;
    }

    @Override
    public void appendTextChild(String str) {
    }

    void appendTextChild(int i, int i2) {
        this.previousSibling = appendNode(3, this.currentParent, i, i2);
    }

    void appendComment(int i, int i2) {
        this.previousSibling = appendNode(8, this.currentParent, i, i2);
    }

    void appendStartElement(int i, int i2, int i3) {
        int i4 = this.currentParent;
        int i5 = i2 | (i3 << 16);
        System.out.println("set w3=" + i5 + " " + (i5 >> 16) + PsuedoNames.PSEUDONAME_ROOT + (65535 & i5));
        int iAppendNode = appendNode((i << 16) | 1, i4, 0, i5);
        this.currentParent = iAppendNode;
        this.previousSibling = 0;
        if (this.m_docElement == -1) {
            this.m_docElement = iAppendNode;
        }
    }

    void appendNSDeclaration(int i, int i2, boolean z) {
        this.m_nsNames.stringToIndex(SerializerConstants.XMLNS_URI);
        this.previousSibling = appendNode((this.m_nsNames.stringToIndex(SerializerConstants.XMLNS_URI) << 16) | 13, this.currentParent, 0, i2);
        this.previousSiblingWasParent = false;
    }

    void appendAttribute(int i, int i2, int i3, boolean z, int i4, int i5) {
        int i6 = this.currentParent;
        int i7 = i2 | (i3 << 16);
        System.out.println("set w3=" + i7 + " " + (i7 >> 16) + PsuedoNames.PSEUDONAME_ROOT + (65535 & i7));
        int iAppendNode = appendNode((i << 16) | 2, i6, 0, i7);
        this.previousSibling = iAppendNode;
        appendNode(3, iAppendNode, i4, i5);
        this.previousSiblingWasParent = true;
    }

    @Override
    public DTMAxisTraverser getAxisTraverser(int i) {
        return null;
    }

    @Override
    public DTMAxisIterator getAxisIterator(int i) {
        return null;
    }

    @Override
    public DTMAxisIterator getTypedAxisIterator(int i, int i2) {
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
    public void setProperty(String str, Object obj) {
    }

    @Override
    public SourceLocator getSourceLocatorFor(int i) {
        return null;
    }

    @Override
    public void documentRegistration() {
    }

    @Override
    public void documentRelease() {
    }

    @Override
    public void migrateTo(DTMManager dTMManager) {
    }
}
