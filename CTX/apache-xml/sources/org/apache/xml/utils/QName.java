package org.apache.xml.utils;

import java.io.Serializable;
import java.util.Stack;
import java.util.StringTokenizer;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.w3c.dom.Element;

public class QName implements Serializable {
    public static final String S_XMLNAMESPACEURI = "http://www.w3.org/XML/1998/namespace";
    static final long serialVersionUID = 467434581652829920L;
    protected String _localName;
    protected String _namespaceURI;
    protected String _prefix;
    private int m_hashCode;

    public QName() {
    }

    public QName(String str, String str2) {
        this(str, str2, false);
    }

    public QName(String str, String str2, boolean z) {
        if (str2 == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_NULL, null));
        }
        if (z && !XML11Char.isXML11ValidNCName(str2)) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this._namespaceURI = str;
        this._localName = str2;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String str, String str2, String str3) {
        this(str, str2, str3, false);
    }

    public QName(String str, String str2, String str3, boolean z) {
        if (str3 == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_NULL, null));
        }
        if (z) {
            if (!XML11Char.isXML11ValidNCName(str3)) {
                throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
            }
            if (str2 != null && !XML11Char.isXML11ValidNCName(str2)) {
                throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_PREFIX_INVALID, null));
            }
        }
        this._namespaceURI = str;
        this._prefix = str2;
        this._localName = str3;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String str) {
        this(str, false);
    }

    public QName(String str, boolean z) {
        if (str == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_NULL, null));
        }
        if (z && !XML11Char.isXML11ValidNCName(str)) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this._namespaceURI = null;
        this._localName = str;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String str, Stack stack) {
        this(str, stack, false);
    }

    public QName(String str, Stack stack, boolean z) {
        String str2;
        String strSubstring;
        int iIndexOf = str.indexOf(58);
        if (iIndexOf > 0) {
            strSubstring = str.substring(0, iIndexOf);
            if (strSubstring.equals("xml")) {
                str2 = "http://www.w3.org/XML/1998/namespace";
            } else {
                if (strSubstring.equals("xmlns")) {
                    return;
                }
                int size = stack.size() - 1;
                String str3 = null;
                while (size >= 0) {
                    NameSpace nameSpace = (NameSpace) stack.elementAt(size);
                    while (true) {
                        if (nameSpace != null) {
                            if (nameSpace.m_prefix != null && strSubstring.equals(nameSpace.m_prefix)) {
                                str3 = nameSpace.m_uri;
                                size = -1;
                                break;
                            }
                            nameSpace = nameSpace.m_next;
                        } else {
                            break;
                        }
                    }
                    size--;
                }
                str2 = str3;
            }
            if (str2 == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage("ER_PREFIX_MUST_RESOLVE", new Object[]{strSubstring}));
            }
        } else {
            str2 = null;
            strSubstring = null;
        }
        this._localName = iIndexOf >= 0 ? str.substring(iIndexOf + 1) : str;
        if (z && (this._localName == null || !XML11Char.isXML11ValidNCName(this._localName))) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this._namespaceURI = str2;
        this._prefix = strSubstring;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String str, Element element, PrefixResolver prefixResolver) {
        this(str, element, prefixResolver, false);
    }

    public QName(String str, Element element, PrefixResolver prefixResolver, boolean z) {
        this._namespaceURI = null;
        int iIndexOf = str.indexOf(58);
        if (iIndexOf > 0 && element != null) {
            String strSubstring = str.substring(0, iIndexOf);
            this._prefix = strSubstring;
            if (strSubstring.equals("xml")) {
                this._namespaceURI = "http://www.w3.org/XML/1998/namespace";
            } else if (strSubstring.equals("xmlns")) {
                return;
            } else {
                this._namespaceURI = prefixResolver.getNamespaceForPrefix(strSubstring, element);
            }
            if (this._namespaceURI == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage("ER_PREFIX_MUST_RESOLVE", new Object[]{strSubstring}));
            }
        }
        this._localName = iIndexOf >= 0 ? str.substring(iIndexOf + 1) : str;
        if (z && (this._localName == null || !XML11Char.isXML11ValidNCName(this._localName))) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this.m_hashCode = toString().hashCode();
    }

    public QName(String str, PrefixResolver prefixResolver) {
        this(str, prefixResolver, false);
    }

    public QName(String str, PrefixResolver prefixResolver, boolean z) {
        String strSubstring;
        this._namespaceURI = null;
        int iIndexOf = str.indexOf(58);
        if (iIndexOf > 0) {
            strSubstring = str.substring(0, iIndexOf);
            if (strSubstring.equals("xml")) {
                this._namespaceURI = "http://www.w3.org/XML/1998/namespace";
            } else {
                this._namespaceURI = prefixResolver.getNamespaceForPrefix(strSubstring);
            }
            if (this._namespaceURI == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage("ER_PREFIX_MUST_RESOLVE", new Object[]{strSubstring}));
            }
            this._localName = str.substring(iIndexOf + 1);
        } else {
            if (iIndexOf == 0) {
                throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NAME_CANT_START_WITH_COLON, null));
            }
            this._localName = str;
            strSubstring = null;
        }
        if (z && (this._localName == null || !XML11Char.isXML11ValidNCName(this._localName))) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this.m_hashCode = toString().hashCode();
        this._prefix = strSubstring;
    }

    public String getNamespaceURI() {
        return this._namespaceURI;
    }

    public String getPrefix() {
        return this._prefix;
    }

    public String getLocalName() {
        return this._localName;
    }

    public String toString() {
        if (this._prefix != null) {
            return this._prefix + ":" + this._localName;
        }
        if (this._namespaceURI == null) {
            return this._localName;
        }
        return "{" + this._namespaceURI + "}" + this._localName;
    }

    public String toNamespacedString() {
        if (this._namespaceURI == null) {
            return this._localName;
        }
        return "{" + this._namespaceURI + "}" + this._localName;
    }

    public String getNamespace() {
        return getNamespaceURI();
    }

    public String getLocalPart() {
        return getLocalName();
    }

    public int hashCode() {
        return this.m_hashCode;
    }

    public boolean equals(String str, String str2) {
        String namespaceURI = getNamespaceURI();
        return getLocalName().equals(str2) && (namespaceURI == null || str == null ? namespaceURI == null && str == null : namespaceURI.equals(str));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof QName)) {
            return false;
        }
        QName qName = (QName) obj;
        String namespaceURI = getNamespaceURI();
        String namespaceURI2 = qName.getNamespaceURI();
        if (getLocalName().equals(qName.getLocalName())) {
            if (namespaceURI == null || namespaceURI2 == null) {
                if (namespaceURI == null && namespaceURI2 == null) {
                    return true;
                }
            } else if (namespaceURI.equals(namespaceURI2)) {
                return true;
            }
        }
        return false;
    }

    public static QName getQNameFromString(String str) {
        StringTokenizer stringTokenizer = new StringTokenizer(str, "{}", false);
        String strNextToken = stringTokenizer.nextToken();
        String strNextToken2 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
        if (strNextToken2 == null) {
            return new QName((String) null, strNextToken);
        }
        return new QName(strNextToken, strNextToken2);
    }

    public static boolean isXMLNSDecl(String str) {
        return str.startsWith("xmlns") && (str.equals("xmlns") || str.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS));
    }

    public static String getPrefixFromXMLNSDecl(String str) {
        int iIndexOf = str.indexOf(58);
        return iIndexOf >= 0 ? str.substring(iIndexOf + 1) : "";
    }

    public static String getLocalPart(String str) {
        int iIndexOf = str.indexOf(58);
        return iIndexOf < 0 ? str : str.substring(iIndexOf + 1);
    }

    public static String getPrefixPart(String str) {
        int iIndexOf = str.indexOf(58);
        return iIndexOf >= 0 ? str.substring(0, iIndexOf) : "";
    }
}
