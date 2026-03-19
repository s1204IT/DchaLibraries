package mf.org.apache.xerces.dom;

import java.util.ArrayList;
import java.util.Hashtable;
import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Node;

public class DeferredDocumentImpl extends DocumentImpl implements DeferredNode {
    protected static final int CHUNK_MASK = 2047;
    protected static final int CHUNK_SHIFT = 11;
    protected static final int CHUNK_SIZE = 2048;
    private static final boolean DEBUG_IDS = false;
    private static final boolean DEBUG_PRINT_REF_COUNTS = false;
    private static final boolean DEBUG_PRINT_TABLES = false;
    protected static final int INITIAL_CHUNK_COUNT = 32;
    private static final int[] INIT_ARRAY = new int[2049];
    static final long serialVersionUID = 5186323580749626857L;
    private final transient StringBuffer fBufferStr;
    protected transient int fIdCount;
    protected transient int[] fIdElement;
    protected transient String[] fIdName;
    protected boolean fNamespacesEnabled;
    protected transient int fNodeCount;
    protected transient int[][] fNodeExtra;
    protected transient int[][] fNodeLastChild;
    protected transient Object[][] fNodeName;
    protected transient int[][] fNodeParent;
    protected transient int[][] fNodePrevSib;
    protected transient int[][] fNodeType;
    protected transient Object[][] fNodeURI;
    protected transient Object[][] fNodeValue;
    private final transient ArrayList fStrChunks;

    public DeferredDocumentImpl() {
        this(false);
    }

    public DeferredDocumentImpl(boolean namespacesEnabled) {
        this(namespacesEnabled, false);
    }

    public DeferredDocumentImpl(boolean namespaces, boolean grammarAccess) {
        super(grammarAccess);
        this.fNodeCount = 0;
        this.fNamespacesEnabled = false;
        this.fBufferStr = new StringBuffer();
        this.fStrChunks = new ArrayList();
        needsSyncData(true);
        needsSyncChildren(true);
        this.fNamespacesEnabled = namespaces;
    }

    @Override
    public DOMImplementation getImplementation() {
        return DeferredDOMImplementationImpl.getDOMImplementation();
    }

    boolean getNamespacesEnabled() {
        return this.fNamespacesEnabled;
    }

    void setNamespacesEnabled(boolean enable) {
        this.fNamespacesEnabled = enable;
    }

    public int createDeferredDocument() {
        int nodeIndex = createNode((short) 9);
        return nodeIndex;
    }

    public int createDeferredDocumentType(String rootElementName, String publicId, String systemId) {
        int nodeIndex = createNode((short) 10);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, rootElementName, chunk, index);
        setChunkValue(this.fNodeValue, publicId, chunk, index);
        setChunkValue(this.fNodeURI, systemId, chunk, index);
        return nodeIndex;
    }

    public void setInternalSubset(int doctypeIndex, String subset) {
        int chunk = doctypeIndex >> 11;
        int index = doctypeIndex & CHUNK_MASK;
        int extraDataIndex = createNode((short) 10);
        int echunk = extraDataIndex >> 11;
        int eindex = extraDataIndex & CHUNK_MASK;
        setChunkIndex(this.fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(this.fNodeValue, subset, echunk, eindex);
    }

    public int createDeferredNotation(String notationName, String publicId, String systemId, String baseURI) {
        int nodeIndex = createNode((short) 12);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        int extraDataIndex = createNode((short) 12);
        int echunk = extraDataIndex >> 11;
        int eindex = extraDataIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, notationName, chunk, index);
        setChunkValue(this.fNodeValue, publicId, chunk, index);
        setChunkValue(this.fNodeURI, systemId, chunk, index);
        setChunkIndex(this.fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(this.fNodeName, baseURI, echunk, eindex);
        return nodeIndex;
    }

    public int createDeferredEntity(String entityName, String publicId, String systemId, String notationName, String baseURI) {
        int nodeIndex = createNode((short) 6);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        int extraDataIndex = createNode((short) 6);
        int echunk = extraDataIndex >> 11;
        int eindex = extraDataIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, entityName, chunk, index);
        setChunkValue(this.fNodeValue, publicId, chunk, index);
        setChunkValue(this.fNodeURI, systemId, chunk, index);
        setChunkIndex(this.fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(this.fNodeName, notationName, echunk, eindex);
        setChunkValue(this.fNodeValue, null, echunk, eindex);
        setChunkValue(this.fNodeURI, null, echunk, eindex);
        int extraDataIndex2 = createNode((short) 6);
        int echunk2 = extraDataIndex2 >> 11;
        int eindex2 = extraDataIndex2 & CHUNK_MASK;
        setChunkIndex(this.fNodeExtra, extraDataIndex2, echunk, eindex);
        setChunkValue(this.fNodeName, baseURI, echunk2, eindex2);
        return nodeIndex;
    }

    public String getDeferredEntityBaseURI(int entityIndex) {
        if (entityIndex != -1) {
            int extraDataIndex = getNodeExtra(entityIndex, false);
            return getNodeName(getNodeExtra(extraDataIndex, false), false);
        }
        return null;
    }

    public void setEntityInfo(int currentEntityDecl, String version, String encoding) {
        int eNodeIndex = getNodeExtra(currentEntityDecl, false);
        if (eNodeIndex != -1) {
            int echunk = eNodeIndex >> 11;
            int eindex = eNodeIndex & CHUNK_MASK;
            setChunkValue(this.fNodeValue, version, echunk, eindex);
            setChunkValue(this.fNodeURI, encoding, echunk, eindex);
        }
    }

    public void setTypeInfo(int elementNodeIndex, Object type) {
        int elementChunk = elementNodeIndex >> 11;
        int elementIndex = elementNodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeValue, type, elementChunk, elementIndex);
    }

    public void setInputEncoding(int currentEntityDecl, String value) {
        int nodeIndex = getNodeExtra(currentEntityDecl, false);
        int extraDataIndex = getNodeExtra(nodeIndex, false);
        int echunk = extraDataIndex >> 11;
        int eindex = extraDataIndex & CHUNK_MASK;
        setChunkValue(this.fNodeValue, value, echunk, eindex);
    }

    public int createDeferredEntityReference(String name, String baseURI) {
        int nodeIndex = createNode((short) 5);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, name, chunk, index);
        setChunkValue(this.fNodeValue, baseURI, chunk, index);
        return nodeIndex;
    }

    public int createDeferredElement(String elementURI, String elementName, Object type) {
        int elementNodeIndex = createNode((short) 1);
        int elementChunk = elementNodeIndex >> 11;
        int elementIndex = elementNodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, elementName, elementChunk, elementIndex);
        setChunkValue(this.fNodeURI, elementURI, elementChunk, elementIndex);
        setChunkValue(this.fNodeValue, type, elementChunk, elementIndex);
        return elementNodeIndex;
    }

    public int createDeferredElement(String elementName) {
        return createDeferredElement(null, elementName);
    }

    public int createDeferredElement(String elementURI, String elementName) {
        int elementNodeIndex = createNode((short) 1);
        int elementChunk = elementNodeIndex >> 11;
        int elementIndex = elementNodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, elementName, elementChunk, elementIndex);
        setChunkValue(this.fNodeURI, elementURI, elementChunk, elementIndex);
        return elementNodeIndex;
    }

    public int setDeferredAttribute(int elementNodeIndex, String attrName, String attrURI, String attrValue, boolean specified, boolean id, Object type) {
        int attrNodeIndex = createDeferredAttribute(attrName, attrURI, attrValue, specified);
        int attrChunk = attrNodeIndex >> 11;
        int attrIndex = attrNodeIndex & CHUNK_MASK;
        setChunkIndex(this.fNodeParent, elementNodeIndex, attrChunk, attrIndex);
        int elementChunk = elementNodeIndex >> 11;
        int elementIndex = elementNodeIndex & CHUNK_MASK;
        int lastAttrNodeIndex = getChunkIndex(this.fNodeExtra, elementChunk, elementIndex);
        if (lastAttrNodeIndex != 0) {
            setChunkIndex(this.fNodePrevSib, lastAttrNodeIndex, attrChunk, attrIndex);
        }
        setChunkIndex(this.fNodeExtra, attrNodeIndex, elementChunk, elementIndex);
        int extra = getChunkIndex(this.fNodeExtra, attrChunk, attrIndex);
        if (id) {
            setChunkIndex(this.fNodeExtra, extra | 512, attrChunk, attrIndex);
            String value = getChunkValue(this.fNodeValue, attrChunk, attrIndex);
            putIdentifier(value, elementNodeIndex);
        }
        if (type != null) {
            int extraDataIndex = createNode((short) 20);
            int echunk = extraDataIndex >> 11;
            int eindex = extraDataIndex & CHUNK_MASK;
            setChunkIndex(this.fNodeLastChild, extraDataIndex, attrChunk, attrIndex);
            setChunkValue(this.fNodeValue, type, echunk, eindex);
        }
        return attrNodeIndex;
    }

    public int setDeferredAttribute(int elementNodeIndex, String attrName, String attrURI, String attrValue, boolean specified) {
        int attrNodeIndex = createDeferredAttribute(attrName, attrURI, attrValue, specified);
        int attrChunk = attrNodeIndex >> 11;
        int attrIndex = attrNodeIndex & CHUNK_MASK;
        setChunkIndex(this.fNodeParent, elementNodeIndex, attrChunk, attrIndex);
        int elementChunk = elementNodeIndex >> 11;
        int elementIndex = elementNodeIndex & CHUNK_MASK;
        int lastAttrNodeIndex = getChunkIndex(this.fNodeExtra, elementChunk, elementIndex);
        if (lastAttrNodeIndex != 0) {
            setChunkIndex(this.fNodePrevSib, lastAttrNodeIndex, attrChunk, attrIndex);
        }
        setChunkIndex(this.fNodeExtra, attrNodeIndex, elementChunk, elementIndex);
        return attrNodeIndex;
    }

    public int createDeferredAttribute(String attrName, String attrValue, boolean specified) {
        return createDeferredAttribute(attrName, null, attrValue, specified);
    }

    public int createDeferredAttribute(String attrName, String attrURI, String attrValue, boolean specified) {
        int nodeIndex = createNode((short) 2);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, attrName, chunk, index);
        setChunkValue(this.fNodeURI, attrURI, chunk, index);
        setChunkValue(this.fNodeValue, attrValue, chunk, index);
        int extra = specified ? 32 : 0;
        setChunkIndex(this.fNodeExtra, extra, chunk, index);
        return nodeIndex;
    }

    public int createDeferredElementDefinition(String elementName) {
        int nodeIndex = createNode((short) 21);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, elementName, chunk, index);
        return nodeIndex;
    }

    public int createDeferredTextNode(String str, boolean z) {
        int iCreateNode = createNode((short) 3);
        int i = iCreateNode >> 11;
        int i2 = iCreateNode & CHUNK_MASK;
        setChunkValue(this.fNodeValue, str, i, i2);
        setChunkIndex(this.fNodeExtra, z ? 1 : 0, i, i2);
        return iCreateNode;
    }

    public int createDeferredCDATASection(String data) {
        int nodeIndex = createNode((short) 4);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeValue, data, chunk, index);
        return nodeIndex;
    }

    public int createDeferredProcessingInstruction(String target, String data) {
        int nodeIndex = createNode((short) 7);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, target, chunk, index);
        setChunkValue(this.fNodeValue, data, chunk, index);
        return nodeIndex;
    }

    public int createDeferredComment(String data) {
        int nodeIndex = createNode((short) 8);
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(this.fNodeValue, data, chunk, index);
        return nodeIndex;
    }

    public int cloneNode(int nodeIndex, boolean deep) {
        int nchunk = nodeIndex >> 11;
        int nindex = nodeIndex & CHUNK_MASK;
        int nodeType = this.fNodeType[nchunk][nindex];
        int cloneIndex = createNode((short) nodeType);
        int cchunk = cloneIndex >> 11;
        int cindex = cloneIndex & CHUNK_MASK;
        setChunkValue(this.fNodeName, this.fNodeName[nchunk][nindex], cchunk, cindex);
        setChunkValue(this.fNodeValue, this.fNodeValue[nchunk][nindex], cchunk, cindex);
        setChunkValue(this.fNodeURI, this.fNodeURI[nchunk][nindex], cchunk, cindex);
        int extraIndex = this.fNodeExtra[nchunk][nindex];
        if (extraIndex != -1) {
            if (nodeType != 2 && nodeType != 3) {
                extraIndex = cloneNode(extraIndex, false);
            }
            setChunkIndex(this.fNodeExtra, extraIndex, cchunk, cindex);
        }
        if (deep) {
            int prevIndex = -1;
            int childIndex = getLastChild(nodeIndex, false);
            while (childIndex != -1) {
                int clonedChildIndex = cloneNode(childIndex, deep);
                insertBefore(cloneIndex, clonedChildIndex, prevIndex);
                prevIndex = clonedChildIndex;
                childIndex = getRealPrevSibling(childIndex, false);
            }
        }
        return cloneIndex;
    }

    public void appendChild(int parentIndex, int childIndex) {
        int pchunk = parentIndex >> 11;
        int pindex = parentIndex & CHUNK_MASK;
        int cchunk = childIndex >> 11;
        int cindex = childIndex & CHUNK_MASK;
        setChunkIndex(this.fNodeParent, parentIndex, cchunk, cindex);
        int olast = getChunkIndex(this.fNodeLastChild, pchunk, pindex);
        setChunkIndex(this.fNodePrevSib, olast, cchunk, cindex);
        setChunkIndex(this.fNodeLastChild, childIndex, pchunk, pindex);
    }

    public int setAttributeNode(int elemIndex, int attrIndex) {
        int echunk = elemIndex >> 11;
        int eindex = elemIndex & CHUNK_MASK;
        int achunk = attrIndex >> 11;
        int aindex = attrIndex & CHUNK_MASK;
        String attrName = getChunkValue(this.fNodeName, achunk, aindex);
        int oldAttrIndex = getChunkIndex(this.fNodeExtra, echunk, eindex);
        int nextIndex = -1;
        int oachunk = -1;
        int oaindex = -1;
        while (oldAttrIndex != -1) {
            oachunk = oldAttrIndex >> 11;
            oaindex = oldAttrIndex & CHUNK_MASK;
            String oldAttrName = getChunkValue(this.fNodeName, oachunk, oaindex);
            if (oldAttrName.equals(attrName)) {
                break;
            }
            nextIndex = oldAttrIndex;
            oldAttrIndex = getChunkIndex(this.fNodePrevSib, oachunk, oaindex);
        }
        if (oldAttrIndex != -1) {
            int prevIndex = getChunkIndex(this.fNodePrevSib, oachunk, oaindex);
            if (nextIndex == -1) {
                setChunkIndex(this.fNodeExtra, prevIndex, echunk, eindex);
            } else {
                int pchunk = nextIndex >> 11;
                int pindex = nextIndex & CHUNK_MASK;
                setChunkIndex(this.fNodePrevSib, prevIndex, pchunk, pindex);
            }
            clearChunkIndex(this.fNodeType, oachunk, oaindex);
            clearChunkValue(this.fNodeName, oachunk, oaindex);
            clearChunkValue(this.fNodeValue, oachunk, oaindex);
            clearChunkIndex(this.fNodeParent, oachunk, oaindex);
            clearChunkIndex(this.fNodePrevSib, oachunk, oaindex);
            int attrTextIndex = clearChunkIndex(this.fNodeLastChild, oachunk, oaindex);
            int atchunk = attrTextIndex >> 11;
            int atindex = attrTextIndex & CHUNK_MASK;
            clearChunkIndex(this.fNodeType, atchunk, atindex);
            clearChunkValue(this.fNodeValue, atchunk, atindex);
            clearChunkIndex(this.fNodeParent, atchunk, atindex);
            clearChunkIndex(this.fNodeLastChild, atchunk, atindex);
        }
        int prevIndex2 = getChunkIndex(this.fNodeExtra, echunk, eindex);
        setChunkIndex(this.fNodeExtra, attrIndex, echunk, eindex);
        setChunkIndex(this.fNodePrevSib, prevIndex2, achunk, aindex);
        return oldAttrIndex;
    }

    public void setIdAttributeNode(int elemIndex, int attrIndex) {
        int chunk = attrIndex >> 11;
        int index = attrIndex & CHUNK_MASK;
        int extra = getChunkIndex(this.fNodeExtra, chunk, index);
        setChunkIndex(this.fNodeExtra, extra | 512, chunk, index);
        String value = getChunkValue(this.fNodeValue, chunk, index);
        putIdentifier(value, elemIndex);
    }

    public void setIdAttribute(int attrIndex) {
        int chunk = attrIndex >> 11;
        int index = attrIndex & CHUNK_MASK;
        int extra = getChunkIndex(this.fNodeExtra, chunk, index);
        setChunkIndex(this.fNodeExtra, extra | 512, chunk, index);
    }

    public int insertBefore(int parentIndex, int newChildIndex, int refChildIndex) {
        if (refChildIndex == -1) {
            appendChild(parentIndex, newChildIndex);
            return newChildIndex;
        }
        int nchunk = newChildIndex >> 11;
        int nindex = newChildIndex & CHUNK_MASK;
        int rchunk = refChildIndex >> 11;
        int rindex = refChildIndex & CHUNK_MASK;
        int previousIndex = getChunkIndex(this.fNodePrevSib, rchunk, rindex);
        setChunkIndex(this.fNodePrevSib, newChildIndex, rchunk, rindex);
        setChunkIndex(this.fNodePrevSib, previousIndex, nchunk, nindex);
        return newChildIndex;
    }

    public void setAsLastChild(int parentIndex, int childIndex) {
        int pchunk = parentIndex >> 11;
        int pindex = parentIndex & CHUNK_MASK;
        setChunkIndex(this.fNodeLastChild, childIndex, pchunk, pindex);
    }

    public int getParentNode(int nodeIndex) {
        return getParentNode(nodeIndex, false);
    }

    public int getParentNode(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return -1;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(this.fNodeParent, chunk, index) : getChunkIndex(this.fNodeParent, chunk, index);
    }

    public int getLastChild(int nodeIndex) {
        return getLastChild(nodeIndex, true);
    }

    public int getLastChild(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return -1;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(this.fNodeLastChild, chunk, index) : getChunkIndex(this.fNodeLastChild, chunk, index);
    }

    public int getPrevSibling(int nodeIndex) {
        return getPrevSibling(nodeIndex, true);
    }

    public int getPrevSibling(int nodeIndex, boolean free) {
        int nodeIndex2;
        int type;
        if (nodeIndex == -1) {
            return -1;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        int type2 = getChunkIndex(this.fNodeType, chunk, index);
        if (type2 == 3) {
            do {
                nodeIndex2 = getChunkIndex(this.fNodePrevSib, chunk, index);
                if (nodeIndex2 != -1) {
                    chunk = nodeIndex2 >> 11;
                    index = nodeIndex2 & CHUNK_MASK;
                    type = getChunkIndex(this.fNodeType, chunk, index);
                } else {
                    return nodeIndex2;
                }
            } while (type == 3);
            return nodeIndex2;
        }
        return getChunkIndex(this.fNodePrevSib, chunk, index);
    }

    public int getRealPrevSibling(int nodeIndex) {
        return getRealPrevSibling(nodeIndex, true);
    }

    public int getRealPrevSibling(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return -1;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(this.fNodePrevSib, chunk, index) : getChunkIndex(this.fNodePrevSib, chunk, index);
    }

    public int lookupElementDefinition(String elementName) {
        if (this.fNodeCount > 1) {
            int docTypeIndex = -1;
            int index = getChunkIndex(this.fNodeLastChild, 0, 0);
            while (true) {
                if (index == -1) {
                    break;
                }
                int nchunk = index >> 11;
                int nindex = index & CHUNK_MASK;
                if (getChunkIndex(this.fNodeType, nchunk, nindex) != 10) {
                    index = getChunkIndex(this.fNodePrevSib, nchunk, nindex);
                } else {
                    docTypeIndex = index;
                    break;
                }
            }
            int docTypeIndex2 = docTypeIndex;
            if (docTypeIndex2 == -1) {
                return -1;
            }
            int nchunk2 = docTypeIndex2 >> 11;
            int nchunk3 = docTypeIndex2 & CHUNK_MASK;
            int index2 = getChunkIndex(this.fNodeLastChild, nchunk2, nchunk3);
            while (index2 != -1) {
                int nchunk4 = index2 >> 11;
                int nindex2 = index2 & CHUNK_MASK;
                if (getChunkIndex(this.fNodeType, nchunk4, nindex2) != 21 || getChunkValue(this.fNodeName, nchunk4, nindex2) != elementName) {
                    index2 = getChunkIndex(this.fNodePrevSib, nchunk4, nindex2);
                } else {
                    return index2;
                }
            }
        }
        return -1;
    }

    public DeferredNode getNodeObject(int i) {
        ?? deferredNotationImpl;
        Element deferredElementImpl;
        if (i == -1) {
            return null;
        }
        int i2 = i >> 11;
        int i3 = i & CHUNK_MASK;
        int chunkIndex = getChunkIndex(this.fNodeType, i2, i3);
        if (chunkIndex != 3 && chunkIndex != 4) {
            clearChunkIndex(this.fNodeType, i2, i3);
        }
        if (chunkIndex == 12) {
            deferredNotationImpl = new DeferredNotationImpl(this, i);
        } else if (chunkIndex != 21) {
            switch (chunkIndex) {
                case 1:
                    if (this.fNamespacesEnabled) {
                        deferredElementImpl = new DeferredElementNSImpl(this, i);
                    } else {
                        deferredElementImpl = new DeferredElementImpl(this, i);
                    }
                    if (this.fIdElement != null) {
                        int iBinarySearch = binarySearch(this.fIdElement, 0, this.fIdCount - 1, i);
                        while (iBinarySearch != -1) {
                            String str = this.fIdName[iBinarySearch];
                            if (str != null) {
                                putIdentifier0(str, deferredElementImpl);
                                this.fIdName[iBinarySearch] = null;
                            }
                            if (iBinarySearch + 1 < this.fIdCount && this.fIdElement[iBinarySearch + 1] == i) {
                                iBinarySearch++;
                            } else {
                                iBinarySearch = -1;
                            }
                        }
                    }
                    deferredNotationImpl = deferredElementImpl;
                    break;
                case 2:
                    if (this.fNamespacesEnabled) {
                        deferredNotationImpl = new DeferredAttrNSImpl(this, i);
                    } else {
                        deferredNotationImpl = new DeferredAttrImpl(this, i);
                    }
                    break;
                case 3:
                    deferredNotationImpl = new DeferredTextImpl(this, i);
                    break;
                case 4:
                    deferredNotationImpl = new DeferredCDATASectionImpl(this, i);
                    break;
                case 5:
                    deferredNotationImpl = new DeferredEntityReferenceImpl(this, i);
                    break;
                case 6:
                    deferredNotationImpl = new DeferredEntityImpl(this, i);
                    break;
                case 7:
                    deferredNotationImpl = new DeferredProcessingInstructionImpl(this, i);
                    break;
                case 8:
                    deferredNotationImpl = new DeferredCommentImpl(this, i);
                    break;
                case 9:
                    deferredNotationImpl = this;
                    break;
                case 10:
                    DeferredDocumentTypeImpl deferredDocumentTypeImpl = new DeferredDocumentTypeImpl(this, i);
                    this.docType = deferredDocumentTypeImpl;
                    deferredNotationImpl = deferredDocumentTypeImpl;
                    break;
                default:
                    throw new IllegalArgumentException("type: " + chunkIndex);
            }
        } else {
            deferredNotationImpl = new DeferredElementDefinitionImpl(this, i);
        }
        if (deferredNotationImpl != 0) {
            return deferredNotationImpl;
        }
        throw new IllegalArgumentException();
    }

    public String getNodeName(int nodeIndex) {
        return getNodeName(nodeIndex, true);
    }

    public String getNodeName(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return null;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(this.fNodeName, chunk, index) : getChunkValue(this.fNodeName, chunk, index);
    }

    public String getNodeValueString(int nodeIndex) {
        return getNodeValueString(nodeIndex, true);
    }

    public String getNodeValueString(int nodeIndex, boolean free) {
        int child;
        if (nodeIndex == -1) {
            return null;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        String value = free ? clearChunkValue(this.fNodeValue, chunk, index) : getChunkValue(this.fNodeValue, chunk, index);
        if (value == null) {
            return null;
        }
        int type = getChunkIndex(this.fNodeType, chunk, index);
        if (type == 3) {
            int prevSib = getRealPrevSibling(nodeIndex);
            if (prevSib != -1 && getNodeType(prevSib, false) == 3) {
                this.fStrChunks.add(value);
                do {
                    int chunk2 = prevSib >> 11;
                    int index2 = prevSib & CHUNK_MASK;
                    this.fStrChunks.add(getChunkValue(this.fNodeValue, chunk2, index2));
                    prevSib = getChunkIndex(this.fNodePrevSib, chunk2, index2);
                    if (prevSib == -1) {
                        break;
                    }
                } while (getNodeType(prevSib, false) == 3);
                int chunkCount = this.fStrChunks.size();
                for (int i = chunkCount - 1; i >= 0; i--) {
                    this.fBufferStr.append((String) this.fStrChunks.get(i));
                }
                String value2 = this.fBufferStr.toString();
                this.fStrChunks.clear();
                this.fBufferStr.setLength(0);
                return value2;
            }
        } else if (type == 4 && (child = getLastChild(nodeIndex, false)) != -1) {
            this.fBufferStr.append(value);
            while (child != -1) {
                int chunk3 = child >> 11;
                int index3 = child & CHUNK_MASK;
                this.fStrChunks.add(getChunkValue(this.fNodeValue, chunk3, index3));
                child = getChunkIndex(this.fNodePrevSib, chunk3, index3);
            }
            for (int i2 = this.fStrChunks.size() - 1; i2 >= 0; i2--) {
                this.fBufferStr.append((String) this.fStrChunks.get(i2));
            }
            String value3 = this.fBufferStr.toString();
            this.fStrChunks.clear();
            this.fBufferStr.setLength(0);
            return value3;
        }
        return value;
    }

    public String getNodeValue(int nodeIndex) {
        return getNodeValue(nodeIndex, true);
    }

    public Object getTypeInfo(int nodeIndex) {
        if (nodeIndex == -1) {
            return null;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        Object value = this.fNodeValue[chunk] != null ? this.fNodeValue[chunk][index] : null;
        if (value != null) {
            this.fNodeValue[chunk][index] = null;
            RefCount c = (RefCount) this.fNodeValue[chunk][2048];
            c.fCount--;
            if (c.fCount == 0) {
                this.fNodeValue[chunk] = null;
            }
        }
        return value;
    }

    public String getNodeValue(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return null;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(this.fNodeValue, chunk, index) : getChunkValue(this.fNodeValue, chunk, index);
    }

    public int getNodeExtra(int nodeIndex) {
        return getNodeExtra(nodeIndex, true);
    }

    public int getNodeExtra(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return -1;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(this.fNodeExtra, chunk, index) : getChunkIndex(this.fNodeExtra, chunk, index);
    }

    public short getNodeType(int nodeIndex) {
        return getNodeType(nodeIndex, true);
    }

    public short getNodeType(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return (short) -1;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? (short) clearChunkIndex(this.fNodeType, chunk, index) : (short) getChunkIndex(this.fNodeType, chunk, index);
    }

    public String getAttribute(int elemIndex, String name) {
        if (elemIndex == -1 || name == null) {
            return null;
        }
        int echunk = elemIndex >> 11;
        int eindex = elemIndex & CHUNK_MASK;
        int attrIndex = getChunkIndex(this.fNodeExtra, echunk, eindex);
        while (attrIndex != -1) {
            int achunk = attrIndex >> 11;
            int aindex = attrIndex & CHUNK_MASK;
            if (getChunkValue(this.fNodeName, achunk, aindex) == name) {
                return getChunkValue(this.fNodeValue, achunk, aindex);
            }
            attrIndex = getChunkIndex(this.fNodePrevSib, achunk, aindex);
        }
        return null;
    }

    public String getNodeURI(int nodeIndex) {
        return getNodeURI(nodeIndex, true);
    }

    public String getNodeURI(int nodeIndex, boolean free) {
        if (nodeIndex == -1) {
            return null;
        }
        int chunk = nodeIndex >> 11;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(this.fNodeURI, chunk, index) : getChunkValue(this.fNodeURI, chunk, index);
    }

    public void putIdentifier(String name, int elementNodeIndex) {
        if (this.fIdName == null) {
            this.fIdName = new String[64];
            this.fIdElement = new int[64];
        }
        if (this.fIdCount == this.fIdName.length) {
            String[] idName = new String[this.fIdCount * 2];
            System.arraycopy(this.fIdName, 0, idName, 0, this.fIdCount);
            this.fIdName = idName;
            int[] idElement = new int[idName.length];
            System.arraycopy(this.fIdElement, 0, idElement, 0, this.fIdCount);
            this.fIdElement = idElement;
        }
        this.fIdName[this.fIdCount] = name;
        this.fIdElement[this.fIdCount] = elementNodeIndex;
        this.fIdCount++;
    }

    public void print() {
    }

    @Override
    public int getNodeIndex() {
        return 0;
    }

    @Override
    protected void synchronizeData() {
        needsSyncData(false);
        if (this.fIdElement != null) {
            IntVector path = new IntVector();
            int i = 0;
            while (i < this.fIdCount) {
                int elementNodeIndex = this.fIdElement[i];
                String idName = this.fIdName[i];
                if (idName != null) {
                    path.removeAllElements();
                    int index = elementNodeIndex;
                    do {
                        path.addElement(index);
                        int pchunk = index >> 11;
                        int pindex = index & CHUNK_MASK;
                        index = getChunkIndex(this.fNodeParent, pchunk, pindex);
                    } while (index != -1);
                    Node place = this;
                    for (int j = path.size() - 2; j >= 0; j--) {
                        int index2 = path.elementAt(j);
                        Node child = place.getLastChild();
                        while (true) {
                            if (child != null) {
                                if (child instanceof DeferredNode) {
                                    int nodeIndex = ((DeferredNode) child).getNodeIndex();
                                    if (nodeIndex == index2) {
                                        place = child;
                                        break;
                                    }
                                }
                                child = child.getPreviousSibling();
                            }
                        }
                    }
                    Element element = (Element) place;
                    putIdentifier0(idName, element);
                    this.fIdName[i] = null;
                    while (i + 1 < this.fIdCount && this.fIdElement[i + 1] == elementNodeIndex) {
                        i++;
                        String idName2 = this.fIdName[i];
                        if (idName2 != null) {
                            putIdentifier0(idName2, element);
                        }
                    }
                }
                i++;
            }
        }
    }

    @Override
    protected void synchronizeChildren() {
        if (needsSyncData()) {
            synchronizeData();
            if (!needsSyncChildren()) {
                return;
            }
        }
        boolean orig = this.mutationEvents;
        this.mutationEvents = false;
        needsSyncChildren(false);
        getNodeType(0);
        ChildNode first = null;
        ChildNode last = null;
        int index = getLastChild(0);
        while (index != -1) {
            ChildNode node = (ChildNode) getNodeObject(index);
            if (last == null) {
                last = node;
            } else {
                first.previousSibling = node;
            }
            node.ownerNode = this;
            node.isOwned(true);
            node.nextSibling = first;
            first = node;
            int type = node.getNodeType();
            if (type == 1) {
                this.docElement = (ElementImpl) node;
            } else if (type == 10) {
                this.docType = (DocumentTypeImpl) node;
            }
            index = getPrevSibling(index);
        }
        if (first != null) {
            this.firstChild = first;
            first.isFirstChild(true);
            lastChild(last);
        }
        this.mutationEvents = orig;
    }

    protected final void synchronizeChildren(AttrImpl a, int nodeIndex) {
        boolean orig = getMutationEvents();
        setMutationEvents(false);
        a.needsSyncChildren(false);
        int last = getLastChild(nodeIndex);
        int prev = getPrevSibling(last);
        if (prev == -1) {
            a.value = getNodeValueString(nodeIndex);
            a.hasStringValue(true);
        } else {
            ChildNode firstNode = null;
            ChildNode lastNode = null;
            int index = last;
            while (index != -1) {
                ChildNode node = (ChildNode) getNodeObject(index);
                if (lastNode == null) {
                    lastNode = node;
                } else {
                    firstNode.previousSibling = node;
                }
                node.ownerNode = a;
                node.isOwned(true);
                node.nextSibling = firstNode;
                firstNode = node;
                index = getPrevSibling(index);
            }
            if (lastNode != null) {
                a.value = firstNode;
                firstNode.isFirstChild(true);
                a.lastChild(lastNode);
            }
            a.hasStringValue(false);
        }
        setMutationEvents(orig);
    }

    protected final void synchronizeChildren(ParentNode p, int nodeIndex) {
        boolean orig = getMutationEvents();
        setMutationEvents(false);
        p.needsSyncChildren(false);
        ChildNode firstNode = null;
        ChildNode lastNode = null;
        int index = getLastChild(nodeIndex);
        while (index != -1) {
            ChildNode node = (ChildNode) getNodeObject(index);
            if (lastNode == null) {
                lastNode = node;
            } else {
                firstNode.previousSibling = node;
            }
            node.ownerNode = p;
            node.isOwned(true);
            node.nextSibling = firstNode;
            firstNode = node;
            index = getPrevSibling(index);
        }
        if (lastNode != null) {
            p.firstChild = firstNode;
            firstNode.isFirstChild(true);
            p.lastChild(lastNode);
        }
        setMutationEvents(orig);
    }

    protected void ensureCapacity(int chunk) {
        if (this.fNodeType == null) {
            this.fNodeType = new int[32][];
            this.fNodeName = new Object[32][];
            this.fNodeValue = new Object[32][];
            this.fNodeParent = new int[32][];
            this.fNodeLastChild = new int[32][];
            this.fNodePrevSib = new int[32][];
            this.fNodeURI = new Object[32][];
            this.fNodeExtra = new int[32][];
        } else if (this.fNodeType.length <= chunk) {
            int newsize = chunk * 2;
            int[][] newArray = new int[newsize][];
            System.arraycopy(this.fNodeType, 0, newArray, 0, chunk);
            this.fNodeType = newArray;
            Object[][] newStrArray = new Object[newsize][];
            System.arraycopy(this.fNodeName, 0, newStrArray, 0, chunk);
            this.fNodeName = newStrArray;
            Object[][] newStrArray2 = new Object[newsize][];
            System.arraycopy(this.fNodeValue, 0, newStrArray2, 0, chunk);
            this.fNodeValue = newStrArray2;
            int[][] newArray2 = new int[newsize][];
            System.arraycopy(this.fNodeParent, 0, newArray2, 0, chunk);
            this.fNodeParent = newArray2;
            int[][] newArray3 = new int[newsize][];
            System.arraycopy(this.fNodeLastChild, 0, newArray3, 0, chunk);
            this.fNodeLastChild = newArray3;
            int[][] newArray4 = new int[newsize][];
            System.arraycopy(this.fNodePrevSib, 0, newArray4, 0, chunk);
            this.fNodePrevSib = newArray4;
            Object[][] newStrArray3 = new Object[newsize][];
            System.arraycopy(this.fNodeURI, 0, newStrArray3, 0, chunk);
            this.fNodeURI = newStrArray3;
            int[][] newArray5 = new int[newsize][];
            System.arraycopy(this.fNodeExtra, 0, newArray5, 0, chunk);
            this.fNodeExtra = newArray5;
        } else if (this.fNodeType[chunk] != null) {
            return;
        }
        createChunk(this.fNodeType, chunk);
        createChunk(this.fNodeName, chunk);
        createChunk(this.fNodeValue, chunk);
        createChunk(this.fNodeParent, chunk);
        createChunk(this.fNodeLastChild, chunk);
        createChunk(this.fNodePrevSib, chunk);
        createChunk(this.fNodeURI, chunk);
        createChunk(this.fNodeExtra, chunk);
    }

    protected int createNode(short nodeType) {
        int chunk = this.fNodeCount >> 11;
        int index = this.fNodeCount & CHUNK_MASK;
        ensureCapacity(chunk);
        setChunkIndex(this.fNodeType, nodeType, chunk, index);
        int i = this.fNodeCount;
        this.fNodeCount = i + 1;
        return i;
    }

    protected static int binarySearch(int[] values, int start, int end, int target) {
        while (start <= end) {
            int middle = (start + end) >>> 1;
            int value = values[middle];
            if (value == target) {
                while (middle > 0 && values[middle - 1] == target) {
                    middle--;
                }
                return middle;
            }
            if (value > target) {
                end = middle - 1;
            } else {
                start = middle + 1;
            }
        }
        return -1;
    }

    static {
        for (int i = 0; i < 2048; i++) {
            INIT_ARRAY[i] = -1;
        }
    }

    private final void createChunk(int[][] data, int chunk) {
        data[chunk] = new int[2049];
        System.arraycopy(INIT_ARRAY, 0, data[chunk], 0, 2048);
    }

    static final class RefCount {
        int fCount;

        RefCount() {
        }
    }

    private final void createChunk(Object[][] data, int chunk) {
        data[chunk] = new Object[2049];
        data[chunk][2048] = new RefCount();
    }

    private final int setChunkIndex(int[][] data, int value, int chunk, int index) {
        if (value == -1) {
            return clearChunkIndex(data, chunk, index);
        }
        int[] dataChunk = data[chunk];
        if (dataChunk == null) {
            createChunk(data, chunk);
            dataChunk = data[chunk];
        }
        int ovalue = dataChunk[index];
        if (ovalue == -1) {
            dataChunk[2048] = dataChunk[2048] + 1;
        }
        dataChunk[index] = value;
        return ovalue;
    }

    private final String setChunkValue(Object[][] data, Object value, int chunk, int index) {
        if (value == null) {
            return clearChunkValue(data, chunk, index);
        }
        Object[] dataChunk = data[chunk];
        if (dataChunk == null) {
            createChunk(data, chunk);
            dataChunk = data[chunk];
        }
        String ovalue = (String) dataChunk[index];
        if (ovalue == null) {
            RefCount c = (RefCount) dataChunk[2048];
            c.fCount++;
        }
        dataChunk[index] = value;
        return ovalue;
    }

    private final int getChunkIndex(int[][] data, int chunk, int index) {
        if (data[chunk] != null) {
            return data[chunk][index];
        }
        return -1;
    }

    private final String getChunkValue(Object[][] data, int chunk, int index) {
        if (data[chunk] != null) {
            return (String) data[chunk][index];
        }
        return null;
    }

    private final String getNodeValue(int chunk, int index) {
        String str = this.fNodeValue[chunk][index];
        if (str == 0) {
            return null;
        }
        if (str instanceof String) {
            return str;
        }
        return str.toString();
    }

    private final int clearChunkIndex(int[][] data, int chunk, int index) {
        int value = data[chunk] != null ? data[chunk][index] : -1;
        if (value != -1) {
            data[chunk][2048] = r2[2048] - 1;
            data[chunk][index] = -1;
            if (data[chunk][2048] == 0) {
                data[chunk] = null;
            }
        }
        return value;
    }

    private final String clearChunkValue(Object[][] data, int chunk, int index) {
        String value = data[chunk] != null ? (String) data[chunk][index] : null;
        if (value != null) {
            data[chunk][index] = null;
            RefCount c = (RefCount) data[chunk][2048];
            c.fCount--;
            if (c.fCount == 0) {
                data[chunk] = null;
            }
        }
        return value;
    }

    private final void putIdentifier0(String idName, Element element) {
        if (this.identifiers == null) {
            this.identifiers = new Hashtable();
        }
        this.identifiers.put(idName, element);
    }

    private static void print(int[] values, int start, int end, int middle, int target) {
    }

    static final class IntVector {
        private int[] data;
        private int size;

        IntVector() {
        }

        public int size() {
            return this.size;
        }

        public int elementAt(int index) {
            return this.data[index];
        }

        public void addElement(int element) {
            ensureCapacity(this.size + 1);
            int[] iArr = this.data;
            int i = this.size;
            this.size = i + 1;
            iArr[i] = element;
        }

        public void removeAllElements() {
            this.size = 0;
        }

        private void ensureCapacity(int newsize) {
            if (this.data == null) {
                this.data = new int[newsize + 15];
            } else if (newsize > this.data.length) {
                int[] newdata = new int[newsize + 15];
                System.arraycopy(this.data, 0, newdata, 0, this.data.length);
                this.data = newdata;
            }
        }
    }
}
