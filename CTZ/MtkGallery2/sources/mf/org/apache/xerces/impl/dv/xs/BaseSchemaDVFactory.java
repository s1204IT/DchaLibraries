package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.SchemaDVFactory;
import mf.org.apache.xerces.impl.dv.XSFacets;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSDeclarationPool;
import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;

public abstract class BaseSchemaDVFactory extends SchemaDVFactory {
    static final String URI_SCHEMAFORSCHEMA = "http://www.w3.org/2001/XMLSchema";
    protected XSDeclarationPool fDeclPool = null;

    protected static void createBuiltInTypes(SymbolHash builtInTypes, XSSimpleTypeDecl baseAtomicType) {
        XSFacets facets = new XSFacets();
        builtInTypes.put(SchemaSymbols.ATTVAL_ANYSIMPLETYPE, XSSimpleTypeDecl.fAnySimpleType);
        XSSimpleTypeDecl stringDV = new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_STRING, (short) 1, (short) 0, false, false, false, true, (short) 2);
        builtInTypes.put(SchemaSymbols.ATTVAL_STRING, stringDV);
        builtInTypes.put(SchemaSymbols.ATTVAL_BOOLEAN, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_BOOLEAN, (short) 2, (short) 0, false, true, false, true, (short) 3));
        XSSimpleTypeDecl decimalDV = new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_DECIMAL, (short) 3, (short) 2, false, false, true, true, (short) 4);
        builtInTypes.put(SchemaSymbols.ATTVAL_DECIMAL, decimalDV);
        builtInTypes.put(SchemaSymbols.ATTVAL_ANYURI, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_ANYURI, (short) 17, (short) 0, false, false, false, true, (short) 18));
        builtInTypes.put(SchemaSymbols.ATTVAL_BASE64BINARY, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_BASE64BINARY, (short) 16, (short) 0, false, false, false, true, (short) 17));
        XSSimpleTypeDecl durationDV = new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_DURATION, (short) 6, (short) 1, false, false, false, true, (short) 7);
        builtInTypes.put(SchemaSymbols.ATTVAL_DURATION, durationDV);
        builtInTypes.put(SchemaSymbols.ATTVAL_DATETIME, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_DATETIME, (short) 7, (short) 1, false, false, false, true, (short) 8));
        builtInTypes.put(SchemaSymbols.ATTVAL_TIME, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_TIME, (short) 8, (short) 1, false, false, false, true, (short) 9));
        builtInTypes.put(SchemaSymbols.ATTVAL_DATE, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_DATE, (short) 9, (short) 1, false, false, false, true, (short) 10));
        builtInTypes.put(SchemaSymbols.ATTVAL_YEARMONTH, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_YEARMONTH, (short) 10, (short) 1, false, false, false, true, (short) 11));
        builtInTypes.put(SchemaSymbols.ATTVAL_YEAR, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_YEAR, (short) 11, (short) 1, false, false, false, true, (short) 12));
        builtInTypes.put(SchemaSymbols.ATTVAL_MONTHDAY, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_MONTHDAY, (short) 12, (short) 1, false, false, false, true, (short) 13));
        builtInTypes.put(SchemaSymbols.ATTVAL_DAY, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_DAY, (short) 13, (short) 1, false, false, false, true, (short) 14));
        builtInTypes.put(SchemaSymbols.ATTVAL_MONTH, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_MONTH, (short) 14, (short) 1, false, false, false, true, (short) 15));
        XSSimpleTypeDecl integerDV = new XSSimpleTypeDecl(decimalDV, SchemaSymbols.ATTVAL_INTEGER, (short) 24, (short) 2, false, false, true, true, (short) 30);
        builtInTypes.put(SchemaSymbols.ATTVAL_INTEGER, integerDV);
        facets.maxInclusive = SchemaSymbols.ATTVAL_FALSE_0;
        XSSimpleTypeDecl nonPositiveDV = new XSSimpleTypeDecl(integerDV, SchemaSymbols.ATTVAL_NONPOSITIVEINTEGER, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 31);
        nonPositiveDV.applyFacets1(facets, (short) 32, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_NONPOSITIVEINTEGER, nonPositiveDV);
        facets.maxInclusive = "-1";
        XSSimpleTypeDecl negativeDV = new XSSimpleTypeDecl(nonPositiveDV, SchemaSymbols.ATTVAL_NEGATIVEINTEGER, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 32);
        negativeDV.applyFacets1(facets, (short) 32, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_NEGATIVEINTEGER, negativeDV);
        facets.maxInclusive = "9223372036854775807";
        facets.minInclusive = "-9223372036854775808";
        XSSimpleTypeDecl longDV = new XSSimpleTypeDecl(integerDV, SchemaSymbols.ATTVAL_LONG, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 33);
        longDV.applyFacets1(facets, (short) 288, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_LONG, longDV);
        facets.maxInclusive = "2147483647";
        facets.minInclusive = "-2147483648";
        XSSimpleTypeDecl intDV = new XSSimpleTypeDecl(longDV, SchemaSymbols.ATTVAL_INT, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 34);
        intDV.applyFacets1(facets, (short) 288, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_INT, intDV);
        facets.maxInclusive = "32767";
        facets.minInclusive = "-32768";
        XSSimpleTypeDecl shortDV = new XSSimpleTypeDecl(intDV, SchemaSymbols.ATTVAL_SHORT, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 35);
        shortDV.applyFacets1(facets, (short) 288, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_SHORT, shortDV);
        facets.maxInclusive = "127";
        facets.minInclusive = "-128";
        XSSimpleTypeDecl byteDV = new XSSimpleTypeDecl(shortDV, SchemaSymbols.ATTVAL_BYTE, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 36);
        byteDV.applyFacets1(facets, (short) 288, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_BYTE, byteDV);
        facets.minInclusive = SchemaSymbols.ATTVAL_FALSE_0;
        XSSimpleTypeDecl nonNegativeDV = new XSSimpleTypeDecl(integerDV, SchemaSymbols.ATTVAL_NONNEGATIVEINTEGER, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 37);
        nonNegativeDV.applyFacets1(facets, XSSimpleTypeDefinition.FACET_MININCLUSIVE, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_NONNEGATIVEINTEGER, nonNegativeDV);
        facets.maxInclusive = "18446744073709551615";
        XSSimpleTypeDecl unsignedLongDV = new XSSimpleTypeDecl(nonNegativeDV, SchemaSymbols.ATTVAL_UNSIGNEDLONG, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 38);
        unsignedLongDV.applyFacets1(facets, (short) 32, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_UNSIGNEDLONG, unsignedLongDV);
        facets.maxInclusive = "4294967295";
        XSSimpleTypeDecl unsignedIntDV = new XSSimpleTypeDecl(unsignedLongDV, SchemaSymbols.ATTVAL_UNSIGNEDINT, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 39);
        unsignedIntDV.applyFacets1(facets, (short) 32, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_UNSIGNEDINT, unsignedIntDV);
        facets.maxInclusive = "65535";
        XSSimpleTypeDecl unsignedShortDV = new XSSimpleTypeDecl(unsignedIntDV, SchemaSymbols.ATTVAL_UNSIGNEDSHORT, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 40);
        unsignedShortDV.applyFacets1(facets, (short) 32, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_UNSIGNEDSHORT, unsignedShortDV);
        facets.maxInclusive = "255";
        XSSimpleTypeDecl unsignedByteDV = new XSSimpleTypeDecl(unsignedShortDV, SchemaSymbols.ATTVAL_UNSIGNEDBYTE, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 41);
        unsignedByteDV.applyFacets1(facets, (short) 32, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_UNSIGNEDBYTE, unsignedByteDV);
        facets.minInclusive = SchemaSymbols.ATTVAL_TRUE_1;
        XSSimpleTypeDecl positiveIntegerDV = new XSSimpleTypeDecl(nonNegativeDV, SchemaSymbols.ATTVAL_POSITIVEINTEGER, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 42);
        positiveIntegerDV.applyFacets1(facets, XSSimpleTypeDefinition.FACET_MININCLUSIVE, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_POSITIVEINTEGER, positiveIntegerDV);
        builtInTypes.put(SchemaSymbols.ATTVAL_FLOAT, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_FLOAT, (short) 4, (short) 1, true, true, true, true, (short) 5));
        builtInTypes.put(SchemaSymbols.ATTVAL_DOUBLE, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_DOUBLE, (short) 5, (short) 1, true, true, true, true, (short) 6));
        builtInTypes.put(SchemaSymbols.ATTVAL_HEXBINARY, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_HEXBINARY, (short) 15, (short) 0, false, false, false, true, (short) 16));
        builtInTypes.put(SchemaSymbols.ATTVAL_NOTATION, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_NOTATION, (short) 20, (short) 0, false, false, false, true, (short) 20));
        facets.whiteSpace = (short) 1;
        XSSimpleTypeDecl normalizedDV = new XSSimpleTypeDecl(stringDV, SchemaSymbols.ATTVAL_NORMALIZEDSTRING, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 21);
        normalizedDV.applyFacets1(facets, (short) 16, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_NORMALIZEDSTRING, normalizedDV);
        facets.whiteSpace = (short) 2;
        XSSimpleTypeDecl tokenDV = new XSSimpleTypeDecl(normalizedDV, SchemaSymbols.ATTVAL_TOKEN, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 22);
        tokenDV.applyFacets1(facets, (short) 16, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_TOKEN, tokenDV);
        facets.whiteSpace = (short) 2;
        facets.pattern = "([a-zA-Z]{1,8})(-[a-zA-Z0-9]{1,8})*";
        XSSimpleTypeDecl languageDV = new XSSimpleTypeDecl(tokenDV, SchemaSymbols.ATTVAL_LANGUAGE, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 23);
        languageDV.applyFacets1(facets, (short) 24, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_LANGUAGE, languageDV);
        facets.whiteSpace = (short) 2;
        XSSimpleTypeDecl nameDV = new XSSimpleTypeDecl(tokenDV, SchemaSymbols.ATTVAL_NAME, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 25);
        nameDV.applyFacets1(facets, (short) 16, (short) 0, (short) 2);
        builtInTypes.put(SchemaSymbols.ATTVAL_NAME, nameDV);
        facets.whiteSpace = (short) 2;
        XSSimpleTypeDecl ncnameDV = new XSSimpleTypeDecl(nameDV, SchemaSymbols.ATTVAL_NCNAME, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 26);
        ncnameDV.applyFacets1(facets, (short) 16, (short) 0, (short) 3);
        builtInTypes.put(SchemaSymbols.ATTVAL_NCNAME, ncnameDV);
        builtInTypes.put(SchemaSymbols.ATTVAL_QNAME, new XSSimpleTypeDecl(baseAtomicType, SchemaSymbols.ATTVAL_QNAME, (short) 18, (short) 0, false, false, false, true, (short) 19));
        builtInTypes.put(SchemaSymbols.ATTVAL_ID, new XSSimpleTypeDecl(ncnameDV, SchemaSymbols.ATTVAL_ID, (short) 21, (short) 0, false, false, false, true, (short) 27));
        XSSimpleTypeDecl idrefDV = new XSSimpleTypeDecl(ncnameDV, SchemaSymbols.ATTVAL_IDREF, (short) 22, (short) 0, false, false, false, true, (short) 28);
        builtInTypes.put(SchemaSymbols.ATTVAL_IDREF, idrefDV);
        facets.minLength = 1;
        XSSimpleTypeDecl tempDV = new XSSimpleTypeDecl((String) null, "http://www.w3.org/2001/XMLSchema", (short) 0, idrefDV, true, (XSObjectList) null);
        XSSimpleTypeDecl idrefsDV = new XSSimpleTypeDecl(tempDV, SchemaSymbols.ATTVAL_IDREFS, "http://www.w3.org/2001/XMLSchema", (short) 0, false, (XSObjectList) null);
        idrefsDV.applyFacets1(facets, (short) 2, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_IDREFS, idrefsDV);
        XSSimpleTypeDecl entityDV = new XSSimpleTypeDecl(ncnameDV, SchemaSymbols.ATTVAL_ENTITY, (short) 23, (short) 0, false, false, false, true, (short) 29);
        builtInTypes.put(SchemaSymbols.ATTVAL_ENTITY, entityDV);
        facets.minLength = 1;
        XSSimpleTypeDecl tempDV2 = new XSSimpleTypeDecl((String) null, "http://www.w3.org/2001/XMLSchema", (short) 0, entityDV, true, (XSObjectList) null);
        XSSimpleTypeDecl entitiesDV = new XSSimpleTypeDecl(tempDV2, SchemaSymbols.ATTVAL_ENTITIES, "http://www.w3.org/2001/XMLSchema", (short) 0, false, (XSObjectList) null);
        entitiesDV.applyFacets1(facets, (short) 2, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_ENTITIES, entitiesDV);
        facets.whiteSpace = (short) 2;
        XSSimpleTypeDecl nmtokenDV = new XSSimpleTypeDecl(tokenDV, SchemaSymbols.ATTVAL_NMTOKEN, "http://www.w3.org/2001/XMLSchema", (short) 0, false, null, (short) 24);
        nmtokenDV.applyFacets1(facets, (short) 16, (short) 0, (short) 1);
        builtInTypes.put(SchemaSymbols.ATTVAL_NMTOKEN, nmtokenDV);
        facets.minLength = 1;
        XSSimpleTypeDecl tempDV3 = new XSSimpleTypeDecl((String) null, "http://www.w3.org/2001/XMLSchema", (short) 0, nmtokenDV, true, (XSObjectList) null);
        XSSimpleTypeDecl nmtokensDV = new XSSimpleTypeDecl(tempDV3, SchemaSymbols.ATTVAL_NMTOKENS, "http://www.w3.org/2001/XMLSchema", (short) 0, false, (XSObjectList) null);
        nmtokensDV.applyFacets1(facets, (short) 2, (short) 0);
        builtInTypes.put(SchemaSymbols.ATTVAL_NMTOKENS, nmtokensDV);
    }

    @Override
    public XSSimpleType createTypeRestriction(String name, String targetNamespace, short finalSet, XSSimpleType base, XSObjectList annotations) {
        if (this.fDeclPool != null) {
            XSSimpleTypeDecl st = this.fDeclPool.getSimpleTypeDecl();
            return st.setRestrictionValues((XSSimpleTypeDecl) base, name, targetNamespace, finalSet, annotations);
        }
        return new XSSimpleTypeDecl((XSSimpleTypeDecl) base, name, targetNamespace, finalSet, false, annotations);
    }

    @Override
    public XSSimpleType createTypeList(String name, String targetNamespace, short finalSet, XSSimpleType itemType, XSObjectList annotations) {
        if (this.fDeclPool != null) {
            XSSimpleTypeDecl st = this.fDeclPool.getSimpleTypeDecl();
            return st.setListValues(name, targetNamespace, finalSet, (XSSimpleTypeDecl) itemType, annotations);
        }
        return new XSSimpleTypeDecl(name, targetNamespace, finalSet, (XSSimpleTypeDecl) itemType, false, annotations);
    }

    @Override
    public XSSimpleType createTypeUnion(String name, String targetNamespace, short finalSet, XSSimpleType[] memberTypes, XSObjectList annotations) {
        int typeNum = memberTypes.length;
        XSSimpleTypeDecl[] mtypes = new XSSimpleTypeDecl[typeNum];
        System.arraycopy(memberTypes, 0, mtypes, 0, typeNum);
        if (this.fDeclPool != null) {
            XSSimpleTypeDecl st = this.fDeclPool.getSimpleTypeDecl();
            return st.setUnionValues(name, targetNamespace, finalSet, mtypes, annotations);
        }
        return new XSSimpleTypeDecl(name, targetNamespace, finalSet, mtypes, annotations);
    }

    public void setDeclPool(XSDeclarationPool declPool) {
        this.fDeclPool = declPool;
    }

    public XSSimpleTypeDecl newXSSimpleTypeDecl() {
        return new XSSimpleTypeDecl();
    }
}
