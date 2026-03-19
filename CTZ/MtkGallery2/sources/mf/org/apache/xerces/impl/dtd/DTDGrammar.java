package mf.org.apache.xerces.impl.dtd;

import java.util.ArrayList;
import java.util.Hashtable;
import mf.org.apache.xerces.dom3.as.ASContentModel;
import mf.org.apache.xerces.impl.dtd.models.CMAny;
import mf.org.apache.xerces.impl.dtd.models.CMBinOp;
import mf.org.apache.xerces.impl.dtd.models.CMLeaf;
import mf.org.apache.xerces.impl.dtd.models.CMNode;
import mf.org.apache.xerces.impl.dtd.models.CMUniOp;
import mf.org.apache.xerces.impl.dtd.models.ContentModelValidator;
import mf.org.apache.xerces.impl.dtd.models.DFAContentModel;
import mf.org.apache.xerces.impl.dtd.models.MixedContentModel;
import mf.org.apache.xerces.impl.dtd.models.SimpleContentModel;
import mf.org.apache.xerces.impl.dv.DatatypeValidator;
import mf.org.apache.xerces.impl.validation.EntityState;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLDTDContentModelHandler;
import mf.org.apache.xerces.xni.XMLDTDHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.parser.XMLDTDContentModelSource;
import mf.org.apache.xerces.xni.parser.XMLDTDSource;

public class DTDGrammar implements EntityState, XMLDTDContentModelHandler, XMLDTDHandler, Grammar {
    private static final int CHUNK_MASK = 255;
    private static final int CHUNK_SHIFT = 8;
    private static final int CHUNK_SIZE = 256;
    private static final boolean DEBUG = false;
    private static final int INITIAL_CHUNK_COUNT = 4;
    private static final short LIST_FLAG = 128;
    private static final short LIST_MASK = -129;
    public static final int TOP_LEVEL_SCOPE = -1;
    protected int fCurrentAttributeIndex;
    protected int fCurrentElementIndex;
    protected XMLDTDDescription fGrammarDescription;
    private boolean fMixed;
    private SymbolTable fSymbolTable;
    protected XMLDTDSource fDTDSource = null;
    protected XMLDTDContentModelSource fDTDContentModelSource = null;
    protected boolean fReadingExternalDTD = false;
    private int fElementDeclCount = 0;
    private QName[][] fElementDeclName = new QName[4][];
    private short[][] fElementDeclType = new short[4][];
    private int[][] fElementDeclContentSpecIndex = new int[4][];
    private ContentModelValidator[][] fElementDeclContentModelValidator = new ContentModelValidator[4][];
    private int[][] fElementDeclFirstAttributeDeclIndex = new int[4][];
    private int[][] fElementDeclLastAttributeDeclIndex = new int[4][];
    private int fAttributeDeclCount = 0;
    private QName[][] fAttributeDeclName = new QName[4][];
    private boolean fIsImmutable = false;
    private short[][] fAttributeDeclType = new short[4][];
    private String[][][] fAttributeDeclEnumeration = new String[4][][];
    private short[][] fAttributeDeclDefaultType = new short[4][];
    private DatatypeValidator[][] fAttributeDeclDatatypeValidator = new DatatypeValidator[4][];
    private String[][] fAttributeDeclDefaultValue = new String[4][];
    private String[][] fAttributeDeclNonNormalizedDefaultValue = new String[4][];
    private int[][] fAttributeDeclNextAttributeDeclIndex = new int[4][];
    private int fContentSpecCount = 0;
    private short[][] fContentSpecType = new short[4][];
    private Object[][] fContentSpecValue = new Object[4][];
    private Object[][] fContentSpecOtherValue = new Object[4][];
    private int fEntityCount = 0;
    private String[][] fEntityName = new String[4][];
    private String[][] fEntityValue = new String[4][];
    private String[][] fEntityPublicId = new String[4][];
    private String[][] fEntitySystemId = new String[4][];
    private String[][] fEntityBaseSystemId = new String[4][];
    private String[][] fEntityNotation = new String[4][];
    private byte[][] fEntityIsPE = new byte[4][];
    private byte[][] fEntityInExternal = new byte[4][];
    private int fNotationCount = 0;
    private String[][] fNotationName = new String[4][];
    private String[][] fNotationPublicId = new String[4][];
    private String[][] fNotationSystemId = new String[4][];
    private String[][] fNotationBaseSystemId = new String[4][];
    private QNameHashtable fElementIndexMap = new QNameHashtable();
    private QNameHashtable fEntityIndexMap = new QNameHashtable();
    private QNameHashtable fNotationIndexMap = new QNameHashtable();
    private final QName fQName = new QName();
    private final QName fQName2 = new QName();
    protected final XMLAttributeDecl fAttributeDecl = new XMLAttributeDecl();
    private int fLeafCount = 0;
    private int fEpsilonIndex = -1;
    private XMLElementDecl fElementDecl = new XMLElementDecl();
    private XMLEntityDecl fEntityDecl = new XMLEntityDecl();
    private XMLSimpleType fSimpleType = new XMLSimpleType();
    private XMLContentSpec fContentSpec = new XMLContentSpec();
    Hashtable fElementDeclTab = new Hashtable();
    private short[] fOpStack = null;
    private int[] fNodeIndexStack = null;
    private int[] fPrevNodeIndexStack = null;
    private int fDepth = 0;
    private boolean[] fPEntityStack = new boolean[4];
    private int fPEDepth = 0;
    private int[][] fElementDeclIsExternal = new int[4][];
    private int[][] fAttributeDeclIsExternal = new int[4][];
    int valueIndex = -1;
    int prevNodeIndex = -1;
    int nodeIndex = -1;

    private static class ChildrenList {
        public int length = 0;
        public QName[] qname = new QName[2];
        public int[] type = new int[2];
    }

    public DTDGrammar(SymbolTable symbolTable, XMLDTDDescription desc) {
        this.fGrammarDescription = null;
        this.fSymbolTable = symbolTable;
        this.fGrammarDescription = desc;
    }

    @Override
    public XMLGrammarDescription getGrammarDescription() {
        return this.fGrammarDescription;
    }

    public boolean getElementDeclIsExternal(int elementDeclIndex) {
        if (elementDeclIndex < 0) {
            return false;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        return this.fElementDeclIsExternal[chunk][index] != 0;
    }

    public boolean getAttributeDeclIsExternal(int attributeDeclIndex) {
        if (attributeDeclIndex < 0) {
            return false;
        }
        int chunk = attributeDeclIndex >> 8;
        int index = attributeDeclIndex & CHUNK_MASK;
        return this.fAttributeDeclIsExternal[chunk][index] != 0;
    }

    public int getAttributeDeclIndex(int elementDeclIndex, String attributeDeclName) {
        if (elementDeclIndex == -1) {
            return -1;
        }
        int attDefIndex = getFirstAttributeDeclIndex(elementDeclIndex);
        while (attDefIndex != -1) {
            getAttributeDecl(attDefIndex, this.fAttributeDecl);
            if (this.fAttributeDecl.name.rawname == attributeDeclName || attributeDeclName.equals(this.fAttributeDecl.name.rawname)) {
                return attDefIndex;
            }
            attDefIndex = getNextAttributeDeclIndex(attDefIndex);
        }
        return -1;
    }

    @Override
    public void startDTD(XMLLocator locator, Augmentations augs) throws XNIException {
        this.fOpStack = null;
        this.fNodeIndexStack = null;
        this.fPrevNodeIndexStack = null;
    }

    @Override
    public void startParameterEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        if (this.fPEDepth == this.fPEntityStack.length) {
            boolean[] entityarray = new boolean[this.fPEntityStack.length * 2];
            System.arraycopy(this.fPEntityStack, 0, entityarray, 0, this.fPEntityStack.length);
            this.fPEntityStack = entityarray;
        }
        this.fPEntityStack[this.fPEDepth] = this.fReadingExternalDTD;
        this.fPEDepth++;
    }

    @Override
    public void startExternalSubset(XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        this.fReadingExternalDTD = true;
    }

    @Override
    public void endParameterEntity(String name, Augmentations augs) throws XNIException {
        this.fPEDepth--;
        this.fReadingExternalDTD = this.fPEntityStack[this.fPEDepth];
    }

    @Override
    public void endExternalSubset(Augmentations augs) throws XNIException {
        this.fReadingExternalDTD = false;
    }

    @Override
    public void elementDecl(String name, String contentModel, Augmentations augs) throws XNIException {
        XMLElementDecl tmpElementDecl = (XMLElementDecl) this.fElementDeclTab.get(name);
        if (tmpElementDecl != null) {
            if (tmpElementDecl.type == -1) {
                this.fCurrentElementIndex = getElementDeclIndex(name);
            } else {
                return;
            }
        } else {
            this.fCurrentElementIndex = createElementDecl();
        }
        XMLElementDecl elementDecl = new XMLElementDecl();
        this.fQName.setValues(null, name, name, null);
        elementDecl.name.setValues(this.fQName);
        elementDecl.contentModelValidator = null;
        elementDecl.scope = -1;
        if (contentModel.equals("EMPTY")) {
            elementDecl.type = (short) 1;
        } else if (contentModel.equals("ANY")) {
            elementDecl.type = (short) 0;
        } else if (contentModel.startsWith("(")) {
            if (contentModel.indexOf("#PCDATA") > 0) {
                elementDecl.type = (short) 2;
            } else {
                elementDecl.type = (short) 3;
            }
        }
        this.fElementDeclTab.put(name, elementDecl);
        this.fElementDecl = elementDecl;
        addContentSpecToElement(elementDecl);
        setElementDecl(this.fCurrentElementIndex, this.fElementDecl);
        int chunk = this.fCurrentElementIndex >> 8;
        int index = this.fCurrentElementIndex & CHUNK_MASK;
        ensureElementDeclCapacity(chunk);
        this.fElementDeclIsExternal[chunk][index] = (this.fReadingExternalDTD || this.fPEDepth > 0) ? 1 : 0;
    }

    @Override
    public void attributeDecl(String elementName, String attributeName, String type, String[] enumeration, String defaultType, XMLString defaultValue, XMLString nonNormalizedDefaultValue, Augmentations augs) throws XNIException {
        if (!this.fElementDeclTab.containsKey(elementName)) {
            this.fCurrentElementIndex = createElementDecl();
            XMLElementDecl elementDecl = new XMLElementDecl();
            elementDecl.name.setValues(null, elementName, elementName, null);
            elementDecl.scope = -1;
            this.fElementDeclTab.put(elementName, elementDecl);
            setElementDecl(this.fCurrentElementIndex, elementDecl);
        }
        int elementIndex = getElementDeclIndex(elementName);
        if (getAttributeDeclIndex(elementIndex, attributeName) != -1) {
            return;
        }
        this.fCurrentAttributeIndex = createAttributeDecl();
        this.fSimpleType.clear();
        if (defaultType != null) {
            if (defaultType.equals("#FIXED")) {
                this.fSimpleType.defaultType = (short) 1;
            } else if (defaultType.equals("#IMPLIED")) {
                this.fSimpleType.defaultType = (short) 0;
            } else if (defaultType.equals("#REQUIRED")) {
                this.fSimpleType.defaultType = (short) 2;
            }
        }
        this.fSimpleType.defaultValue = defaultValue != null ? defaultValue.toString() : null;
        this.fSimpleType.nonNormalizedDefaultValue = nonNormalizedDefaultValue != null ? nonNormalizedDefaultValue.toString() : null;
        this.fSimpleType.enumeration = enumeration;
        if (type.equals("CDATA")) {
            this.fSimpleType.type = (short) 0;
        } else if (type.equals(SchemaSymbols.ATTVAL_ID)) {
            this.fSimpleType.type = (short) 3;
        } else if (type.startsWith(SchemaSymbols.ATTVAL_IDREF)) {
            this.fSimpleType.type = (short) 4;
            if (type.indexOf("S") > 0) {
                this.fSimpleType.list = true;
            }
        } else if (type.equals(SchemaSymbols.ATTVAL_ENTITIES)) {
            this.fSimpleType.type = (short) 1;
            this.fSimpleType.list = true;
        } else if (type.equals(SchemaSymbols.ATTVAL_ENTITY)) {
            this.fSimpleType.type = (short) 1;
        } else if (type.equals(SchemaSymbols.ATTVAL_NMTOKENS)) {
            this.fSimpleType.type = (short) 5;
            this.fSimpleType.list = true;
        } else if (type.equals(SchemaSymbols.ATTVAL_NMTOKEN)) {
            this.fSimpleType.type = (short) 5;
        } else if (type.startsWith(SchemaSymbols.ATTVAL_NOTATION)) {
            this.fSimpleType.type = (short) 6;
        } else if (type.startsWith("ENUMERATION")) {
            this.fSimpleType.type = (short) 2;
        } else {
            System.err.println("!!! unknown attribute type " + type);
        }
        this.fQName.setValues(null, attributeName, attributeName, null);
        this.fAttributeDecl.setValues(this.fQName, this.fSimpleType, false);
        setAttributeDecl(elementIndex, this.fCurrentAttributeIndex, this.fAttributeDecl);
        int chunk = this.fCurrentAttributeIndex >> 8;
        int index = this.fCurrentAttributeIndex & CHUNK_MASK;
        ensureAttributeDeclCapacity(chunk);
        this.fAttributeDeclIsExternal[chunk][index] = (this.fReadingExternalDTD || this.fPEDepth > 0) ? 1 : 0;
    }

    @Override
    public void internalEntityDecl(String name, XMLString text, XMLString nonNormalizedText, Augmentations augs) throws XNIException {
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex == -1) {
            int entityIndex2 = createEntityDecl();
            boolean isPE = name.startsWith("%");
            boolean inExternal = this.fReadingExternalDTD || this.fPEDepth > 0;
            XMLEntityDecl entityDecl = new XMLEntityDecl();
            entityDecl.setValues(name, null, null, null, null, text.toString(), isPE, inExternal);
            setEntityDecl(entityIndex2, entityDecl);
        }
    }

    @Override
    public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex == -1) {
            int entityIndex2 = createEntityDecl();
            boolean isPE = name.startsWith("%");
            boolean inExternal = this.fReadingExternalDTD || this.fPEDepth > 0;
            XMLEntityDecl entityDecl = new XMLEntityDecl();
            entityDecl.setValues(name, identifier.getPublicId(), identifier.getLiteralSystemId(), identifier.getBaseSystemId(), null, null, isPE, inExternal);
            setEntityDecl(entityIndex2, entityDecl);
        }
    }

    @Override
    public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier, String notation, Augmentations augs) throws XNIException {
        XMLEntityDecl entityDecl = new XMLEntityDecl();
        boolean isPE = name.startsWith("%");
        boolean inExternal = this.fReadingExternalDTD || this.fPEDepth > 0;
        entityDecl.setValues(name, identifier.getPublicId(), identifier.getLiteralSystemId(), identifier.getBaseSystemId(), notation, null, isPE, inExternal);
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex == -1) {
            int entityIndex2 = createEntityDecl();
            setEntityDecl(entityIndex2, entityDecl);
        }
    }

    @Override
    public void notationDecl(String name, XMLResourceIdentifier identifier, Augmentations augs) throws XNIException {
        XMLNotationDecl notationDecl = new XMLNotationDecl();
        notationDecl.setValues(name, identifier.getPublicId(), identifier.getLiteralSystemId(), identifier.getBaseSystemId());
        int notationIndex = getNotationDeclIndex(name);
        if (notationIndex == -1) {
            int notationIndex2 = createNotationDecl();
            setNotationDecl(notationIndex2, notationDecl);
        }
    }

    public void endDTD(Augmentations augs) throws XNIException {
        this.fIsImmutable = true;
        if (this.fGrammarDescription.getRootName() == null) {
            int size = this.fElementDeclCount;
            ArrayList elements = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                int chunk = i >> 8;
                int index = i & CHUNK_MASK;
                String currName = this.fElementDeclName[chunk][index].rawname;
                elements.add(currName);
            }
            this.fGrammarDescription.setPossibleRoots(elements);
        }
    }

    @Override
    public void setDTDSource(XMLDTDSource source) {
        this.fDTDSource = source;
    }

    @Override
    public XMLDTDSource getDTDSource() {
        return this.fDTDSource;
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
    }

    @Override
    public void startAttlist(String elementName, Augmentations augs) throws XNIException {
    }

    @Override
    public void endAttlist(Augmentations augs) throws XNIException {
    }

    @Override
    public void startConditional(short type, Augmentations augs) throws XNIException {
    }

    @Override
    public void ignoredCharacters(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void endConditional(Augmentations augs) throws XNIException {
    }

    @Override
    public void setDTDContentModelSource(XMLDTDContentModelSource source) {
        this.fDTDContentModelSource = source;
    }

    @Override
    public XMLDTDContentModelSource getDTDContentModelSource() {
        return this.fDTDContentModelSource;
    }

    public void startContentModel(String elementName, Augmentations augs) throws XNIException {
        XMLElementDecl elementDecl = (XMLElementDecl) this.fElementDeclTab.get(elementName);
        if (elementDecl != null) {
            this.fElementDecl = elementDecl;
        }
        this.fDepth = 0;
        initializeContentModelStack();
    }

    public void startGroup(Augmentations augs) throws XNIException {
        this.fDepth++;
        initializeContentModelStack();
        this.fMixed = false;
    }

    public void pcdata(Augmentations augs) throws XNIException {
        this.fMixed = true;
    }

    public void element(String elementName, Augmentations augs) throws XNIException {
        if (this.fMixed) {
            if (this.fNodeIndexStack[this.fDepth] == -1) {
                this.fNodeIndexStack[this.fDepth] = addUniqueLeafNode(elementName);
                return;
            } else {
                this.fNodeIndexStack[this.fDepth] = addContentSpecNode((short) 4, this.fNodeIndexStack[this.fDepth], addUniqueLeafNode(elementName));
                return;
            }
        }
        this.fNodeIndexStack[this.fDepth] = addContentSpecNode((short) 0, elementName);
    }

    public void separator(short separator, Augmentations augs) throws XNIException {
        if (!this.fMixed) {
            if (this.fOpStack[this.fDepth] != 5 && separator == 0) {
                if (this.fPrevNodeIndexStack[this.fDepth] != -1) {
                    this.fNodeIndexStack[this.fDepth] = addContentSpecNode(this.fOpStack[this.fDepth], this.fPrevNodeIndexStack[this.fDepth], this.fNodeIndexStack[this.fDepth]);
                }
                this.fPrevNodeIndexStack[this.fDepth] = this.fNodeIndexStack[this.fDepth];
                this.fOpStack[this.fDepth] = 4;
                return;
            }
            if (this.fOpStack[this.fDepth] != 4 && separator == 1) {
                if (this.fPrevNodeIndexStack[this.fDepth] != -1) {
                    this.fNodeIndexStack[this.fDepth] = addContentSpecNode(this.fOpStack[this.fDepth], this.fPrevNodeIndexStack[this.fDepth], this.fNodeIndexStack[this.fDepth]);
                }
                this.fPrevNodeIndexStack[this.fDepth] = this.fNodeIndexStack[this.fDepth];
                this.fOpStack[this.fDepth] = 5;
            }
        }
    }

    public void occurrence(short occurrence, Augmentations augs) throws XNIException {
        if (!this.fMixed) {
            if (occurrence == 2) {
                this.fNodeIndexStack[this.fDepth] = addContentSpecNode((short) 1, this.fNodeIndexStack[this.fDepth], -1);
            } else if (occurrence == 3) {
                this.fNodeIndexStack[this.fDepth] = addContentSpecNode((short) 2, this.fNodeIndexStack[this.fDepth], -1);
            } else if (occurrence == 4) {
                this.fNodeIndexStack[this.fDepth] = addContentSpecNode((short) 3, this.fNodeIndexStack[this.fDepth], -1);
            }
        }
    }

    public void endGroup(Augmentations augs) throws XNIException {
        if (!this.fMixed) {
            if (this.fPrevNodeIndexStack[this.fDepth] != -1) {
                this.fNodeIndexStack[this.fDepth] = addContentSpecNode(this.fOpStack[this.fDepth], this.fPrevNodeIndexStack[this.fDepth], this.fNodeIndexStack[this.fDepth]);
            }
            int[] iArr = this.fNodeIndexStack;
            int i = this.fDepth;
            this.fDepth = i - 1;
            int nodeIndex = iArr[i];
            this.fNodeIndexStack[this.fDepth] = nodeIndex;
        }
    }

    @Override
    public void any(Augmentations augs) throws XNIException {
    }

    @Override
    public void empty(Augmentations augs) throws XNIException {
    }

    @Override
    public void endContentModel(Augmentations augs) throws XNIException {
    }

    public boolean isNamespaceAware() {
        return false;
    }

    public SymbolTable getSymbolTable() {
        return this.fSymbolTable;
    }

    public int getFirstElementDeclIndex() {
        return this.fElementDeclCount >= 0 ? 0 : -1;
    }

    public int getNextElementDeclIndex(int elementDeclIndex) {
        if (elementDeclIndex < this.fElementDeclCount - 1) {
            return elementDeclIndex + 1;
        }
        return -1;
    }

    public int getElementDeclIndex(String elementDeclName) {
        int mapping = this.fElementIndexMap.get(elementDeclName);
        return mapping;
    }

    public int getElementDeclIndex(QName elementDeclQName) {
        return getElementDeclIndex(elementDeclQName.rawname);
    }

    public short getContentSpecType(int elementIndex) {
        if (elementIndex < 0 || elementIndex >= this.fElementDeclCount) {
            return (short) -1;
        }
        int chunk = elementIndex >> 8;
        int index = elementIndex & CHUNK_MASK;
        if (this.fElementDeclType[chunk][index] == -1) {
            return (short) -1;
        }
        return (short) (this.fElementDeclType[chunk][index] & LIST_MASK);
    }

    public boolean getElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return false;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        elementDecl.name.setValues(this.fElementDeclName[chunk][index]);
        if (this.fElementDeclType[chunk][index] == -1) {
            elementDecl.type = (short) -1;
            elementDecl.simpleType.list = false;
        } else {
            elementDecl.type = (short) (this.fElementDeclType[chunk][index] & LIST_MASK);
            elementDecl.simpleType.list = (this.fElementDeclType[chunk][index] & 128) != 0;
        }
        if (elementDecl.type == 3 || elementDecl.type == 2) {
            elementDecl.contentModelValidator = getElementContentModelValidator(elementDeclIndex);
        }
        elementDecl.simpleType.datatypeValidator = null;
        elementDecl.simpleType.defaultType = (short) -1;
        elementDecl.simpleType.defaultValue = null;
        return true;
    }

    QName getElementDeclName(int elementDeclIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return null;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        return this.fElementDeclName[chunk][index];
    }

    public int getFirstAttributeDeclIndex(int elementDeclIndex) {
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        return this.fElementDeclFirstAttributeDeclIndex[chunk][index];
    }

    public int getNextAttributeDeclIndex(int attributeDeclIndex) {
        int chunk = attributeDeclIndex >> 8;
        int index = attributeDeclIndex & CHUNK_MASK;
        return this.fAttributeDeclNextAttributeDeclIndex[chunk][index];
    }

    public boolean getAttributeDecl(int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
        boolean isList;
        short attributeType;
        if (attributeDeclIndex < 0 || attributeDeclIndex >= this.fAttributeDeclCount) {
            return false;
        }
        int chunk = attributeDeclIndex >> 8;
        int index = attributeDeclIndex & CHUNK_MASK;
        attributeDecl.name.setValues(this.fAttributeDeclName[chunk][index]);
        if (this.fAttributeDeclType[chunk][index] == -1) {
            attributeType = -1;
            isList = false;
        } else {
            short attributeType2 = (short) (this.fAttributeDeclType[chunk][index] & LIST_MASK);
            isList = (this.fAttributeDeclType[chunk][index] & 128) != 0;
            attributeType = attributeType2;
        }
        attributeDecl.simpleType.setValues(attributeType, this.fAttributeDeclName[chunk][index].localpart, this.fAttributeDeclEnumeration[chunk][index], isList, this.fAttributeDeclDefaultType[chunk][index], this.fAttributeDeclDefaultValue[chunk][index], this.fAttributeDeclNonNormalizedDefaultValue[chunk][index], this.fAttributeDeclDatatypeValidator[chunk][index]);
        return true;
    }

    public boolean isCDATAAttribute(QName elName, QName atName) {
        int elDeclIdx = getElementDeclIndex(elName);
        if (getAttributeDecl(elDeclIdx, this.fAttributeDecl) && this.fAttributeDecl.simpleType.type != 0) {
            return false;
        }
        return true;
    }

    public int getEntityDeclIndex(String entityDeclName) {
        if (entityDeclName == null) {
            return -1;
        }
        return this.fEntityIndexMap.get(entityDeclName);
    }

    public boolean getEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
        if (entityDeclIndex < 0 || entityDeclIndex >= this.fEntityCount) {
            return false;
        }
        int chunk = entityDeclIndex >> 8;
        int index = entityDeclIndex & CHUNK_MASK;
        entityDecl.setValues(this.fEntityName[chunk][index], this.fEntityPublicId[chunk][index], this.fEntitySystemId[chunk][index], this.fEntityBaseSystemId[chunk][index], this.fEntityNotation[chunk][index], this.fEntityValue[chunk][index], this.fEntityIsPE[chunk][index] != 0, this.fEntityInExternal[chunk][index] != 0);
        return true;
    }

    public int getNotationDeclIndex(String notationDeclName) {
        if (notationDeclName == null) {
            return -1;
        }
        return this.fNotationIndexMap.get(notationDeclName);
    }

    public boolean getNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
        if (notationDeclIndex < 0 || notationDeclIndex >= this.fNotationCount) {
            return false;
        }
        int chunk = notationDeclIndex >> 8;
        int index = notationDeclIndex & CHUNK_MASK;
        notationDecl.setValues(this.fNotationName[chunk][index], this.fNotationPublicId[chunk][index], this.fNotationSystemId[chunk][index], this.fNotationBaseSystemId[chunk][index]);
        return true;
    }

    public boolean getContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
        if (contentSpecIndex < 0 || contentSpecIndex >= this.fContentSpecCount) {
            return false;
        }
        int chunk = contentSpecIndex >> 8;
        int index = contentSpecIndex & CHUNK_MASK;
        contentSpec.type = this.fContentSpecType[chunk][index];
        contentSpec.value = this.fContentSpecValue[chunk][index];
        contentSpec.otherValue = this.fContentSpecOtherValue[chunk][index];
        return true;
    }

    public int getContentSpecIndex(int elementDeclIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return -1;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        return this.fElementDeclContentSpecIndex[chunk][index];
    }

    public String getContentSpecAsString(int elementDeclIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return null;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        int contentSpecIndex = this.fElementDeclContentSpecIndex[chunk][index];
        XMLContentSpec contentSpec = new XMLContentSpec();
        if (!getContentSpec(contentSpecIndex, contentSpec)) {
            return null;
        }
        StringBuffer str = new StringBuffer();
        int parentContentSpecType = contentSpec.type & 15;
        switch (parentContentSpecType) {
            case 0:
                str.append('(');
                if (contentSpec.value == null && contentSpec.otherValue == null) {
                    str.append("#PCDATA");
                } else {
                    str.append(contentSpec.value);
                }
                str.append(')');
                break;
            case 1:
                getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                int nextContentSpec = contentSpec.type;
                if (nextContentSpec == 0) {
                    str.append('(');
                    str.append(contentSpec.value);
                    str.append(')');
                } else if (nextContentSpec == 3 || nextContentSpec == 2 || nextContentSpec == 1) {
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, parentContentSpecType);
                    str.append(')');
                } else {
                    appendContentSpec(contentSpec, str, true, parentContentSpecType);
                }
                str.append('?');
                break;
            case 2:
                getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                int nextContentSpec2 = contentSpec.type;
                if (nextContentSpec2 == 0) {
                    str.append('(');
                    if (contentSpec.value == null && contentSpec.otherValue == null) {
                        str.append("#PCDATA");
                    } else if (contentSpec.otherValue != null) {
                        str.append("##any:uri=");
                        str.append(contentSpec.otherValue);
                    } else if (contentSpec.value == null) {
                        str.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                    } else {
                        appendContentSpec(contentSpec, str, true, parentContentSpecType);
                    }
                    str.append(')');
                } else if (nextContentSpec2 == 3 || nextContentSpec2 == 2 || nextContentSpec2 == 1) {
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, parentContentSpecType);
                    str.append(')');
                } else {
                    appendContentSpec(contentSpec, str, true, parentContentSpecType);
                }
                str.append('*');
                break;
            case 3:
                getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                int nextContentSpec3 = contentSpec.type;
                if (nextContentSpec3 == 0) {
                    str.append('(');
                    if (contentSpec.value == null && contentSpec.otherValue == null) {
                        str.append("#PCDATA");
                    } else if (contentSpec.otherValue != null) {
                        str.append("##any:uri=");
                        str.append(contentSpec.otherValue);
                    } else if (contentSpec.value == null) {
                        str.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                    } else {
                        str.append(contentSpec.value);
                    }
                    str.append(')');
                } else if (nextContentSpec3 == 3 || nextContentSpec3 == 2 || nextContentSpec3 == 1) {
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, parentContentSpecType);
                    str.append(')');
                } else {
                    appendContentSpec(contentSpec, str, true, parentContentSpecType);
                }
                str.append('+');
                break;
            case 4:
            case 5:
                appendContentSpec(contentSpec, str, true, parentContentSpecType);
                break;
            case 6:
                str.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                if (contentSpec.otherValue != null) {
                    str.append(":uri=");
                    str.append(contentSpec.otherValue);
                }
                break;
            case 7:
                str.append("##other:uri=");
                str.append(contentSpec.otherValue);
                break;
            case 8:
                str.append(SchemaSymbols.ATTVAL_TWOPOUNDLOCAL);
                break;
            default:
                str.append("???");
                break;
        }
        return str.toString();
    }

    public void printElements() {
        int elementDeclIndex = 0;
        XMLElementDecl elementDecl = new XMLElementDecl();
        while (true) {
            int elementDeclIndex2 = elementDeclIndex + 1;
            if (getElementDecl(elementDeclIndex, elementDecl)) {
                System.out.println("element decl: " + elementDecl.name + ", " + elementDecl.name.rawname);
                elementDeclIndex = elementDeclIndex2;
            } else {
                return;
            }
        }
    }

    public void printAttributes(int elementDeclIndex) {
        int attributeDeclIndex = getFirstAttributeDeclIndex(elementDeclIndex);
        System.out.print(elementDeclIndex);
        System.out.print(" [");
        while (attributeDeclIndex != -1) {
            System.out.print(' ');
            System.out.print(attributeDeclIndex);
            printAttribute(attributeDeclIndex);
            attributeDeclIndex = getNextAttributeDeclIndex(attributeDeclIndex);
            if (attributeDeclIndex != -1) {
                System.out.print(",");
            }
        }
        System.out.println(" ]");
    }

    protected void addContentSpecToElement(XMLElementDecl elementDecl) {
        if ((this.fDepth == 0 || (this.fDepth == 1 && elementDecl.type == 2)) && this.fNodeIndexStack != null) {
            if (elementDecl.type == 2) {
                int pcdata = addUniqueLeafNode(null);
                if (this.fNodeIndexStack[0] == -1) {
                    this.fNodeIndexStack[0] = pcdata;
                } else {
                    this.fNodeIndexStack[0] = addContentSpecNode((short) 4, pcdata, this.fNodeIndexStack[0]);
                }
            }
            setContentSpecIndex(this.fCurrentElementIndex, this.fNodeIndexStack[this.fDepth]);
        }
    }

    protected ContentModelValidator getElementContentModelValidator(int elementDeclIndex) {
        ContentModelValidator contentModel;
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        ContentModelValidator contentModel2 = this.fElementDeclContentModelValidator[chunk][index];
        if (contentModel2 != null) {
            return contentModel2;
        }
        short s = this.fElementDeclType[chunk][index];
        if (s == 4) {
            return null;
        }
        int contentSpecIndex = this.fElementDeclContentSpecIndex[chunk][index];
        XMLContentSpec contentSpec = new XMLContentSpec();
        getContentSpec(contentSpecIndex, contentSpec);
        if (s == 2) {
            ChildrenList children = new ChildrenList();
            contentSpecTree(contentSpecIndex, contentSpec, children);
            contentModel = new MixedContentModel(children.qname, children.type, 0, children.length, false);
        } else if (s == 3) {
            contentModel = createChildModel(contentSpecIndex);
        } else {
            throw new RuntimeException("Unknown content type for a element decl in getElementContentModelValidator() in AbstractDTDGrammar class");
        }
        this.fElementDeclContentModelValidator[chunk][index] = contentModel;
        return contentModel;
    }

    protected int createElementDecl() {
        int chunk = this.fElementDeclCount >> 8;
        int index = this.fElementDeclCount & CHUNK_MASK;
        ensureElementDeclCapacity(chunk);
        this.fElementDeclName[chunk][index] = new QName();
        this.fElementDeclType[chunk][index] = -1;
        this.fElementDeclContentModelValidator[chunk][index] = null;
        this.fElementDeclFirstAttributeDeclIndex[chunk][index] = -1;
        this.fElementDeclLastAttributeDeclIndex[chunk][index] = -1;
        int i = this.fElementDeclCount;
        this.fElementDeclCount = i + 1;
        return i;
    }

    protected void setElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        this.fElementDeclName[chunk][index].setValues(elementDecl.name);
        this.fElementDeclType[chunk][index] = elementDecl.type;
        this.fElementDeclContentModelValidator[chunk][index] = elementDecl.contentModelValidator;
        if (elementDecl.simpleType.list) {
            short[] sArr = this.fElementDeclType[chunk];
            sArr[index] = (short) (sArr[index] | 128);
        }
        this.fElementIndexMap.put(elementDecl.name.rawname, elementDeclIndex);
    }

    protected void putElementNameMapping(QName name, int scope, int elementDeclIndex) {
    }

    protected void setFirstAttributeDeclIndex(int elementDeclIndex, int newFirstAttrIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        this.fElementDeclFirstAttributeDeclIndex[chunk][index] = newFirstAttrIndex;
    }

    protected void setContentSpecIndex(int elementDeclIndex, int contentSpecIndex) {
        if (elementDeclIndex < 0 || elementDeclIndex >= this.fElementDeclCount) {
            return;
        }
        int chunk = elementDeclIndex >> 8;
        int index = elementDeclIndex & CHUNK_MASK;
        this.fElementDeclContentSpecIndex[chunk][index] = contentSpecIndex;
    }

    protected int createAttributeDecl() {
        int chunk = this.fAttributeDeclCount >> 8;
        int index = this.fAttributeDeclCount & CHUNK_MASK;
        ensureAttributeDeclCapacity(chunk);
        this.fAttributeDeclName[chunk][index] = new QName();
        this.fAttributeDeclType[chunk][index] = -1;
        this.fAttributeDeclDatatypeValidator[chunk][index] = null;
        this.fAttributeDeclEnumeration[chunk][index] = null;
        this.fAttributeDeclDefaultType[chunk][index] = 0;
        this.fAttributeDeclDefaultValue[chunk][index] = null;
        this.fAttributeDeclNonNormalizedDefaultValue[chunk][index] = null;
        this.fAttributeDeclNextAttributeDeclIndex[chunk][index] = -1;
        int i = this.fAttributeDeclCount;
        this.fAttributeDeclCount = i + 1;
        return i;
    }

    protected void setAttributeDecl(int elementDeclIndex, int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
        int attrChunk = attributeDeclIndex >> 8;
        int attrIndex = attributeDeclIndex & CHUNK_MASK;
        this.fAttributeDeclName[attrChunk][attrIndex].setValues(attributeDecl.name);
        this.fAttributeDeclType[attrChunk][attrIndex] = attributeDecl.simpleType.type;
        if (attributeDecl.simpleType.list) {
            short[] sArr = this.fAttributeDeclType[attrChunk];
            sArr[attrIndex] = (short) (sArr[attrIndex] | 128);
        }
        this.fAttributeDeclEnumeration[attrChunk][attrIndex] = attributeDecl.simpleType.enumeration;
        this.fAttributeDeclDefaultType[attrChunk][attrIndex] = attributeDecl.simpleType.defaultType;
        this.fAttributeDeclDatatypeValidator[attrChunk][attrIndex] = attributeDecl.simpleType.datatypeValidator;
        this.fAttributeDeclDefaultValue[attrChunk][attrIndex] = attributeDecl.simpleType.defaultValue;
        this.fAttributeDeclNonNormalizedDefaultValue[attrChunk][attrIndex] = attributeDecl.simpleType.nonNormalizedDefaultValue;
        int elemChunk = elementDeclIndex >> 8;
        int elemIndex = elementDeclIndex & CHUNK_MASK;
        int index = this.fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex];
        while (index != -1 && index != attributeDeclIndex) {
            index = this.fAttributeDeclNextAttributeDeclIndex[index >> 8][index & CHUNK_MASK];
        }
        if (index == -1) {
            if (this.fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex] == -1) {
                this.fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;
            } else {
                int index2 = this.fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex];
                this.fAttributeDeclNextAttributeDeclIndex[index2 >> 8][index2 & CHUNK_MASK] = attributeDeclIndex;
            }
            this.fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;
        }
    }

    protected int createContentSpec() {
        int chunk = this.fContentSpecCount >> 8;
        int index = this.fContentSpecCount & CHUNK_MASK;
        ensureContentSpecCapacity(chunk);
        this.fContentSpecType[chunk][index] = -1;
        this.fContentSpecValue[chunk][index] = null;
        this.fContentSpecOtherValue[chunk][index] = null;
        int i = this.fContentSpecCount;
        this.fContentSpecCount = i + 1;
        return i;
    }

    protected void setContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
        int chunk = contentSpecIndex >> 8;
        int index = contentSpecIndex & CHUNK_MASK;
        this.fContentSpecType[chunk][index] = contentSpec.type;
        this.fContentSpecValue[chunk][index] = contentSpec.value;
        this.fContentSpecOtherValue[chunk][index] = contentSpec.otherValue;
    }

    protected int createEntityDecl() {
        int chunk = this.fEntityCount >> 8;
        int index = this.fEntityCount & CHUNK_MASK;
        ensureEntityDeclCapacity(chunk);
        this.fEntityIsPE[chunk][index] = 0;
        this.fEntityInExternal[chunk][index] = 0;
        int i = this.fEntityCount;
        this.fEntityCount = i + 1;
        return i;
    }

    protected void setEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
        int chunk = entityDeclIndex >> 8;
        int index = entityDeclIndex & CHUNK_MASK;
        this.fEntityName[chunk][index] = entityDecl.name;
        this.fEntityValue[chunk][index] = entityDecl.value;
        this.fEntityPublicId[chunk][index] = entityDecl.publicId;
        this.fEntitySystemId[chunk][index] = entityDecl.systemId;
        this.fEntityBaseSystemId[chunk][index] = entityDecl.baseSystemId;
        this.fEntityNotation[chunk][index] = entityDecl.notation;
        this.fEntityIsPE[chunk][index] = entityDecl.isPE;
        this.fEntityInExternal[chunk][index] = entityDecl.inExternal;
        this.fEntityIndexMap.put(entityDecl.name, entityDeclIndex);
    }

    protected int createNotationDecl() {
        int chunk = this.fNotationCount >> 8;
        ensureNotationDeclCapacity(chunk);
        int i = this.fNotationCount;
        this.fNotationCount = i + 1;
        return i;
    }

    protected void setNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
        int chunk = notationDeclIndex >> 8;
        int index = notationDeclIndex & CHUNK_MASK;
        this.fNotationName[chunk][index] = notationDecl.name;
        this.fNotationPublicId[chunk][index] = notationDecl.publicId;
        this.fNotationSystemId[chunk][index] = notationDecl.systemId;
        this.fNotationBaseSystemId[chunk][index] = notationDecl.baseSystemId;
        this.fNotationIndexMap.put(notationDecl.name, notationDeclIndex);
    }

    protected int addContentSpecNode(short nodeType, String nodeValue) {
        int contentSpecIndex = createContentSpec();
        this.fContentSpec.setValues(nodeType, nodeValue, null);
        setContentSpec(contentSpecIndex, this.fContentSpec);
        return contentSpecIndex;
    }

    protected int addUniqueLeafNode(String elementName) {
        int contentSpecIndex = createContentSpec();
        this.fContentSpec.setValues((short) 0, elementName, null);
        setContentSpec(contentSpecIndex, this.fContentSpec);
        return contentSpecIndex;
    }

    protected int addContentSpecNode(short nodeType, int leftNodeIndex, int rightNodeIndex) {
        int contentSpecIndex = createContentSpec();
        int[] leftIntArray = {leftNodeIndex};
        int[] rightIntArray = {rightNodeIndex};
        this.fContentSpec.setValues(nodeType, leftIntArray, rightIntArray);
        setContentSpec(contentSpecIndex, this.fContentSpec);
        return contentSpecIndex;
    }

    protected void initializeContentModelStack() {
        if (this.fOpStack == null) {
            this.fOpStack = new short[8];
            this.fNodeIndexStack = new int[8];
            this.fPrevNodeIndexStack = new int[8];
        } else if (this.fDepth == this.fOpStack.length) {
            short[] newStack = new short[this.fDepth * 2];
            System.arraycopy(this.fOpStack, 0, newStack, 0, this.fDepth);
            this.fOpStack = newStack;
            int[] newIntStack = new int[this.fDepth * 2];
            System.arraycopy(this.fNodeIndexStack, 0, newIntStack, 0, this.fDepth);
            this.fNodeIndexStack = newIntStack;
            int[] newIntStack2 = new int[this.fDepth * 2];
            System.arraycopy(this.fPrevNodeIndexStack, 0, newIntStack2, 0, this.fDepth);
            this.fPrevNodeIndexStack = newIntStack2;
        }
        this.fOpStack[this.fDepth] = -1;
        this.fNodeIndexStack[this.fDepth] = -1;
        this.fPrevNodeIndexStack[this.fDepth] = -1;
    }

    boolean isImmutable() {
        return this.fIsImmutable;
    }

    private void appendContentSpec(XMLContentSpec contentSpec, StringBuffer str, boolean parens, int parentContentSpecType) {
        int thisContentSpec = contentSpec.type & 15;
        switch (thisContentSpec) {
            case 0:
                if (contentSpec.value == null && contentSpec.otherValue == null) {
                    str.append("#PCDATA");
                } else if (contentSpec.value == null && contentSpec.otherValue != null) {
                    str.append("##any:uri=");
                    str.append(contentSpec.otherValue);
                } else if (contentSpec.value == null) {
                    str.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                } else {
                    str.append(contentSpec.value);
                }
                break;
            case 1:
                if (parentContentSpecType == 3 || parentContentSpecType == 2 || parentContentSpecType == 1) {
                    getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')');
                } else {
                    getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('?');
                break;
            case 2:
                if (parentContentSpecType == 3 || parentContentSpecType == 2 || parentContentSpecType == 1) {
                    getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')');
                } else {
                    getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('*');
                break;
            case 3:
                if (parentContentSpecType == 3 || parentContentSpecType == 2 || parentContentSpecType == 1) {
                    str.append('(');
                    getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')');
                } else {
                    getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('+');
                break;
            case 4:
            case 5:
                if (parens) {
                    str.append('(');
                }
                int type = contentSpec.type;
                int otherValue = ((int[]) contentSpec.otherValue)[0];
                getContentSpec(((int[]) contentSpec.value)[0], contentSpec);
                appendContentSpec(contentSpec, str, contentSpec.type != type, thisContentSpec);
                if (type == 4) {
                    str.append('|');
                } else {
                    str.append(',');
                }
                getContentSpec(otherValue, contentSpec);
                appendContentSpec(contentSpec, str, true, thisContentSpec);
                if (parens) {
                    str.append(')');
                }
                break;
            case 6:
                str.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                if (contentSpec.otherValue != null) {
                    str.append(":uri=");
                    str.append(contentSpec.otherValue);
                }
                break;
            case 7:
                str.append("##other:uri=");
                str.append(contentSpec.otherValue);
                break;
            case 8:
                str.append(SchemaSymbols.ATTVAL_TWOPOUNDLOCAL);
                break;
            default:
                str.append("???");
                break;
        }
    }

    private void printAttribute(int attributeDeclIndex) {
        XMLAttributeDecl attributeDecl = new XMLAttributeDecl();
        if (getAttributeDecl(attributeDeclIndex, attributeDecl)) {
            System.out.print(" { ");
            System.out.print(attributeDecl.name.localpart);
            System.out.print(" }");
        }
    }

    private synchronized ContentModelValidator createChildModel(int contentSpecIndex) {
        XMLContentSpec contentSpec = new XMLContentSpec();
        getContentSpec(contentSpecIndex, contentSpec);
        if ((contentSpec.type & 15) != 6 && (contentSpec.type & 15) != 7 && (contentSpec.type & 15) != 8) {
            if (contentSpec.type == 0) {
                if (contentSpec.value == null && contentSpec.otherValue == null) {
                    throw new RuntimeException("ImplementationMessages.VAL_NPCD");
                }
                this.fQName.setValues(null, (String) contentSpec.value, (String) contentSpec.value, (String) contentSpec.otherValue);
                return new SimpleContentModel(contentSpec.type, this.fQName, null);
            }
            if (contentSpec.type == 4 || contentSpec.type == 5) {
                XMLContentSpec contentSpecLeft = new XMLContentSpec();
                XMLContentSpec contentSpecRight = new XMLContentSpec();
                getContentSpec(((int[]) contentSpec.value)[0], contentSpecLeft);
                getContentSpec(((int[]) contentSpec.otherValue)[0], contentSpecRight);
                if (contentSpecLeft.type == 0 && contentSpecRight.type == 0) {
                    this.fQName.setValues(null, (String) contentSpecLeft.value, (String) contentSpecLeft.value, (String) contentSpecLeft.otherValue);
                    this.fQName2.setValues(null, (String) contentSpecRight.value, (String) contentSpecRight.value, (String) contentSpecRight.otherValue);
                    return new SimpleContentModel(contentSpec.type, this.fQName, this.fQName2);
                }
            } else {
                if (contentSpec.type != 1 && contentSpec.type != 2 && contentSpec.type != 3) {
                    throw new RuntimeException("ImplementationMessages.VAL_CST");
                }
                XMLContentSpec contentSpecLeft2 = new XMLContentSpec();
                getContentSpec(((int[]) contentSpec.value)[0], contentSpecLeft2);
                if (contentSpecLeft2.type == 0) {
                    this.fQName.setValues(null, (String) contentSpecLeft2.value, (String) contentSpecLeft2.value, (String) contentSpecLeft2.otherValue);
                    return new SimpleContentModel(contentSpec.type, this.fQName, null);
                }
            }
        }
        this.fLeafCount = 0;
        this.fLeafCount = 0;
        CMNode cmn = buildSyntaxTree(contentSpecIndex, contentSpec);
        return new DFAContentModel(cmn, this.fLeafCount, false);
    }

    private final CMNode buildSyntaxTree(int startNode, XMLContentSpec contentSpec) {
        getContentSpec(startNode, contentSpec);
        if ((contentSpec.type & 15) == 6) {
            short s = contentSpec.type;
            String str = (String) contentSpec.otherValue;
            int i = this.fLeafCount;
            this.fLeafCount = i + 1;
            CMNode nodeRet = new CMAny(s, str, i);
            return nodeRet;
        }
        if ((contentSpec.type & 15) == 7) {
            short s2 = contentSpec.type;
            String str2 = (String) contentSpec.otherValue;
            int i2 = this.fLeafCount;
            this.fLeafCount = i2 + 1;
            CMNode nodeRet2 = new CMAny(s2, str2, i2);
            return nodeRet2;
        }
        if ((contentSpec.type & 15) == 8) {
            short s3 = contentSpec.type;
            int i3 = this.fLeafCount;
            this.fLeafCount = i3 + 1;
            CMNode nodeRet3 = new CMAny(s3, null, i3);
            return nodeRet3;
        }
        if (contentSpec.type == 0) {
            this.fQName.setValues(null, (String) contentSpec.value, (String) contentSpec.value, (String) contentSpec.otherValue);
            QName qName = this.fQName;
            int i4 = this.fLeafCount;
            this.fLeafCount = i4 + 1;
            CMNode nodeRet4 = new CMLeaf(qName, i4);
            return nodeRet4;
        }
        int leftNode = ((int[]) contentSpec.value)[0];
        int rightNode = ((int[]) contentSpec.otherValue)[0];
        if (contentSpec.type == 4 || contentSpec.type == 5) {
            CMNode nodeRet5 = new CMBinOp(contentSpec.type, buildSyntaxTree(leftNode, contentSpec), buildSyntaxTree(rightNode, contentSpec));
            return nodeRet5;
        }
        if (contentSpec.type == 2) {
            CMNode nodeRet6 = new CMUniOp(contentSpec.type, buildSyntaxTree(leftNode, contentSpec));
            return nodeRet6;
        }
        if (contentSpec.type == 2 || contentSpec.type == 1 || contentSpec.type == 3) {
            CMNode nodeRet7 = new CMUniOp(contentSpec.type, buildSyntaxTree(leftNode, contentSpec));
            return nodeRet7;
        }
        throw new RuntimeException("ImplementationMessages.VAL_CST");
    }

    private void contentSpecTree(int contentSpecIndex, XMLContentSpec contentSpec, ChildrenList children) {
        getContentSpec(contentSpecIndex, contentSpec);
        if (contentSpec.type == 0 || (contentSpec.type & 15) == 6 || (contentSpec.type & 15) == 8 || (contentSpec.type & 15) == 7) {
            if (children.length == children.qname.length) {
                QName[] newQName = new QName[children.length * 2];
                System.arraycopy(children.qname, 0, newQName, 0, children.length);
                children.qname = newQName;
                int[] newType = new int[children.length * 2];
                System.arraycopy(children.type, 0, newType, 0, children.length);
                children.type = newType;
            }
            children.qname[children.length] = new QName(null, (String) contentSpec.value, (String) contentSpec.value, (String) contentSpec.otherValue);
            children.type[children.length] = contentSpec.type;
            children.length++;
            return;
        }
        int leftNode = contentSpec.value != null ? ((int[]) contentSpec.value)[0] : -1;
        if (contentSpec.otherValue != null) {
            int rightNode = ((int[]) contentSpec.otherValue)[0];
            int rightNode2 = contentSpec.type;
            if (rightNode2 == 4 || contentSpec.type == 5) {
                contentSpecTree(leftNode, contentSpec, children);
                contentSpecTree(rightNode, contentSpec, children);
            } else if (contentSpec.type == 1 || contentSpec.type == 2 || contentSpec.type == 3) {
                contentSpecTree(leftNode, contentSpec, children);
            } else {
                throw new RuntimeException("Invalid content spec type seen in contentSpecTree() method of AbstractDTDGrammar class : " + ((int) contentSpec.type));
            }
        }
    }

    private void ensureElementDeclCapacity(int chunk) {
        if (chunk >= this.fElementDeclName.length) {
            this.fElementDeclIsExternal = resize(this.fElementDeclIsExternal, this.fElementDeclIsExternal.length * 2);
            this.fElementDeclName = resize(this.fElementDeclName, this.fElementDeclName.length * 2);
            this.fElementDeclType = resize(this.fElementDeclType, this.fElementDeclType.length * 2);
            this.fElementDeclContentModelValidator = resize(this.fElementDeclContentModelValidator, this.fElementDeclContentModelValidator.length * 2);
            this.fElementDeclContentSpecIndex = resize(this.fElementDeclContentSpecIndex, this.fElementDeclContentSpecIndex.length * 2);
            this.fElementDeclFirstAttributeDeclIndex = resize(this.fElementDeclFirstAttributeDeclIndex, this.fElementDeclFirstAttributeDeclIndex.length * 2);
            this.fElementDeclLastAttributeDeclIndex = resize(this.fElementDeclLastAttributeDeclIndex, this.fElementDeclLastAttributeDeclIndex.length * 2);
        } else if (this.fElementDeclName[chunk] != null) {
            return;
        }
        this.fElementDeclIsExternal[chunk] = new int[256];
        this.fElementDeclName[chunk] = new QName[256];
        this.fElementDeclType[chunk] = new short[256];
        this.fElementDeclContentModelValidator[chunk] = new ContentModelValidator[256];
        this.fElementDeclContentSpecIndex[chunk] = new int[256];
        this.fElementDeclFirstAttributeDeclIndex[chunk] = new int[256];
        this.fElementDeclLastAttributeDeclIndex[chunk] = new int[256];
    }

    private void ensureAttributeDeclCapacity(int chunk) {
        if (chunk >= this.fAttributeDeclName.length) {
            this.fAttributeDeclIsExternal = resize(this.fAttributeDeclIsExternal, this.fAttributeDeclIsExternal.length * 2);
            this.fAttributeDeclName = resize(this.fAttributeDeclName, this.fAttributeDeclName.length * 2);
            this.fAttributeDeclType = resize(this.fAttributeDeclType, this.fAttributeDeclType.length * 2);
            this.fAttributeDeclEnumeration = resize(this.fAttributeDeclEnumeration, this.fAttributeDeclEnumeration.length * 2);
            this.fAttributeDeclDefaultType = resize(this.fAttributeDeclDefaultType, this.fAttributeDeclDefaultType.length * 2);
            this.fAttributeDeclDatatypeValidator = resize(this.fAttributeDeclDatatypeValidator, this.fAttributeDeclDatatypeValidator.length * 2);
            this.fAttributeDeclDefaultValue = resize(this.fAttributeDeclDefaultValue, this.fAttributeDeclDefaultValue.length * 2);
            this.fAttributeDeclNonNormalizedDefaultValue = resize(this.fAttributeDeclNonNormalizedDefaultValue, this.fAttributeDeclNonNormalizedDefaultValue.length * 2);
            this.fAttributeDeclNextAttributeDeclIndex = resize(this.fAttributeDeclNextAttributeDeclIndex, this.fAttributeDeclNextAttributeDeclIndex.length * 2);
        } else if (this.fAttributeDeclName[chunk] != null) {
            return;
        }
        this.fAttributeDeclIsExternal[chunk] = new int[256];
        this.fAttributeDeclName[chunk] = new QName[256];
        this.fAttributeDeclType[chunk] = new short[256];
        this.fAttributeDeclEnumeration[chunk] = new String[256][];
        this.fAttributeDeclDefaultType[chunk] = new short[256];
        this.fAttributeDeclDatatypeValidator[chunk] = new DatatypeValidator[256];
        this.fAttributeDeclDefaultValue[chunk] = new String[256];
        this.fAttributeDeclNonNormalizedDefaultValue[chunk] = new String[256];
        this.fAttributeDeclNextAttributeDeclIndex[chunk] = new int[256];
    }

    private void ensureEntityDeclCapacity(int chunk) {
        if (chunk >= this.fEntityName.length) {
            this.fEntityName = resize(this.fEntityName, this.fEntityName.length * 2);
            this.fEntityValue = resize(this.fEntityValue, this.fEntityValue.length * 2);
            this.fEntityPublicId = resize(this.fEntityPublicId, this.fEntityPublicId.length * 2);
            this.fEntitySystemId = resize(this.fEntitySystemId, this.fEntitySystemId.length * 2);
            this.fEntityBaseSystemId = resize(this.fEntityBaseSystemId, this.fEntityBaseSystemId.length * 2);
            this.fEntityNotation = resize(this.fEntityNotation, this.fEntityNotation.length * 2);
            this.fEntityIsPE = resize(this.fEntityIsPE, this.fEntityIsPE.length * 2);
            this.fEntityInExternal = resize(this.fEntityInExternal, this.fEntityInExternal.length * 2);
        } else if (this.fEntityName[chunk] != null) {
            return;
        }
        this.fEntityName[chunk] = new String[256];
        this.fEntityValue[chunk] = new String[256];
        this.fEntityPublicId[chunk] = new String[256];
        this.fEntitySystemId[chunk] = new String[256];
        this.fEntityBaseSystemId[chunk] = new String[256];
        this.fEntityNotation[chunk] = new String[256];
        this.fEntityIsPE[chunk] = new byte[256];
        this.fEntityInExternal[chunk] = new byte[256];
    }

    private void ensureNotationDeclCapacity(int chunk) {
        if (chunk >= this.fNotationName.length) {
            this.fNotationName = resize(this.fNotationName, this.fNotationName.length * 2);
            this.fNotationPublicId = resize(this.fNotationPublicId, this.fNotationPublicId.length * 2);
            this.fNotationSystemId = resize(this.fNotationSystemId, this.fNotationSystemId.length * 2);
            this.fNotationBaseSystemId = resize(this.fNotationBaseSystemId, this.fNotationBaseSystemId.length * 2);
        } else if (this.fNotationName[chunk] != null) {
            return;
        }
        this.fNotationName[chunk] = new String[256];
        this.fNotationPublicId[chunk] = new String[256];
        this.fNotationSystemId[chunk] = new String[256];
        this.fNotationBaseSystemId[chunk] = new String[256];
    }

    private void ensureContentSpecCapacity(int chunk) {
        if (chunk >= this.fContentSpecType.length) {
            this.fContentSpecType = resize(this.fContentSpecType, this.fContentSpecType.length * 2);
            this.fContentSpecValue = resize(this.fContentSpecValue, this.fContentSpecValue.length * 2);
            this.fContentSpecOtherValue = resize(this.fContentSpecOtherValue, this.fContentSpecOtherValue.length * 2);
        } else if (this.fContentSpecType[chunk] != null) {
            return;
        }
        this.fContentSpecType[chunk] = new short[256];
        this.fContentSpecValue[chunk] = new Object[256];
        this.fContentSpecOtherValue[chunk] = new Object[256];
    }

    private static byte[][] resize(byte[][] array, int newsize) {
        byte[][] newarray = new byte[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static short[][] resize(short[][] array, int newsize) {
        short[][] newarray = new short[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static int[][] resize(int[][] array, int newsize) {
        int[][] newarray = new int[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static DatatypeValidator[][] resize(DatatypeValidator[][] array, int newsize) {
        DatatypeValidator[][] newarray = new DatatypeValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static ContentModelValidator[][] resize(ContentModelValidator[][] array, int newsize) {
        ContentModelValidator[][] newarray = new ContentModelValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static Object[][] resize(Object[][] array, int newsize) {
        Object[][] newarray = new Object[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static QName[][] resize(QName[][] array, int newsize) {
        QName[][] newarray = new QName[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static String[][] resize(String[][] array, int newsize) {
        String[][] newarray = new String[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static String[][][] resize(String[][][] array, int newsize) {
        String[][][] newarray = new String[newsize][][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    protected static final class QNameHashtable {
        private static final int HASHTABLE_SIZE = 101;
        private static final int INITIAL_BUCKET_SIZE = 4;
        private Object[][] fHashTable = new Object[HASHTABLE_SIZE][];

        protected QNameHashtable() {
        }

        public void put(String key, int value) {
            int hash = (key.hashCode() & ASContentModel.AS_UNBOUNDED) % HASHTABLE_SIZE;
            Object[] bucket = this.fHashTable[hash];
            if (bucket == null) {
                Object[] bucket2 = new Object[9];
                bucket2[0] = new int[]{1};
                bucket2[1] = key;
                bucket2[2] = new int[]{value};
                this.fHashTable[hash] = bucket2;
                return;
            }
            int count = ((int[]) bucket[0])[0];
            int offset = (2 * count) + 1;
            if (offset == bucket.length) {
                int newSize = count + 4;
                Object[] newBucket = new Object[(2 * newSize) + 1];
                System.arraycopy(bucket, 0, newBucket, 0, offset);
                bucket = newBucket;
                this.fHashTable[hash] = bucket;
            }
            boolean found = false;
            int j = 1;
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                if (((String) bucket[j]) == key) {
                    ((int[]) bucket[j + 1])[0] = value;
                    found = true;
                    break;
                } else {
                    j += 2;
                    i++;
                }
            }
            if (!found) {
                bucket[offset] = key;
                bucket[offset + 1] = new int[]{value};
                ((int[]) bucket[0])[0] = count + 1;
            }
        }

        public int get(String key) {
            int hash = (key.hashCode() & ASContentModel.AS_UNBOUNDED) % HASHTABLE_SIZE;
            Object[] bucket = this.fHashTable[hash];
            if (bucket == null) {
                return -1;
            }
            int count = ((int[]) bucket[0])[0];
            int j = 1;
            for (int i = 0; i < count; i++) {
                if (((String) bucket[j]) == key) {
                    return ((int[]) bucket[j + 1])[0];
                }
                j += 2;
            }
            return -1;
        }
    }

    @Override
    public boolean isEntityDeclared(String name) {
        return getEntityDeclIndex(name) != -1;
    }

    @Override
    public boolean isEntityUnparsed(String name) {
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex <= -1) {
            return false;
        }
        int chunk = entityIndex >> 8;
        int index = entityIndex & CHUNK_MASK;
        return this.fEntityNotation[chunk][index] != null;
    }
}
