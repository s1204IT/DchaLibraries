package org.apache.xalan.processor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.AVT;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.StringToIntTable;
import org.apache.xml.utils.StringVector;
import org.apache.xml.utils.XML11Char;
import org.apache.xpath.XPath;
import org.xml.sax.SAXException;

public class XSLTAttributeDef {
    static final int ERROR = 1;
    static final int FATAL = 0;
    static final String S_FOREIGNATTR_SETTER = "setForeignAttr";
    static final int T_AVT = 3;
    static final int T_AVT_QNAME = 18;
    static final int T_CDATA = 1;
    static final int T_CHAR = 6;
    static final int T_ENUM = 11;
    static final int T_ENUM_OR_PQNAME = 16;
    static final int T_EXPR = 5;
    static final int T_NCNAME = 17;
    static final int T_NMTOKEN = 13;
    static final int T_NUMBER = 7;
    static final int T_PATTERN = 4;
    static final int T_PREFIXLIST = 20;
    static final int T_PREFIX_URLLIST = 15;
    static final int T_QNAME = 9;
    static final int T_QNAMES = 10;
    static final int T_QNAMES_RESOLVE_NULL = 19;
    static final int T_SIMPLEPATTERNLIST = 12;
    static final int T_STRINGLIST = 14;
    static final int T_URL = 2;
    static final int T_YESNO = 8;
    static final int WARNING = 2;
    static final XSLTAttributeDef m_foreignAttr = new XSLTAttributeDef("*", "*", 1, false, false, 2);
    private String m_default;
    private StringToIntTable m_enums;
    int m_errorType;
    private String m_name;
    private String m_namespace;
    private boolean m_required;
    String m_setterString;
    private boolean m_supportsAVT;
    private int m_type;

    XSLTAttributeDef(String namespace, String name, int type, boolean required, boolean supportsAVT, int errorType) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = namespace;
        this.m_name = name;
        this.m_type = type;
        this.m_required = required;
        this.m_supportsAVT = supportsAVT;
        this.m_errorType = errorType;
    }

    XSLTAttributeDef(String namespace, String name, int type, boolean supportsAVT, int errorType, String defaultVal) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = namespace;
        this.m_name = name;
        this.m_type = type;
        this.m_required = false;
        this.m_supportsAVT = supportsAVT;
        this.m_errorType = errorType;
        this.m_default = defaultVal;
    }

    XSLTAttributeDef(String namespace, String name, boolean required, boolean supportsAVT, boolean prefixedQNameValAllowed, int errorType, String k1, int v1, String k2, int v2) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = namespace;
        this.m_name = name;
        this.m_type = prefixedQNameValAllowed ? 16 : 11;
        this.m_required = required;
        this.m_supportsAVT = supportsAVT;
        this.m_errorType = errorType;
        this.m_enums = new StringToIntTable(2);
        this.m_enums.put(k1, v1);
        this.m_enums.put(k2, v2);
    }

    XSLTAttributeDef(String namespace, String name, boolean required, boolean supportsAVT, boolean prefixedQNameValAllowed, int errorType, String k1, int v1, String k2, int v2, String k3, int v3) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = namespace;
        this.m_name = name;
        this.m_type = prefixedQNameValAllowed ? 16 : 11;
        this.m_required = required;
        this.m_supportsAVT = supportsAVT;
        this.m_errorType = errorType;
        this.m_enums = new StringToIntTable(3);
        this.m_enums.put(k1, v1);
        this.m_enums.put(k2, v2);
        this.m_enums.put(k3, v3);
    }

    XSLTAttributeDef(String namespace, String name, boolean required, boolean supportsAVT, boolean prefixedQNameValAllowed, int errorType, String k1, int v1, String k2, int v2, String k3, int v3, String k4, int v4) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = namespace;
        this.m_name = name;
        this.m_type = prefixedQNameValAllowed ? 16 : 11;
        this.m_required = required;
        this.m_supportsAVT = supportsAVT;
        this.m_errorType = errorType;
        this.m_enums = new StringToIntTable(4);
        this.m_enums.put(k1, v1);
        this.m_enums.put(k2, v2);
        this.m_enums.put(k3, v3);
        this.m_enums.put(k4, v4);
    }

    String getNamespace() {
        return this.m_namespace;
    }

    String getName() {
        return this.m_name;
    }

    int getType() {
        return this.m_type;
    }

    private int getEnum(String key) {
        return this.m_enums.get(key);
    }

    private String[] getEnumNames() {
        return this.m_enums.keys();
    }

    String getDefault() {
        return this.m_default;
    }

    void setDefault(String def) {
        this.m_default = def;
    }

    boolean getRequired() {
        return this.m_required;
    }

    boolean getSupportsAVT() {
        return this.m_supportsAVT;
    }

    int getErrorType() {
        return this.m_errorType;
    }

    public String getSetterMethodName() {
        if (this.m_setterString == null) {
            if (m_foreignAttr == this) {
                return S_FOREIGNATTR_SETTER;
            }
            if (this.m_name.equals("*")) {
                this.m_setterString = "addLiteralResultAttribute";
                return this.m_setterString;
            }
            StringBuffer outBuf = new StringBuffer();
            outBuf.append("set");
            if (this.m_namespace != null && this.m_namespace.equals("http://www.w3.org/XML/1998/namespace")) {
                outBuf.append("Xml");
            }
            int n = this.m_name.length();
            int i = 0;
            while (i < n) {
                char c = this.m_name.charAt(i);
                if ('-' == c) {
                    i++;
                    c = Character.toUpperCase(this.m_name.charAt(i));
                } else if (i == 0) {
                    c = Character.toUpperCase(c);
                }
                outBuf.append(c);
                i++;
            }
            this.m_setterString = outBuf.toString();
        }
        return this.m_setterString;
    }

    AVT processAVT(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        try {
            AVT avt = new AVT(handler, uri, name, rawName, value, owner);
            return avt;
        } catch (TransformerException te) {
            throw new SAXException(te);
        }
    }

    Object processCDATA(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        if (!getSupportsAVT()) {
            return value;
        }
        try {
            return new AVT(handler, uri, name, rawName, value, owner);
        } catch (TransformerException te) {
            throw new SAXException(te);
        }
    }

    Object processCHAR(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(handler, uri, name, rawName, value, owner);
                if (avt.isSimple() && value.length() != 1) {
                    handleError(handler, XSLTErrorResources.INVALID_TCHAR, new Object[]{name, value}, null);
                    return null;
                }
                return avt;
            } catch (TransformerException te) {
                throw new SAXException(te);
            }
        }
        if (value.length() != 1) {
            handleError(handler, XSLTErrorResources.INVALID_TCHAR, new Object[]{name, value}, null);
            return null;
        }
        return new Character(value.charAt(0));
    }

    Object processENUM(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        AVT avt;
        if (!getSupportsAVT()) {
            avt = null;
        } else {
            try {
                avt = new AVT(handler, uri, name, rawName, value, owner);
            } catch (TransformerException e) {
                te = e;
            }
            try {
                if (!avt.isSimple()) {
                    return avt;
                }
            } catch (TransformerException e2) {
                te = e2;
                throw new SAXException(te);
            }
        }
        int retVal = getEnum(value);
        if (retVal != -10000) {
            return getSupportsAVT() ? avt : new Integer(retVal);
        }
        StringBuffer enumNamesList = getListOfEnums();
        handleError(handler, XSLTErrorResources.INVALID_ENUM, new Object[]{name, value, enumNamesList.toString()}, null);
        return null;
    }

    Object processENUM_OR_PQNAME(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        Object objToReturn = null;
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(handler, uri, name, rawName, value, owner);
                if (avt.isSimple()) {
                    objToReturn = avt;
                } else {
                    return avt;
                }
            } catch (TransformerException te) {
                throw new SAXException(te);
            }
        }
        int key = getEnum(value);
        if (key != -10000) {
            if (objToReturn == null) {
                objToReturn = new Integer(key);
            }
        } else {
            try {
                QName qname = new QName(value, (PrefixResolver) handler, true);
                if (objToReturn == null) {
                    objToReturn = qname;
                }
                if (qname.getPrefix() == null) {
                    StringBuffer enumNamesList = getListOfEnums();
                    enumNamesList.append(" <qname-but-not-ncname>");
                    handleError(handler, XSLTErrorResources.INVALID_ENUM, new Object[]{name, value, enumNamesList.toString()}, null);
                    return null;
                }
            } catch (IllegalArgumentException ie) {
                StringBuffer enumNamesList2 = getListOfEnums();
                enumNamesList2.append(" <qname-but-not-ncname>");
                handleError(handler, XSLTErrorResources.INVALID_ENUM, new Object[]{name, value, enumNamesList2.toString()}, ie);
                return null;
            } catch (RuntimeException re) {
                StringBuffer enumNamesList3 = getListOfEnums();
                enumNamesList3.append(" <qname-but-not-ncname>");
                handleError(handler, XSLTErrorResources.INVALID_ENUM, new Object[]{name, value, enumNamesList3.toString()}, re);
                return null;
            }
        }
        return objToReturn;
    }

    Object processEXPR(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        try {
            XPath expr = handler.createXPath(value, owner);
            return expr;
        } catch (TransformerException te) {
            throw new SAXException(te);
        }
    }

    Object processNMTOKEN(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(handler, uri, name, rawName, value, owner);
                if (avt.isSimple() && !XML11Char.isXML11ValidNmtoken(value)) {
                    handleError(handler, XSLTErrorResources.INVALID_NMTOKEN, new Object[]{name, value}, null);
                    return null;
                }
                return avt;
            } catch (TransformerException te) {
                throw new SAXException(te);
            }
        }
        if (XML11Char.isXML11ValidNmtoken(value)) {
            return value;
        }
        handleError(handler, XSLTErrorResources.INVALID_NMTOKEN, new Object[]{name, value}, null);
        return null;
    }

    Object processPATTERN(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        try {
            XPath pattern = handler.createMatchPatternXPath(value, owner);
            return pattern;
        } catch (TransformerException te) {
            throw new SAXException(te);
        }
    }

    Object processNUMBER(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        AVT avt;
        if (getSupportsAVT()) {
            try {
                avt = new AVT(handler, uri, name, rawName, value, owner);
            } catch (NumberFormatException e) {
                nfe = e;
            } catch (TransformerException e2) {
                te = e2;
            }
            try {
                if (avt.isSimple()) {
                    Double.valueOf(value);
                    return avt;
                }
                return avt;
            } catch (NumberFormatException e3) {
                nfe = e3;
                handleError(handler, XSLTErrorResources.INVALID_NUMBER, new Object[]{name, value}, nfe);
                return null;
            } catch (TransformerException e4) {
                te = e4;
                throw new SAXException(te);
            }
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException nfe) {
            handleError(handler, XSLTErrorResources.INVALID_NUMBER, new Object[]{name, value}, nfe);
            return null;
        }
    }

    Object processQNAME(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        try {
            return new QName(value, (PrefixResolver) handler, true);
        } catch (IllegalArgumentException ie) {
            handleError(handler, XSLTErrorResources.INVALID_QNAME, new Object[]{name, value}, ie);
            return null;
        } catch (RuntimeException re) {
            handleError(handler, XSLTErrorResources.INVALID_QNAME, new Object[]{name, value}, re);
            return null;
        }
    }

    Object processAVT_QNAME(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        try {
            AVT avt = new AVT(handler, uri, name, rawName, value, owner);
            try {
                if (avt.isSimple()) {
                    int indexOfNSSep = value.indexOf(58);
                    if (indexOfNSSep >= 0) {
                        String prefix = value.substring(0, indexOfNSSep);
                        if (!XML11Char.isXML11ValidNCName(prefix)) {
                            handleError(handler, XSLTErrorResources.INVALID_QNAME, new Object[]{name, value}, null);
                            return null;
                        }
                    }
                    String localName = indexOfNSSep < 0 ? value : value.substring(indexOfNSSep + 1);
                    if (localName == null || localName.length() == 0 || !XML11Char.isXML11ValidNCName(localName)) {
                        handleError(handler, XSLTErrorResources.INVALID_QNAME, new Object[]{name, value}, null);
                        return null;
                    }
                    return avt;
                }
                return avt;
            } catch (TransformerException e) {
                te = e;
                throw new SAXException(te);
            }
        } catch (TransformerException e2) {
            te = e2;
        }
    }

    Object processNCNAME(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        AVT avt;
        if (getSupportsAVT()) {
            try {
                avt = new AVT(handler, uri, name, rawName, value, owner);
            } catch (TransformerException e) {
                te = e;
            }
            try {
                if (avt.isSimple() && !XML11Char.isXML11ValidNCName(value)) {
                    handleError(handler, XSLTErrorResources.INVALID_NCNAME, new Object[]{name, value}, null);
                    return null;
                }
                return avt;
            } catch (TransformerException e2) {
                te = e2;
                throw new SAXException(te);
            }
        }
        if (XML11Char.isXML11ValidNCName(value)) {
            return value;
        }
        handleError(handler, XSLTErrorResources.INVALID_NCNAME, new Object[]{name, value}, null);
        return null;
    }

    Vector processQNAMES(StylesheetHandler handler, String uri, String name, String rawName, String value) throws SAXException {
        StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");
        int nQNames = tokenizer.countTokens();
        Vector qnames = new Vector(nQNames);
        for (int i = 0; i < nQNames; i++) {
            qnames.addElement(new QName(tokenizer.nextToken(), handler));
        }
        return qnames;
    }

    final Vector processQNAMESRNU(StylesheetHandler handler, String uri, String name, String rawName, String value) throws SAXException {
        StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");
        int nQNames = tokenizer.countTokens();
        Vector qnames = new Vector(nQNames);
        String defaultURI = handler.getNamespaceForPrefix("");
        for (int i = 0; i < nQNames; i++) {
            String tok = tokenizer.nextToken();
            if (tok.indexOf(58) == -1) {
                qnames.addElement(new QName(defaultURI, tok));
            } else {
                qnames.addElement(new QName(tok, handler));
            }
        }
        return qnames;
    }

    Vector processSIMPLEPATTERNLIST(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        try {
            StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");
            int nPatterns = tokenizer.countTokens();
            Vector patterns = new Vector(nPatterns);
            for (int i = 0; i < nPatterns; i++) {
                XPath pattern = handler.createMatchPatternXPath(tokenizer.nextToken(), owner);
                patterns.addElement(pattern);
            }
            return patterns;
        } catch (TransformerException te) {
            throw new SAXException(te);
        }
    }

    StringVector processSTRINGLIST(StylesheetHandler handler, String uri, String name, String rawName, String value) {
        StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");
        int nStrings = tokenizer.countTokens();
        StringVector strings = new StringVector(nStrings);
        for (int i = 0; i < nStrings; i++) {
            strings.addElement(tokenizer.nextToken());
        }
        return strings;
    }

    StringVector processPREFIX_URLLIST(StylesheetHandler handler, String uri, String name, String rawName, String value) throws SAXException {
        StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");
        int nStrings = tokenizer.countTokens();
        StringVector strings = new StringVector(nStrings);
        for (int i = 0; i < nStrings; i++) {
            String prefix = tokenizer.nextToken();
            String url = handler.getNamespaceForPrefix(prefix);
            if (url != null) {
                strings.addElement(url);
            } else {
                throw new SAXException(XSLMessages.createMessage(XSLTErrorResources.ER_CANT_RESOLVE_NSPREFIX, new Object[]{prefix}));
            }
        }
        return strings;
    }

    StringVector processPREFIX_LIST(StylesheetHandler handler, String uri, String name, String rawName, String value) throws SAXException {
        StringTokenizer tokenizer = new StringTokenizer(value, " \t\n\r\f");
        int nStrings = tokenizer.countTokens();
        StringVector strings = new StringVector(nStrings);
        for (int i = 0; i < nStrings; i++) {
            String prefix = tokenizer.nextToken();
            String url = handler.getNamespaceForPrefix(prefix);
            if (prefix.equals("#default") || url != null) {
                strings.addElement(prefix);
            } else {
                throw new SAXException(XSLMessages.createMessage(XSLTErrorResources.ER_CANT_RESOLVE_NSPREFIX, new Object[]{prefix}));
            }
        }
        return strings;
    }

    Object processURL(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        if (!getSupportsAVT()) {
            return value;
        }
        try {
            return new AVT(handler, uri, name, rawName, value, owner);
        } catch (TransformerException te) {
            throw new SAXException(te);
        }
    }

    private Boolean processYESNO(StylesheetHandler handler, String uri, String name, String rawName, String value) throws SAXException {
        if (value.equals("yes") || value.equals("no")) {
            return new Boolean(value.equals("yes"));
        }
        handleError(handler, XSLTErrorResources.INVALID_BOOLEAN, new Object[]{name, value}, null);
        return null;
    }

    Object processValue(StylesheetHandler handler, String uri, String name, String rawName, String value, ElemTemplateElement owner) throws SAXException {
        int type = getType();
        switch (type) {
            case 1:
                Object processedValue = processCDATA(handler, uri, name, rawName, value, owner);
                return processedValue;
            case 2:
                Object processedValue2 = processURL(handler, uri, name, rawName, value, owner);
                return processedValue2;
            case 3:
                Object processedValue3 = processAVT(handler, uri, name, rawName, value, owner);
                return processedValue3;
            case 4:
                Object processedValue4 = processPATTERN(handler, uri, name, rawName, value, owner);
                return processedValue4;
            case 5:
                Object processedValue5 = processEXPR(handler, uri, name, rawName, value, owner);
                return processedValue5;
            case 6:
                Object processedValue6 = processCHAR(handler, uri, name, rawName, value, owner);
                return processedValue6;
            case 7:
                Object processedValue7 = processNUMBER(handler, uri, name, rawName, value, owner);
                return processedValue7;
            case 8:
                Object processedValue8 = processYESNO(handler, uri, name, rawName, value);
                return processedValue8;
            case 9:
                Object processedValue9 = processQNAME(handler, uri, name, rawName, value, owner);
                return processedValue9;
            case 10:
                Object processedValue10 = processQNAMES(handler, uri, name, rawName, value);
                return processedValue10;
            case 11:
                Object processedValue11 = processENUM(handler, uri, name, rawName, value, owner);
                return processedValue11;
            case 12:
                Object processedValue12 = processSIMPLEPATTERNLIST(handler, uri, name, rawName, value, owner);
                return processedValue12;
            case 13:
                Object processedValue13 = processNMTOKEN(handler, uri, name, rawName, value, owner);
                return processedValue13;
            case 14:
                Object processedValue14 = processSTRINGLIST(handler, uri, name, rawName, value);
                return processedValue14;
            case 15:
                Object processedValue15 = processPREFIX_URLLIST(handler, uri, name, rawName, value);
                return processedValue15;
            case 16:
                Object processedValue16 = processENUM_OR_PQNAME(handler, uri, name, rawName, value, owner);
                return processedValue16;
            case 17:
                Object processedValue17 = processNCNAME(handler, uri, name, rawName, value, owner);
                return processedValue17;
            case 18:
                Object processedValue18 = processAVT_QNAME(handler, uri, name, rawName, value, owner);
                return processedValue18;
            case 19:
                Object processedValue19 = processQNAMESRNU(handler, uri, name, rawName, value);
                return processedValue19;
            case 20:
                Object processedValue20 = processPREFIX_LIST(handler, uri, name, rawName, value);
                return processedValue20;
            default:
                return null;
        }
    }

    void setDefAttrValue(StylesheetHandler handler, ElemTemplateElement elem) throws SAXException {
        setAttrValue(handler, getNamespace(), getName(), getName(), getDefault(), elem);
    }

    private Class getPrimativeClass(Object obj) {
        if (obj instanceof XPath) {
            return XPath.class;
        }
        Class<?> cls = obj.getClass();
        if (cls == Double.class) {
            cls = Double.TYPE;
        }
        if (cls == Float.class) {
            Class cl = Float.TYPE;
            return cl;
        }
        if (cls == Boolean.class) {
            Class cl2 = Boolean.TYPE;
            return cl2;
        }
        if (cls == Byte.class) {
            Class cl3 = Byte.TYPE;
            return cl3;
        }
        if (cls == Character.class) {
            Class cl4 = Character.TYPE;
            return cl4;
        }
        if (cls == Short.class) {
            Class cl5 = Short.TYPE;
            return cl5;
        }
        if (cls == Integer.class) {
            Class cl6 = Integer.TYPE;
            return cl6;
        }
        if (cls == Long.class) {
            Class cl7 = Long.TYPE;
            return cl7;
        }
        return cls;
    }

    private StringBuffer getListOfEnums() {
        StringBuffer enumNamesList = new StringBuffer();
        String[] enumValues = getEnumNames();
        for (int i = 0; i < enumValues.length; i++) {
            if (i > 0) {
                enumNamesList.append(' ');
            }
            enumNamesList.append(enumValues[i]);
        }
        return enumNamesList;
    }

    boolean setAttrValue(StylesheetHandler handler, String attrUri, String attrLocalName, String attrRawName, String attrValue, ElemTemplateElement elem) throws SAXException {
        Method meth;
        Object[] args;
        if (attrRawName.equals("xmlns") || attrRawName.startsWith(Constants.ATTRNAME_XMLNS)) {
            return true;
        }
        String setterString = getSetterMethodName();
        if (setterString != null) {
            try {
                try {
                    if (setterString.equals(S_FOREIGNATTR_SETTER)) {
                        if (attrUri == null) {
                            attrUri = "";
                        }
                        Class<?> cls = attrUri.getClass();
                        meth = elem.getClass().getMethod(setterString, cls, cls, cls, cls);
                        args = new Object[]{attrUri, attrLocalName, attrRawName, attrValue};
                    } else {
                        Object value = processValue(handler, attrUri, attrLocalName, attrRawName, attrValue, elem);
                        if (value == null) {
                            return false;
                        }
                        Class<?>[] clsArr = {getPrimativeClass(value)};
                        try {
                            meth = elem.getClass().getMethod(setterString, clsArr);
                        } catch (NoSuchMethodException e) {
                            clsArr[0] = value.getClass();
                            meth = elem.getClass().getMethod(setterString, clsArr);
                        }
                        args = new Object[]{value};
                    }
                    meth.invoke(elem, args);
                } catch (NoSuchMethodException nsme) {
                    if (!setterString.equals(S_FOREIGNATTR_SETTER)) {
                        handler.error(XSLTErrorResources.ER_FAILED_CALLING_METHOD, new Object[]{setterString}, nsme);
                        return false;
                    }
                }
            } catch (IllegalAccessException iae) {
                handler.error(XSLTErrorResources.ER_FAILED_CALLING_METHOD, new Object[]{setterString}, iae);
                return false;
            } catch (InvocationTargetException nsme2) {
                handleError(handler, XSLTErrorResources.WG_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{"name", getName()}, nsme2);
                return false;
            }
        }
        return true;
    }

    private void handleError(StylesheetHandler handler, String msg, Object[] args, Exception exc) throws SAXException {
        switch (getErrorType()) {
            case 0:
            case 1:
                handler.error(msg, args, exc);
                break;
            case 2:
                handler.warn(msg, args);
                break;
        }
    }
}
