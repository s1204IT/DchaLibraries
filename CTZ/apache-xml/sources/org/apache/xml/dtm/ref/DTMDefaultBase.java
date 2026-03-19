package org.apache.xml.dtm.ref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;
import javax.xml.transform.Source;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.BoolStack;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class DTMDefaultBase implements DTM {
    public static final int DEFAULT_BLOCKSIZE = 512;
    public static final int DEFAULT_NUMBLOCKS = 32;
    public static final int DEFAULT_NUMBLOCKS_SMALL = 4;
    static final boolean JJK_DEBUG = false;
    protected static final int NOTPROCESSED = -2;
    public static final int ROOTNODE = 0;
    protected String m_documentBaseURI;
    protected SuballocatedIntVector m_dtmIdent;
    protected int[][][] m_elemIndexes;
    protected ExpandedNameTable m_expandedNameTable;
    protected SuballocatedIntVector m_exptype;
    protected SuballocatedIntVector m_firstch;
    protected boolean m_indexing;
    public DTMManager m_mgr;
    protected DTMManagerDefault m_mgrDefault;
    protected SuballocatedIntVector m_namespaceDeclSetElements;
    protected Vector m_namespaceDeclSets;
    private Vector m_namespaceLists;
    protected SuballocatedIntVector m_nextsib;
    protected SuballocatedIntVector m_parent;
    protected SuballocatedIntVector m_prevsib;
    protected boolean m_shouldStripWS;
    protected BoolStack m_shouldStripWhitespaceStack;
    protected int m_size;
    protected DTMAxisTraverser[] m_traversers;
    protected DTMWSFilter m_wsfilter;
    protected XMLStringFactory m_xstrf;

    @Override
    public abstract void dispatchCharactersEvents(int i, ContentHandler contentHandler, boolean z) throws SAXException;

    @Override
    public abstract void dispatchToEvents(int i, ContentHandler contentHandler) throws SAXException;

    @Override
    public abstract int getAttributeNode(int i, String str, String str2);

    @Override
    public abstract String getDocumentTypeDeclarationPublicIdentifier();

    @Override
    public abstract String getDocumentTypeDeclarationSystemIdentifier();

    @Override
    public abstract int getElementById(String str);

    @Override
    public abstract String getLocalName(int i);

    @Override
    public abstract String getNamespaceURI(int i);

    protected abstract int getNextNodeIdentity(int i);

    @Override
    public abstract String getNodeName(int i);

    @Override
    public abstract String getNodeValue(int i);

    protected abstract int getNumberOfNodes();

    @Override
    public abstract String getPrefix(int i);

    @Override
    public abstract XMLString getStringValue(int i);

    @Override
    public abstract String getUnparsedEntityURI(String str);

    @Override
    public abstract boolean isAttributeSpecified(int i);

    protected abstract boolean nextNode();

    public DTMDefaultBase(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z) {
        this(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, 512, true, false);
    }

    public DTMDefaultBase(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z, int i2, boolean z2, boolean z3) {
        this.m_size = 0;
        this.m_namespaceDeclSets = null;
        this.m_namespaceDeclSetElements = null;
        this.m_mgrDefault = null;
        this.m_shouldStripWS = false;
        this.m_namespaceLists = null;
        int i3 = 32;
        if (i2 <= 64) {
            this.m_dtmIdent = new SuballocatedIntVector(4, 1);
            i3 = 4;
        } else {
            this.m_dtmIdent = new SuballocatedIntVector(32);
        }
        this.m_exptype = new SuballocatedIntVector(i2, i3);
        this.m_firstch = new SuballocatedIntVector(i2, i3);
        this.m_nextsib = new SuballocatedIntVector(i2, i3);
        this.m_parent = new SuballocatedIntVector(i2, i3);
        if (z2) {
            this.m_prevsib = new SuballocatedIntVector(i2, i3);
        }
        this.m_mgr = dTMManager;
        if (dTMManager instanceof DTMManagerDefault) {
            this.m_mgrDefault = (DTMManagerDefault) dTMManager;
        }
        this.m_documentBaseURI = source != null ? source.getSystemId() : null;
        this.m_dtmIdent.setElementAt(i, 0);
        this.m_wsfilter = dTMWSFilter;
        this.m_xstrf = xMLStringFactory;
        this.m_indexing = z;
        if (z) {
            this.m_expandedNameTable = new ExpandedNameTable();
        } else {
            this.m_expandedNameTable = this.m_mgrDefault.getExpandedNameTable(this);
        }
        if (dTMWSFilter != null) {
            this.m_shouldStripWhitespaceStack = new BoolStack();
            pushShouldStripWhitespace(false);
        }
    }

    protected void ensureSizeOfIndex(int i, int i2) {
        if (this.m_elemIndexes == null) {
            this.m_elemIndexes = new int[i + 20][][];
        } else if (this.m_elemIndexes.length <= i) {
            int[][][] iArr = this.m_elemIndexes;
            this.m_elemIndexes = new int[i + 20][][];
            System.arraycopy(iArr, 0, this.m_elemIndexes, 0, iArr.length);
        }
        int[][] iArr2 = this.m_elemIndexes[i];
        if (iArr2 == null) {
            iArr2 = new int[i2 + 100][];
            this.m_elemIndexes[i] = iArr2;
        } else if (iArr2.length <= i2) {
            int[][] iArr3 = new int[i2 + 100][];
            System.arraycopy(iArr2, 0, iArr3, 0, iArr2.length);
            this.m_elemIndexes[i] = iArr3;
            iArr2 = iArr3;
        }
        int[] iArr4 = iArr2[i2];
        if (iArr4 != null) {
            if (iArr4.length <= iArr4[0] + 1) {
                int[] iArr5 = new int[iArr4[0] + 1024];
                System.arraycopy(iArr4, 0, iArr5, 0, iArr4.length);
                iArr2[i2] = iArr5;
                return;
            }
            return;
        }
        int[] iArr6 = new int[128];
        iArr2[i2] = iArr6;
        iArr6[0] = 1;
    }

    protected void indexNode(int i, int i2) {
        ExpandedNameTable expandedNameTable = this.m_expandedNameTable;
        if (1 == expandedNameTable.getType(i)) {
            int namespaceID = expandedNameTable.getNamespaceID(i);
            int localNameID = expandedNameTable.getLocalNameID(i);
            ensureSizeOfIndex(namespaceID, localNameID);
            int[] iArr = this.m_elemIndexes[namespaceID][localNameID];
            iArr[iArr[0]] = i2;
            iArr[0] = iArr[0] + 1;
        }
    }

    protected int findGTE(int[] iArr, int i, int i2, int i3) {
        int i4 = (i2 - 1) + i;
        int i5 = i4;
        while (i <= i5) {
            int i6 = (i + i5) / 2;
            int i7 = iArr[i6];
            if (i7 > i3) {
                i5 = i6 - 1;
            } else if (i7 < i3) {
                i = i6 + 1;
            } else {
                return i6;
            }
        }
        if (i > i4 || iArr[i] <= i3) {
            return -1;
        }
        return i;
    }

    int findElementFromIndex(int i, int i2, int i3) {
        int[][] iArr;
        int[] iArr2;
        int iFindGTE;
        int[][][] iArr3 = this.m_elemIndexes;
        if (iArr3 != null && i < iArr3.length && (iArr = iArr3[i]) != null && i2 < iArr.length && (iArr2 = iArr[i2]) != null && (iFindGTE = findGTE(iArr2, 1, iArr2[0], i3)) > -1) {
            return iArr2[iFindGTE];
        }
        return -2;
    }

    protected short _type(int i) {
        int i_exptype = _exptype(i);
        if (-1 == i_exptype) {
            return (short) -1;
        }
        return this.m_expandedNameTable.getType(i_exptype);
    }

    protected int _exptype(int i) {
        if (i == -1) {
            return -1;
        }
        while (i >= this.m_size) {
            if (!nextNode() && i >= this.m_size) {
                return -1;
            }
        }
        return this.m_exptype.elementAt(i);
    }

    protected int _level(int i) {
        while (i >= this.m_size) {
            if (!nextNode() && i >= this.m_size) {
                return -1;
            }
        }
        int i2 = 0;
        while (true) {
            i = _parent(i);
            if (-1 != i) {
                i2++;
            } else {
                return i2;
            }
        }
    }

    protected int _firstch(int i) {
        int iElementAt;
        if (i < this.m_size) {
            iElementAt = this.m_firstch.elementAt(i);
        } else {
            iElementAt = -2;
        }
        while (iElementAt == -2) {
            boolean zNextNode = nextNode();
            if (i >= this.m_size && !zNextNode) {
                return -1;
            }
            int iElementAt2 = this.m_firstch.elementAt(i);
            if (iElementAt2 == -2 && !zNextNode) {
                return -1;
            }
            iElementAt = iElementAt2;
        }
        return iElementAt;
    }

    protected int _nextsib(int i) {
        int iElementAt;
        if (i < this.m_size) {
            iElementAt = this.m_nextsib.elementAt(i);
        } else {
            iElementAt = -2;
        }
        while (iElementAt == -2) {
            boolean zNextNode = nextNode();
            if (i >= this.m_size && !zNextNode) {
                return -1;
            }
            int iElementAt2 = this.m_nextsib.elementAt(i);
            if (iElementAt2 == -2 && !zNextNode) {
                return -1;
            }
            iElementAt = iElementAt2;
        }
        return iElementAt;
    }

    protected int _prevsib(int i) {
        if (i < this.m_size) {
            return this.m_prevsib.elementAt(i);
        }
        do {
            boolean zNextNode = nextNode();
            if (i >= this.m_size && !zNextNode) {
                return -1;
            }
        } while (i >= this.m_size);
        return this.m_prevsib.elementAt(i);
    }

    protected int _parent(int i) {
        if (i < this.m_size) {
            return this.m_parent.elementAt(i);
        }
        do {
            boolean zNextNode = nextNode();
            if (i >= this.m_size && !zNextNode) {
                return -1;
            }
        } while (i >= this.m_size);
        return this.m_parent.elementAt(i);
    }

    public void dumpDTM(OutputStream outputStream) {
        String str;
        if (outputStream == null) {
            try {
                File file = new File("DTMDump" + hashCode() + ".txt");
                System.err.println("Dumping... " + file.getAbsolutePath());
                outputStream = new FileOutputStream(file);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                throw new RuntimeException(e.getMessage());
            }
        }
        PrintStream printStream = new PrintStream(outputStream);
        while (nextNode()) {
        }
        int i = this.m_size;
        printStream.println("Total nodes: " + i);
        for (int i2 = 0; i2 < i; i2++) {
            int iMakeNodeHandle = makeNodeHandle(i2);
            printStream.println("=========== index=" + i2 + " handle=" + iMakeNodeHandle + " ===========");
            StringBuilder sb = new StringBuilder();
            sb.append("NodeName: ");
            sb.append(getNodeName(iMakeNodeHandle));
            printStream.println(sb.toString());
            printStream.println("NodeNameX: " + getNodeNameX(iMakeNodeHandle));
            printStream.println("LocalName: " + getLocalName(iMakeNodeHandle));
            printStream.println("NamespaceURI: " + getNamespaceURI(iMakeNodeHandle));
            printStream.println("Prefix: " + getPrefix(iMakeNodeHandle));
            printStream.println("Expanded Type ID: " + Integer.toHexString(_exptype(i2)));
            short s_type = _type(i2);
            if (s_type != -1) {
                switch (s_type) {
                    case 1:
                        str = "ELEMENT_NODE";
                        break;
                    case 2:
                        str = "ATTRIBUTE_NODE";
                        break;
                    case 3:
                        str = "TEXT_NODE";
                        break;
                    case 4:
                        str = "CDATA_SECTION_NODE";
                        break;
                    case 5:
                        str = "ENTITY_REFERENCE_NODE";
                        break;
                    case 6:
                        str = "ENTITY_NODE";
                        break;
                    case 7:
                        str = "PROCESSING_INSTRUCTION_NODE";
                        break;
                    case 8:
                        str = "COMMENT_NODE";
                        break;
                    case 9:
                        str = "DOCUMENT_NODE";
                        break;
                    case 10:
                        str = "DOCUMENT_NODE";
                        break;
                    case 11:
                        str = "DOCUMENT_FRAGMENT_NODE";
                        break;
                    case 12:
                        str = "NOTATION_NODE";
                        break;
                    case 13:
                        str = "NAMESPACE_NODE";
                        break;
                    default:
                        str = "Unknown!";
                        break;
                }
            } else {
                str = "NULL";
            }
            printStream.println("Type: " + str);
            int i_firstch = _firstch(i2);
            if (-1 == i_firstch) {
                printStream.println("First child: DTM.NULL");
            } else if (-2 == i_firstch) {
                printStream.println("First child: NOTPROCESSED");
            } else {
                printStream.println("First child: " + i_firstch);
            }
            if (this.m_prevsib != null) {
                int i_prevsib = _prevsib(i2);
                if (-1 == i_prevsib) {
                    printStream.println("Prev sibling: DTM.NULL");
                } else if (-2 == i_prevsib) {
                    printStream.println("Prev sibling: NOTPROCESSED");
                } else {
                    printStream.println("Prev sibling: " + i_prevsib);
                }
            }
            int i_nextsib = _nextsib(i2);
            if (-1 == i_nextsib) {
                printStream.println("Next sibling: DTM.NULL");
            } else if (-2 == i_nextsib) {
                printStream.println("Next sibling: NOTPROCESSED");
            } else {
                printStream.println("Next sibling: " + i_nextsib);
            }
            int i_parent = _parent(i2);
            if (-1 == i_parent) {
                printStream.println("Parent: DTM.NULL");
            } else if (-2 == i_parent) {
                printStream.println("Parent: NOTPROCESSED");
            } else {
                printStream.println("Parent: " + i_parent);
            }
            printStream.println("Level: " + _level(i2));
            printStream.println("Node Value: " + getNodeValue(iMakeNodeHandle));
            printStream.println("String Value: " + getStringValue(iMakeNodeHandle));
        }
    }

    public String dumpNode(int i) {
        String str;
        if (i == -1) {
            return "[null]";
        }
        short nodeType = getNodeType(i);
        if (nodeType != -1) {
            switch (nodeType) {
                case 1:
                    str = "ELEMENT";
                    break;
                case 2:
                    str = "ATTR";
                    break;
                case 3:
                    str = "TEXT";
                    break;
                case 4:
                    str = "CDATA";
                    break;
                case 5:
                    str = "ENT_REF";
                    break;
                case 6:
                    str = "ENTITY";
                    break;
                case 7:
                    str = "PI";
                    break;
                case 8:
                    str = "COMMENT";
                    break;
                case 9:
                    str = "DOC";
                    break;
                case 10:
                    str = "DOC_TYPE";
                    break;
                case 11:
                    str = "DOC_FRAG";
                    break;
                case 12:
                    str = "NOTATION";
                    break;
                case 13:
                    str = "NAMESPACE";
                    break;
                default:
                    str = "Unknown!";
                    break;
            }
        } else {
            str = "null";
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[" + i + ": " + str + "(0x" + Integer.toHexString(getExpandedTypeID(i)) + ") " + getNodeNameX(i) + " {" + getNamespaceURI(i) + "}=\"" + getNodeValue(i) + "\"]");
        return stringBuffer.toString();
    }

    @Override
    public void setFeature(String str, boolean z) {
    }

    @Override
    public boolean hasChildNodes(int i) {
        return _firstch(makeNodeIdentity(i)) != -1;
    }

    public final int makeNodeHandle(int i) {
        if (-1 == i) {
            return -1;
        }
        return this.m_dtmIdent.elementAt(i >>> 16) + (i & DTMManager.IDENT_NODE_DEFAULT);
    }

    public final int makeNodeIdentity(int i) {
        if (-1 == i) {
            return -1;
        }
        if (this.m_mgrDefault != null) {
            int i2 = i >>> 16;
            if (this.m_mgrDefault.m_dtms[i2] != this) {
                return -1;
            }
            return (i & DTMManager.IDENT_NODE_DEFAULT) | this.m_mgrDefault.m_dtm_offsets[i2];
        }
        int iIndexOf = this.m_dtmIdent.indexOf((-65536) & i);
        if (iIndexOf == -1) {
            return -1;
        }
        return (iIndexOf << 16) + (i & DTMManager.IDENT_NODE_DEFAULT);
    }

    @Override
    public int getFirstChild(int i) {
        return makeNodeHandle(_firstch(makeNodeIdentity(i)));
    }

    public int getTypedFirstChild(int i, int i2) {
        if (i2 < 14) {
            int i_firstch = _firstch(makeNodeIdentity(i));
            while (i_firstch != -1) {
                int i_exptype = _exptype(i_firstch);
                if (i_exptype != i2 && (i_exptype < 14 || this.m_expandedNameTable.getType(i_exptype) != i2)) {
                    i_firstch = _nextsib(i_firstch);
                } else {
                    return makeNodeHandle(i_firstch);
                }
            }
        } else {
            int i_firstch2 = _firstch(makeNodeIdentity(i));
            while (i_firstch2 != -1) {
                if (_exptype(i_firstch2) != i2) {
                    i_firstch2 = _nextsib(i_firstch2);
                } else {
                    return makeNodeHandle(i_firstch2);
                }
            }
        }
        return -1;
    }

    @Override
    public int getLastChild(int i) {
        int i_firstch = _firstch(makeNodeIdentity(i));
        int i2 = -1;
        while (i_firstch != -1) {
            i2 = i_firstch;
            i_firstch = _nextsib(i_firstch);
        }
        return makeNodeHandle(i2);
    }

    @Override
    public int getFirstAttribute(int i) {
        return makeNodeHandle(getFirstAttributeIdentity(makeNodeIdentity(i)));
    }

    protected int getFirstAttributeIdentity(int i) {
        short s_type;
        if (1 == _type(i)) {
            do {
                i = getNextNodeIdentity(i);
                if (-1 == i) {
                    break;
                }
                s_type = _type(i);
                if (s_type == 2) {
                    return i;
                }
            } while (13 == s_type);
        }
        return -1;
    }

    protected int getTypedAttribute(int i, int i2) {
        if (1 == getNodeType(i)) {
            int iMakeNodeIdentity = makeNodeIdentity(i);
            while (true) {
                iMakeNodeIdentity = getNextNodeIdentity(iMakeNodeIdentity);
                if (-1 == iMakeNodeIdentity) {
                    break;
                }
                short s_type = _type(iMakeNodeIdentity);
                if (s_type == 2) {
                    if (_exptype(iMakeNodeIdentity) == i2) {
                        return makeNodeHandle(iMakeNodeIdentity);
                    }
                } else if (13 != s_type) {
                    break;
                }
            }
        }
        return -1;
    }

    @Override
    public int getNextSibling(int i) {
        if (i == -1) {
            return -1;
        }
        return makeNodeHandle(_nextsib(makeNodeIdentity(i)));
    }

    public int getTypedNextSibling(int i, int i2) {
        int i_exptype;
        if (i == -1) {
            return -1;
        }
        int iMakeNodeIdentity = makeNodeIdentity(i);
        do {
            iMakeNodeIdentity = _nextsib(iMakeNodeIdentity);
            if (iMakeNodeIdentity == -1 || (i_exptype = _exptype(iMakeNodeIdentity)) == i2) {
                break;
            }
        } while (this.m_expandedNameTable.getType(i_exptype) != i2);
        if (iMakeNodeIdentity == -1) {
            return -1;
        }
        return makeNodeHandle(iMakeNodeIdentity);
    }

    @Override
    public int getPreviousSibling(int i) {
        int i2 = -1;
        if (i == -1) {
            return -1;
        }
        if (this.m_prevsib != null) {
            return makeNodeHandle(_prevsib(makeNodeIdentity(i)));
        }
        int iMakeNodeIdentity = makeNodeIdentity(i);
        int i_firstch = _firstch(_parent(iMakeNodeIdentity));
        while (true) {
            int i3 = i_firstch;
            int i4 = i2;
            i2 = i3;
            if (i2 != iMakeNodeIdentity) {
                i_firstch = _nextsib(i2);
            } else {
                return makeNodeHandle(i4);
            }
        }
    }

    @Override
    public int getNextAttribute(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (_type(iMakeNodeIdentity) == 2) {
            return makeNodeHandle(getNextAttributeIdentity(iMakeNodeIdentity));
        }
        return -1;
    }

    protected int getNextAttributeIdentity(int i) {
        short s_type;
        do {
            i = getNextNodeIdentity(i);
            if (-1 == i) {
                break;
            }
            s_type = _type(i);
            if (s_type == 2) {
                return i;
            }
        } while (s_type == 13);
        return -1;
    }

    protected void declareNamespaceInContext(int i, int i2) {
        SuballocatedIntVector suballocatedIntVector;
        if (this.m_namespaceDeclSets == null) {
            this.m_namespaceDeclSetElements = new SuballocatedIntVector(32);
            this.m_namespaceDeclSetElements.addElement(i);
            this.m_namespaceDeclSets = new Vector();
            suballocatedIntVector = new SuballocatedIntVector(32);
            this.m_namespaceDeclSets.addElement(suballocatedIntVector);
        } else {
            int size = this.m_namespaceDeclSetElements.size() - 1;
            if (size >= 0 && i == this.m_namespaceDeclSetElements.elementAt(size)) {
                suballocatedIntVector = (SuballocatedIntVector) this.m_namespaceDeclSets.elementAt(size);
            } else {
                suballocatedIntVector = null;
            }
        }
        if (suballocatedIntVector == null) {
            this.m_namespaceDeclSetElements.addElement(i);
            SuballocatedIntVector suballocatedIntVectorFindNamespaceContext = findNamespaceContext(_parent(i));
            if (suballocatedIntVectorFindNamespaceContext != null) {
                int size2 = suballocatedIntVectorFindNamespaceContext.size();
                SuballocatedIntVector suballocatedIntVector2 = new SuballocatedIntVector(Math.max(Math.min(size2 + 16, DTMFilter.SHOW_NOTATION), 32));
                for (int i3 = 0; i3 < size2; i3++) {
                    suballocatedIntVector2.addElement(suballocatedIntVectorFindNamespaceContext.elementAt(i3));
                }
                suballocatedIntVector = suballocatedIntVector2;
            } else {
                suballocatedIntVector = new SuballocatedIntVector(32);
            }
            this.m_namespaceDeclSets.addElement(suballocatedIntVector);
        }
        int i_exptype = _exptype(i2);
        for (int size3 = suballocatedIntVector.size() - 1; size3 >= 0; size3--) {
            if (i_exptype == getExpandedTypeID(suballocatedIntVector.elementAt(size3))) {
                suballocatedIntVector.setElementAt(makeNodeHandle(i2), size3);
                return;
            }
        }
        suballocatedIntVector.addElement(makeNodeHandle(i2));
    }

    protected SuballocatedIntVector findNamespaceContext(int i) {
        int i_firstch;
        if (this.m_namespaceDeclSetElements != null) {
            int iFindInSortedSuballocatedIntVector = findInSortedSuballocatedIntVector(this.m_namespaceDeclSetElements, i);
            if (iFindInSortedSuballocatedIntVector >= 0) {
                return (SuballocatedIntVector) this.m_namespaceDeclSets.elementAt(iFindInSortedSuballocatedIntVector);
            }
            if (iFindInSortedSuballocatedIntVector != -1) {
                int i2 = ((-1) - iFindInSortedSuballocatedIntVector) - 1;
                int iElementAt = this.m_namespaceDeclSetElements.elementAt(i2);
                int i_parent = _parent(i);
                if (i2 == 0 && iElementAt < i_parent) {
                    int documentRoot = getDocumentRoot(makeNodeHandle(i));
                    int iMakeNodeIdentity = makeNodeIdentity(documentRoot);
                    if (getNodeType(documentRoot) == 9 && (i_firstch = _firstch(iMakeNodeIdentity)) != -1) {
                        iMakeNodeIdentity = i_firstch;
                    }
                    if (iElementAt == iMakeNodeIdentity) {
                        return (SuballocatedIntVector) this.m_namespaceDeclSets.elementAt(i2);
                    }
                }
                while (i2 >= 0 && i_parent > 0) {
                    if (iElementAt == i_parent) {
                        return (SuballocatedIntVector) this.m_namespaceDeclSets.elementAt(i2);
                    }
                    if (iElementAt < i_parent) {
                        do {
                            i_parent = _parent(i_parent);
                        } while (iElementAt < i_parent);
                    } else {
                        if (i2 <= 0) {
                            break;
                        }
                        i2--;
                        iElementAt = this.m_namespaceDeclSetElements.elementAt(i2);
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

    protected int findInSortedSuballocatedIntVector(SuballocatedIntVector suballocatedIntVector, int i) {
        int i2 = 0;
        if (suballocatedIntVector != null) {
            int size = suballocatedIntVector.size() - 1;
            int i3 = 0;
            while (i2 <= size) {
                i3 = (i2 + size) / 2;
                int iElementAt = i - suballocatedIntVector.elementAt(i3);
                if (iElementAt == 0) {
                    return i3;
                }
                if (iElementAt < 0) {
                    size = i3 - 1;
                } else {
                    i2 = i3 + 1;
                }
            }
            if (i2 <= i3) {
                i2 = i3;
            }
        }
        return (-1) - i2;
    }

    @Override
    public int getFirstNamespaceNode(int i, boolean z) {
        short s_type;
        SuballocatedIntVector suballocatedIntVectorFindNamespaceContext;
        if (z) {
            int iMakeNodeIdentity = makeNodeIdentity(i);
            if (_type(iMakeNodeIdentity) != 1 || (suballocatedIntVectorFindNamespaceContext = findNamespaceContext(iMakeNodeIdentity)) == null || suballocatedIntVectorFindNamespaceContext.size() < 1) {
                return -1;
            }
            return suballocatedIntVectorFindNamespaceContext.elementAt(0);
        }
        int iMakeNodeIdentity2 = makeNodeIdentity(i);
        if (_type(iMakeNodeIdentity2) != 1) {
            return -1;
        }
        do {
            iMakeNodeIdentity2 = getNextNodeIdentity(iMakeNodeIdentity2);
            if (-1 == iMakeNodeIdentity2) {
                break;
            }
            s_type = _type(iMakeNodeIdentity2);
            if (s_type == 13) {
                return makeNodeHandle(iMakeNodeIdentity2);
            }
        } while (2 == s_type);
        return -1;
    }

    @Override
    public int getNextNamespaceNode(int i, int i2, boolean z) {
        short s_type;
        int iIndexOf;
        if (z) {
            SuballocatedIntVector suballocatedIntVectorFindNamespaceContext = findNamespaceContext(makeNodeIdentity(i));
            if (suballocatedIntVectorFindNamespaceContext == null || (iIndexOf = 1 + suballocatedIntVectorFindNamespaceContext.indexOf(i2)) <= 0 || iIndexOf == suballocatedIntVectorFindNamespaceContext.size()) {
                return -1;
            }
            return suballocatedIntVectorFindNamespaceContext.elementAt(iIndexOf);
        }
        int iMakeNodeIdentity = makeNodeIdentity(i2);
        do {
            iMakeNodeIdentity = getNextNodeIdentity(iMakeNodeIdentity);
            if (-1 == iMakeNodeIdentity) {
                break;
            }
            s_type = _type(iMakeNodeIdentity);
            if (s_type == 13) {
                return makeNodeHandle(iMakeNodeIdentity);
            }
        } while (s_type == 2);
        return -1;
    }

    @Override
    public int getParent(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity > 0) {
            return makeNodeHandle(_parent(iMakeNodeIdentity));
        }
        return -1;
    }

    @Override
    public int getDocument() {
        return this.m_dtmIdent.elementAt(0);
    }

    @Override
    public int getOwnerDocument(int i) {
        if (9 == getNodeType(i)) {
            return -1;
        }
        return getDocumentRoot(i);
    }

    @Override
    public int getDocumentRoot(int i) {
        return getManager().getDTM(i).getDocument();
    }

    @Override
    public int getStringValueChunkCount(int i) {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
        return 0;
    }

    @Override
    public char[] getStringValueChunk(int i, int i2, int[] iArr) {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
        return null;
    }

    @Override
    public int getExpandedTypeID(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return -1;
        }
        return _exptype(iMakeNodeIdentity);
    }

    @Override
    public int getExpandedTypeID(String str, String str2, int i) {
        return this.m_expandedNameTable.getExpandedTypeID(str, str2, i);
    }

    @Override
    public String getLocalNameFromExpandedNameID(int i) {
        return this.m_expandedNameTable.getLocalName(i);
    }

    @Override
    public String getNamespaceFromExpandedNameID(int i) {
        return this.m_expandedNameTable.getNamespace(i);
    }

    public int getNamespaceType(int i) {
        return this.m_expandedNameTable.getNamespaceID(_exptype(makeNodeIdentity(i)));
    }

    @Override
    public String getNodeNameX(int i) {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
        return null;
    }

    @Override
    public short getNodeType(int i) {
        if (i == -1) {
            return (short) -1;
        }
        return this.m_expandedNameTable.getType(_exptype(makeNodeIdentity(i)));
    }

    @Override
    public short getLevel(int i) {
        return (short) (_level(makeNodeIdentity(i)) + 1);
    }

    public int getNodeIdent(int i) {
        return makeNodeIdentity(i);
    }

    public int getNodeHandle(int i) {
        return makeNodeHandle(i);
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
        return this.m_documentBaseURI;
    }

    @Override
    public String getDocumentEncoding(int i) {
        return "UTF-8";
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
        return true;
    }

    @Override
    public boolean supportsPreStripping() {
        return true;
    }

    @Override
    public boolean isNodeAfter(int i, int i2) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        int iMakeNodeIdentity2 = makeNodeIdentity(i2);
        return (iMakeNodeIdentity == -1 || iMakeNodeIdentity2 == -1 || iMakeNodeIdentity > iMakeNodeIdentity2) ? false : true;
    }

    @Override
    public boolean isCharacterElementContentWhitespace(int i) {
        return false;
    }

    @Override
    public boolean isDocumentAllDeclarationsProcessed(int i) {
        return true;
    }

    @Override
    public Node getNode(int i) {
        return new DTMNodeProxy(this, i);
    }

    @Override
    public void appendChild(int i, boolean z, boolean z2) {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
    }

    @Override
    public void appendTextChild(String str) {
        error(XMLMessages.createXMLMessage(XMLErrorResources.ER_METHOD_NOT_SUPPORTED, null));
    }

    protected void error(String str) {
        throw new DTMException(str);
    }

    protected boolean getShouldStripWhitespace() {
        return this.m_shouldStripWS;
    }

    protected void pushShouldStripWhitespace(boolean z) {
        this.m_shouldStripWS = z;
        if (this.m_shouldStripWhitespaceStack != null) {
            this.m_shouldStripWhitespaceStack.push(z);
        }
    }

    protected void popShouldStripWhitespace() {
        if (this.m_shouldStripWhitespaceStack != null) {
            this.m_shouldStripWS = this.m_shouldStripWhitespaceStack.popAndTop();
        }
    }

    protected void setShouldStripWhitespace(boolean z) {
        this.m_shouldStripWS = z;
        if (this.m_shouldStripWhitespaceStack != null) {
            this.m_shouldStripWhitespaceStack.setTop(z);
        }
    }

    @Override
    public void documentRegistration() {
    }

    @Override
    public void documentRelease() {
    }

    @Override
    public void migrateTo(DTMManager dTMManager) {
        this.m_mgr = dTMManager;
        if (dTMManager instanceof DTMManagerDefault) {
            this.m_mgrDefault = (DTMManagerDefault) dTMManager;
        }
    }

    public DTMManager getManager() {
        return this.m_mgr;
    }

    public SuballocatedIntVector getDTMIDs() {
        if (this.m_mgr == null) {
            return null;
        }
        return this.m_dtmIdent;
    }
}
