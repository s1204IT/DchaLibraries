package mf.org.apache.xerces.impl.dv.xs;

import com.mediatek.plugin.preload.SoOperater;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import mf.org.apache.xerces.impl.dv.DatatypeException;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeFacetException;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSFacets;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.dv.xs.ListDV;
import mf.org.apache.xerces.impl.xpath.regex.RegularExpression;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.util.ObjectListImpl;
import mf.org.apache.xerces.impl.xs.util.ShortListImpl;
import mf.org.apache.xerces.impl.xs.util.StringListImpl;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xs.ShortList;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSAnnotation;
import mf.org.apache.xerces.xs.XSFacet;
import mf.org.apache.xerces.xs.XSMultiValueFacet;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.apache.xerces.xs.datatypes.ObjectList;
import mf.org.w3c.dom.TypeInfo;

public class XSSimpleTypeDecl implements XSSimpleType, TypeInfo {
    public static final short ANYATOMICTYPE_DT = 49;
    static final String ANY_TYPE = "anyType";
    public static final short DAYTIMEDURATION_DT = 47;
    static final int DERIVATION_ANY = 0;
    static final int DERIVATION_EXTENSION = 2;
    static final int DERIVATION_LIST = 8;
    static final int DERIVATION_RESTRICTION = 1;
    static final int DERIVATION_UNION = 4;
    protected static final short DV_ANYATOMICTYPE = 29;
    protected static final short DV_ANYSIMPLETYPE = 0;
    protected static final short DV_ANYURI = 17;
    protected static final short DV_BASE64BINARY = 16;
    protected static final short DV_BOOLEAN = 2;
    protected static final short DV_DATE = 9;
    protected static final short DV_DATETIME = 7;
    protected static final short DV_DAYTIMEDURATION = 28;
    protected static final short DV_DECIMAL = 3;
    protected static final short DV_DOUBLE = 5;
    protected static final short DV_DURATION = 6;
    protected static final short DV_ENTITY = 23;
    protected static final short DV_FLOAT = 4;
    protected static final short DV_GDAY = 13;
    protected static final short DV_GMONTH = 14;
    protected static final short DV_GMONTHDAY = 12;
    protected static final short DV_GYEAR = 11;
    protected static final short DV_GYEARMONTH = 10;
    protected static final short DV_HEXBINARY = 15;
    protected static final short DV_ID = 21;
    protected static final short DV_IDREF = 22;
    protected static final short DV_INTEGER = 24;
    protected static final short DV_LIST = 25;
    protected static final short DV_NOTATION = 20;
    protected static final short DV_PRECISIONDECIMAL = 19;
    protected static final short DV_QNAME = 18;
    protected static final short DV_STRING = 1;
    protected static final short DV_TIME = 8;
    protected static final short DV_UNION = 26;
    protected static final short DV_YEARMONTHDURATION = 27;
    static final short NORMALIZE_FULL = 2;
    static final short NORMALIZE_NONE = 0;
    static final short NORMALIZE_TRIM = 1;
    public static final short PRECISIONDECIMAL_DT = 48;
    static final short SPECIAL_PATTERN_NAME = 2;
    static final short SPECIAL_PATTERN_NCNAME = 3;
    static final short SPECIAL_PATTERN_NMTOKEN = 1;
    static final short SPECIAL_PATTERN_NONE = 0;
    static final String URI_SCHEMAFORSCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final short YEARMONTHDURATION_DT = 46;
    public XSObjectList enumerationAnnotations;
    private ObjectList fActualEnumeration;
    private XSObjectList fAnnotations;
    private boolean fAnonymous;
    private XSSimpleTypeDecl fBase;
    private boolean fBounded;
    private short fBuiltInKind;
    private TypeValidator[] fDVs;
    private ValidatedInfo[] fEnumeration;
    private ObjectList fEnumerationItemTypeList;
    private int fEnumerationSize;
    private ShortList fEnumerationTypeList;
    private XSObjectListImpl fFacets;
    private short fFacetsDefined;
    private short fFinalSet;
    private boolean fFinite;
    private short fFixedFacet;
    private int fFractionDigits;
    private boolean fIsImmutable;
    private XSSimpleTypeDecl fItemType;
    private int fLength;
    private StringList fLexicalEnumeration;
    private StringList fLexicalPattern;
    private Object fMaxExclusive;
    private Object fMaxInclusive;
    private int fMaxLength;
    private XSSimpleTypeDecl[] fMemberTypes;
    private Object fMinExclusive;
    private Object fMinInclusive;
    private int fMinLength;
    private XSObjectListImpl fMultiValueFacets;
    private XSNamespaceItem fNamespaceItem;
    private boolean fNumeric;
    private short fOrdered;
    private Vector fPattern;
    private Vector fPatternStr;
    private short fPatternType;
    private String fTargetNamespace;
    private int fTotalDigits;
    private String fTypeName;
    private short fValidationDV;
    private short fVariety;
    private short fWhiteSpace;
    public XSAnnotation fractionDigitsAnnotation;
    public XSAnnotation lengthAnnotation;
    public XSAnnotation maxExclusiveAnnotation;
    public XSAnnotation maxInclusiveAnnotation;
    public XSAnnotation maxLengthAnnotation;
    public XSAnnotation minExclusiveAnnotation;
    public XSAnnotation minInclusiveAnnotation;
    public XSAnnotation minLengthAnnotation;
    public XSObjectListImpl patternAnnotations;
    public XSAnnotation totalDigitsAnnotation;
    public XSAnnotation whiteSpaceAnnotation;
    private static final TypeValidator[] gDVs = {new AnySimpleDV(), new StringDV(), new BooleanDV(), new DecimalDV(), new FloatDV(), new DoubleDV(), new DurationDV(), new DateTimeDV(), new TimeDV(), new DateDV(), new YearMonthDV(), new YearDV(), new MonthDayDV(), new DayDV(), new MonthDV(), new HexBinaryDV(), new Base64BinaryDV(), new AnyURIDV(), new QNameDV(), new PrecisionDecimalDV(), new QNameDV(), new IDDV(), new IDREFDV(), new EntityDV(), new IntegerDV(), new ListDV(), new UnionDV(), new YearMonthDurationDV(), new DayTimeDurationDV(), new AnyAtomicDV()};
    static final short[] fDVNormalizeType = {0, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 1, 0};
    static final String[] SPECIAL_PATTERN_STRING = {"NONE", SchemaSymbols.ATTVAL_NMTOKEN, SchemaSymbols.ATTVAL_NAME, SchemaSymbols.ATTVAL_NCNAME};
    static final String[] WS_FACET_STRING = {SchemaSymbols.ATTVAL_PRESERVE, SchemaSymbols.ATTVAL_REPLACE, SchemaSymbols.ATTVAL_COLLAPSE};
    static final ValidationContext fEmptyContext = new ValidationContext() {
        @Override
        public boolean needFacetChecking() {
            return true;
        }

        @Override
        public boolean needExtraChecking() {
            return false;
        }

        @Override
        public boolean needToNormalize() {
            return true;
        }

        @Override
        public boolean useNamespaces() {
            return true;
        }

        @Override
        public boolean isEntityDeclared(String name) {
            return false;
        }

        @Override
        public boolean isEntityUnparsed(String name) {
            return false;
        }

        @Override
        public boolean isIdDeclared(String name) {
            return false;
        }

        @Override
        public void addId(String name) {
        }

        @Override
        public void addIdRef(String name) {
        }

        @Override
        public String getSymbol(String symbol) {
            return symbol.intern();
        }

        @Override
        public String getURI(String prefix) {
            return null;
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }
    };
    static final XSSimpleTypeDecl fAnySimpleType = new XSSimpleTypeDecl(null, SchemaSymbols.ATTVAL_ANYSIMPLETYPE, 0, 0, false, true, false, true, 1);
    static final XSSimpleTypeDecl fAnyAtomicType = new XSSimpleTypeDecl(fAnySimpleType, "anyAtomicType", 29, 0, false, true, false, true, 49);
    static final ValidationContext fDummyContext = new ValidationContext() {
        @Override
        public boolean needFacetChecking() {
            return true;
        }

        @Override
        public boolean needExtraChecking() {
            return false;
        }

        @Override
        public boolean needToNormalize() {
            return false;
        }

        @Override
        public boolean useNamespaces() {
            return true;
        }

        @Override
        public boolean isEntityDeclared(String name) {
            return false;
        }

        @Override
        public boolean isEntityUnparsed(String name) {
            return false;
        }

        @Override
        public boolean isIdDeclared(String name) {
            return false;
        }

        @Override
        public void addId(String name) {
        }

        @Override
        public void addIdRef(String name) {
        }

        @Override
        public String getSymbol(String symbol) {
            return symbol.intern();
        }

        @Override
        public String getURI(String prefix) {
            return null;
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }
    };

    protected static TypeValidator[] getGDVs() {
        return (TypeValidator[]) gDVs.clone();
    }

    protected void setDVs(TypeValidator[] dvs) {
        this.fDVs = dvs;
    }

    public XSSimpleTypeDecl() {
        this.fDVs = gDVs;
        this.fIsImmutable = false;
        this.fFinalSet = (short) 0;
        this.fVariety = (short) -1;
        this.fValidationDV = (short) -1;
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        this.fWhiteSpace = (short) 0;
        this.fLength = -1;
        this.fMinLength = -1;
        this.fMaxLength = -1;
        this.fTotalDigits = -1;
        this.fFractionDigits = -1;
        this.fAnnotations = null;
        this.fPatternType = (short) 0;
        this.fNamespaceItem = null;
        this.fAnonymous = false;
    }

    protected XSSimpleTypeDecl(XSSimpleTypeDecl base, String name, short validateDV, short ordered, boolean bounded, boolean finite, boolean numeric, boolean isImmutable, short builtInKind) {
        this.fDVs = gDVs;
        this.fIsImmutable = false;
        this.fFinalSet = (short) 0;
        this.fVariety = (short) -1;
        this.fValidationDV = (short) -1;
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        this.fWhiteSpace = (short) 0;
        this.fLength = -1;
        this.fMinLength = -1;
        this.fMaxLength = -1;
        this.fTotalDigits = -1;
        this.fFractionDigits = -1;
        this.fAnnotations = null;
        this.fPatternType = (short) 0;
        this.fNamespaceItem = null;
        this.fAnonymous = false;
        this.fIsImmutable = isImmutable;
        this.fBase = base;
        this.fTypeName = name;
        this.fTargetNamespace = "http://www.w3.org/2001/XMLSchema";
        this.fVariety = (short) 1;
        this.fValidationDV = validateDV;
        this.fFacetsDefined = (short) 16;
        if (validateDV == 0 || validateDV == 29 || validateDV == 1) {
            this.fWhiteSpace = (short) 0;
        } else {
            this.fWhiteSpace = (short) 2;
            this.fFixedFacet = (short) 16;
        }
        this.fOrdered = ordered;
        this.fBounded = bounded;
        this.fFinite = finite;
        this.fNumeric = numeric;
        this.fAnnotations = null;
        this.fBuiltInKind = builtInKind;
    }

    protected XSSimpleTypeDecl(XSSimpleTypeDecl base, String name, String uri, short finalSet, boolean isImmutable, XSObjectList annotations, short builtInKind) {
        this(base, name, uri, finalSet, isImmutable, annotations);
        this.fBuiltInKind = builtInKind;
    }

    protected XSSimpleTypeDecl(XSSimpleTypeDecl base, String name, String uri, short finalSet, boolean isImmutable, XSObjectList annotations) {
        this.fDVs = gDVs;
        this.fIsImmutable = false;
        this.fFinalSet = (short) 0;
        this.fVariety = (short) -1;
        this.fValidationDV = (short) -1;
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        this.fWhiteSpace = (short) 0;
        this.fLength = -1;
        this.fMinLength = -1;
        this.fMaxLength = -1;
        this.fTotalDigits = -1;
        this.fFractionDigits = -1;
        this.fAnnotations = null;
        this.fPatternType = (short) 0;
        this.fNamespaceItem = null;
        this.fAnonymous = false;
        this.fBase = base;
        this.fTypeName = name;
        this.fTargetNamespace = uri;
        this.fFinalSet = finalSet;
        this.fAnnotations = annotations;
        this.fVariety = this.fBase.fVariety;
        this.fValidationDV = this.fBase.fValidationDV;
        switch (this.fVariety) {
            case 2:
                this.fItemType = this.fBase.fItemType;
                break;
            case 3:
                this.fMemberTypes = this.fBase.fMemberTypes;
                break;
        }
        this.fLength = this.fBase.fLength;
        this.fMinLength = this.fBase.fMinLength;
        this.fMaxLength = this.fBase.fMaxLength;
        this.fPattern = this.fBase.fPattern;
        this.fPatternStr = this.fBase.fPatternStr;
        this.fEnumeration = this.fBase.fEnumeration;
        this.fEnumerationSize = this.fBase.fEnumerationSize;
        this.fWhiteSpace = this.fBase.fWhiteSpace;
        this.fMaxExclusive = this.fBase.fMaxExclusive;
        this.fMaxInclusive = this.fBase.fMaxInclusive;
        this.fMinExclusive = this.fBase.fMinExclusive;
        this.fMinInclusive = this.fBase.fMinInclusive;
        this.fTotalDigits = this.fBase.fTotalDigits;
        this.fFractionDigits = this.fBase.fFractionDigits;
        this.fPatternType = this.fBase.fPatternType;
        this.fFixedFacet = this.fBase.fFixedFacet;
        this.fFacetsDefined = this.fBase.fFacetsDefined;
        this.lengthAnnotation = this.fBase.lengthAnnotation;
        this.minLengthAnnotation = this.fBase.minLengthAnnotation;
        this.maxLengthAnnotation = this.fBase.maxLengthAnnotation;
        this.patternAnnotations = this.fBase.patternAnnotations;
        this.enumerationAnnotations = this.fBase.enumerationAnnotations;
        this.whiteSpaceAnnotation = this.fBase.whiteSpaceAnnotation;
        this.maxExclusiveAnnotation = this.fBase.maxExclusiveAnnotation;
        this.maxInclusiveAnnotation = this.fBase.maxInclusiveAnnotation;
        this.minExclusiveAnnotation = this.fBase.minExclusiveAnnotation;
        this.minInclusiveAnnotation = this.fBase.minInclusiveAnnotation;
        this.totalDigitsAnnotation = this.fBase.totalDigitsAnnotation;
        this.fractionDigitsAnnotation = this.fBase.fractionDigitsAnnotation;
        calcFundamentalFacets();
        this.fIsImmutable = isImmutable;
        this.fBuiltInKind = base.fBuiltInKind;
    }

    protected XSSimpleTypeDecl(String name, String uri, short finalSet, XSSimpleTypeDecl itemType, boolean isImmutable, XSObjectList annotations) {
        this.fDVs = gDVs;
        this.fIsImmutable = false;
        this.fFinalSet = (short) 0;
        this.fVariety = (short) -1;
        this.fValidationDV = (short) -1;
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        this.fWhiteSpace = (short) 0;
        this.fLength = -1;
        this.fMinLength = -1;
        this.fMaxLength = -1;
        this.fTotalDigits = -1;
        this.fFractionDigits = -1;
        this.fAnnotations = null;
        this.fPatternType = (short) 0;
        this.fNamespaceItem = null;
        this.fAnonymous = false;
        this.fBase = fAnySimpleType;
        this.fTypeName = name;
        this.fTargetNamespace = uri;
        this.fFinalSet = finalSet;
        this.fAnnotations = annotations;
        this.fVariety = (short) 2;
        this.fItemType = itemType;
        this.fValidationDV = (short) 25;
        this.fFacetsDefined = (short) 16;
        this.fFixedFacet = (short) 16;
        this.fWhiteSpace = (short) 2;
        calcFundamentalFacets();
        this.fIsImmutable = isImmutable;
        this.fBuiltInKind = (short) 44;
    }

    protected XSSimpleTypeDecl(String name, String uri, short finalSet, XSSimpleTypeDecl[] memberTypes, XSObjectList annotations) {
        this.fDVs = gDVs;
        this.fIsImmutable = false;
        this.fFinalSet = (short) 0;
        this.fVariety = (short) -1;
        this.fValidationDV = (short) -1;
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        this.fWhiteSpace = (short) 0;
        this.fLength = -1;
        this.fMinLength = -1;
        this.fMaxLength = -1;
        this.fTotalDigits = -1;
        this.fFractionDigits = -1;
        this.fAnnotations = null;
        this.fPatternType = (short) 0;
        this.fNamespaceItem = null;
        this.fAnonymous = false;
        this.fBase = fAnySimpleType;
        this.fTypeName = name;
        this.fTargetNamespace = uri;
        this.fFinalSet = finalSet;
        this.fAnnotations = annotations;
        this.fVariety = (short) 3;
        this.fMemberTypes = memberTypes;
        this.fValidationDV = (short) 26;
        this.fFacetsDefined = (short) 16;
        this.fWhiteSpace = (short) 2;
        calcFundamentalFacets();
        this.fIsImmutable = false;
        this.fBuiltInKind = (short) 45;
    }

    protected XSSimpleTypeDecl setRestrictionValues(XSSimpleTypeDecl base, String name, String uri, short finalSet, XSObjectList annotations) {
        if (this.fIsImmutable) {
            return null;
        }
        this.fBase = base;
        this.fAnonymous = false;
        this.fTypeName = name;
        this.fTargetNamespace = uri;
        this.fFinalSet = finalSet;
        this.fAnnotations = annotations;
        this.fVariety = this.fBase.fVariety;
        this.fValidationDV = this.fBase.fValidationDV;
        switch (this.fVariety) {
            case 2:
                this.fItemType = this.fBase.fItemType;
                break;
            case 3:
                this.fMemberTypes = this.fBase.fMemberTypes;
                break;
        }
        this.fLength = this.fBase.fLength;
        this.fMinLength = this.fBase.fMinLength;
        this.fMaxLength = this.fBase.fMaxLength;
        this.fPattern = this.fBase.fPattern;
        this.fPatternStr = this.fBase.fPatternStr;
        this.fEnumeration = this.fBase.fEnumeration;
        this.fEnumerationSize = this.fBase.fEnumerationSize;
        this.fWhiteSpace = this.fBase.fWhiteSpace;
        this.fMaxExclusive = this.fBase.fMaxExclusive;
        this.fMaxInclusive = this.fBase.fMaxInclusive;
        this.fMinExclusive = this.fBase.fMinExclusive;
        this.fMinInclusive = this.fBase.fMinInclusive;
        this.fTotalDigits = this.fBase.fTotalDigits;
        this.fFractionDigits = this.fBase.fFractionDigits;
        this.fPatternType = this.fBase.fPatternType;
        this.fFixedFacet = this.fBase.fFixedFacet;
        this.fFacetsDefined = this.fBase.fFacetsDefined;
        calcFundamentalFacets();
        this.fBuiltInKind = base.fBuiltInKind;
        return this;
    }

    protected XSSimpleTypeDecl setListValues(String name, String uri, short finalSet, XSSimpleTypeDecl itemType, XSObjectList annotations) {
        if (this.fIsImmutable) {
            return null;
        }
        this.fBase = fAnySimpleType;
        this.fAnonymous = false;
        this.fTypeName = name;
        this.fTargetNamespace = uri;
        this.fFinalSet = finalSet;
        this.fAnnotations = annotations;
        this.fVariety = (short) 2;
        this.fItemType = itemType;
        this.fValidationDV = (short) 25;
        this.fFacetsDefined = (short) 16;
        this.fFixedFacet = (short) 16;
        this.fWhiteSpace = (short) 2;
        calcFundamentalFacets();
        this.fBuiltInKind = (short) 44;
        return this;
    }

    protected XSSimpleTypeDecl setUnionValues(String name, String uri, short finalSet, XSSimpleTypeDecl[] memberTypes, XSObjectList annotations) {
        if (this.fIsImmutable) {
            return null;
        }
        this.fBase = fAnySimpleType;
        this.fAnonymous = false;
        this.fTypeName = name;
        this.fTargetNamespace = uri;
        this.fFinalSet = finalSet;
        this.fAnnotations = annotations;
        this.fVariety = (short) 3;
        this.fMemberTypes = memberTypes;
        this.fValidationDV = (short) 26;
        this.fFacetsDefined = (short) 16;
        this.fWhiteSpace = (short) 2;
        calcFundamentalFacets();
        this.fBuiltInKind = (short) 45;
        return this;
    }

    @Override
    public short getType() {
        return (short) 3;
    }

    @Override
    public short getTypeCategory() {
        return (short) 16;
    }

    @Override
    public String getName() {
        if (getAnonymous()) {
            return null;
        }
        return this.fTypeName;
    }

    public String getTypeName() {
        return this.fTypeName;
    }

    @Override
    public String getNamespace() {
        return this.fTargetNamespace;
    }

    @Override
    public short getFinal() {
        return this.fFinalSet;
    }

    @Override
    public boolean isFinal(short derivation) {
        return (this.fFinalSet & derivation) != 0;
    }

    @Override
    public XSTypeDefinition getBaseType() {
        return this.fBase;
    }

    @Override
    public boolean getAnonymous() {
        return this.fAnonymous || this.fTypeName == null;
    }

    @Override
    public short getVariety() {
        if (this.fValidationDV == 0) {
            return (short) 0;
        }
        return this.fVariety;
    }

    @Override
    public boolean isIDType() {
        switch (this.fVariety) {
            case 1:
                return this.fValidationDV == 21;
            case 2:
                return this.fItemType.isIDType();
            case 3:
                for (int i = 0; i < this.fMemberTypes.length; i++) {
                    if (this.fMemberTypes[i].isIDType()) {
                        return true;
                    }
                }
            default:
                return false;
        }
    }

    @Override
    public short getWhitespace() throws DatatypeException {
        if (this.fVariety == 3) {
            throw new DatatypeException("dt-whitespace", new Object[]{this.fTypeName});
        }
        return this.fWhiteSpace;
    }

    @Override
    public short getPrimitiveKind() {
        if (this.fVariety == 1 && this.fValidationDV != 0) {
            if (this.fValidationDV == 21 || this.fValidationDV == 22 || this.fValidationDV == 23) {
                return (short) 1;
            }
            if (this.fValidationDV == 24) {
                return (short) 3;
            }
            return this.fValidationDV;
        }
        return (short) 0;
    }

    @Override
    public short getBuiltInKind() {
        return this.fBuiltInKind;
    }

    @Override
    public XSSimpleTypeDefinition getPrimitiveType() {
        if (this.fVariety == 1 && this.fValidationDV != 0) {
            XSSimpleTypeDecl pri = this;
            while (pri.fBase != fAnySimpleType) {
                pri = pri.fBase;
            }
            return pri;
        }
        return null;
    }

    @Override
    public XSSimpleTypeDefinition getItemType() {
        if (this.fVariety == 2) {
            return this.fItemType;
        }
        return null;
    }

    @Override
    public XSObjectList getMemberTypes() {
        if (this.fVariety == 3) {
            return new XSObjectListImpl(this.fMemberTypes, this.fMemberTypes.length);
        }
        return XSObjectListImpl.EMPTY_LIST;
    }

    @Override
    public void applyFacets(XSFacets facets, short presentFacet, short fixedFacet, ValidationContext context) throws InvalidDatatypeFacetException {
        if (context == null) {
            context = fEmptyContext;
        }
        applyFacets(facets, presentFacet, fixedFacet, (short) 0, context);
    }

    void applyFacets1(XSFacets facets, short presentFacet, short fixedFacet) {
        try {
            applyFacets(facets, presentFacet, fixedFacet, (short) 0, fDummyContext);
            this.fIsImmutable = true;
        } catch (InvalidDatatypeFacetException e) {
            throw new RuntimeException("internal error");
        }
    }

    void applyFacets1(XSFacets facets, short presentFacet, short fixedFacet, short patternType) {
        try {
            applyFacets(facets, presentFacet, fixedFacet, patternType, fDummyContext);
            this.fIsImmutable = true;
        } catch (InvalidDatatypeFacetException e) {
            throw new RuntimeException("internal error");
        }
    }

    void applyFacets(XSFacets facets, short presentFacet, short fixedFacet, short patternType, ValidationContext context) throws InvalidDatatypeFacetException {
        int result;
        int result2;
        short s;
        short s2;
        short s3;
        short s4;
        int i;
        int result3;
        int size;
        ValidatedInfo info;
        ValidatedInfo[] validatedInfoArr;
        int result4;
        if (this.fIsImmutable) {
            return;
        }
        ValidatedInfo tempInfo = new ValidatedInfo();
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        int result5 = 0;
        short allowedFacet = this.fDVs[this.fValidationDV].getAllowedFacets();
        if ((presentFacet & 1) != 0) {
            if ((allowedFacet & 1) == 0) {
                reportError("cos-applicable-facets", new Object[]{"length", this.fTypeName});
            } else {
                this.fLength = facets.length;
                this.lengthAnnotation = facets.lengthAnnotation;
                this.fFacetsDefined = (short) (this.fFacetsDefined | 1);
                if ((fixedFacet & 1) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | 1);
                }
            }
        }
        if ((presentFacet & 2) != 0) {
            if ((allowedFacet & 2) == 0) {
                reportError("cos-applicable-facets", new Object[]{"minLength", this.fTypeName});
            } else {
                this.fMinLength = facets.minLength;
                this.minLengthAnnotation = facets.minLengthAnnotation;
                this.fFacetsDefined = (short) (this.fFacetsDefined | 2);
                if ((fixedFacet & 2) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | 2);
                }
            }
        }
        if ((presentFacet & 4) != 0) {
            if ((allowedFacet & 4) == 0) {
                reportError("cos-applicable-facets", new Object[]{"maxLength", this.fTypeName});
            } else {
                this.fMaxLength = facets.maxLength;
                this.maxLengthAnnotation = facets.maxLengthAnnotation;
                this.fFacetsDefined = (short) (this.fFacetsDefined | 4);
                if ((fixedFacet & 4) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | 4);
                }
            }
        }
        if ((presentFacet & 8) != 0) {
            if ((allowedFacet & 8) == 0) {
                reportError("cos-applicable-facets", new Object[]{"pattern", this.fTypeName});
            } else {
                this.patternAnnotations = facets.patternAnnotations;
                RegularExpression regex = null;
                try {
                    regex = new RegularExpression(facets.pattern, "X", context.getLocale());
                } catch (Exception e) {
                    reportError("InvalidRegex", new Object[]{facets.pattern, e.getLocalizedMessage()});
                }
                if (regex != null) {
                    this.fPattern = new Vector();
                    this.fPattern.addElement(regex);
                    this.fPatternStr = new Vector();
                    this.fPatternStr.addElement(facets.pattern);
                    this.fFacetsDefined = (short) (this.fFacetsDefined | 8);
                    if ((fixedFacet & 8) != 0) {
                        this.fFixedFacet = (short) (this.fFixedFacet | 8);
                    }
                }
            }
        }
        if ((presentFacet & 16) != 0) {
            if ((allowedFacet & 16) == 0) {
                reportError("cos-applicable-facets", new Object[]{"whiteSpace", this.fTypeName});
            } else {
                this.fWhiteSpace = facets.whiteSpace;
                this.whiteSpaceAnnotation = facets.whiteSpaceAnnotation;
                this.fFacetsDefined = (short) (this.fFacetsDefined | 16);
                if ((fixedFacet & 16) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | 16);
                }
            }
        }
        if ((presentFacet & XSSimpleTypeDefinition.FACET_ENUMERATION) != 0) {
            if ((allowedFacet & XSSimpleTypeDefinition.FACET_ENUMERATION) == 0) {
                reportError("cos-applicable-facets", new Object[]{"enumeration", this.fTypeName});
            } else {
                Vector enumVals = facets.enumeration;
                int size2 = enumVals.size();
                this.fEnumeration = new ValidatedInfo[size2];
                Vector enumNSDecls = facets.enumNSDecls;
                ValidationContextImpl ctx = new ValidationContextImpl(context);
                this.enumerationAnnotations = facets.enumAnnotations;
                this.fEnumerationSize = 0;
                int i2 = 0;
                while (i2 < size2) {
                    if (enumNSDecls != null) {
                        ctx.setNSContext((NamespaceContext) enumNSDecls.elementAt(i2));
                    }
                    try {
                        info = getActualEnumValue((String) enumVals.elementAt(i2), ctx, null);
                        validatedInfoArr = this.fEnumeration;
                        result3 = result5;
                        try {
                            result4 = this.fEnumerationSize;
                            size = size2;
                        } catch (InvalidDatatypeValueException e2) {
                            size = size2;
                        }
                    } catch (InvalidDatatypeValueException e3) {
                        result3 = result5;
                        size = size2;
                    }
                    try {
                        this.fEnumerationSize = result4 + 1;
                        validatedInfoArr[result4] = info;
                    } catch (InvalidDatatypeValueException e4) {
                        reportError("enumeration-valid-restriction", new Object[]{enumVals.elementAt(i2), getBaseType().getName()});
                    }
                    i2++;
                    result5 = result3;
                    size2 = size;
                }
                this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_ENUMERATION);
                if ((fixedFacet & XSSimpleTypeDefinition.FACET_ENUMERATION) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | XSSimpleTypeDefinition.FACET_ENUMERATION);
                }
            }
            result = result5;
        } else {
            result = 0;
        }
        if ((presentFacet & 32) != 0) {
            if ((allowedFacet & 32) == 0) {
                reportError("cos-applicable-facets", new Object[]{"maxInclusive", this.fTypeName});
            } else {
                this.maxInclusiveAnnotation = facets.maxInclusiveAnnotation;
                try {
                    this.fMaxInclusive = this.fBase.getActualValue(facets.maxInclusive, context, tempInfo, true);
                    this.fFacetsDefined = (short) (this.fFacetsDefined | 32);
                    if ((fixedFacet & 32) != 0) {
                        this.fFixedFacet = (short) (this.fFixedFacet | 32);
                    }
                } catch (InvalidDatatypeValueException ide) {
                    reportError(ide.getKey(), ide.getArgs());
                    reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.maxInclusive, "maxInclusive", this.fBase.getName()});
                }
                if ((this.fBase.fFacetsDefined & 32) != 0 && (this.fBase.fFixedFacet & 32) != 0 && this.fDVs[this.fValidationDV].compare(this.fMaxInclusive, this.fBase.fMaxInclusive) != 0) {
                    reportError("FixedFacetValue", new Object[]{"maxInclusive", this.fMaxInclusive, this.fBase.fMaxInclusive, this.fTypeName});
                }
                try {
                    this.fBase.validate(context, tempInfo);
                } catch (InvalidDatatypeValueException ide2) {
                    reportError(ide2.getKey(), ide2.getArgs());
                    reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.maxInclusive, "maxInclusive", this.fBase.getName()});
                }
            }
        }
        boolean needCheckBase = true;
        if ((presentFacet & 64) != 0) {
            if ((allowedFacet & 64) == 0) {
                reportError("cos-applicable-facets", new Object[]{"maxExclusive", this.fTypeName});
            } else {
                this.maxExclusiveAnnotation = facets.maxExclusiveAnnotation;
                try {
                    this.fMaxExclusive = this.fBase.getActualValue(facets.maxExclusive, context, tempInfo, true);
                    this.fFacetsDefined = (short) (this.fFacetsDefined | 64);
                    if ((fixedFacet & 64) != 0) {
                        this.fFixedFacet = (short) (this.fFixedFacet | 64);
                    }
                } catch (InvalidDatatypeValueException ide3) {
                    reportError(ide3.getKey(), ide3.getArgs());
                    reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.maxExclusive, "maxExclusive", this.fBase.getName()});
                }
                if ((this.fBase.fFacetsDefined & 64) != 0) {
                    int result6 = this.fDVs[this.fValidationDV].compare(this.fMaxExclusive, this.fBase.fMaxExclusive);
                    if ((this.fBase.fFixedFacet & 64) != 0 && result6 != 0) {
                        reportError("FixedFacetValue", new Object[]{"maxExclusive", facets.maxExclusive, this.fBase.fMaxExclusive, this.fTypeName});
                    }
                    if (result6 == 0) {
                        needCheckBase = false;
                    }
                    result = result6;
                }
                if (needCheckBase) {
                    try {
                        this.fBase.validate(context, tempInfo);
                    } catch (InvalidDatatypeValueException ide4) {
                        reportError(ide4.getKey(), ide4.getArgs());
                        reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.maxExclusive, "maxExclusive", this.fBase.getName()});
                    }
                } else if ((this.fBase.fFacetsDefined & 32) != 0 && this.fDVs[this.fValidationDV].compare(this.fMaxExclusive, this.fBase.fMaxInclusive) > 0) {
                    reportError("maxExclusive-valid-restriction.2", new Object[]{facets.maxExclusive, this.fBase.fMaxInclusive});
                }
            }
        }
        boolean needCheckBase2 = true;
        if ((presentFacet & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0) {
            if ((allowedFacet & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"minExclusive", this.fTypeName});
            } else {
                this.minExclusiveAnnotation = facets.minExclusiveAnnotation;
                try {
                    this.fMinExclusive = this.fBase.getActualValue(facets.minExclusive, context, tempInfo, true);
                    this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
                    if ((fixedFacet & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0) {
                        this.fFixedFacet = (short) (this.fFixedFacet | XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
                    }
                } catch (InvalidDatatypeValueException ide5) {
                    reportError(ide5.getKey(), ide5.getArgs());
                    reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.minExclusive, "minExclusive", this.fBase.getName()});
                }
                if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0) {
                    int result7 = this.fDVs[this.fValidationDV].compare(this.fMinExclusive, this.fBase.fMinExclusive);
                    if ((this.fBase.fFixedFacet & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0 && result7 != 0) {
                        reportError("FixedFacetValue", new Object[]{"minExclusive", facets.minExclusive, this.fBase.fMinExclusive, this.fTypeName});
                    }
                    if (result7 == 0) {
                        needCheckBase2 = false;
                    }
                    result = result7;
                }
                if (needCheckBase2) {
                    try {
                        this.fBase.validate(context, tempInfo);
                    } catch (InvalidDatatypeValueException ide6) {
                        reportError(ide6.getKey(), ide6.getArgs());
                        reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.minExclusive, "minExclusive", this.fBase.getName()});
                    }
                } else if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && this.fDVs[this.fValidationDV].compare(this.fMinExclusive, this.fBase.fMinInclusive) < 0) {
                    reportError("minExclusive-valid-restriction.3", new Object[]{facets.minExclusive, this.fBase.fMinInclusive});
                }
            }
        }
        if ((presentFacet & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
            if ((allowedFacet & XSSimpleTypeDefinition.FACET_MININCLUSIVE) == 0) {
                reportError("cos-applicable-facets", new Object[]{"minInclusive", this.fTypeName});
            } else {
                this.minInclusiveAnnotation = facets.minInclusiveAnnotation;
                try {
                    this.fMinInclusive = this.fBase.getActualValue(facets.minInclusive, context, tempInfo, true);
                    this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_MININCLUSIVE);
                    if ((fixedFacet & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                        this.fFixedFacet = (short) (this.fFixedFacet | XSSimpleTypeDefinition.FACET_MININCLUSIVE);
                    }
                } catch (InvalidDatatypeValueException ide7) {
                    reportError(ide7.getKey(), ide7.getArgs());
                    reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.minInclusive, "minInclusive", this.fBase.getName()});
                }
                if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && (this.fBase.fFixedFacet & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && this.fDVs[this.fValidationDV].compare(this.fMinInclusive, this.fBase.fMinInclusive) != 0) {
                    reportError("FixedFacetValue", new Object[]{"minInclusive", facets.minInclusive, this.fBase.fMinInclusive, this.fTypeName});
                }
                try {
                    this.fBase.validate(context, tempInfo);
                } catch (InvalidDatatypeValueException ide8) {
                    reportError(ide8.getKey(), ide8.getArgs());
                    reportError("FacetValueFromBase", new Object[]{this.fTypeName, facets.minInclusive, "minInclusive", this.fBase.getName()});
                }
            }
        }
        if ((presentFacet & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0) {
            if ((allowedFacet & XSSimpleTypeDefinition.FACET_TOTALDIGITS) == 0) {
                reportError("cos-applicable-facets", new Object[]{"totalDigits", this.fTypeName});
            } else {
                this.totalDigitsAnnotation = facets.totalDigitsAnnotation;
                this.fTotalDigits = facets.totalDigits;
                this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_TOTALDIGITS);
                if ((fixedFacet & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | XSSimpleTypeDefinition.FACET_TOTALDIGITS);
                }
            }
        }
        if ((presentFacet & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0) {
            if ((allowedFacet & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) == 0) {
                reportError("cos-applicable-facets", new Object[]{"fractionDigits", this.fTypeName});
            } else {
                this.fFractionDigits = facets.fractionDigits;
                this.fractionDigitsAnnotation = facets.fractionDigitsAnnotation;
                this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
                if ((fixedFacet & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0) {
                    this.fFixedFacet = (short) (this.fFixedFacet | XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
                }
            }
        }
        if (patternType != 0) {
            this.fPatternType = patternType;
        }
        if (this.fFacetsDefined != 0) {
            if ((this.fFacetsDefined & 2) != 0 && (this.fFacetsDefined & 4) != 0 && this.fMinLength > this.fMaxLength) {
                reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(this.fMinLength), Integer.toString(this.fMaxLength), this.fTypeName});
            }
            if ((this.fFacetsDefined & 64) != 0 && (this.fFacetsDefined & 32) != 0) {
                reportError("maxInclusive-maxExclusive", new Object[]{this.fMaxInclusive, this.fMaxExclusive, this.fTypeName});
            }
            if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                reportError("minInclusive-minExclusive", new Object[]{this.fMinInclusive, this.fMinExclusive, this.fTypeName});
            }
            if ((this.fFacetsDefined & 32) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                result2 = this.fDVs[this.fValidationDV].compare(this.fMinInclusive, this.fMaxInclusive);
                if (result2 != -1 && result2 != 0) {
                    reportError("minInclusive-less-than-equal-to-maxInclusive", new Object[]{this.fMinInclusive, this.fMaxInclusive, this.fTypeName});
                }
            } else {
                result2 = result;
            }
            if ((this.fFacetsDefined & 64) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0 && (result2 = this.fDVs[this.fValidationDV].compare(this.fMinExclusive, this.fMaxExclusive)) != -1 && result2 != 0) {
                reportError("minExclusive-less-than-equal-to-maxExclusive", new Object[]{this.fMinExclusive, this.fMaxExclusive, this.fTypeName});
            }
            if ((this.fFacetsDefined & 32) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0 && this.fDVs[this.fValidationDV].compare(this.fMinExclusive, this.fMaxInclusive) != -1) {
                reportError("minExclusive-less-than-maxInclusive", new Object[]{this.fMinExclusive, this.fMaxInclusive, this.fTypeName});
            }
            if ((this.fFacetsDefined & 64) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && this.fDVs[this.fValidationDV].compare(this.fMinInclusive, this.fMaxExclusive) != -1) {
                reportError("minInclusive-less-than-maxExclusive", new Object[]{this.fMinInclusive, this.fMaxExclusive, this.fTypeName});
            }
            if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0 && this.fFractionDigits > this.fTotalDigits) {
                s = 1;
                reportError("fractionDigits-totalDigits", new Object[]{Integer.toString(this.fFractionDigits), Integer.toString(this.fTotalDigits), this.fTypeName});
            } else {
                s = 1;
            }
            if ((this.fFacetsDefined & s) != 0) {
                if ((this.fBase.fFacetsDefined & 2) != 0 && this.fLength < this.fBase.fMinLength) {
                    reportError("length-minLength-maxLength.1.1", new Object[]{this.fTypeName, Integer.toString(this.fLength), Integer.toString(this.fBase.fMinLength)});
                }
                if ((this.fBase.fFacetsDefined & 4) != 0 && this.fLength > this.fBase.fMaxLength) {
                    reportError("length-minLength-maxLength.2.1", new Object[]{this.fTypeName, Integer.toString(this.fLength), Integer.toString(this.fBase.fMaxLength)});
                }
                if ((this.fBase.fFacetsDefined & 1) != 0 && this.fLength != this.fBase.fLength) {
                    s2 = 1;
                    reportError("length-valid-restriction", new Object[]{Integer.toString(this.fLength), Integer.toString(this.fBase.fLength), this.fTypeName});
                }
                if ((this.fBase.fFacetsDefined & s2) == 0) {
                    if ((this.fFacetsDefined & 2) != 0) {
                    }
                    if ((this.fFacetsDefined & 4) != 0) {
                    }
                }
            } else {
                s2 = 1;
                if ((this.fBase.fFacetsDefined & s2) == 0 || (this.fFacetsDefined & s2) != 0) {
                    if ((this.fFacetsDefined & 2) != 0) {
                        if (this.fBase.fLength < this.fMinLength) {
                            s4 = 2;
                            reportError("length-minLength-maxLength.1.1", new Object[]{this.fTypeName, Integer.toString(this.fBase.fLength), Integer.toString(this.fMinLength)});
                        } else {
                            s4 = 2;
                        }
                        if ((this.fBase.fFacetsDefined & s4) == 0) {
                            reportError("length-minLength-maxLength.1.2.a", new Object[]{this.fTypeName});
                        }
                        if (this.fMinLength != this.fBase.fMinLength) {
                            reportError("length-minLength-maxLength.1.2.b", new Object[]{this.fTypeName, Integer.toString(this.fMinLength), Integer.toString(this.fBase.fMinLength)});
                        }
                    }
                    if ((this.fFacetsDefined & 4) != 0) {
                        if (this.fBase.fLength > this.fMaxLength) {
                            reportError("length-minLength-maxLength.2.1", new Object[]{this.fTypeName, Integer.toString(this.fBase.fLength), Integer.toString(this.fMaxLength)});
                        }
                        if ((this.fBase.fFacetsDefined & 4) == 0) {
                            reportError("length-minLength-maxLength.2.2.a", new Object[]{this.fTypeName});
                        }
                        if (this.fMaxLength != this.fBase.fMaxLength) {
                            s3 = 2;
                            reportError("length-minLength-maxLength.2.2.b", new Object[]{this.fTypeName, Integer.toString(this.fMaxLength), Integer.toString(this.fBase.fBase.fMaxLength)});
                        }
                        if ((this.fFacetsDefined & s3) != 0) {
                        }
                        if ((this.fFacetsDefined & 4) != 0) {
                            reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(this.fBase.fMinLength), Integer.toString(this.fMaxLength)});
                        }
                        if ((this.fFacetsDefined & 4) != 0) {
                            if ((this.fBase.fFixedFacet & 4) != 0) {
                                reportError("FixedFacetValue", new Object[]{"maxLength", Integer.toString(this.fMaxLength), Integer.toString(this.fBase.fMaxLength), this.fTypeName});
                            }
                            if (this.fMaxLength > this.fBase.fMaxLength) {
                            }
                        }
                        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0) {
                            if ((this.fBase.fFixedFacet & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0) {
                                reportError("FixedFacetValue", new Object[]{"totalDigits", Integer.toString(this.fTotalDigits), Integer.toString(this.fBase.fTotalDigits), this.fTypeName});
                            }
                            if (this.fTotalDigits > this.fBase.fTotalDigits) {
                            }
                        }
                        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0) {
                            reportError("fractionDigits-totalDigits", new Object[]{Integer.toString(this.fFractionDigits), Integer.toString(this.fTotalDigits), this.fTypeName});
                        }
                        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0) {
                        }
                        if ((this.fFacetsDefined & 16) != 0) {
                            if ((this.fBase.fFixedFacet & 16) != 0) {
                                reportError("FixedFacetValue", new Object[]{"whiteSpace", whiteSpaceValue(this.fWhiteSpace), whiteSpaceValue(this.fBase.fWhiteSpace), this.fTypeName});
                            }
                            if (this.fWhiteSpace != 0) {
                                i = 1;
                                if (this.fWhiteSpace == i) {
                                    Object[] objArr = new Object[2];
                                    objArr[0] = this.fTypeName;
                                    objArr[i] = SchemaSymbols.ATTVAL_REPLACE;
                                    reportError("whiteSpace-valid-restriction.1", objArr);
                                }
                                if (this.fWhiteSpace == 0) {
                                    Object[] objArr2 = new Object[i];
                                    objArr2[0] = this.fTypeName;
                                    reportError("whiteSpace-valid-restriction.2", objArr2);
                                }
                            }
                        }
                    } else {
                        s3 = 2;
                        if ((this.fFacetsDefined & s3) != 0) {
                            if ((this.fBase.fFacetsDefined & 4) != 0) {
                                if (this.fMinLength > this.fBase.fMaxLength) {
                                    reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(this.fMinLength), Integer.toString(this.fBase.fMaxLength), this.fTypeName});
                                }
                            } else if ((this.fBase.fFacetsDefined & 2) != 0) {
                                if ((this.fBase.fFixedFacet & 2) != 0 && this.fMinLength != this.fBase.fMinLength) {
                                    reportError("FixedFacetValue", new Object[]{"minLength", Integer.toString(this.fMinLength), Integer.toString(this.fBase.fMinLength), this.fTypeName});
                                }
                                if (this.fMinLength < this.fBase.fMinLength) {
                                    reportError("minLength-valid-restriction", new Object[]{Integer.toString(this.fMinLength), Integer.toString(this.fBase.fMinLength), this.fTypeName});
                                }
                            }
                        }
                        if ((this.fFacetsDefined & 4) != 0 && (this.fBase.fFacetsDefined & 2) != 0 && this.fMaxLength < this.fBase.fMinLength) {
                            reportError("minLength-less-than-equal-to-maxLength", new Object[]{Integer.toString(this.fBase.fMinLength), Integer.toString(this.fMaxLength)});
                        }
                        if ((this.fFacetsDefined & 4) != 0 && (this.fBase.fFacetsDefined & 4) != 0) {
                            if ((this.fBase.fFixedFacet & 4) != 0 && this.fMaxLength != this.fBase.fMaxLength) {
                                reportError("FixedFacetValue", new Object[]{"maxLength", Integer.toString(this.fMaxLength), Integer.toString(this.fBase.fMaxLength), this.fTypeName});
                            }
                            if (this.fMaxLength > this.fBase.fMaxLength) {
                                reportError("maxLength-valid-restriction", new Object[]{Integer.toString(this.fMaxLength), Integer.toString(this.fBase.fMaxLength), this.fTypeName});
                            }
                        }
                        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0 && (this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0) {
                            if ((this.fBase.fFixedFacet & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0 && this.fTotalDigits != this.fBase.fTotalDigits) {
                                reportError("FixedFacetValue", new Object[]{"totalDigits", Integer.toString(this.fTotalDigits), Integer.toString(this.fBase.fTotalDigits), this.fTypeName});
                            }
                            if (this.fTotalDigits > this.fBase.fTotalDigits) {
                                reportError("totalDigits-valid-restriction", new Object[]{Integer.toString(this.fTotalDigits), Integer.toString(this.fBase.fTotalDigits), this.fTypeName});
                            }
                        }
                        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0 && (this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0 && this.fFractionDigits > this.fBase.fTotalDigits) {
                            reportError("fractionDigits-totalDigits", new Object[]{Integer.toString(this.fFractionDigits), Integer.toString(this.fTotalDigits), this.fTypeName});
                        }
                        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0) {
                            if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0) {
                                if (((this.fBase.fFixedFacet & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0 && this.fFractionDigits != this.fBase.fFractionDigits) || (this.fValidationDV == 24 && this.fFractionDigits != 0)) {
                                    reportError("FixedFacetValue", new Object[]{"fractionDigits", Integer.toString(this.fFractionDigits), Integer.toString(this.fBase.fFractionDigits), this.fTypeName});
                                }
                                if (this.fFractionDigits > this.fBase.fFractionDigits) {
                                    reportError("fractionDigits-valid-restriction", new Object[]{Integer.toString(this.fFractionDigits), Integer.toString(this.fBase.fFractionDigits), this.fTypeName});
                                }
                            } else if (this.fValidationDV == 24 && this.fFractionDigits != 0) {
                                reportError("FixedFacetValue", new Object[]{"fractionDigits", Integer.toString(this.fFractionDigits), SchemaSymbols.ATTVAL_FALSE_0, this.fTypeName});
                            }
                        }
                        if ((this.fFacetsDefined & 16) != 0 && (this.fBase.fFacetsDefined & 16) != 0) {
                            if ((this.fBase.fFixedFacet & 16) != 0 && this.fWhiteSpace != this.fBase.fWhiteSpace) {
                                reportError("FixedFacetValue", new Object[]{"whiteSpace", whiteSpaceValue(this.fWhiteSpace), whiteSpaceValue(this.fBase.fWhiteSpace), this.fTypeName});
                            }
                            if (this.fWhiteSpace != 0 && this.fBase.fWhiteSpace == 2) {
                                i = 1;
                                reportError("whiteSpace-valid-restriction.1", new Object[]{this.fTypeName, SchemaSymbols.ATTVAL_PRESERVE});
                            } else {
                                i = 1;
                            }
                            if (this.fWhiteSpace == i && this.fBase.fWhiteSpace == 2) {
                                Object[] objArr3 = new Object[2];
                                objArr3[0] = this.fTypeName;
                                objArr3[i] = SchemaSymbols.ATTVAL_REPLACE;
                                reportError("whiteSpace-valid-restriction.1", objArr3);
                            }
                            if (this.fWhiteSpace == 0 && this.fBase.fWhiteSpace == i) {
                                Object[] objArr22 = new Object[i];
                                objArr22[0] = this.fTypeName;
                                reportError("whiteSpace-valid-restriction.2", objArr22);
                            }
                        }
                    }
                }
            }
        }
        if ((this.fFacetsDefined & 1) == 0 && (this.fBase.fFacetsDefined & 1) != 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | 1);
            this.fLength = this.fBase.fLength;
            this.lengthAnnotation = this.fBase.lengthAnnotation;
        }
        if ((this.fFacetsDefined & 2) == 0 && (this.fBase.fFacetsDefined & 2) != 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | 2);
            this.fMinLength = this.fBase.fMinLength;
            this.minLengthAnnotation = this.fBase.minLengthAnnotation;
        }
        if ((this.fFacetsDefined & 4) == 0 && (this.fBase.fFacetsDefined & 4) != 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | 4);
            this.fMaxLength = this.fBase.fMaxLength;
            this.maxLengthAnnotation = this.fBase.maxLengthAnnotation;
        }
        if ((this.fBase.fFacetsDefined & 8) != 0) {
            if ((this.fFacetsDefined & 8) == 0) {
                this.fFacetsDefined = (short) (this.fFacetsDefined | 8);
                this.fPattern = this.fBase.fPattern;
                this.fPatternStr = this.fBase.fPatternStr;
                this.patternAnnotations = this.fBase.patternAnnotations;
            } else {
                for (int i3 = this.fBase.fPattern.size() - 1; i3 >= 0; i3--) {
                    this.fPattern.addElement(this.fBase.fPattern.elementAt(i3));
                    this.fPatternStr.addElement(this.fBase.fPatternStr.elementAt(i3));
                }
                if (this.fBase.patternAnnotations != null) {
                    if (this.patternAnnotations != null) {
                        for (int i4 = this.fBase.patternAnnotations.getLength() - 1; i4 >= 0; i4--) {
                            this.patternAnnotations.addXSObject(this.fBase.patternAnnotations.item(i4));
                        }
                    } else {
                        this.patternAnnotations = this.fBase.patternAnnotations;
                    }
                }
            }
        }
        int i5 = this.fFacetsDefined;
        if ((i5 & 16) == 0 && (this.fBase.fFacetsDefined & 16) != 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | 16);
            this.fWhiteSpace = this.fBase.fWhiteSpace;
            this.whiteSpaceAnnotation = this.fBase.whiteSpaceAnnotation;
        }
        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_ENUMERATION) == 0 && (this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_ENUMERATION) != 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_ENUMERATION);
            this.fEnumeration = this.fBase.fEnumeration;
            this.fEnumerationSize = this.fBase.fEnumerationSize;
            this.enumerationAnnotations = this.fBase.enumerationAnnotations;
        }
        if ((this.fBase.fFacetsDefined & 64) != 0 && (this.fFacetsDefined & 64) == 0 && (this.fFacetsDefined & 32) == 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | 64);
            this.fMaxExclusive = this.fBase.fMaxExclusive;
            this.maxExclusiveAnnotation = this.fBase.maxExclusiveAnnotation;
        }
        if ((this.fBase.fFacetsDefined & 32) != 0 && (this.fFacetsDefined & 64) == 0 && (this.fFacetsDefined & 32) == 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | 32);
            this.fMaxInclusive = this.fBase.fMaxInclusive;
            this.maxInclusiveAnnotation = this.fBase.maxInclusiveAnnotation;
        }
        if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) == 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) == 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
            this.fMinExclusive = this.fBase.fMinExclusive;
            this.minExclusiveAnnotation = this.fBase.minExclusiveAnnotation;
        }
        if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) == 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) == 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_MININCLUSIVE);
            this.fMinInclusive = this.fBase.fMinInclusive;
            this.minInclusiveAnnotation = this.fBase.minInclusiveAnnotation;
        }
        if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) == 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_TOTALDIGITS);
            this.fTotalDigits = this.fBase.fTotalDigits;
            this.totalDigitsAnnotation = this.fBase.totalDigitsAnnotation;
        }
        if ((this.fBase.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0 && (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) == 0) {
            this.fFacetsDefined = (short) (this.fFacetsDefined | XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
            this.fFractionDigits = this.fBase.fFractionDigits;
            this.fractionDigitsAnnotation = this.fBase.fractionDigitsAnnotation;
        }
        if (this.fPatternType == 0 && this.fBase.fPatternType != 0) {
            this.fPatternType = this.fBase.fPatternType;
        }
        this.fFixedFacet = (short) (this.fFixedFacet | this.fBase.fFixedFacet);
        calcFundamentalFacets();
    }

    @Override
    public Object validate(String content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        if (context == null) {
            context = fEmptyContext;
        }
        if (validatedInfo == null) {
            validatedInfo = new ValidatedInfo();
        } else {
            validatedInfo.memberType = null;
        }
        boolean needNormalize = context == null || context.needToNormalize();
        Object ob = getActualValue(content, context, validatedInfo, needNormalize);
        validate(context, validatedInfo);
        return ob;
    }

    protected ValidatedInfo getActualEnumValue(String lexical, ValidationContext ctx, ValidatedInfo info) throws InvalidDatatypeValueException {
        return this.fBase.validateWithInfo(lexical, ctx, info);
    }

    public ValidatedInfo validateWithInfo(String content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        if (context == null) {
            context = fEmptyContext;
        }
        if (validatedInfo == null) {
            validatedInfo = new ValidatedInfo();
        } else {
            validatedInfo.memberType = null;
        }
        boolean needNormalize = context == null || context.needToNormalize();
        getActualValue(content, context, validatedInfo, needNormalize);
        validate(context, validatedInfo);
        return validatedInfo;
    }

    @Override
    public Object validate(Object content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        if (context == null) {
            context = fEmptyContext;
        }
        if (validatedInfo == null) {
            validatedInfo = new ValidatedInfo();
        } else {
            validatedInfo.memberType = null;
        }
        boolean needNormalize = context == null || context.needToNormalize();
        Object ob = getActualValue(content, context, validatedInfo, needNormalize);
        validate(context, validatedInfo);
        return ob;
    }

    @Override
    public void validate(ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        if (context == null) {
            context = fEmptyContext;
        }
        if (context.needFacetChecking() && this.fFacetsDefined != 0 && this.fFacetsDefined != 16) {
            checkFacets(validatedInfo);
        }
        if (context.needExtraChecking()) {
            checkExtraRules(context, validatedInfo);
        }
    }

    private void checkFacets(ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        int compare;
        int compare2;
        int totalDigits;
        int scale;
        short type;
        short type2;
        Object ob = validatedInfo.actualValue;
        String content = validatedInfo.normalizedValue;
        short type3 = validatedInfo.actualValueType;
        ShortList itemType = validatedInfo.itemValueTypes;
        int i = 0;
        short s = 2;
        short s2 = 1;
        if (this.fValidationDV != 18 && this.fValidationDV != 20) {
            int length = this.fDVs[this.fValidationDV].getDataLength(ob);
            if ((this.fFacetsDefined & 4) != 0 && length > this.fMaxLength) {
                throw new InvalidDatatypeValueException("cvc-maxLength-valid", new Object[]{content, Integer.toString(length), Integer.toString(this.fMaxLength), this.fTypeName});
            }
            if ((this.fFacetsDefined & 2) != 0 && length < this.fMinLength) {
                throw new InvalidDatatypeValueException("cvc-minLength-valid", new Object[]{content, Integer.toString(length), Integer.toString(this.fMinLength), this.fTypeName});
            }
            if ((this.fFacetsDefined & 1) != 0 && length != this.fLength) {
                throw new InvalidDatatypeValueException("cvc-length-valid", new Object[]{content, Integer.toString(length), Integer.toString(this.fLength), this.fTypeName});
            }
        }
        if ((this.fFacetsDefined & 2048) != 0) {
            boolean present = false;
            int enumSize = this.fEnumerationSize;
            short primitiveType1 = convertToPrimitiveKind(type3);
            int i2 = 0;
            while (true) {
                if (i2 >= enumSize) {
                    break;
                }
                short primitiveType2 = convertToPrimitiveKind(this.fEnumeration[i2].actualValueType);
                if (primitiveType1 != primitiveType2 && ((primitiveType1 != s2 || primitiveType2 != s) && (primitiveType1 != s || primitiveType2 != s2))) {
                    type = type3;
                } else if (!this.fEnumeration[i2].actualValue.equals(ob)) {
                    type = type3;
                } else if (primitiveType1 == 44 || primitiveType1 == 43) {
                    ShortList enumItemType = this.fEnumeration[i2].itemValueTypes;
                    int typeList1Length = itemType != null ? itemType.getLength() : i;
                    int typeList2Length = enumItemType != null ? enumItemType.getLength() : i;
                    if (typeList1Length == typeList2Length) {
                        int j = 0;
                        while (true) {
                            if (j < typeList1Length) {
                                short primitiveItem1 = convertToPrimitiveKind(itemType.item(j));
                                short primitiveItem2 = convertToPrimitiveKind(enumItemType.item(j));
                                if (primitiveItem1 == primitiveItem2) {
                                    type = type3;
                                } else {
                                    type = type3;
                                    if (primitiveItem1 == 1) {
                                        type2 = 2;
                                        if (primitiveItem2 == 2) {
                                            continue;
                                        }
                                    } else {
                                        type2 = 2;
                                    }
                                    if (primitiveItem1 != type2 || primitiveItem2 != 1) {
                                        break;
                                    }
                                }
                                j++;
                                type3 = type;
                            } else {
                                type = type3;
                                break;
                            }
                        }
                        if (j == typeList1Length) {
                            present = true;
                            break;
                        }
                    }
                } else {
                    present = true;
                    break;
                }
                i2++;
                type3 = type;
                i = 0;
                s = 2;
                s2 = 1;
            }
            if (!present) {
                StringBuffer sb = new StringBuffer();
                appendEnumString(sb);
                throw new InvalidDatatypeValueException("cvc-enumeration-valid", new Object[]{content, sb.toString()});
            }
        }
        if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0 && (scale = this.fDVs[this.fValidationDV].getFractionDigits(ob)) > this.fFractionDigits) {
            throw new InvalidDatatypeValueException("cvc-fractionDigits-valid", new Object[]{content, Integer.toString(scale), Integer.toString(this.fFractionDigits)});
        }
        if ((this.fFacetsDefined & 512) != 0 && (totalDigits = this.fDVs[this.fValidationDV].getTotalDigits(ob)) > this.fTotalDigits) {
            throw new InvalidDatatypeValueException("cvc-totalDigits-valid", new Object[]{content, Integer.toString(totalDigits), Integer.toString(this.fTotalDigits)});
        }
        if ((this.fFacetsDefined & 32) != 0 && (compare2 = this.fDVs[this.fValidationDV].compare(ob, this.fMaxInclusive)) != -1 && compare2 != 0) {
            throw new InvalidDatatypeValueException("cvc-maxInclusive-valid", new Object[]{content, this.fMaxInclusive, this.fTypeName});
        }
        int compare3 = this.fFacetsDefined;
        if ((compare3 & 64) != 0) {
            int compare4 = this.fDVs[this.fValidationDV].compare(ob, this.fMaxExclusive);
            if (compare4 != -1) {
                throw new InvalidDatatypeValueException("cvc-maxExclusive-valid", new Object[]{content, this.fMaxExclusive, this.fTypeName});
            }
        }
        int compare5 = this.fFacetsDefined;
        if ((compare5 & 256) != 0 && (compare = this.fDVs[this.fValidationDV].compare(ob, this.fMinInclusive)) != 1 && compare != 0) {
            throw new InvalidDatatypeValueException("cvc-minInclusive-valid", new Object[]{content, this.fMinInclusive, this.fTypeName});
        }
        int compare6 = this.fFacetsDefined;
        if ((compare6 & 128) != 0) {
            int compare7 = this.fDVs[this.fValidationDV].compare(ob, this.fMinExclusive);
            if (compare7 != 1) {
                throw new InvalidDatatypeValueException("cvc-minExclusive-valid", new Object[]{content, this.fMinExclusive, this.fTypeName});
            }
        }
    }

    private void checkExtraRules(ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        Object ob = validatedInfo.actualValue;
        if (this.fVariety == 1) {
            this.fDVs[this.fValidationDV].checkExtraRules(ob, context);
            return;
        }
        if (this.fVariety == 2) {
            ListDV.ListData values = (ListDV.ListData) ob;
            XSSimpleType memberType = validatedInfo.memberType;
            int len = values.getLength();
            try {
                if (this.fItemType.fVariety == 3) {
                    XSSimpleTypeDecl[] memberTypes = (XSSimpleTypeDecl[]) validatedInfo.memberTypes;
                    for (int i = len - 1; i >= 0; i--) {
                        validatedInfo.actualValue = values.item(i);
                        validatedInfo.memberType = memberTypes[i];
                        this.fItemType.checkExtraRules(context, validatedInfo);
                    }
                } else {
                    for (int i2 = len - 1; i2 >= 0; i2--) {
                        validatedInfo.actualValue = values.item(i2);
                        this.fItemType.checkExtraRules(context, validatedInfo);
                    }
                }
                return;
            } finally {
                validatedInfo.actualValue = values;
                validatedInfo.memberType = memberType;
            }
        }
        ((XSSimpleTypeDecl) validatedInfo.memberType).checkExtraRules(context, validatedInfo);
    }

    private Object getActualValue(Object content, ValidationContext context, ValidatedInfo validatedInfo, boolean needNormalize) throws InvalidDatatypeValueException {
        String nvalue;
        if (needNormalize) {
            nvalue = normalize(content, this.fWhiteSpace);
        } else {
            nvalue = content.toString();
        }
        String nvalue2 = nvalue;
        if ((this.fFacetsDefined & 8) != 0) {
            for (int idx = this.fPattern.size() - 1; idx >= 0; idx--) {
                RegularExpression regex = (RegularExpression) this.fPattern.elementAt(idx);
                if (!regex.matches(nvalue2)) {
                    throw new InvalidDatatypeValueException("cvc-pattern-valid", new Object[]{content, this.fPatternStr.elementAt(idx), this.fTypeName});
                }
            }
        }
        int idx2 = this.fVariety;
        if (idx2 == 1) {
            if (this.fPatternType != 0) {
                boolean seenErr = false;
                if (this.fPatternType == 1) {
                    seenErr = !XMLChar.isValidNmtoken(nvalue2);
                } else if (this.fPatternType == 2) {
                    seenErr = !XMLChar.isValidName(nvalue2);
                } else if (this.fPatternType == 3) {
                    seenErr = !XMLChar.isValidNCName(nvalue2);
                }
                if (seenErr) {
                    throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{nvalue2, SPECIAL_PATTERN_STRING[this.fPatternType]});
                }
            }
            validatedInfo.normalizedValue = nvalue2;
            Object avalue = this.fDVs[this.fValidationDV].getActualValue(nvalue2, context);
            validatedInfo.actualValue = avalue;
            validatedInfo.actualValueType = this.fBuiltInKind;
            validatedInfo.actualType = this;
            return avalue;
        }
        if (this.fVariety == 2) {
            StringTokenizer parsedList = new StringTokenizer(nvalue2, " ");
            int countOfTokens = parsedList.countTokens();
            Object[] avalue2 = new Object[countOfTokens];
            boolean isUnion = this.fItemType.getVariety() == 3;
            short[] itemTypes = new short[isUnion ? countOfTokens : 1];
            if (!isUnion) {
                itemTypes[0] = this.fItemType.fBuiltInKind;
            }
            XSSimpleTypeDecl[] memberTypes = new XSSimpleTypeDecl[countOfTokens];
            for (int i = 0; i < countOfTokens; i++) {
                avalue2[i] = this.fItemType.getActualValue(parsedList.nextToken(), context, validatedInfo, false);
                if (context.needFacetChecking() && this.fItemType.fFacetsDefined != 0 && this.fItemType.fFacetsDefined != 16) {
                    this.fItemType.checkFacets(validatedInfo);
                }
                memberTypes[i] = (XSSimpleTypeDecl) validatedInfo.memberType;
                if (isUnion) {
                    itemTypes[i] = memberTypes[i].fBuiltInKind;
                }
            }
            ListDV.ListData v = new ListDV.ListData(avalue2);
            validatedInfo.actualValue = v;
            validatedInfo.actualValueType = isUnion ? (short) 43 : (short) 44;
            validatedInfo.memberType = null;
            validatedInfo.memberTypes = memberTypes;
            validatedInfo.itemValueTypes = new ShortListImpl(itemTypes, itemTypes.length);
            validatedInfo.normalizedValue = nvalue2;
            validatedInfo.actualType = this;
            return v;
        }
        Object _content = (this.fMemberTypes.length <= 1 || content == null) ? content : content.toString();
        for (int i2 = 0; i2 < this.fMemberTypes.length; i2++) {
            try {
                Object aValue = this.fMemberTypes[i2].getActualValue(_content, context, validatedInfo, true);
                if (context.needFacetChecking() && this.fMemberTypes[i2].fFacetsDefined != 0) {
                    if (this.fMemberTypes[i2].fFacetsDefined != 16) {
                        try {
                            this.fMemberTypes[i2].checkFacets(validatedInfo);
                        } catch (InvalidDatatypeValueException e) {
                        }
                    }
                }
                validatedInfo.memberType = this.fMemberTypes[i2];
                validatedInfo.actualType = this;
                return aValue;
            } catch (InvalidDatatypeValueException e2) {
            }
        }
        StringBuffer typesBuffer = new StringBuffer();
        for (int i3 = 0; i3 < this.fMemberTypes.length; i3++) {
            if (i3 != 0) {
                typesBuffer.append(" | ");
            }
            XSSimpleTypeDecl decl = this.fMemberTypes[i3];
            if (decl.fTargetNamespace != null) {
                typesBuffer.append('{');
                typesBuffer.append(decl.fTargetNamespace);
                typesBuffer.append('}');
            }
            typesBuffer.append(decl.fTypeName);
            if (decl.fEnumeration != null) {
                typesBuffer.append(" : ");
                decl.appendEnumString(typesBuffer);
            }
        }
        throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.3", new Object[]{content, this.fTypeName, typesBuffer.toString()});
    }

    @Override
    public boolean isEqual(Object value1, Object value2) {
        if (value1 == null) {
            return false;
        }
        return value1.equals(value2);
    }

    public boolean isIdentical(Object value1, Object value2) {
        if (value1 == null) {
            return false;
        }
        return this.fDVs[this.fValidationDV].isIdentical(value1, value2);
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

    protected String normalize(Object obj, short ws) {
        int j;
        char ch;
        if (obj == 0) {
            return null;
        }
        if ((this.fFacetsDefined & 8) == 0) {
            short norm_type = fDVNormalizeType[this.fValidationDV];
            if (norm_type == 0) {
                return obj.toString();
            }
            if (norm_type == 1) {
                return XMLChar.trim(obj.toString());
            }
        }
        if (!(obj instanceof StringBuffer)) {
            String strContent = obj.toString();
            return normalize(strContent, ws);
        }
        int len = obj.length();
        if (len == 0) {
            return "";
        }
        if (ws == 0) {
            return obj.toString();
        }
        if (ws == 1) {
            for (int i = 0; i < len; i++) {
                char ch2 = obj.charAt(i);
                if (ch2 == '\t' || ch2 == '\n' || ch2 == '\r') {
                    obj.setCharAt(i, ' ');
                }
            }
        } else {
            int j2 = 0;
            boolean isLeading = true;
            int i2 = 0;
            while (i2 < len) {
                char ch3 = obj.charAt(i2);
                if (ch3 != '\t' && ch3 != '\n' && ch3 != '\r' && ch3 != ' ') {
                    j = j2 + 1;
                    obj.setCharAt(j2, ch3);
                    isLeading = false;
                } else {
                    while (i2 < len - 1 && ((ch = obj.charAt(i2 + 1)) == '\t' || ch == '\n' || ch == '\r' || ch == ' ')) {
                        i2++;
                    }
                    if (i2 >= len - 1 || isLeading) {
                        i2++;
                    } else {
                        j = j2 + 1;
                        obj.setCharAt(j2, ' ');
                    }
                }
                j2 = j;
                i2++;
            }
            obj.setLength(j2);
        }
        return obj.toString();
    }

    void reportError(String key, Object[] args) throws InvalidDatatypeFacetException {
        throw new InvalidDatatypeFacetException(key, args);
    }

    private String whiteSpaceValue(short ws) {
        return WS_FACET_STRING[ws];
    }

    @Override
    public short getOrdered() {
        return this.fOrdered;
    }

    @Override
    public boolean getBounded() {
        return this.fBounded;
    }

    @Override
    public boolean getFinite() {
        return this.fFinite;
    }

    @Override
    public boolean getNumeric() {
        return this.fNumeric;
    }

    @Override
    public boolean isDefinedFacet(short facetName) {
        if (this.fValidationDV == 0 || this.fValidationDV == 29) {
            return false;
        }
        if ((this.fFacetsDefined & facetName) != 0) {
            return true;
        }
        if (this.fPatternType != 0) {
            return facetName == 8;
        }
        if (this.fValidationDV == 24) {
            return facetName == 8 || facetName == 1024;
        }
        return false;
    }

    @Override
    public short getDefinedFacets() {
        if (this.fValidationDV == 0 || this.fValidationDV == 29) {
            return (short) 0;
        }
        if (this.fPatternType != 0) {
            return (short) (this.fFacetsDefined | 8);
        }
        if (this.fValidationDV == 24) {
            return (short) (this.fFacetsDefined | 8 | SoOperater.STEP);
        }
        return this.fFacetsDefined;
    }

    @Override
    public boolean isFixedFacet(short facetName) {
        if ((this.fFixedFacet & facetName) != 0) {
            return true;
        }
        return this.fValidationDV == 24 && facetName == 1024;
    }

    @Override
    public short getFixedFacets() {
        if (this.fValidationDV == 24) {
            return (short) (this.fFixedFacet | XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
        }
        return this.fFixedFacet;
    }

    @Override
    public String getLexicalFacetValue(short facetName) {
        if (facetName == 4) {
            if (this.fMaxLength == -1) {
                return null;
            }
            return Integer.toString(this.fMaxLength);
        }
        if (facetName == 16) {
            if (this.fValidationDV == 0 || this.fValidationDV == 29) {
                return null;
            }
            return WS_FACET_STRING[this.fWhiteSpace];
        }
        if (facetName == 32) {
            if (this.fMaxInclusive == null) {
                return null;
            }
            return this.fMaxInclusive.toString();
        }
        if (facetName == 64) {
            if (this.fMaxExclusive == null) {
                return null;
            }
            return this.fMaxExclusive.toString();
        }
        if (facetName == 128) {
            if (this.fMinExclusive == null) {
                return null;
            }
            return this.fMinExclusive.toString();
        }
        if (facetName == 256) {
            if (this.fMinInclusive == null) {
                return null;
            }
            return this.fMinInclusive.toString();
        }
        if (facetName == 512) {
            if (this.fTotalDigits == -1) {
                return null;
            }
            return Integer.toString(this.fTotalDigits);
        }
        if (facetName != 1024) {
            switch (facetName) {
                case 1:
                    if (this.fLength == -1) {
                        return null;
                    }
                    return Integer.toString(this.fLength);
                case 2:
                    if (this.fMinLength == -1) {
                        return null;
                    }
                    return Integer.toString(this.fMinLength);
                default:
                    return null;
            }
        }
        if (this.fValidationDV == 24) {
            return SchemaSymbols.ATTVAL_FALSE_0;
        }
        if (this.fFractionDigits == -1) {
            return null;
        }
        return Integer.toString(this.fFractionDigits);
    }

    @Override
    public StringList getLexicalEnumeration() {
        if (this.fLexicalEnumeration == null) {
            if (this.fEnumeration == null) {
                return StringListImpl.EMPTY_LIST;
            }
            int size = this.fEnumerationSize;
            String[] strs = new String[size];
            for (int i = 0; i < size; i++) {
                strs[i] = this.fEnumeration[i].normalizedValue;
            }
            this.fLexicalEnumeration = new StringListImpl(strs, size);
        }
        return this.fLexicalEnumeration;
    }

    public ObjectList getActualEnumeration() {
        if (this.fActualEnumeration == null) {
            this.fActualEnumeration = new AbstractObjectList() {
                @Override
                public int getLength() {
                    if (XSSimpleTypeDecl.this.fEnumeration != null) {
                        return XSSimpleTypeDecl.this.fEnumerationSize;
                    }
                    return 0;
                }

                @Override
                public boolean contains(Object item) {
                    if (XSSimpleTypeDecl.this.fEnumeration == null) {
                        return false;
                    }
                    for (int i = 0; i < XSSimpleTypeDecl.this.fEnumerationSize; i++) {
                        if (XSSimpleTypeDecl.this.fEnumeration[i].getActualValue().equals(item)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public Object item(int index) {
                    if (index >= 0 && index < getLength()) {
                        return XSSimpleTypeDecl.this.fEnumeration[index].getActualValue();
                    }
                    return null;
                }
            };
        }
        return this.fActualEnumeration;
    }

    public ObjectList getEnumerationItemTypeList() {
        if (this.fEnumerationItemTypeList == null) {
            if (this.fEnumeration == null) {
                return null;
            }
            this.fEnumerationItemTypeList = new AbstractObjectList() {
                @Override
                public int getLength() {
                    if (XSSimpleTypeDecl.this.fEnumeration != null) {
                        return XSSimpleTypeDecl.this.fEnumerationSize;
                    }
                    return 0;
                }

                @Override
                public boolean contains(Object item) {
                    if (XSSimpleTypeDecl.this.fEnumeration == null || !(item instanceof ShortList)) {
                        return false;
                    }
                    for (int i = 0; i < XSSimpleTypeDecl.this.fEnumerationSize; i++) {
                        if (XSSimpleTypeDecl.this.fEnumeration[i].itemValueTypes == item) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public Object item(int index) {
                    if (index >= 0 && index < getLength()) {
                        return XSSimpleTypeDecl.this.fEnumeration[index].itemValueTypes;
                    }
                    return null;
                }
            };
        }
        return this.fEnumerationItemTypeList;
    }

    public ShortList getEnumerationTypeList() {
        if (this.fEnumerationTypeList == null) {
            if (this.fEnumeration == null) {
                return ShortListImpl.EMPTY_LIST;
            }
            short[] list = new short[this.fEnumerationSize];
            for (int i = 0; i < this.fEnumerationSize; i++) {
                list[i] = this.fEnumeration[i].actualValueType;
            }
            this.fEnumerationTypeList = new ShortListImpl(list, this.fEnumerationSize);
        }
        return this.fEnumerationTypeList;
    }

    @Override
    public StringList getLexicalPattern() {
        String[] strs;
        if (this.fPatternType == 0 && this.fValidationDV != 24 && this.fPatternStr == null) {
            return StringListImpl.EMPTY_LIST;
        }
        if (this.fLexicalPattern == null) {
            int size = this.fPatternStr == null ? 0 : this.fPatternStr.size();
            if (this.fPatternType == 1) {
                strs = new String[size + 1];
                strs[size] = "\\c+";
            } else if (this.fPatternType == 2) {
                strs = new String[size + 1];
                strs[size] = "\\i\\c*";
            } else if (this.fPatternType == 3) {
                strs = new String[size + 2];
                strs[size] = "\\i\\c*";
                strs[size + 1] = "[\\i-[:]][\\c-[:]]*";
            } else if (this.fValidationDV == 24) {
                strs = new String[size + 1];
                strs[size] = "[\\-+]?[0-9]+";
            } else {
                strs = new String[size];
            }
            for (int i = 0; i < size; i++) {
                strs[i] = (String) this.fPatternStr.elementAt(i);
            }
            this.fLexicalPattern = new StringListImpl(strs, strs.length);
        }
        return this.fLexicalPattern;
    }

    @Override
    public XSObjectList getAnnotations() {
        return this.fAnnotations != null ? this.fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }

    private void calcFundamentalFacets() {
        setOrdered();
        setNumeric();
        setBounded();
        setCardinality();
    }

    private void setOrdered() {
        if (this.fVariety == 1) {
            this.fOrdered = this.fBase.fOrdered;
            return;
        }
        if (this.fVariety == 2) {
            this.fOrdered = (short) 0;
            return;
        }
        if (this.fVariety == 3) {
            int length = this.fMemberTypes.length;
            if (length == 0) {
                this.fOrdered = (short) 1;
                return;
            }
            short ancestorId = getPrimitiveDV(this.fMemberTypes[0].fValidationDV);
            boolean commonAnc = ancestorId != 0;
            boolean allFalse = this.fMemberTypes[0].fOrdered == 0;
            for (int i = 1; i < this.fMemberTypes.length && (commonAnc || allFalse); i++) {
                if (commonAnc) {
                    commonAnc = ancestorId == getPrimitiveDV(this.fMemberTypes[i].fValidationDV);
                }
                if (allFalse) {
                    allFalse = this.fMemberTypes[i].fOrdered == 0;
                }
            }
            if (commonAnc) {
                this.fOrdered = this.fMemberTypes[0].fOrdered;
            } else if (allFalse) {
                this.fOrdered = (short) 0;
            } else {
                this.fOrdered = (short) 1;
            }
        }
    }

    private void setNumeric() {
        if (this.fVariety == 1) {
            this.fNumeric = this.fBase.fNumeric;
            return;
        }
        if (this.fVariety == 2) {
            this.fNumeric = false;
            return;
        }
        if (this.fVariety == 3) {
            XSSimpleType[] memberTypes = this.fMemberTypes;
            for (XSSimpleType xSSimpleType : memberTypes) {
                if (!xSSimpleType.getNumeric()) {
                    this.fNumeric = false;
                    return;
                }
            }
            this.fNumeric = true;
        }
    }

    private void setBounded() {
        if (this.fVariety == 1) {
            if (((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 || (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0) && ((this.fFacetsDefined & 32) != 0 || (this.fFacetsDefined & 64) != 0)) {
                this.fBounded = true;
                return;
            } else {
                this.fBounded = false;
                return;
            }
        }
        if (this.fVariety == 2) {
            if ((this.fFacetsDefined & 1) != 0 || ((this.fFacetsDefined & 2) != 0 && (this.fFacetsDefined & 4) != 0)) {
                this.fBounded = true;
                return;
            } else {
                this.fBounded = false;
                return;
            }
        }
        if (this.fVariety == 3) {
            XSSimpleTypeDecl[] memberTypes = this.fMemberTypes;
            short ancestorId = 0;
            if (memberTypes.length > 0) {
                ancestorId = getPrimitiveDV(memberTypes[0].fValidationDV);
            }
            for (int i = 0; i < memberTypes.length; i++) {
                if (!memberTypes[i].getBounded() || ancestorId != getPrimitiveDV(memberTypes[i].fValidationDV)) {
                    this.fBounded = false;
                    return;
                }
            }
            this.fBounded = true;
        }
    }

    private boolean specialCardinalityCheck() {
        if (this.fBase.fValidationDV == 9 || this.fBase.fValidationDV == 10 || this.fBase.fValidationDV == 11 || this.fBase.fValidationDV == 12 || this.fBase.fValidationDV == 13 || this.fBase.fValidationDV == 14) {
            return true;
        }
        return false;
    }

    private void setCardinality() {
        if (this.fVariety == 1) {
            if (this.fBase.fFinite) {
                this.fFinite = true;
                return;
            }
            if ((this.fFacetsDefined & 1) != 0 || (this.fFacetsDefined & 4) != 0 || (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0) {
                this.fFinite = true;
                return;
            }
            if (((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 || (this.fFacetsDefined & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0) && ((this.fFacetsDefined & 32) != 0 || (this.fFacetsDefined & 64) != 0)) {
                if ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0 || specialCardinalityCheck()) {
                    this.fFinite = true;
                    return;
                } else {
                    this.fFinite = false;
                    return;
                }
            }
            this.fFinite = false;
            return;
        }
        if (this.fVariety == 2) {
            if ((this.fFacetsDefined & 1) != 0 || ((this.fFacetsDefined & 2) != 0 && (this.fFacetsDefined & 4) != 0)) {
                this.fFinite = true;
                return;
            } else {
                this.fFinite = false;
                return;
            }
        }
        if (this.fVariety == 3) {
            XSSimpleType[] memberTypes = this.fMemberTypes;
            for (XSSimpleType xSSimpleType : memberTypes) {
                if (!xSSimpleType.getFinite()) {
                    this.fFinite = false;
                    return;
                }
            }
            this.fFinite = true;
        }
    }

    private short getPrimitiveDV(short validationDV) {
        if (validationDV == 21 || validationDV == 22 || validationDV == 23) {
            return (short) 1;
        }
        if (validationDV == 24) {
            return (short) 3;
        }
        return validationDV;
    }

    @Override
    public boolean derivedFromType(XSTypeDefinition ancestor, short derivation) {
        if (ancestor == null) {
            return false;
        }
        while (ancestor instanceof XSSimpleTypeDelegate) {
            ancestor = ancestor.type;
        }
        if (ancestor.getBaseType() == ancestor) {
            return true;
        }
        XSTypeDefinition type = this;
        while (type != ancestor && type != fAnySimpleType) {
            type = type.getBaseType();
        }
        return type == ancestor;
    }

    @Override
    public boolean derivedFrom(String ancestorNS, String ancestorName, short derivation) {
        if (ancestorName == null) {
            return false;
        }
        if ("http://www.w3.org/2001/XMLSchema".equals(ancestorNS) && "anyType".equals(ancestorName)) {
            return true;
        }
        XSTypeDefinition type = this;
        while (true) {
            if ((ancestorName.equals(type.getName()) && ((ancestorNS == null && type.getNamespace() == null) || (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) || type == fAnySimpleType) {
                break;
            }
            type = type.getBaseType();
        }
        return type != fAnySimpleType;
    }

    public boolean isDOMDerivedFrom(String ancestorNS, String ancestorName, int derivationMethod) {
        if (ancestorName == null) {
            return false;
        }
        if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(ancestorNS) && "anyType".equals(ancestorName) && ((derivationMethod & 1) != 0 || derivationMethod == 0)) {
            return true;
        }
        if ((derivationMethod & 1) != 0 && isDerivedByRestriction(ancestorNS, ancestorName, this)) {
            return true;
        }
        if ((derivationMethod & 8) != 0 && isDerivedByList(ancestorNS, ancestorName, this)) {
            return true;
        }
        if ((derivationMethod & 4) != 0 && isDerivedByUnion(ancestorNS, ancestorName, this)) {
            return true;
        }
        if (((derivationMethod & 2) != 0 && (derivationMethod & 1) == 0 && (derivationMethod & 8) == 0 && (derivationMethod & 4) == 0) || (derivationMethod & 2) != 0 || (derivationMethod & 1) != 0 || (derivationMethod & 8) != 0 || (derivationMethod & 4) != 0) {
            return false;
        }
        return isDerivedByAny(ancestorNS, ancestorName, this);
    }

    private boolean isDerivedByAny(String ancestorNS, String ancestorName, XSTypeDefinition type) {
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType) {
            if ((ancestorName.equals(type.getName()) && ((ancestorNS == null && type.getNamespace() == null) || (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) || isDerivedByRestriction(ancestorNS, ancestorName, type) || isDerivedByList(ancestorNS, ancestorName, type) || isDerivedByUnion(ancestorNS, ancestorName, type)) {
                return true;
            }
            oldType = type;
            if (((XSSimpleTypeDecl) type).getVariety() == 0 || ((XSSimpleTypeDecl) type).getVariety() == 1) {
                type = type.getBaseType();
            } else if (((XSSimpleTypeDecl) type).getVariety() == 3) {
                if (0 < ((XSSimpleTypeDecl) type).getMemberTypes().getLength()) {
                    return isDerivedByAny(ancestorNS, ancestorName, (XSTypeDefinition) ((XSSimpleTypeDecl) type).getMemberTypes().item(0));
                }
            } else if (((XSSimpleTypeDecl) type).getVariety() == 2) {
                type = ((XSSimpleTypeDecl) type).getItemType();
            }
        }
        return false;
    }

    private boolean isDerivedByRestriction(String ancestorNS, String ancestorName, XSTypeDefinition type) {
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType) {
            if (ancestorName.equals(type.getName())) {
                if (ancestorNS == null || !ancestorNS.equals(type.getNamespace())) {
                    if (type.getNamespace() == null && ancestorNS == null) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            oldType = type;
            type = type.getBaseType();
        }
        return false;
    }

    private boolean isDerivedByList(String ancestorNS, String ancestorName, XSTypeDefinition type) {
        XSTypeDefinition itemType;
        if (type != null && ((XSSimpleTypeDefinition) type).getVariety() == 2 && (itemType = ((XSSimpleTypeDefinition) type).getItemType()) != null && isDerivedByRestriction(ancestorNS, ancestorName, itemType)) {
            return true;
        }
        return false;
    }

    private boolean isDerivedByUnion(String ancestorNS, String ancestorName, XSTypeDefinition type) {
        if (type != null && ((XSSimpleTypeDefinition) type).getVariety() == 3) {
            XSObjectList memberTypes = ((XSSimpleTypeDefinition) type).getMemberTypes();
            for (int i = 0; i < memberTypes.getLength(); i++) {
                if (memberTypes.item(i) != null && isDerivedByRestriction(ancestorNS, ancestorName, (XSSimpleTypeDefinition) memberTypes.item(i))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    static final class ValidationContextImpl implements ValidationContext {
        final ValidationContext fExternal;
        NamespaceContext fNSContext;

        ValidationContextImpl(ValidationContext external) {
            this.fExternal = external;
        }

        void setNSContext(NamespaceContext nsContext) {
            this.fNSContext = nsContext;
        }

        @Override
        public boolean needFacetChecking() {
            return this.fExternal.needFacetChecking();
        }

        @Override
        public boolean needExtraChecking() {
            return this.fExternal.needExtraChecking();
        }

        @Override
        public boolean needToNormalize() {
            return this.fExternal.needToNormalize();
        }

        @Override
        public boolean useNamespaces() {
            return true;
        }

        @Override
        public boolean isEntityDeclared(String name) {
            return this.fExternal.isEntityDeclared(name);
        }

        @Override
        public boolean isEntityUnparsed(String name) {
            return this.fExternal.isEntityUnparsed(name);
        }

        @Override
        public boolean isIdDeclared(String name) {
            return this.fExternal.isIdDeclared(name);
        }

        @Override
        public void addId(String name) {
            this.fExternal.addId(name);
        }

        @Override
        public void addIdRef(String name) {
            this.fExternal.addIdRef(name);
        }

        @Override
        public String getSymbol(String symbol) {
            return this.fExternal.getSymbol(symbol);
        }

        @Override
        public String getURI(String prefix) {
            if (this.fNSContext == null) {
                return this.fExternal.getURI(prefix);
            }
            return this.fNSContext.getURI(prefix);
        }

        @Override
        public Locale getLocale() {
            return this.fExternal.getLocale();
        }
    }

    public void reset() {
        if (this.fIsImmutable) {
            return;
        }
        this.fItemType = null;
        this.fMemberTypes = null;
        this.fTypeName = null;
        this.fTargetNamespace = null;
        this.fFinalSet = (short) 0;
        this.fBase = null;
        this.fVariety = (short) -1;
        this.fValidationDV = (short) -1;
        this.fFacetsDefined = (short) 0;
        this.fFixedFacet = (short) 0;
        this.fWhiteSpace = (short) 0;
        this.fLength = -1;
        this.fMinLength = -1;
        this.fMaxLength = -1;
        this.fTotalDigits = -1;
        this.fFractionDigits = -1;
        this.fPattern = null;
        this.fPatternStr = null;
        this.fEnumeration = null;
        this.fLexicalPattern = null;
        this.fLexicalEnumeration = null;
        this.fActualEnumeration = null;
        this.fEnumerationTypeList = null;
        this.fEnumerationItemTypeList = null;
        this.fMaxInclusive = null;
        this.fMaxExclusive = null;
        this.fMinExclusive = null;
        this.fMinInclusive = null;
        this.lengthAnnotation = null;
        this.minLengthAnnotation = null;
        this.maxLengthAnnotation = null;
        this.whiteSpaceAnnotation = null;
        this.totalDigitsAnnotation = null;
        this.fractionDigitsAnnotation = null;
        this.patternAnnotations = null;
        this.enumerationAnnotations = null;
        this.maxInclusiveAnnotation = null;
        this.maxExclusiveAnnotation = null;
        this.minInclusiveAnnotation = null;
        this.minExclusiveAnnotation = null;
        this.fPatternType = (short) 0;
        this.fAnnotations = null;
        this.fFacets = null;
    }

    @Override
    public XSNamespaceItem getNamespaceItem() {
        return this.fNamespaceItem;
    }

    public void setNamespaceItem(XSNamespaceItem namespaceItem) {
        this.fNamespaceItem = namespaceItem;
    }

    public String toString() {
        return String.valueOf(this.fTargetNamespace) + "," + this.fTypeName;
    }

    @Override
    public XSObjectList getFacets() {
        if (this.fFacets == null && (this.fFacetsDefined != 0 || this.fValidationDV == 24)) {
            XSFacetImpl[] facets = new XSFacetImpl[10];
            int count = 0;
            if ((this.fFacetsDefined & 16) != 0 && this.fValidationDV != 0 && this.fValidationDV != 29) {
                facets[0] = new XSFacetImpl((short) 16, WS_FACET_STRING[this.fWhiteSpace], 0, null, (this.fFixedFacet & 16) != 0, this.whiteSpaceAnnotation);
                count = 0 + 1;
            }
            if (this.fLength != -1) {
                facets[count] = new XSFacetImpl((short) 1, Integer.toString(this.fLength), this.fLength, null, (this.fFixedFacet & 1) != 0, this.lengthAnnotation);
                count++;
            }
            if (this.fMinLength != -1) {
                facets[count] = new XSFacetImpl((short) 2, Integer.toString(this.fMinLength), this.fMinLength, null, (this.fFixedFacet & 2) != 0, this.minLengthAnnotation);
                count++;
            }
            if (this.fMaxLength != -1) {
                facets[count] = new XSFacetImpl((short) 4, Integer.toString(this.fMaxLength), this.fMaxLength, null, (this.fFixedFacet & 4) != 0, this.maxLengthAnnotation);
                count++;
            }
            if (this.fTotalDigits != -1) {
                facets[count] = new XSFacetImpl(XSSimpleTypeDefinition.FACET_TOTALDIGITS, Integer.toString(this.fTotalDigits), this.fTotalDigits, null, (this.fFixedFacet & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0, this.totalDigitsAnnotation);
                count++;
            }
            if (this.fValidationDV == 24) {
                facets[count] = new XSFacetImpl(XSSimpleTypeDefinition.FACET_FRACTIONDIGITS, SchemaSymbols.ATTVAL_FALSE_0, 0, null, true, this.fractionDigitsAnnotation);
                count++;
            } else if (this.fFractionDigits != -1) {
                facets[count] = new XSFacetImpl(XSSimpleTypeDefinition.FACET_FRACTIONDIGITS, Integer.toString(this.fFractionDigits), this.fFractionDigits, null, (this.fFixedFacet & XSSimpleTypeDefinition.FACET_FRACTIONDIGITS) != 0, this.fractionDigitsAnnotation);
                count++;
            }
            if (this.fMaxInclusive != null) {
                facets[count] = new XSFacetImpl((short) 32, this.fMaxInclusive.toString(), 0, this.fMaxInclusive, (this.fFixedFacet & 32) != 0, this.maxInclusiveAnnotation);
                count++;
            }
            if (this.fMaxExclusive != null) {
                facets[count] = new XSFacetImpl((short) 64, this.fMaxExclusive.toString(), 0, this.fMaxExclusive, (this.fFixedFacet & 64) != 0, this.maxExclusiveAnnotation);
                count++;
            }
            if (this.fMinExclusive != null) {
                facets[count] = new XSFacetImpl(XSSimpleTypeDefinition.FACET_MINEXCLUSIVE, this.fMinExclusive.toString(), 0, this.fMinExclusive, (this.fFixedFacet & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) != 0, this.minExclusiveAnnotation);
                count++;
            }
            if (this.fMinInclusive != null) {
                facets[count] = new XSFacetImpl(XSSimpleTypeDefinition.FACET_MININCLUSIVE, this.fMinInclusive.toString(), 0, this.fMinInclusive, (this.fFixedFacet & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0, this.minInclusiveAnnotation);
                count++;
            }
            this.fFacets = count > 0 ? new XSObjectListImpl(facets, count) : XSObjectListImpl.EMPTY_LIST;
        }
        return this.fFacets != null ? this.fFacets : XSObjectListImpl.EMPTY_LIST;
    }

    @Override
    public XSObject getFacet(int facetType) {
        if (facetType == 2048 || facetType == 8) {
            XSObjectList list = getMultiValueFacets();
            for (int i = 0; i < list.getLength(); i++) {
                XSMultiValueFacet f = (XSMultiValueFacet) list.item(i);
                if (f.getFacetKind() == facetType) {
                    return f;
                }
            }
            return null;
        }
        XSObjectList list2 = getFacets();
        for (int i2 = 0; i2 < list2.getLength(); i2++) {
            XSFacet f2 = (XSFacet) list2.item(i2);
            if (f2.getFacetKind() == facetType) {
                return f2;
            }
        }
        return null;
    }

    @Override
    public XSObjectList getMultiValueFacets() {
        if (this.fMultiValueFacets == null && ((this.fFacetsDefined & XSSimpleTypeDefinition.FACET_ENUMERATION) != 0 || (this.fFacetsDefined & 8) != 0 || this.fPatternType != 0 || this.fValidationDV == 24)) {
            XSMVFacetImpl[] facets = new XSMVFacetImpl[2];
            int count = 0;
            if ((this.fFacetsDefined & 8) != 0 || this.fPatternType != 0 || this.fValidationDV == 24) {
                facets[0] = new XSMVFacetImpl((short) 8, getLexicalPattern(), null, this.patternAnnotations);
                count = 0 + 1;
            }
            if (this.fEnumeration != null) {
                facets[count] = new XSMVFacetImpl(XSSimpleTypeDefinition.FACET_ENUMERATION, getLexicalEnumeration(), new ObjectListImpl(this.fEnumeration, this.fEnumerationSize), this.enumerationAnnotations);
                count++;
            }
            this.fMultiValueFacets = new XSObjectListImpl(facets, count);
        }
        return this.fMultiValueFacets != null ? this.fMultiValueFacets : XSObjectListImpl.EMPTY_LIST;
    }

    public Object getMinInclusiveValue() {
        return this.fMinInclusive;
    }

    public Object getMinExclusiveValue() {
        return this.fMinExclusive;
    }

    public Object getMaxInclusiveValue() {
        return this.fMaxInclusive;
    }

    public Object getMaxExclusiveValue() {
        return this.fMaxExclusive;
    }

    public void setAnonymous(boolean anon) {
        this.fAnonymous = anon;
    }

    private static final class XSFacetImpl implements XSFacet {
        final XSObjectList annotations;
        Object avalue;
        final boolean fixed;
        final int ivalue;
        final short kind;
        final String svalue;

        public XSFacetImpl(short kind, String svalue, int ivalue, Object avalue, boolean fixed, XSAnnotation annotation) {
            this.kind = kind;
            this.svalue = svalue;
            this.ivalue = ivalue;
            this.avalue = avalue;
            this.fixed = fixed;
            if (annotation != null) {
                this.annotations = new XSObjectListImpl();
                ((XSObjectListImpl) this.annotations).addXSObject(annotation);
            } else {
                this.annotations = XSObjectListImpl.EMPTY_LIST;
            }
        }

        @Override
        public XSAnnotation getAnnotation() {
            return (XSAnnotation) this.annotations.item(0);
        }

        @Override
        public XSObjectList getAnnotations() {
            return this.annotations;
        }

        @Override
        public short getFacetKind() {
            return this.kind;
        }

        @Override
        public String getLexicalFacetValue() {
            return this.svalue;
        }

        @Override
        public Object getActualFacetValue() {
            if (this.avalue == null) {
                if (this.kind == 16) {
                    this.avalue = this.svalue;
                } else {
                    this.avalue = BigInteger.valueOf(this.ivalue);
                }
            }
            return this.avalue;
        }

        @Override
        public int getIntFacetValue() {
            return this.ivalue;
        }

        @Override
        public boolean getFixed() {
            return this.fixed;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public XSNamespaceItem getNamespaceItem() {
            return null;
        }

        @Override
        public short getType() {
            return (short) 13;
        }
    }

    private static final class XSMVFacetImpl implements XSMultiValueFacet {
        final XSObjectList annotations;
        final ObjectList avalues;
        final short kind;
        final StringList svalues;

        public XSMVFacetImpl(short kind, StringList svalues, ObjectList avalues, XSObjectList annotations) {
            this.kind = kind;
            this.svalues = svalues;
            this.avalues = avalues;
            this.annotations = annotations != null ? annotations : XSObjectListImpl.EMPTY_LIST;
        }

        @Override
        public short getFacetKind() {
            return this.kind;
        }

        @Override
        public XSObjectList getAnnotations() {
            return this.annotations;
        }

        @Override
        public StringList getLexicalFacetValues() {
            return this.svalues;
        }

        @Override
        public ObjectList getEnumerationValues() {
            return this.avalues;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public XSNamespaceItem getNamespaceItem() {
            return null;
        }

        @Override
        public short getType() {
            return (short) 14;
        }
    }

    private static abstract class AbstractObjectList extends AbstractList implements ObjectList {
        private AbstractObjectList() {
        }

        AbstractObjectList(AbstractObjectList abstractObjectList) {
            this();
        }

        @Override
        public Object get(int index) {
            if (index >= 0 && index < getLength()) {
                return item(index);
            }
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        @Override
        public int size() {
            return getLength();
        }
    }

    public String getTypeNamespace() {
        return getNamespace();
    }

    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        return isDOMDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
    }

    private short convertToPrimitiveKind(short valueType) {
        if (valueType <= 20) {
            return valueType;
        }
        if (valueType <= 29) {
            return (short) 2;
        }
        if (valueType <= 42) {
            return (short) 4;
        }
        return valueType;
    }

    private void appendEnumString(StringBuffer sb) {
        sb.append('[');
        for (int i = 0; i < this.fEnumerationSize; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(this.fEnumeration[i].actualValue);
        }
        sb.append(']');
    }
}
