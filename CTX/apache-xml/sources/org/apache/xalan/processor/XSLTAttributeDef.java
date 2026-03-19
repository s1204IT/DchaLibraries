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

    XSLTAttributeDef(String str, String str2, int i, boolean z, boolean z2, int i2) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = str;
        this.m_name = str2;
        this.m_type = i;
        this.m_required = z;
        this.m_supportsAVT = z2;
        this.m_errorType = i2;
    }

    XSLTAttributeDef(String str, String str2, int i, boolean z, int i2, String str3) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = str;
        this.m_name = str2;
        this.m_type = i;
        this.m_required = false;
        this.m_supportsAVT = z;
        this.m_errorType = i2;
        this.m_default = str3;
    }

    XSLTAttributeDef(String str, String str2, boolean z, boolean z2, boolean z3, int i, String str3, int i2, String str4, int i3) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = str;
        this.m_name = str2;
        this.m_type = z3 ? 16 : 11;
        this.m_required = z;
        this.m_supportsAVT = z2;
        this.m_errorType = i;
        this.m_enums = new StringToIntTable(2);
        this.m_enums.put(str3, i2);
        this.m_enums.put(str4, i3);
    }

    XSLTAttributeDef(String str, String str2, boolean z, boolean z2, boolean z3, int i, String str3, int i2, String str4, int i3, String str5, int i4) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = str;
        this.m_name = str2;
        this.m_type = z3 ? 16 : 11;
        this.m_required = z;
        this.m_supportsAVT = z2;
        this.m_errorType = i;
        this.m_enums = new StringToIntTable(3);
        this.m_enums.put(str3, i2);
        this.m_enums.put(str4, i3);
        this.m_enums.put(str5, i4);
    }

    XSLTAttributeDef(String str, String str2, boolean z, boolean z2, boolean z3, int i, String str3, int i2, String str4, int i3, String str5, int i4, String str6, int i5) {
        this.m_errorType = 2;
        this.m_setterString = null;
        this.m_namespace = str;
        this.m_name = str2;
        this.m_type = z3 ? 16 : 11;
        this.m_required = z;
        this.m_supportsAVT = z2;
        this.m_errorType = i;
        this.m_enums = new StringToIntTable(4);
        this.m_enums.put(str3, i2);
        this.m_enums.put(str4, i3);
        this.m_enums.put(str5, i4);
        this.m_enums.put(str6, i5);
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

    private int getEnum(String str) {
        return this.m_enums.get(str);
    }

    private String[] getEnumNames() {
        return this.m_enums.keys();
    }

    String getDefault() {
        return this.m_default;
    }

    void setDefault(String str) {
        this.m_default = str;
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
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("set");
            if (this.m_namespace != null && this.m_namespace.equals("http://www.w3.org/XML/1998/namespace")) {
                stringBuffer.append("Xml");
            }
            int length = this.m_name.length();
            int i = 0;
            while (i < length) {
                char cCharAt = this.m_name.charAt(i);
                if ('-' == cCharAt) {
                    i++;
                    cCharAt = Character.toUpperCase(this.m_name.charAt(i));
                } else if (i == 0) {
                    cCharAt = Character.toUpperCase(cCharAt);
                }
                stringBuffer.append(cCharAt);
                i++;
            }
            this.m_setterString = stringBuffer.toString();
        }
        return this.m_setterString;
    }

    AVT processAVT(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        try {
            return new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    Object processCDATA(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        if (getSupportsAVT()) {
            try {
                return new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        return str4;
    }

    Object processCHAR(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                if (avt.isSimple() && str4.length() != 1) {
                    handleError(stylesheetHandler, XSLTErrorResources.INVALID_TCHAR, new Object[]{str2, str4}, null);
                    return null;
                }
                return avt;
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        if (str4.length() != 1) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_TCHAR, new Object[]{str2, str4}, null);
            return null;
        }
        return new Character(str4.charAt(0));
    }

    Object processENUM(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        AVT avt;
        if (getSupportsAVT()) {
            try {
                avt = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                if (!avt.isSimple()) {
                    return avt;
                }
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        } else {
            avt = null;
        }
        int i = getEnum(str4);
        if (i != -10000) {
            return getSupportsAVT() ? avt : new Integer(i);
        }
        handleError(stylesheetHandler, XSLTErrorResources.INVALID_ENUM, new Object[]{str2, str4, getListOfEnums().toString()}, null);
        return null;
    }

    Object processENUM_OR_PQNAME(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        AVT avt;
        if (getSupportsAVT()) {
            try {
                AVT avt2 = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                boolean zIsSimple = avt2.isSimple();
                avt = avt2;
                if (!zIsSimple) {
                    return avt2;
                }
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        } else {
            avt = null;
        }
        int i = getEnum(str4);
        if (i != -10000) {
            return avt == null ? new Integer(i) : avt;
        }
        try {
            QName qName = new QName(str4, (PrefixResolver) stylesheetHandler, true);
            Object obj = avt;
            if (avt == null) {
                obj = qName;
            }
            if (qName.getPrefix() == null) {
                StringBuffer listOfEnums = getListOfEnums();
                listOfEnums.append(" <qname-but-not-ncname>");
                handleError(stylesheetHandler, XSLTErrorResources.INVALID_ENUM, new Object[]{str2, str4, listOfEnums.toString()}, null);
                return null;
            }
            return obj;
        } catch (IllegalArgumentException e2) {
            StringBuffer listOfEnums2 = getListOfEnums();
            listOfEnums2.append(" <qname-but-not-ncname>");
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_ENUM, new Object[]{str2, str4, listOfEnums2.toString()}, e2);
            return null;
        } catch (RuntimeException e3) {
            StringBuffer listOfEnums3 = getListOfEnums();
            listOfEnums3.append(" <qname-but-not-ncname>");
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_ENUM, new Object[]{str2, str4, listOfEnums3.toString()}, e3);
            return null;
        }
    }

    Object processEXPR(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        try {
            return stylesheetHandler.createXPath(str4, elemTemplateElement);
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    Object processNMTOKEN(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                if (avt.isSimple() && !XML11Char.isXML11ValidNmtoken(str4)) {
                    handleError(stylesheetHandler, XSLTErrorResources.INVALID_NMTOKEN, new Object[]{str2, str4}, null);
                    return null;
                }
                return avt;
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        if (!XML11Char.isXML11ValidNmtoken(str4)) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_NMTOKEN, new Object[]{str2, str4}, null);
            return null;
        }
        return str4;
    }

    Object processPATTERN(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        try {
            return stylesheetHandler.createMatchPatternXPath(str4, elemTemplateElement);
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    Object processNUMBER(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                if (avt.isSimple()) {
                    Double.valueOf(str4);
                }
                return avt;
            } catch (NumberFormatException e) {
                handleError(stylesheetHandler, XSLTErrorResources.INVALID_NUMBER, new Object[]{str2, str4}, e);
                return null;
            } catch (TransformerException e2) {
                throw new SAXException(e2);
            }
        }
        try {
            return Double.valueOf(str4);
        } catch (NumberFormatException e3) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_NUMBER, new Object[]{str2, str4}, e3);
            return null;
        }
    }

    Object processQNAME(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        try {
            return new QName(str4, (PrefixResolver) stylesheetHandler, true);
        } catch (IllegalArgumentException e) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_QNAME, new Object[]{str2, str4}, e);
            return null;
        } catch (RuntimeException e2) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_QNAME, new Object[]{str2, str4}, e2);
            return null;
        }
    }

    Object processAVT_QNAME(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        String strSubstring;
        try {
            AVT avt = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            if (avt.isSimple()) {
                int iIndexOf = str4.indexOf(58);
                if (iIndexOf >= 0 && !XML11Char.isXML11ValidNCName(str4.substring(0, iIndexOf))) {
                    handleError(stylesheetHandler, XSLTErrorResources.INVALID_QNAME, new Object[]{str2, str4}, null);
                    return null;
                }
                if (iIndexOf >= 0) {
                    strSubstring = str4.substring(iIndexOf + 1);
                } else {
                    strSubstring = str4;
                }
                if (strSubstring == null || strSubstring.length() == 0 || !XML11Char.isXML11ValidNCName(strSubstring)) {
                    handleError(stylesheetHandler, XSLTErrorResources.INVALID_QNAME, new Object[]{str2, str4}, null);
                    return null;
                }
            }
            return avt;
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    Object processNCNAME(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        if (getSupportsAVT()) {
            try {
                AVT avt = new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                if (avt.isSimple() && !XML11Char.isXML11ValidNCName(str4)) {
                    handleError(stylesheetHandler, XSLTErrorResources.INVALID_NCNAME, new Object[]{str2, str4}, null);
                    return null;
                }
                return avt;
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        if (!XML11Char.isXML11ValidNCName(str4)) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_NCNAME, new Object[]{str2, str4}, null);
            return null;
        }
        return str4;
    }

    Vector processQNAMES(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) throws SAXException {
        StringTokenizer stringTokenizer = new StringTokenizer(str4, " \t\n\r\f");
        int iCountTokens = stringTokenizer.countTokens();
        Vector vector = new Vector(iCountTokens);
        for (int i = 0; i < iCountTokens; i++) {
            vector.addElement(new QName(stringTokenizer.nextToken(), stylesheetHandler));
        }
        return vector;
    }

    final Vector processQNAMESRNU(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) throws SAXException {
        StringTokenizer stringTokenizer = new StringTokenizer(str4, " \t\n\r\f");
        int iCountTokens = stringTokenizer.countTokens();
        Vector vector = new Vector(iCountTokens);
        String namespaceForPrefix = stylesheetHandler.getNamespaceForPrefix("");
        for (int i = 0; i < iCountTokens; i++) {
            String strNextToken = stringTokenizer.nextToken();
            if (strNextToken.indexOf(58) == -1) {
                vector.addElement(new QName(namespaceForPrefix, strNextToken));
            } else {
                vector.addElement(new QName(strNextToken, stylesheetHandler));
            }
        }
        return vector;
    }

    Vector processSIMPLEPATTERNLIST(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        try {
            StringTokenizer stringTokenizer = new StringTokenizer(str4, " \t\n\r\f");
            int iCountTokens = stringTokenizer.countTokens();
            Vector vector = new Vector(iCountTokens);
            for (int i = 0; i < iCountTokens; i++) {
                vector.addElement(stylesheetHandler.createMatchPatternXPath(stringTokenizer.nextToken(), elemTemplateElement));
            }
            return vector;
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    StringVector processSTRINGLIST(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) {
        StringTokenizer stringTokenizer = new StringTokenizer(str4, " \t\n\r\f");
        int iCountTokens = stringTokenizer.countTokens();
        StringVector stringVector = new StringVector(iCountTokens);
        for (int i = 0; i < iCountTokens; i++) {
            stringVector.addElement(stringTokenizer.nextToken());
        }
        return stringVector;
    }

    StringVector processPREFIX_URLLIST(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) throws SAXException {
        StringTokenizer stringTokenizer = new StringTokenizer(str4, " \t\n\r\f");
        int iCountTokens = stringTokenizer.countTokens();
        StringVector stringVector = new StringVector(iCountTokens);
        for (int i = 0; i < iCountTokens; i++) {
            String strNextToken = stringTokenizer.nextToken();
            String namespaceForPrefix = stylesheetHandler.getNamespaceForPrefix(strNextToken);
            if (namespaceForPrefix != null) {
                stringVector.addElement(namespaceForPrefix);
            } else {
                throw new SAXException(XSLMessages.createMessage(XSLTErrorResources.ER_CANT_RESOLVE_NSPREFIX, new Object[]{strNextToken}));
            }
        }
        return stringVector;
    }

    StringVector processPREFIX_LIST(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) throws SAXException {
        StringTokenizer stringTokenizer = new StringTokenizer(str4, " \t\n\r\f");
        int iCountTokens = stringTokenizer.countTokens();
        StringVector stringVector = new StringVector(iCountTokens);
        for (int i = 0; i < iCountTokens; i++) {
            String strNextToken = stringTokenizer.nextToken();
            String namespaceForPrefix = stylesheetHandler.getNamespaceForPrefix(strNextToken);
            if (strNextToken.equals("#default") || namespaceForPrefix != null) {
                stringVector.addElement(strNextToken);
            } else {
                throw new SAXException(XSLMessages.createMessage(XSLTErrorResources.ER_CANT_RESOLVE_NSPREFIX, new Object[]{strNextToken}));
            }
        }
        return stringVector;
    }

    Object processURL(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        if (getSupportsAVT()) {
            try {
                return new AVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        return str4;
    }

    private Boolean processYESNO(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) throws SAXException {
        if (!str4.equals("yes") && !str4.equals("no")) {
            handleError(stylesheetHandler, XSLTErrorResources.INVALID_BOOLEAN, new Object[]{str2, str4}, null);
            return null;
        }
        return new Boolean(str4.equals("yes"));
    }

    Object processValue(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws SAXException {
        switch (getType()) {
            case 1:
                return processCDATA(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 2:
                return processURL(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 3:
                return processAVT(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 4:
                return processPATTERN(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 5:
                return processEXPR(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 6:
                return processCHAR(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 7:
                return processNUMBER(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 8:
                return processYESNO(stylesheetHandler, str, str2, str3, str4);
            case 9:
                return processQNAME(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 10:
                return processQNAMES(stylesheetHandler, str, str2, str3, str4);
            case 11:
                return processENUM(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 12:
                return processSIMPLEPATTERNLIST(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 13:
                return processNMTOKEN(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 14:
                return processSTRINGLIST(stylesheetHandler, str, str2, str3, str4);
            case 15:
                return processPREFIX_URLLIST(stylesheetHandler, str, str2, str3, str4);
            case 16:
                return processENUM_OR_PQNAME(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 17:
                return processNCNAME(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 18:
                return processAVT_QNAME(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
            case 19:
                return processQNAMESRNU(stylesheetHandler, str, str2, str3, str4);
            case 20:
                return processPREFIX_LIST(stylesheetHandler, str, str2, str3, str4);
            default:
                return null;
        }
    }

    void setDefAttrValue(StylesheetHandler stylesheetHandler, ElemTemplateElement elemTemplateElement) throws NoSuchMethodException, SAXException {
        setAttrValue(stylesheetHandler, getNamespace(), getName(), getName(), getDefault(), elemTemplateElement);
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
            return Float.TYPE;
        }
        if (cls == Boolean.class) {
            return Boolean.TYPE;
        }
        if (cls == Byte.class) {
            return Byte.TYPE;
        }
        if (cls == Character.class) {
            return Character.TYPE;
        }
        if (cls == Short.class) {
            return Short.TYPE;
        }
        if (cls == Integer.class) {
            return Integer.TYPE;
        }
        if (cls == Long.class) {
            return Long.TYPE;
        }
        return cls;
    }

    private StringBuffer getListOfEnums() {
        StringBuffer stringBuffer = new StringBuffer();
        String[] enumNames = getEnumNames();
        for (int i = 0; i < enumNames.length; i++) {
            if (i > 0) {
                stringBuffer.append(' ');
            }
            stringBuffer.append(enumNames[i]);
        }
        return stringBuffer;
    }

    boolean setAttrValue(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4, ElemTemplateElement elemTemplateElement) throws NoSuchMethodException, SAXException {
        String setterMethodName;
        Method method;
        Object[] objArr;
        if (!str3.equals("xmlns") && !str3.startsWith(Constants.ATTRNAME_XMLNS) && (setterMethodName = getSetterMethodName()) != null) {
            try {
                try {
                    if (setterMethodName.equals(S_FOREIGNATTR_SETTER)) {
                        if (str == null) {
                            str = "";
                        }
                        Class<?> cls = str.getClass();
                        method = elemTemplateElement.getClass().getMethod(setterMethodName, cls, cls, cls, cls);
                        objArr = new Object[]{str, str2, str3, str4};
                    } else {
                        Object objProcessValue = processValue(stylesheetHandler, str, str2, str3, str4, elemTemplateElement);
                        if (objProcessValue == null) {
                            return false;
                        }
                        Class<?>[] clsArr = {getPrimativeClass(objProcessValue)};
                        try {
                            method = elemTemplateElement.getClass().getMethod(setterMethodName, clsArr);
                        } catch (NoSuchMethodException e) {
                            clsArr[0] = objProcessValue.getClass();
                            method = elemTemplateElement.getClass().getMethod(setterMethodName, clsArr);
                        }
                        objArr = new Object[]{objProcessValue};
                    }
                    method.invoke(elemTemplateElement, objArr);
                } catch (NoSuchMethodException e2) {
                    if (!setterMethodName.equals(S_FOREIGNATTR_SETTER)) {
                        stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CALLING_METHOD, new Object[]{setterMethodName}, e2);
                        return false;
                    }
                }
            } catch (IllegalAccessException e3) {
                stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CALLING_METHOD, new Object[]{setterMethodName}, e3);
                return false;
            } catch (InvocationTargetException e4) {
                handleError(stylesheetHandler, XSLTErrorResources.WG_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{"name", getName()}, e4);
                return false;
            }
        }
        return true;
    }

    private void handleError(StylesheetHandler stylesheetHandler, String str, Object[] objArr, Exception exc) throws SAXException {
        switch (getErrorType()) {
            case 0:
            case 1:
                stylesheetHandler.error(str, objArr, exc);
                break;
            case 2:
                stylesheetHandler.warn(str, objArr);
                break;
        }
    }
}
