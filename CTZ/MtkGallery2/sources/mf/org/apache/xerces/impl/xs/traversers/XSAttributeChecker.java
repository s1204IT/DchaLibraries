package mf.org.apache.xerces.impl.xs.traversers;

import java.lang.reflect.Array;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaNamespaceSupport;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAttributeDecl;
import mf.org.apache.xerces.impl.xs.XSGrammarBucket;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XIntPool;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Element;

public class XSAttributeChecker {
    public static final int ATTIDX_ABSTRACT;
    public static final int ATTIDX_AFORMDEFAULT;
    public static final int ATTIDX_BASE;
    public static final int ATTIDX_BLOCK;
    public static final int ATTIDX_BLOCKDEFAULT;
    private static int ATTIDX_COUNT = 0;
    public static final int ATTIDX_DEFAULT;
    public static final int ATTIDX_EFORMDEFAULT;
    public static final int ATTIDX_ENUMNSDECLS;
    public static final int ATTIDX_FINAL;
    public static final int ATTIDX_FINALDEFAULT;
    public static final int ATTIDX_FIXED;
    public static final int ATTIDX_FORM;
    public static final int ATTIDX_FROMDEFAULT;
    public static final int ATTIDX_ID;
    public static final int ATTIDX_ISRETURNED;
    public static final int ATTIDX_ITEMTYPE;
    public static final int ATTIDX_MAXOCCURS;
    public static final int ATTIDX_MEMBERTYPES;
    public static final int ATTIDX_MINOCCURS;
    public static final int ATTIDX_MIXED;
    public static final int ATTIDX_NAME;
    public static final int ATTIDX_NAMESPACE;
    public static final int ATTIDX_NAMESPACE_LIST;
    public static final int ATTIDX_NILLABLE;
    public static final int ATTIDX_NONSCHEMA;
    public static final int ATTIDX_PROCESSCONTENTS;
    public static final int ATTIDX_PUBLIC;
    public static final int ATTIDX_REF;
    public static final int ATTIDX_REFER;
    public static final int ATTIDX_SCHEMALOCATION;
    public static final int ATTIDX_SOURCE;
    public static final int ATTIDX_SUBSGROUP;
    public static final int ATTIDX_SYSTEM;
    public static final int ATTIDX_TARGETNAMESPACE;
    public static final int ATTIDX_TYPE;
    public static final int ATTIDX_USE;
    public static final int ATTIDX_VALUE;
    public static final int ATTIDX_VERSION;
    public static final int ATTIDX_XML_LANG;
    public static final int ATTIDX_XPATH;
    private static final String ATTRIBUTE_N = "attribute_n";
    private static final String ATTRIBUTE_R = "attribute_r";
    protected static final int DT_ANYURI = 0;
    protected static final int DT_BLOCK = -1;
    protected static final int DT_BLOCK1 = -2;
    protected static final int DT_BOOLEAN = -15;
    protected static final int DT_COUNT = 9;
    protected static final int DT_FINAL = -3;
    protected static final int DT_FINAL1 = -4;
    protected static final int DT_FINAL2 = -5;
    protected static final int DT_FORM = -6;
    protected static final int DT_ID = 1;
    protected static final int DT_LANGUAGE = 8;
    protected static final int DT_MAXOCCURS = -7;
    protected static final int DT_MAXOCCURS1 = -8;
    protected static final int DT_MEMBERTYPES = -9;
    protected static final int DT_MINOCCURS1 = -10;
    protected static final int DT_NAMESPACE = -11;
    protected static final int DT_NCNAME = 5;
    protected static final int DT_NONNEGINT = -16;
    protected static final int DT_POSINT = -17;
    protected static final int DT_PROCESSCONTENTS = -12;
    protected static final int DT_QNAME = 2;
    protected static final int DT_STRING = 3;
    protected static final int DT_TOKEN = 4;
    protected static final int DT_USE = -13;
    protected static final int DT_WHITESPACE = -14;
    protected static final int DT_XPATH = 6;
    protected static final int DT_XPATH1 = 7;
    private static final String ELEMENT_N = "element_n";
    private static final String ELEMENT_R = "element_r";
    static final int INC_POOL_SIZE = 10;
    static final int INIT_POOL_SIZE = 10;
    private static final XInt INT_ANY_ANY;
    private static final XInt INT_ANY_LAX;
    private static final XInt INT_ANY_LIST;
    private static final XInt INT_ANY_NOT;
    private static final XInt INT_ANY_SKIP;
    private static final XInt INT_ANY_STRICT;
    private static final XInt INT_EMPTY_SET;
    private static final XInt INT_QUALIFIED;
    private static final XInt INT_UNBOUNDED;
    private static final XInt INT_UNQUALIFIED;
    private static final XInt INT_USE_OPTIONAL;
    private static final XInt INT_USE_PROHIBITED;
    private static final XInt INT_USE_REQUIRED;
    private static final XInt INT_WS_COLLAPSE;
    private static final XInt INT_WS_PRESERVE;
    private static final XInt INT_WS_REPLACE;
    private static final Hashtable fEleAttrsMapG;
    private static final Hashtable fEleAttrsMapL;
    private static final XSSimpleType[] fExtraDVs;
    private static boolean[] fSeenTemp;
    private static Object[] fTempArray;
    private static final XIntPool fXIntPool;
    protected XSDHandler fSchemaHandler;
    protected SymbolTable fSymbolTable = null;
    protected Hashtable fNonSchemaAttrs = new Hashtable();
    protected Vector fNamespaceList = new Vector();
    protected boolean[] fSeen = new boolean[ATTIDX_COUNT];
    Object[][] fArrayPool = (Object[][]) Array.newInstance((Class<?>) Object.class, 10, ATTIDX_COUNT);
    int fPoolPos = 0;

    static {
        ATTIDX_COUNT = 0;
        int i = ATTIDX_COUNT;
        ATTIDX_COUNT = i + 1;
        ATTIDX_ABSTRACT = i;
        int i2 = ATTIDX_COUNT;
        ATTIDX_COUNT = i2 + 1;
        ATTIDX_AFORMDEFAULT = i2;
        int i3 = ATTIDX_COUNT;
        ATTIDX_COUNT = i3 + 1;
        ATTIDX_BASE = i3;
        int i4 = ATTIDX_COUNT;
        ATTIDX_COUNT = i4 + 1;
        ATTIDX_BLOCK = i4;
        int i5 = ATTIDX_COUNT;
        ATTIDX_COUNT = i5 + 1;
        ATTIDX_BLOCKDEFAULT = i5;
        int i6 = ATTIDX_COUNT;
        ATTIDX_COUNT = i6 + 1;
        ATTIDX_DEFAULT = i6;
        int i7 = ATTIDX_COUNT;
        ATTIDX_COUNT = i7 + 1;
        ATTIDX_EFORMDEFAULT = i7;
        int i8 = ATTIDX_COUNT;
        ATTIDX_COUNT = i8 + 1;
        ATTIDX_FINAL = i8;
        int i9 = ATTIDX_COUNT;
        ATTIDX_COUNT = i9 + 1;
        ATTIDX_FINALDEFAULT = i9;
        int i10 = ATTIDX_COUNT;
        ATTIDX_COUNT = i10 + 1;
        ATTIDX_FIXED = i10;
        int i11 = ATTIDX_COUNT;
        ATTIDX_COUNT = i11 + 1;
        ATTIDX_FORM = i11;
        int i12 = ATTIDX_COUNT;
        ATTIDX_COUNT = i12 + 1;
        ATTIDX_ID = i12;
        int i13 = ATTIDX_COUNT;
        ATTIDX_COUNT = i13 + 1;
        ATTIDX_ITEMTYPE = i13;
        int i14 = ATTIDX_COUNT;
        ATTIDX_COUNT = i14 + 1;
        ATTIDX_MAXOCCURS = i14;
        int i15 = ATTIDX_COUNT;
        ATTIDX_COUNT = i15 + 1;
        ATTIDX_MEMBERTYPES = i15;
        int i16 = ATTIDX_COUNT;
        ATTIDX_COUNT = i16 + 1;
        ATTIDX_MINOCCURS = i16;
        int i17 = ATTIDX_COUNT;
        ATTIDX_COUNT = i17 + 1;
        ATTIDX_MIXED = i17;
        int i18 = ATTIDX_COUNT;
        ATTIDX_COUNT = i18 + 1;
        ATTIDX_NAME = i18;
        int i19 = ATTIDX_COUNT;
        ATTIDX_COUNT = i19 + 1;
        ATTIDX_NAMESPACE = i19;
        int i20 = ATTIDX_COUNT;
        ATTIDX_COUNT = i20 + 1;
        ATTIDX_NAMESPACE_LIST = i20;
        int i21 = ATTIDX_COUNT;
        ATTIDX_COUNT = i21 + 1;
        ATTIDX_NILLABLE = i21;
        int i22 = ATTIDX_COUNT;
        ATTIDX_COUNT = i22 + 1;
        ATTIDX_NONSCHEMA = i22;
        int i23 = ATTIDX_COUNT;
        ATTIDX_COUNT = i23 + 1;
        ATTIDX_PROCESSCONTENTS = i23;
        int i24 = ATTIDX_COUNT;
        ATTIDX_COUNT = i24 + 1;
        ATTIDX_PUBLIC = i24;
        int i25 = ATTIDX_COUNT;
        ATTIDX_COUNT = i25 + 1;
        ATTIDX_REF = i25;
        int i26 = ATTIDX_COUNT;
        ATTIDX_COUNT = i26 + 1;
        ATTIDX_REFER = i26;
        int i27 = ATTIDX_COUNT;
        ATTIDX_COUNT = i27 + 1;
        ATTIDX_SCHEMALOCATION = i27;
        int i28 = ATTIDX_COUNT;
        ATTIDX_COUNT = i28 + 1;
        ATTIDX_SOURCE = i28;
        int i29 = ATTIDX_COUNT;
        ATTIDX_COUNT = i29 + 1;
        ATTIDX_SUBSGROUP = i29;
        int i30 = ATTIDX_COUNT;
        ATTIDX_COUNT = i30 + 1;
        ATTIDX_SYSTEM = i30;
        int i31 = ATTIDX_COUNT;
        ATTIDX_COUNT = i31 + 1;
        ATTIDX_TARGETNAMESPACE = i31;
        int i32 = ATTIDX_COUNT;
        ATTIDX_COUNT = i32 + 1;
        ATTIDX_TYPE = i32;
        int i33 = ATTIDX_COUNT;
        ATTIDX_COUNT = i33 + 1;
        ATTIDX_USE = i33;
        int i34 = ATTIDX_COUNT;
        ATTIDX_COUNT = i34 + 1;
        ATTIDX_VALUE = i34;
        int i35 = ATTIDX_COUNT;
        ATTIDX_COUNT = i35 + 1;
        ATTIDX_ENUMNSDECLS = i35;
        int i36 = ATTIDX_COUNT;
        ATTIDX_COUNT = i36 + 1;
        ATTIDX_VERSION = i36;
        int i37 = ATTIDX_COUNT;
        ATTIDX_COUNT = i37 + 1;
        ATTIDX_XML_LANG = i37;
        int i38 = ATTIDX_COUNT;
        ATTIDX_COUNT = i38 + 1;
        ATTIDX_XPATH = i38;
        int i39 = ATTIDX_COUNT;
        ATTIDX_COUNT = i39 + 1;
        ATTIDX_FROMDEFAULT = i39;
        int i40 = ATTIDX_COUNT;
        ATTIDX_COUNT = i40 + 1;
        ATTIDX_ISRETURNED = i40;
        fXIntPool = new XIntPool();
        INT_QUALIFIED = fXIntPool.getXInt(1);
        INT_UNQUALIFIED = fXIntPool.getXInt(0);
        INT_EMPTY_SET = fXIntPool.getXInt(0);
        INT_ANY_STRICT = fXIntPool.getXInt(1);
        INT_ANY_LAX = fXIntPool.getXInt(3);
        INT_ANY_SKIP = fXIntPool.getXInt(2);
        INT_ANY_ANY = fXIntPool.getXInt(1);
        INT_ANY_LIST = fXIntPool.getXInt(3);
        INT_ANY_NOT = fXIntPool.getXInt(2);
        INT_USE_OPTIONAL = fXIntPool.getXInt(0);
        INT_USE_REQUIRED = fXIntPool.getXInt(1);
        INT_USE_PROHIBITED = fXIntPool.getXInt(2);
        INT_WS_PRESERVE = fXIntPool.getXInt(0);
        INT_WS_REPLACE = fXIntPool.getXInt(1);
        INT_WS_COLLAPSE = fXIntPool.getXInt(2);
        INT_UNBOUNDED = fXIntPool.getXInt(-1);
        fEleAttrsMapG = new Hashtable(29);
        fEleAttrsMapL = new Hashtable(79);
        fExtraDVs = new XSSimpleType[9];
        SchemaGrammar grammar = SchemaGrammar.SG_SchemaNS;
        fExtraDVs[0] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_ANYURI);
        fExtraDVs[1] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_ID);
        fExtraDVs[2] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_QNAME);
        fExtraDVs[3] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_STRING);
        fExtraDVs[4] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_TOKEN);
        fExtraDVs[5] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_NCNAME);
        fExtraDVs[6] = fExtraDVs[3];
        fExtraDVs[6] = fExtraDVs[3];
        fExtraDVs[8] = (XSSimpleType) grammar.getGlobalTypeDecl(SchemaSymbols.ATTVAL_LANGUAGE);
        int attCount = 0 + 1;
        int attCount2 = attCount + 1;
        int attCount3 = attCount2 + 1;
        int attCount4 = attCount3 + 1;
        int attCount5 = attCount4 + 1;
        int attCount6 = attCount5 + 1;
        int attCount7 = attCount6 + 1;
        int attCount8 = attCount7 + 1;
        int attCount9 = attCount8 + 1;
        int attCount10 = attCount9 + 1;
        int attCount11 = attCount10 + 1;
        int attCount12 = attCount11 + 1;
        int attCount13 = attCount12 + 1;
        int attCount14 = attCount13 + 1;
        int attCount15 = attCount14 + 1;
        int attCount16 = attCount15 + 1;
        int attCount17 = attCount16 + 1;
        int attCount18 = attCount17 + 1;
        int attCount19 = attCount18 + 1;
        int attCount20 = attCount19 + 1;
        int attCount21 = attCount20 + 1;
        int attCount22 = attCount21 + 1;
        int attCount23 = attCount22 + 1;
        int attCount24 = attCount23 + 1;
        int attCount25 = attCount24 + 1;
        int attCount26 = attCount25 + 1;
        int attCount27 = attCount26 + 1;
        int attCount28 = attCount27 + 1;
        int attCount29 = attCount28 + 1;
        int attCount30 = attCount29 + 1;
        int attCount31 = attCount30 + 1;
        int attCount32 = attCount31 + 1;
        int attCount33 = attCount32 + 1;
        int attCount34 = attCount33 + 1;
        int attCount35 = attCount34 + 1;
        int attCount36 = attCount35 + 1;
        int attCount37 = attCount36 + 1;
        int attCount38 = attCount37 + 1;
        int attCount39 = attCount38 + 1;
        int attCount40 = attCount39 + 1;
        int attCount41 = attCount40 + 1;
        int attCount42 = attCount41 + 1;
        int attCount43 = attCount42 + 1;
        int attCount44 = attCount43 + 1;
        int attCount45 = attCount44 + 1;
        int attCount46 = attCount45 + 1;
        int attCount47 = attCount46 + 1;
        OneAttr[] allAttrs = new OneAttr[attCount47 + 1];
        allAttrs[0] = new OneAttr(SchemaSymbols.ATT_ABSTRACT, DT_BOOLEAN, ATTIDX_ABSTRACT, Boolean.FALSE);
        allAttrs[attCount] = new OneAttr(SchemaSymbols.ATT_ATTRIBUTEFORMDEFAULT, DT_FORM, ATTIDX_AFORMDEFAULT, INT_UNQUALIFIED);
        allAttrs[attCount2] = new OneAttr(SchemaSymbols.ATT_BASE, 2, ATTIDX_BASE, null);
        allAttrs[attCount3] = new OneAttr(SchemaSymbols.ATT_BASE, 2, ATTIDX_BASE, null);
        allAttrs[attCount4] = new OneAttr(SchemaSymbols.ATT_BLOCK, -1, ATTIDX_BLOCK, null);
        allAttrs[attCount5] = new OneAttr(SchemaSymbols.ATT_BLOCK, -2, ATTIDX_BLOCK, null);
        allAttrs[attCount6] = new OneAttr(SchemaSymbols.ATT_BLOCKDEFAULT, -1, ATTIDX_BLOCKDEFAULT, INT_EMPTY_SET);
        allAttrs[attCount7] = new OneAttr(SchemaSymbols.ATT_DEFAULT, 3, ATTIDX_DEFAULT, null);
        allAttrs[attCount8] = new OneAttr(SchemaSymbols.ATT_ELEMENTFORMDEFAULT, DT_FORM, ATTIDX_EFORMDEFAULT, INT_UNQUALIFIED);
        allAttrs[attCount9] = new OneAttr(SchemaSymbols.ATT_FINAL, -3, ATTIDX_FINAL, null);
        allAttrs[attCount10] = new OneAttr(SchemaSymbols.ATT_FINAL, DT_FINAL1, ATTIDX_FINAL, null);
        allAttrs[attCount11] = new OneAttr(SchemaSymbols.ATT_FINALDEFAULT, DT_FINAL2, ATTIDX_FINALDEFAULT, INT_EMPTY_SET);
        allAttrs[attCount12] = new OneAttr(SchemaSymbols.ATT_FIXED, 3, ATTIDX_FIXED, null);
        allAttrs[attCount13] = new OneAttr(SchemaSymbols.ATT_FIXED, DT_BOOLEAN, ATTIDX_FIXED, Boolean.FALSE);
        allAttrs[attCount14] = new OneAttr(SchemaSymbols.ATT_FORM, DT_FORM, ATTIDX_FORM, null);
        allAttrs[attCount15] = new OneAttr(SchemaSymbols.ATT_ID, 1, ATTIDX_ID, null);
        allAttrs[attCount16] = new OneAttr(SchemaSymbols.ATT_ITEMTYPE, 2, ATTIDX_ITEMTYPE, null);
        allAttrs[attCount17] = new OneAttr(SchemaSymbols.ATT_MAXOCCURS, DT_MAXOCCURS, ATTIDX_MAXOCCURS, fXIntPool.getXInt(1));
        allAttrs[attCount18] = new OneAttr(SchemaSymbols.ATT_MAXOCCURS, DT_MAXOCCURS1, ATTIDX_MAXOCCURS, fXIntPool.getXInt(1));
        allAttrs[attCount19] = new OneAttr(SchemaSymbols.ATT_MEMBERTYPES, DT_MEMBERTYPES, ATTIDX_MEMBERTYPES, null);
        allAttrs[attCount20] = new OneAttr(SchemaSymbols.ATT_MINOCCURS, DT_NONNEGINT, ATTIDX_MINOCCURS, fXIntPool.getXInt(1));
        allAttrs[attCount21] = new OneAttr(SchemaSymbols.ATT_MINOCCURS, DT_MINOCCURS1, ATTIDX_MINOCCURS, fXIntPool.getXInt(1));
        allAttrs[attCount22] = new OneAttr(SchemaSymbols.ATT_MIXED, DT_BOOLEAN, ATTIDX_MIXED, Boolean.FALSE);
        allAttrs[attCount23] = new OneAttr(SchemaSymbols.ATT_MIXED, DT_BOOLEAN, ATTIDX_MIXED, null);
        allAttrs[attCount24] = new OneAttr(SchemaSymbols.ATT_NAME, 5, ATTIDX_NAME, null);
        allAttrs[attCount25] = new OneAttr(SchemaSymbols.ATT_NAMESPACE, DT_NAMESPACE, ATTIDX_NAMESPACE, INT_ANY_ANY);
        allAttrs[attCount26] = new OneAttr(SchemaSymbols.ATT_NAMESPACE, 0, ATTIDX_NAMESPACE, null);
        allAttrs[attCount27] = new OneAttr(SchemaSymbols.ATT_NILLABLE, DT_BOOLEAN, ATTIDX_NILLABLE, Boolean.FALSE);
        allAttrs[attCount28] = new OneAttr(SchemaSymbols.ATT_PROCESSCONTENTS, DT_PROCESSCONTENTS, ATTIDX_PROCESSCONTENTS, INT_ANY_STRICT);
        allAttrs[attCount29] = new OneAttr(SchemaSymbols.ATT_PUBLIC, 4, ATTIDX_PUBLIC, null);
        allAttrs[attCount30] = new OneAttr(SchemaSymbols.ATT_REF, 2, ATTIDX_REF, null);
        allAttrs[attCount31] = new OneAttr(SchemaSymbols.ATT_REFER, 2, ATTIDX_REFER, null);
        allAttrs[attCount32] = new OneAttr(SchemaSymbols.ATT_SCHEMALOCATION, 0, ATTIDX_SCHEMALOCATION, null);
        allAttrs[attCount33] = new OneAttr(SchemaSymbols.ATT_SCHEMALOCATION, 0, ATTIDX_SCHEMALOCATION, null);
        allAttrs[attCount34] = new OneAttr(SchemaSymbols.ATT_SOURCE, 0, ATTIDX_SOURCE, null);
        allAttrs[attCount35] = new OneAttr(SchemaSymbols.ATT_SUBSTITUTIONGROUP, 2, ATTIDX_SUBSGROUP, null);
        allAttrs[attCount36] = new OneAttr(SchemaSymbols.ATT_SYSTEM, 0, ATTIDX_SYSTEM, null);
        allAttrs[attCount37] = new OneAttr(SchemaSymbols.ATT_TARGETNAMESPACE, 0, ATTIDX_TARGETNAMESPACE, null);
        allAttrs[attCount38] = new OneAttr(SchemaSymbols.ATT_TYPE, 2, ATTIDX_TYPE, null);
        allAttrs[attCount39] = new OneAttr(SchemaSymbols.ATT_USE, DT_USE, ATTIDX_USE, INT_USE_OPTIONAL);
        allAttrs[attCount40] = new OneAttr(SchemaSymbols.ATT_VALUE, DT_NONNEGINT, ATTIDX_VALUE, null);
        allAttrs[attCount41] = new OneAttr(SchemaSymbols.ATT_VALUE, DT_POSINT, ATTIDX_VALUE, null);
        allAttrs[attCount42] = new OneAttr(SchemaSymbols.ATT_VALUE, 3, ATTIDX_VALUE, null);
        allAttrs[attCount43] = new OneAttr(SchemaSymbols.ATT_VALUE, DT_WHITESPACE, ATTIDX_VALUE, null);
        allAttrs[attCount44] = new OneAttr(SchemaSymbols.ATT_VERSION, 4, ATTIDX_VERSION, null);
        allAttrs[attCount45] = new OneAttr(SchemaSymbols.ATT_XML_LANG, 8, ATTIDX_XML_LANG, null);
        allAttrs[attCount46] = new OneAttr(SchemaSymbols.ATT_XPATH, 6, ATTIDX_XPATH, null);
        allAttrs[attCount47] = new OneAttr(SchemaSymbols.ATT_XPATH, 7, ATTIDX_XPATH, null);
        Container attrList = Container.getContainer(5);
        attrList.put(SchemaSymbols.ATT_DEFAULT, allAttrs[attCount7]);
        attrList.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount12]);
        attrList.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        attrList.put(SchemaSymbols.ATT_TYPE, allAttrs[attCount38]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ATTRIBUTE, attrList);
        Container attrList2 = Container.getContainer(7);
        attrList2.put(SchemaSymbols.ATT_DEFAULT, allAttrs[attCount7]);
        attrList2.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount12]);
        attrList2.put(SchemaSymbols.ATT_FORM, allAttrs[attCount14]);
        attrList2.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList2.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        attrList2.put(SchemaSymbols.ATT_TYPE, allAttrs[attCount38]);
        attrList2.put(SchemaSymbols.ATT_USE, allAttrs[attCount39]);
        fEleAttrsMapL.put(ATTRIBUTE_N, attrList2);
        Container attrList3 = Container.getContainer(5);
        attrList3.put(SchemaSymbols.ATT_DEFAULT, allAttrs[attCount7]);
        attrList3.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount12]);
        attrList3.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList3.put(SchemaSymbols.ATT_REF, allAttrs[attCount30]);
        attrList3.put(SchemaSymbols.ATT_USE, allAttrs[attCount39]);
        fEleAttrsMapL.put(ATTRIBUTE_R, attrList3);
        Container attrList4 = Container.getContainer(10);
        attrList4.put(SchemaSymbols.ATT_ABSTRACT, allAttrs[0]);
        attrList4.put(SchemaSymbols.ATT_BLOCK, allAttrs[attCount4]);
        attrList4.put(SchemaSymbols.ATT_DEFAULT, allAttrs[attCount7]);
        attrList4.put(SchemaSymbols.ATT_FINAL, allAttrs[attCount9]);
        attrList4.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount12]);
        attrList4.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList4.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        attrList4.put(SchemaSymbols.ATT_NILLABLE, allAttrs[attCount27]);
        attrList4.put(SchemaSymbols.ATT_SUBSTITUTIONGROUP, allAttrs[attCount35]);
        attrList4.put(SchemaSymbols.ATT_TYPE, allAttrs[attCount38]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ELEMENT, attrList4);
        Container attrList5 = Container.getContainer(10);
        attrList5.put(SchemaSymbols.ATT_BLOCK, allAttrs[attCount4]);
        attrList5.put(SchemaSymbols.ATT_DEFAULT, allAttrs[attCount7]);
        attrList5.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount12]);
        attrList5.put(SchemaSymbols.ATT_FORM, allAttrs[attCount14]);
        attrList5.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList5.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[attCount17]);
        attrList5.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[attCount20]);
        attrList5.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        attrList5.put(SchemaSymbols.ATT_NILLABLE, allAttrs[attCount27]);
        attrList5.put(SchemaSymbols.ATT_TYPE, allAttrs[attCount38]);
        fEleAttrsMapL.put(ELEMENT_N, attrList5);
        Container attrList6 = Container.getContainer(4);
        attrList6.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList6.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[attCount17]);
        attrList6.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[attCount20]);
        attrList6.put(SchemaSymbols.ATT_REF, allAttrs[attCount30]);
        fEleAttrsMapL.put(ELEMENT_R, attrList6);
        Container attrList7 = Container.getContainer(6);
        attrList7.put(SchemaSymbols.ATT_ABSTRACT, allAttrs[0]);
        attrList7.put(SchemaSymbols.ATT_BLOCK, allAttrs[attCount5]);
        attrList7.put(SchemaSymbols.ATT_FINAL, allAttrs[attCount9]);
        attrList7.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList7.put(SchemaSymbols.ATT_MIXED, allAttrs[attCount22]);
        attrList7.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_COMPLEXTYPE, attrList7);
        Container attrList8 = Container.getContainer(4);
        attrList8.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList8.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        attrList8.put(SchemaSymbols.ATT_PUBLIC, allAttrs[attCount29]);
        attrList8.put(SchemaSymbols.ATT_SYSTEM, allAttrs[attCount36]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_NOTATION, attrList8);
        Container attrList9 = Container.getContainer(2);
        attrList9.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList9.put(SchemaSymbols.ATT_MIXED, allAttrs[attCount22]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_COMPLEXTYPE, attrList9);
        Container attrList10 = Container.getContainer(1);
        attrList10.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SIMPLECONTENT, attrList10);
        Container attrList11 = Container.getContainer(2);
        attrList11.put(SchemaSymbols.ATT_BASE, allAttrs[attCount3]);
        attrList11.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_RESTRICTION, attrList11);
        Container attrList12 = Container.getContainer(2);
        attrList12.put(SchemaSymbols.ATT_BASE, allAttrs[attCount2]);
        attrList12.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_EXTENSION, attrList12);
        Container attrList13 = Container.getContainer(2);
        attrList13.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList13.put(SchemaSymbols.ATT_REF, allAttrs[attCount30]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ATTRIBUTEGROUP, attrList13);
        Container attrList14 = Container.getContainer(3);
        attrList14.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList14.put(SchemaSymbols.ATT_NAMESPACE, allAttrs[attCount25]);
        attrList14.put(SchemaSymbols.ATT_PROCESSCONTENTS, allAttrs[attCount28]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ANYATTRIBUTE, attrList14);
        Container attrList15 = Container.getContainer(2);
        attrList15.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList15.put(SchemaSymbols.ATT_MIXED, allAttrs[attCount23]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_COMPLEXCONTENT, attrList15);
        Container attrList16 = Container.getContainer(2);
        attrList16.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList16.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ATTRIBUTEGROUP, attrList16);
        Container attrList17 = Container.getContainer(2);
        attrList17.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList17.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_GROUP, attrList17);
        Container attrList18 = Container.getContainer(4);
        attrList18.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList18.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[attCount17]);
        attrList18.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[attCount20]);
        attrList18.put(SchemaSymbols.ATT_REF, allAttrs[attCount30]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_GROUP, attrList18);
        Container attrList19 = Container.getContainer(3);
        attrList19.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList19.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[attCount18]);
        attrList19.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[attCount21]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ALL, attrList19);
        Container attrList20 = Container.getContainer(3);
        attrList20.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList20.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[attCount17]);
        attrList20.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[attCount20]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_CHOICE, attrList20);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SEQUENCE, attrList20);
        Container attrList21 = Container.getContainer(5);
        attrList21.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList21.put(SchemaSymbols.ATT_MAXOCCURS, allAttrs[attCount17]);
        attrList21.put(SchemaSymbols.ATT_MINOCCURS, allAttrs[attCount20]);
        attrList21.put(SchemaSymbols.ATT_NAMESPACE, allAttrs[attCount25]);
        attrList21.put(SchemaSymbols.ATT_PROCESSCONTENTS, allAttrs[attCount28]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ANY, attrList21);
        Container attrList22 = Container.getContainer(2);
        attrList22.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList22.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_UNIQUE, attrList22);
        fEleAttrsMapL.put(SchemaSymbols.ELT_KEY, attrList22);
        Container attrList23 = Container.getContainer(3);
        attrList23.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList23.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        attrList23.put(SchemaSymbols.ATT_REFER, allAttrs[attCount31]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_KEYREF, attrList23);
        Container attrList24 = Container.getContainer(2);
        attrList24.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList24.put(SchemaSymbols.ATT_XPATH, allAttrs[attCount46]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SELECTOR, attrList24);
        Container attrList25 = Container.getContainer(2);
        attrList25.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList25.put(SchemaSymbols.ATT_XPATH, allAttrs[attCount47]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_FIELD, attrList25);
        Container attrList26 = Container.getContainer(1);
        attrList26.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_ANNOTATION, attrList26);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ANNOTATION, attrList26);
        Container attrList27 = Container.getContainer(1);
        attrList27.put(SchemaSymbols.ATT_SOURCE, allAttrs[attCount34]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_APPINFO, attrList27);
        fEleAttrsMapL.put(SchemaSymbols.ELT_APPINFO, attrList27);
        Container attrList28 = Container.getContainer(2);
        attrList28.put(SchemaSymbols.ATT_SOURCE, allAttrs[attCount34]);
        attrList28.put(SchemaSymbols.ATT_XML_LANG, allAttrs[attCount45]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_DOCUMENTATION, attrList28);
        fEleAttrsMapL.put(SchemaSymbols.ELT_DOCUMENTATION, attrList28);
        Container attrList29 = Container.getContainer(3);
        attrList29.put(SchemaSymbols.ATT_FINAL, allAttrs[attCount10]);
        attrList29.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList29.put(SchemaSymbols.ATT_NAME, allAttrs[attCount24]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_SIMPLETYPE, attrList29);
        Container attrList30 = Container.getContainer(2);
        attrList30.put(SchemaSymbols.ATT_FINAL, allAttrs[attCount10]);
        attrList30.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_SIMPLETYPE, attrList30);
        Container attrList31 = Container.getContainer(2);
        attrList31.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList31.put(SchemaSymbols.ATT_ITEMTYPE, allAttrs[attCount16]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_LIST, attrList31);
        Container attrList32 = Container.getContainer(2);
        attrList32.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList32.put(SchemaSymbols.ATT_MEMBERTYPES, allAttrs[attCount19]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_UNION, attrList32);
        Container attrList33 = Container.getContainer(8);
        attrList33.put(SchemaSymbols.ATT_ATTRIBUTEFORMDEFAULT, allAttrs[attCount]);
        attrList33.put(SchemaSymbols.ATT_BLOCKDEFAULT, allAttrs[attCount6]);
        attrList33.put(SchemaSymbols.ATT_ELEMENTFORMDEFAULT, allAttrs[attCount8]);
        attrList33.put(SchemaSymbols.ATT_FINALDEFAULT, allAttrs[attCount11]);
        attrList33.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList33.put(SchemaSymbols.ATT_TARGETNAMESPACE, allAttrs[attCount37]);
        attrList33.put(SchemaSymbols.ATT_VERSION, allAttrs[attCount44]);
        attrList33.put(SchemaSymbols.ATT_XML_LANG, allAttrs[attCount45]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_SCHEMA, attrList33);
        Container attrList34 = Container.getContainer(2);
        attrList34.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList34.put(SchemaSymbols.ATT_SCHEMALOCATION, allAttrs[attCount32]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_INCLUDE, attrList34);
        fEleAttrsMapG.put(SchemaSymbols.ELT_REDEFINE, attrList34);
        Container attrList35 = Container.getContainer(3);
        attrList35.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList35.put(SchemaSymbols.ATT_NAMESPACE, allAttrs[attCount26]);
        attrList35.put(SchemaSymbols.ATT_SCHEMALOCATION, allAttrs[attCount33]);
        fEleAttrsMapG.put(SchemaSymbols.ELT_IMPORT, attrList35);
        Container attrList36 = Container.getContainer(3);
        attrList36.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList36.put(SchemaSymbols.ATT_VALUE, allAttrs[attCount40]);
        attrList36.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount13]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_LENGTH, attrList36);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MINLENGTH, attrList36);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MAXLENGTH, attrList36);
        fEleAttrsMapL.put(SchemaSymbols.ELT_FRACTIONDIGITS, attrList36);
        Container attrList37 = Container.getContainer(3);
        attrList37.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList37.put(SchemaSymbols.ATT_VALUE, allAttrs[attCount41]);
        attrList37.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount13]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_TOTALDIGITS, attrList37);
        Container attrList38 = Container.getContainer(2);
        attrList38.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList38.put(SchemaSymbols.ATT_VALUE, allAttrs[attCount42]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_PATTERN, attrList38);
        Container attrList39 = Container.getContainer(2);
        attrList39.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList39.put(SchemaSymbols.ATT_VALUE, allAttrs[attCount42]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_ENUMERATION, attrList39);
        Container attrList40 = Container.getContainer(3);
        attrList40.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList40.put(SchemaSymbols.ATT_VALUE, allAttrs[attCount43]);
        attrList40.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount13]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_WHITESPACE, attrList40);
        Container attrList41 = Container.getContainer(3);
        attrList41.put(SchemaSymbols.ATT_ID, allAttrs[attCount15]);
        attrList41.put(SchemaSymbols.ATT_VALUE, allAttrs[attCount42]);
        attrList41.put(SchemaSymbols.ATT_FIXED, allAttrs[attCount13]);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MAXINCLUSIVE, attrList41);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MAXEXCLUSIVE, attrList41);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MININCLUSIVE, attrList41);
        fEleAttrsMapL.put(SchemaSymbols.ELT_MINEXCLUSIVE, attrList41);
        fSeenTemp = new boolean[ATTIDX_COUNT];
        fTempArray = new Object[ATTIDX_COUNT];
    }

    public XSAttributeChecker(XSDHandler schemaHandler) {
        this.fSchemaHandler = null;
        this.fSchemaHandler = schemaHandler;
    }

    public void reset(SymbolTable symbolTable) {
        this.fSymbolTable = symbolTable;
        this.fNonSchemaAttrs.clear();
    }

    public Object[] checkAttributes(Element element, boolean isGlobal, XSDocumentInfo schemaDoc) {
        return checkAttributes(element, isGlobal, schemaDoc, false);
    }

    public Object[] checkAttributes(Element element, boolean isGlobal, XSDocumentInfo schemaDoc, boolean enumAsQName) {
        OneAttr[] reqAttrs;
        String attrURI;
        int i;
        int length;
        Container attrList;
        String lookupName;
        Attr sattr;
        Attr[] attrs;
        String attrPrefix;
        Hashtable eleAttrsMap;
        boolean z;
        OneAttr oneAttr;
        OneAttr oneAttr2;
        OneAttr oneAttr3;
        int i2;
        OneAttr oneAttr4;
        int i3;
        int length2;
        if (element == null) {
            return null;
        }
        Attr[] attrs2 = DOMUtil.getAttrs(element);
        resolveNamespace(element, attrs2, schemaDoc.fNamespaceSupport);
        String uri = DOMUtil.getNamespaceURI(element);
        String elName = DOMUtil.getLocalName(element);
        if (!SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(uri)) {
            reportSchemaError("s4s-elt-schema-ns", new Object[]{elName}, element);
        }
        Hashtable eleAttrsMap2 = fEleAttrsMapG;
        String lookupName2 = elName;
        if (!isGlobal) {
            eleAttrsMap2 = fEleAttrsMapL;
            if (elName.equals(SchemaSymbols.ELT_ELEMENT)) {
                lookupName2 = DOMUtil.getAttr(element, SchemaSymbols.ATT_REF) != null ? ELEMENT_R : ELEMENT_N;
            } else if (elName.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                lookupName2 = DOMUtil.getAttr(element, SchemaSymbols.ATT_REF) != null ? ATTRIBUTE_R : ATTRIBUTE_N;
            }
        }
        Hashtable eleAttrsMap3 = eleAttrsMap2;
        String lookupName3 = lookupName2;
        Container attrList2 = (Container) eleAttrsMap3.get(lookupName3);
        if (attrList2 == null) {
            reportSchemaError("s4s-elt-invalid", new Object[]{elName}, element);
            return null;
        }
        Object[] attrValues = getAvailableArray();
        System.arraycopy(fSeenTemp, 0, this.fSeen, 0, ATTIDX_COUNT);
        int length3 = attrs2.length;
        Attr sattr2 = null;
        int i4 = 0;
        while (i4 < length3) {
            Attr sattr3 = attrs2[i4];
            String attrName = sattr3.getName();
            String attrURI2 = DOMUtil.getNamespaceURI(sattr3);
            String attrVal = DOMUtil.getValue(sattr3);
            if (attrName.startsWith("xml")) {
                String attrPrefix2 = DOMUtil.getPrefix(sattr3);
                attrURI = attrURI2;
                if (!"xmlns".equals(attrPrefix2) && !"xmlns".equals(attrName)) {
                    if (SchemaSymbols.ATT_XML_LANG.equals(attrName) && (SchemaSymbols.ELT_SCHEMA.equals(elName) || SchemaSymbols.ELT_DOCUMENTATION.equals(elName))) {
                        attrPrefix = null;
                    }
                    if (attrPrefix != null || attrPrefix.length() == 0) {
                        int i5 = i4;
                        int length4 = length3;
                        oneAttr2 = attrList2.get(attrName);
                        if (oneAttr2 != null) {
                            reportSchemaError("s4s-att-not-allowed", new Object[]{elName, attrName}, element);
                            attrList = attrList2;
                            lookupName = lookupName3;
                            sattr = sattr3;
                            attrs = attrs2;
                            i = i5;
                            length = length4;
                            oneAttr = null;
                            z = true;
                            eleAttrsMap = eleAttrsMap3;
                            i4 = i + 1;
                            eleAttrsMap3 = eleAttrsMap;
                            length3 = length;
                            attrList2 = attrList;
                            lookupName3 = lookupName;
                            sattr2 = sattr;
                            attrs2 = attrs;
                        } else {
                            this.fSeen[oneAttr2.valueIndex] = true;
                            try {
                            } catch (InvalidDatatypeValueException e) {
                                ide = e;
                                oneAttr3 = oneAttr2;
                                attrList = attrList2;
                                lookupName = lookupName3;
                                sattr = sattr3;
                                attrs = attrs2;
                                i = i5;
                                length = length4;
                                i2 = 3;
                                eleAttrsMap = eleAttrsMap3;
                            }
                            if (oneAttr2.dvIndex >= 0) {
                                try {
                                    if (oneAttr2.dvIndex != 3) {
                                        try {
                                            if (oneAttr2.dvIndex == 6 || oneAttr2.dvIndex == 7) {
                                                sattr = sattr3;
                                                attrValues[oneAttr2.valueIndex] = attrVal;
                                            } else {
                                                XSSimpleType dv = fExtraDVs[oneAttr2.dvIndex];
                                                sattr = sattr3;
                                                try {
                                                    Object avalue = dv.validate(attrVal, (ValidationContext) schemaDoc.fValidationContext, (ValidatedInfo) null);
                                                    try {
                                                        if (oneAttr2.dvIndex == 2) {
                                                            QName qname = (QName) avalue;
                                                            if (qname.prefix == XMLSymbols.EMPTY_STRING && qname.uri == null && schemaDoc.fIsChameleonSchema) {
                                                                qname.uri = schemaDoc.fTargetNamespace;
                                                            }
                                                        }
                                                        attrValues[oneAttr2.valueIndex] = avalue;
                                                    } catch (InvalidDatatypeValueException e2) {
                                                        ide = e2;
                                                        oneAttr3 = oneAttr2;
                                                        attrList = attrList2;
                                                        lookupName = lookupName3;
                                                        eleAttrsMap = eleAttrsMap3;
                                                        attrs = attrs2;
                                                        i = i5;
                                                        length = length4;
                                                        i2 = 3;
                                                        Object[] objArr = new Object[i2];
                                                        oneAttr = null;
                                                        objArr[0] = elName;
                                                        z = true;
                                                        objArr[1] = attrName;
                                                        objArr[2] = ide.getMessage();
                                                        reportSchemaError("s4s-att-invalid-value", objArr, element);
                                                        oneAttr4 = oneAttr3;
                                                        if (oneAttr4.dfltValue != null) {
                                                        }
                                                    }
                                                } catch (InvalidDatatypeValueException e3) {
                                                    ide = e3;
                                                    oneAttr3 = oneAttr2;
                                                    attrList = attrList2;
                                                    lookupName = lookupName3;
                                                    eleAttrsMap = eleAttrsMap3;
                                                    attrs = attrs2;
                                                    i = i5;
                                                    length = length4;
                                                    i2 = 3;
                                                }
                                            }
                                            attrList = attrList2;
                                            lookupName = lookupName3;
                                            eleAttrsMap = eleAttrsMap3;
                                            attrs = attrs2;
                                            i = i5;
                                            length = length4;
                                            oneAttr = null;
                                            z = true;
                                        } catch (InvalidDatatypeValueException e4) {
                                            ide = e4;
                                            sattr = sattr3;
                                            oneAttr3 = oneAttr2;
                                            attrList = attrList2;
                                            lookupName = lookupName3;
                                            eleAttrsMap = eleAttrsMap3;
                                            attrs = attrs2;
                                            i = i5;
                                            length = length4;
                                            i2 = 3;
                                        }
                                    }
                                } catch (InvalidDatatypeValueException e5) {
                                    ide = e5;
                                    sattr = sattr3;
                                    oneAttr3 = oneAttr2;
                                    attrList = attrList2;
                                    lookupName = lookupName3;
                                    eleAttrsMap = eleAttrsMap3;
                                    attrs = attrs2;
                                    i = i5;
                                    length = length4;
                                    i2 = 3;
                                }
                                i4 = i + 1;
                                eleAttrsMap3 = eleAttrsMap;
                                length3 = length;
                                attrList2 = attrList;
                                lookupName3 = lookupName;
                                sattr2 = sattr;
                                attrs2 = attrs;
                            } else {
                                sattr = sattr3;
                                try {
                                    attrs = attrs2;
                                    i = i5;
                                    i2 = 3;
                                    oneAttr3 = oneAttr2;
                                    length = length4;
                                    attrList = attrList2;
                                    lookupName = lookupName3;
                                    eleAttrsMap = eleAttrsMap3;
                                } catch (InvalidDatatypeValueException e6) {
                                    ide = e6;
                                    oneAttr3 = oneAttr2;
                                    attrList = attrList2;
                                    lookupName = lookupName3;
                                    eleAttrsMap = eleAttrsMap3;
                                    attrs = attrs2;
                                    i = i5;
                                    length = length4;
                                    i2 = 3;
                                }
                                try {
                                    attrValues[oneAttr2.valueIndex] = validate(attrValues, attrName, attrVal, oneAttr2.dvIndex, schemaDoc);
                                    oneAttr = null;
                                    z = true;
                                } catch (InvalidDatatypeValueException e7) {
                                    ide = e7;
                                    Object[] objArr2 = new Object[i2];
                                    oneAttr = null;
                                    objArr2[0] = elName;
                                    z = true;
                                    objArr2[1] = attrName;
                                    objArr2[2] = ide.getMessage();
                                    reportSchemaError("s4s-att-invalid-value", objArr2, element);
                                    oneAttr4 = oneAttr3;
                                    if (oneAttr4.dfltValue != null) {
                                        attrValues[oneAttr4.valueIndex] = oneAttr4.dfltValue;
                                    }
                                }
                            }
                            if (elName.equals(SchemaSymbols.ELT_ENUMERATION) && enumAsQName) {
                                attrValues[ATTIDX_ENUMNSDECLS] = new SchemaNamespaceSupport(schemaDoc.fNamespaceSupport);
                            }
                            i4 = i + 1;
                            eleAttrsMap3 = eleAttrsMap;
                            length3 = length;
                            attrList2 = attrList;
                            lookupName3 = lookupName;
                            sattr2 = sattr;
                            attrs2 = attrs;
                        }
                    } else if (attrPrefix.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA)) {
                        reportSchemaError("s4s-att-not-allowed", new Object[]{elName, attrName}, element);
                    } else {
                        if (attrValues[ATTIDX_NONSCHEMA] == null) {
                            i3 = i4;
                            length2 = length3;
                            attrValues[ATTIDX_NONSCHEMA] = new Vector(4, 2);
                        } else {
                            i3 = i4;
                            length2 = length3;
                        }
                        ((Vector) attrValues[ATTIDX_NONSCHEMA]).addElement(attrName);
                        ((Vector) attrValues[ATTIDX_NONSCHEMA]).addElement(attrVal);
                        attrList = attrList2;
                        lookupName = lookupName3;
                        sattr = sattr3;
                        attrs = attrs2;
                        i = i3;
                        length = length2;
                        oneAttr = null;
                        z = true;
                        eleAttrsMap = eleAttrsMap3;
                        i4 = i + 1;
                        eleAttrsMap3 = eleAttrsMap;
                        length3 = length;
                        attrList2 = attrList;
                        lookupName3 = lookupName;
                        sattr2 = sattr;
                        attrs2 = attrs;
                    }
                }
                i = i4;
                length = length3;
                attrList = attrList2;
                lookupName = lookupName3;
                sattr = sattr3;
                attrs = attrs2;
                oneAttr = null;
                z = true;
                eleAttrsMap = eleAttrsMap3;
                i4 = i + 1;
                eleAttrsMap3 = eleAttrsMap;
                length3 = length;
                attrList2 = attrList;
                lookupName3 = lookupName;
                sattr2 = sattr;
                attrs2 = attrs;
            } else {
                attrURI = attrURI2;
            }
            attrPrefix = attrURI;
            if (attrPrefix != null) {
            }
            int i52 = i4;
            int length42 = length3;
            oneAttr2 = attrList2.get(attrName);
            if (oneAttr2 != null) {
            }
        }
        OneAttr[] reqAttrs2 = attrList2.values;
        long fromDefault = 0;
        int i6 = 0;
        while (i6 < reqAttrs2.length) {
            Attr sattr4 = sattr2;
            OneAttr oneAttr5 = reqAttrs2[i6];
            if (oneAttr5.dfltValue != null) {
                reqAttrs = reqAttrs2;
                if (!this.fSeen[oneAttr5.valueIndex]) {
                    attrValues[oneAttr5.valueIndex] = oneAttr5.dfltValue;
                    fromDefault |= (long) (1 << oneAttr5.valueIndex);
                }
            } else {
                reqAttrs = reqAttrs2;
            }
            i6++;
            sattr2 = sattr4;
            reqAttrs2 = reqAttrs;
        }
        attrValues[ATTIDX_FROMDEFAULT] = new Long(fromDefault);
        if (attrValues[ATTIDX_MAXOCCURS] != null) {
            int min = ((XInt) attrValues[ATTIDX_MINOCCURS]).intValue();
            int max = ((XInt) attrValues[ATTIDX_MAXOCCURS]).intValue();
            if (max != -1 && min > max) {
                reportSchemaError("p-props-correct.2.1", new Object[]{elName, attrValues[ATTIDX_MINOCCURS], attrValues[ATTIDX_MAXOCCURS]}, element);
                attrValues[ATTIDX_MINOCCURS] = attrValues[ATTIDX_MAXOCCURS];
            }
        }
        return attrValues;
    }

    private Object validate(Object[] attrValues, String attr, String ivalue, int dvIndex, XSDocumentInfo schemaDoc) throws InvalidDatatypeValueException {
        String tempNamespace;
        if (ivalue == null) {
            return null;
        }
        String value = XMLChar.trim(ivalue);
        switch (dvIndex) {
            case DT_POSINT:
                try {
                    if (value.length() > 0 && value.charAt(0) == '+') {
                        value = value.substring(1);
                    }
                    XInt xInt = fXIntPool.getXInt(Integer.parseInt(value));
                    if (xInt.intValue() > 0) {
                        return xInt;
                    }
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, SchemaSymbols.ATTVAL_POSITIVEINTEGER});
                } catch (NumberFormatException e) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, SchemaSymbols.ATTVAL_POSITIVEINTEGER});
                }
            case DT_NONNEGINT:
                try {
                    if (value.length() > 0 && value.charAt(0) == '+') {
                        value = value.substring(1);
                    }
                    XInt xInt2 = fXIntPool.getXInt(Integer.parseInt(value));
                    if (xInt2.intValue() >= 0) {
                        return xInt2;
                    }
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, SchemaSymbols.ATTVAL_NONNEGATIVEINTEGER});
                } catch (NumberFormatException e2) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, SchemaSymbols.ATTVAL_NONNEGATIVEINTEGER});
                }
            case DT_BOOLEAN:
                if (value.equals(SchemaSymbols.ATTVAL_FALSE) || value.equals(SchemaSymbols.ATTVAL_FALSE_0)) {
                    Object retValue = Boolean.FALSE;
                    return retValue;
                }
                if (value.equals(SchemaSymbols.ATTVAL_TRUE) || value.equals(SchemaSymbols.ATTVAL_TRUE_1)) {
                    Object retValue2 = Boolean.TRUE;
                    return retValue2;
                }
                throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{value, SchemaSymbols.ATTVAL_BOOLEAN});
            case DT_WHITESPACE:
                if (value.equals(SchemaSymbols.ATTVAL_PRESERVE)) {
                    Object retValue3 = INT_WS_PRESERVE;
                    return retValue3;
                }
                if (value.equals(SchemaSymbols.ATTVAL_REPLACE)) {
                    Object retValue4 = INT_WS_REPLACE;
                    return retValue4;
                }
                if (value.equals(SchemaSymbols.ATTVAL_COLLAPSE)) {
                    Object retValue5 = INT_WS_COLLAPSE;
                    return retValue5;
                }
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{value, "(preserve | replace | collapse)"});
            case DT_USE:
                if (value.equals(SchemaSymbols.ATTVAL_OPTIONAL)) {
                    Object retValue6 = INT_USE_OPTIONAL;
                    return retValue6;
                }
                if (value.equals(SchemaSymbols.ATTVAL_REQUIRED)) {
                    Object retValue7 = INT_USE_REQUIRED;
                    return retValue7;
                }
                if (value.equals(SchemaSymbols.ATTVAL_PROHIBITED)) {
                    Object retValue8 = INT_USE_PROHIBITED;
                    return retValue8;
                }
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{value, "(optional | prohibited | required)"});
            case DT_PROCESSCONTENTS:
                if (value.equals(SchemaSymbols.ATTVAL_STRICT)) {
                    Object retValue9 = INT_ANY_STRICT;
                    return retValue9;
                }
                if (value.equals(SchemaSymbols.ATTVAL_LAX)) {
                    Object retValue10 = INT_ANY_LAX;
                    return retValue10;
                }
                if (value.equals(SchemaSymbols.ATTVAL_SKIP)) {
                    Object retValue11 = INT_ANY_SKIP;
                    return retValue11;
                }
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{value, "(lax | skip | strict)"});
            case DT_NAMESPACE:
                if (value.equals(SchemaSymbols.ATTVAL_TWOPOUNDANY)) {
                    Object retValue12 = INT_ANY_ANY;
                    return retValue12;
                }
                if (value.equals(SchemaSymbols.ATTVAL_TWOPOUNDOTHER)) {
                    Object retValue13 = INT_ANY_NOT;
                    attrValues[ATTIDX_NAMESPACE_LIST] = new String[]{schemaDoc.fTargetNamespace, null};
                    return retValue13;
                }
                Object retValue14 = INT_ANY_LIST;
                this.fNamespaceList.removeAllElements();
                StringTokenizer tokens = new StringTokenizer(value, " \n\t\r");
                while (tokens.hasMoreTokens()) {
                    try {
                        String token = tokens.nextToken();
                        if (token.equals(SchemaSymbols.ATTVAL_TWOPOUNDLOCAL)) {
                            tempNamespace = null;
                        } else if (!token.equals(SchemaSymbols.ATTVAL_TWOPOUNDTARGETNS)) {
                            fExtraDVs[0].validate(token, (ValidationContext) schemaDoc.fValidationContext, (ValidatedInfo) null);
                            tempNamespace = this.fSymbolTable.addSymbol(token);
                        } else {
                            tempNamespace = schemaDoc.fTargetNamespace;
                        }
                        if (!this.fNamespaceList.contains(tempNamespace)) {
                            this.fNamespaceList.addElement(tempNamespace);
                        }
                    } catch (InvalidDatatypeValueException e3) {
                        throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "((##any | ##other) | List of (anyURI | (##targetNamespace | ##local)) )"});
                    }
                    break;
                }
                int num = this.fNamespaceList.size();
                String[] list = new String[num];
                this.fNamespaceList.copyInto(list);
                attrValues[ATTIDX_NAMESPACE_LIST] = list;
                return retValue14;
            case DT_MINOCCURS1:
                if (value.equals(SchemaSymbols.ATTVAL_FALSE_0)) {
                    Object retValue15 = fXIntPool.getXInt(0);
                    return retValue15;
                }
                if (value.equals(SchemaSymbols.ATTVAL_TRUE_1)) {
                    Object retValue16 = fXIntPool.getXInt(1);
                    return retValue16;
                }
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{value, "(0 | 1)"});
            case DT_MEMBERTYPES:
                Vector memberType = new Vector();
                try {
                    StringTokenizer t = new StringTokenizer(value, " \n\t\r");
                    while (t.hasMoreTokens()) {
                        QName qname = (QName) fExtraDVs[2].validate(t.nextToken(), (ValidationContext) schemaDoc.fValidationContext, (ValidatedInfo) null);
                        if (qname.prefix == XMLSymbols.EMPTY_STRING && qname.uri == null && schemaDoc.fIsChameleonSchema) {
                            qname.uri = schemaDoc.fTargetNamespace;
                        }
                        memberType.addElement(qname);
                        break;
                    }
                    return memberType;
                } catch (InvalidDatatypeValueException e4) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.2", new Object[]{value, "(List of QName)"});
                }
            case DT_MAXOCCURS1:
                if (value.equals(SchemaSymbols.ATTVAL_TRUE_1)) {
                    Object retValue17 = fXIntPool.getXInt(1);
                    return retValue17;
                }
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{value, "(1)"});
            case DT_MAXOCCURS:
                if (!value.equals(SchemaSymbols.ATTVAL_UNBOUNDED)) {
                    try {
                        Object retValue18 = validate(attrValues, attr, value, DT_NONNEGINT, schemaDoc);
                        return retValue18;
                    } catch (NumberFormatException e5) {
                        throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(nonNegativeInteger | unbounded)"});
                    }
                }
                Object retValue19 = INT_UNBOUNDED;
                return retValue19;
            case DT_FORM:
                if (value.equals(SchemaSymbols.ATTVAL_QUALIFIED)) {
                    Object retValue20 = INT_QUALIFIED;
                    return retValue20;
                }
                if (value.equals(SchemaSymbols.ATTVAL_UNQUALIFIED)) {
                    Object retValue21 = INT_UNQUALIFIED;
                    return retValue21;
                }
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{value, "(qualified | unqualified)"});
            case DT_FINAL2:
                int choice = 0;
                if (value.equals(SchemaSymbols.ATTVAL_POUNDALL)) {
                    choice = 31;
                } else {
                    StringTokenizer t2 = new StringTokenizer(value, " \n\t\r");
                    while (t2.hasMoreTokens()) {
                        String token2 = t2.nextToken();
                        if (token2.equals(SchemaSymbols.ATTVAL_EXTENSION)) {
                            choice |= 1;
                        } else if (token2.equals(SchemaSymbols.ATTVAL_RESTRICTION)) {
                            choice |= 2;
                        } else if (token2.equals(SchemaSymbols.ATTVAL_LIST)) {
                            choice |= 16;
                        } else if (token2.equals(SchemaSymbols.ATTVAL_UNION)) {
                            choice |= 8;
                        } else {
                            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (extension | restriction | list | union))"});
                        }
                    }
                }
                Object retValue22 = fXIntPool.getXInt(choice);
                return retValue22;
            case DT_FINAL1:
                int choice2 = 0;
                if (value.equals(SchemaSymbols.ATTVAL_POUNDALL)) {
                    choice2 = 31;
                } else {
                    StringTokenizer t3 = new StringTokenizer(value, " \n\t\r");
                    while (t3.hasMoreTokens()) {
                        String token3 = t3.nextToken();
                        if (token3.equals(SchemaSymbols.ATTVAL_LIST)) {
                            choice2 |= 16;
                        } else if (token3.equals(SchemaSymbols.ATTVAL_UNION)) {
                            choice2 |= 8;
                        } else if (token3.equals(SchemaSymbols.ATTVAL_RESTRICTION)) {
                            choice2 |= 2;
                        } else {
                            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (list | union | restriction))"});
                        }
                    }
                }
                Object retValue23 = fXIntPool.getXInt(choice2);
                return retValue23;
            case -3:
            case -2:
                int choice3 = 0;
                if (value.equals(SchemaSymbols.ATTVAL_POUNDALL)) {
                    choice3 = 31;
                } else {
                    StringTokenizer t4 = new StringTokenizer(value, " \n\t\r");
                    while (t4.hasMoreTokens()) {
                        String token4 = t4.nextToken();
                        if (token4.equals(SchemaSymbols.ATTVAL_EXTENSION)) {
                            choice3 |= 1;
                        } else if (token4.equals(SchemaSymbols.ATTVAL_RESTRICTION)) {
                            choice3 |= 2;
                        } else {
                            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (extension | restriction))"});
                        }
                    }
                }
                Object retValue24 = fXIntPool.getXInt(choice3);
                return retValue24;
            case -1:
                int choice4 = 0;
                if (value.equals(SchemaSymbols.ATTVAL_POUNDALL)) {
                    choice4 = 31;
                } else {
                    StringTokenizer t5 = new StringTokenizer(value, " \n\t\r");
                    while (t5.hasMoreTokens()) {
                        String token5 = t5.nextToken();
                        if (token5.equals(SchemaSymbols.ATTVAL_EXTENSION)) {
                            choice4 |= 1;
                        } else if (token5.equals(SchemaSymbols.ATTVAL_RESTRICTION)) {
                            choice4 |= 2;
                        } else if (token5.equals(SchemaSymbols.ATTVAL_SUBSTITUTION)) {
                            choice4 |= 4;
                        } else {
                            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{value, "(#all | List of (extension | restriction | substitution))"});
                        }
                    }
                }
                Object retValue25 = fXIntPool.getXInt(choice4);
                return retValue25;
            default:
                return null;
        }
    }

    void reportSchemaError(String key, Object[] args, Element ele) {
        this.fSchemaHandler.reportSchemaError(key, args, ele);
    }

    public void checkNonSchemaAttributes(XSGrammarBucket grammarBucket) {
        XSAttributeDecl attrDecl;
        XSSimpleType dv;
        Iterator entries;
        boolean z;
        Map.Entry entry;
        Iterator entries2 = this.fNonSchemaAttrs.entrySet().iterator();
        while (entries2.hasNext()) {
            Map.Entry entry2 = (Map.Entry) entries2.next();
            String attrRName = (String) entry2.getKey();
            String attrURI = attrRName.substring(0, attrRName.indexOf(44));
            String attrLocal = attrRName.substring(attrRName.indexOf(44) + 1);
            SchemaGrammar sGrammar = grammarBucket.getGrammar(attrURI);
            if (sGrammar != null && (attrDecl = sGrammar.getGlobalAttributeDecl(attrLocal)) != null && (dv = (XSSimpleType) attrDecl.getTypeDefinition()) != null) {
                Vector values = (Vector) entry2.getValue();
                String attrName = (String) values.elementAt(0);
                int count = values.size();
                int i = 1;
                while (i < count) {
                    String elName = (String) values.elementAt(i);
                    try {
                        dv.validate((String) values.elementAt(i + 1), (ValidationContext) null, (ValidatedInfo) null);
                        entries = entries2;
                        entry = entry2;
                        z = false;
                    } catch (InvalidDatatypeValueException ide) {
                        entries = entries2;
                        z = false;
                        entry = entry2;
                        reportSchemaError("s4s-att-invalid-value", new Object[]{elName, attrName, ide.getMessage()}, null);
                    }
                    i += 2;
                    entries2 = entries;
                    entry2 = entry;
                }
            }
        }
    }

    public static String normalize(String content, short ws) {
        char ch;
        int len = content == null ? 0 : content.length();
        if (len == 0 || ws == 0) {
            return content;
        }
        StringBuffer sb = new StringBuffer();
        if (ws == 1) {
            for (int i = 0; i < len; i++) {
                char ch2 = content.charAt(i);
                if (ch2 != '\t' && ch2 != '\n' && ch2 != '\r') {
                    sb.append(ch2);
                } else {
                    sb.append(' ');
                }
            }
        } else {
            boolean isLeading = true;
            int i2 = 0;
            while (i2 < len) {
                char ch3 = content.charAt(i2);
                if (ch3 != '\t' && ch3 != '\n' && ch3 != '\r' && ch3 != ' ') {
                    sb.append(ch3);
                    isLeading = false;
                } else {
                    while (i2 < len - 1 && ((ch = content.charAt(i2 + 1)) == '\t' || ch == '\n' || ch == '\r' || ch == ' ')) {
                        i2++;
                    }
                    if (i2 < len - 1 && !isLeading) {
                        sb.append(' ');
                    }
                }
                i2++;
            }
        }
        return sb.toString();
    }

    protected Object[] getAvailableArray() {
        if (this.fArrayPool.length == this.fPoolPos) {
            this.fArrayPool = new Object[this.fPoolPos + 10][];
            for (int i = this.fPoolPos; i < this.fArrayPool.length; i++) {
                this.fArrayPool[i] = new Object[ATTIDX_COUNT];
            }
        }
        Object[] retArray = this.fArrayPool[this.fPoolPos];
        Object[][] objArr = this.fArrayPool;
        int i2 = this.fPoolPos;
        this.fPoolPos = i2 + 1;
        objArr[i2] = null;
        System.arraycopy(fTempArray, 0, retArray, 0, ATTIDX_COUNT - 1);
        retArray[ATTIDX_ISRETURNED] = Boolean.FALSE;
        return retArray;
    }

    public void returnAttrArray(Object[] attrArray, XSDocumentInfo schemaDoc) {
        if (schemaDoc != null) {
            schemaDoc.fNamespaceSupport.popContext();
        }
        if (this.fPoolPos == 0 || attrArray == null || attrArray.length != ATTIDX_COUNT || ((Boolean) attrArray[ATTIDX_ISRETURNED]).booleanValue()) {
            return;
        }
        attrArray[ATTIDX_ISRETURNED] = Boolean.TRUE;
        if (attrArray[ATTIDX_NONSCHEMA] != null) {
            ((Vector) attrArray[ATTIDX_NONSCHEMA]).clear();
        }
        Object[][] objArr = this.fArrayPool;
        int i = this.fPoolPos - 1;
        this.fPoolPos = i;
        objArr[i] = attrArray;
    }

    public void resolveNamespace(Element element, Attr[] attrs, SchemaNamespaceSupport nsSupport) {
        nsSupport.pushContext();
        for (Attr sattr : attrs) {
            String rawname = DOMUtil.getName(sattr);
            String prefix = null;
            if (rawname.equals(XMLSymbols.PREFIX_XMLNS)) {
                prefix = XMLSymbols.EMPTY_STRING;
            } else if (rawname.startsWith("xmlns:")) {
                prefix = this.fSymbolTable.addSymbol(DOMUtil.getLocalName(sattr));
            }
            if (prefix != null) {
                String uri = this.fSymbolTable.addSymbol(DOMUtil.getValue(sattr));
                nsSupport.declarePrefix(prefix, uri.length() != 0 ? uri : null);
            }
        }
    }
}
