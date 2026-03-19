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

    public QName(String namespaceURI, String localName) {
        this(namespaceURI, localName, false);
    }

    public QName(String namespaceURI, String localName, boolean validate) {
        if (localName == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_NULL, null));
        }
        if (validate && !XML11Char.isXML11ValidNCName(localName)) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this._namespaceURI = namespaceURI;
        this._localName = localName;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String namespaceURI, String prefix, String localName) {
        this(namespaceURI, prefix, localName, false);
    }

    public QName(String namespaceURI, String prefix, String localName, boolean validate) {
        if (localName == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_NULL, null));
        }
        if (validate) {
            if (!XML11Char.isXML11ValidNCName(localName)) {
                throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
            }
            if (prefix != null && !XML11Char.isXML11ValidNCName(prefix)) {
                throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_PREFIX_INVALID, null));
            }
        }
        this._namespaceURI = namespaceURI;
        this._prefix = prefix;
        this._localName = localName;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String localName) {
        this(localName, false);
    }

    public QName(String localName, boolean validate) {
        if (localName == null) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_NULL, null));
        }
        if (validate && !XML11Char.isXML11ValidNCName(localName)) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this._namespaceURI = null;
        this._localName = localName;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String qname, Stack namespaces) {
        this(qname, namespaces, false);
    }

    public QName(String qname, Stack namespaces, boolean validate) {
        String namespace = null;
        String prefix = null;
        int indexOfNSSep = qname.indexOf(58);
        if (indexOfNSSep > 0) {
            prefix = qname.substring(0, indexOfNSSep);
            if (prefix.equals("xml")) {
                namespace = "http://www.w3.org/XML/1998/namespace";
            } else {
                if (prefix.equals("xmlns")) {
                    return;
                }
                int depth = namespaces.size();
                int i = depth - 1;
                while (i >= 0) {
                    NameSpace ns = (NameSpace) namespaces.elementAt(i);
                    while (true) {
                        if (ns != null) {
                            if (ns.m_prefix != null && prefix.equals(ns.m_prefix)) {
                                namespace = ns.m_uri;
                                i = -1;
                                break;
                            }
                            ns = ns.m_next;
                        } else {
                            break;
                        }
                    }
                    i--;
                }
            }
            if (namespace == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage("ER_PREFIX_MUST_RESOLVE", new Object[]{prefix}));
            }
        }
        this._localName = indexOfNSSep >= 0 ? qname.substring(indexOfNSSep + 1) : qname;
        if (validate && (this._localName == null || !XML11Char.isXML11ValidNCName(this._localName))) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this._namespaceURI = namespace;
        this._prefix = prefix;
        this.m_hashCode = toString().hashCode();
    }

    public QName(String qname, Element namespaceContext, PrefixResolver resolver) {
        this(qname, namespaceContext, resolver, false);
    }

    public QName(String qname, Element namespaceContext, PrefixResolver resolver, boolean validate) {
        this._namespaceURI = null;
        int indexOfNSSep = qname.indexOf(58);
        if (indexOfNSSep > 0 && namespaceContext != null) {
            String prefix = qname.substring(0, indexOfNSSep);
            this._prefix = prefix;
            if (prefix.equals("xml")) {
                this._namespaceURI = "http://www.w3.org/XML/1998/namespace";
            } else if (prefix.equals("xmlns")) {
                return;
            } else {
                this._namespaceURI = resolver.getNamespaceForPrefix(prefix, namespaceContext);
            }
            if (this._namespaceURI == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage("ER_PREFIX_MUST_RESOLVE", new Object[]{prefix}));
            }
        }
        this._localName = indexOfNSSep >= 0 ? qname.substring(indexOfNSSep + 1) : qname;
        if (validate && (this._localName == null || !XML11Char.isXML11ValidNCName(this._localName))) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this.m_hashCode = toString().hashCode();
    }

    public QName(String qname, PrefixResolver resolver) {
        this(qname, resolver, false);
    }

    public QName(String qname, PrefixResolver resolver, boolean validate) {
        String prefix = null;
        this._namespaceURI = null;
        int indexOfNSSep = qname.indexOf(58);
        if (indexOfNSSep > 0) {
            prefix = qname.substring(0, indexOfNSSep);
            if (prefix.equals("xml")) {
                this._namespaceURI = "http://www.w3.org/XML/1998/namespace";
            } else {
                this._namespaceURI = resolver.getNamespaceForPrefix(prefix);
            }
            if (this._namespaceURI == null) {
                throw new RuntimeException(XMLMessages.createXMLMessage("ER_PREFIX_MUST_RESOLVE", new Object[]{prefix}));
            }
            this._localName = qname.substring(indexOfNSSep + 1);
        } else {
            if (indexOfNSSep == 0) {
                throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_NAME_CANT_START_WITH_COLON, null));
            }
            this._localName = qname;
        }
        if (validate && (this._localName == null || !XML11Char.isXML11ValidNCName(this._localName))) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ARG_LOCALNAME_INVALID, null));
        }
        this.m_hashCode = toString().hashCode();
        this._prefix = prefix;
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
        return this._namespaceURI != null ? "{" + this._namespaceURI + "}" + this._localName : this._localName;
    }

    public String toNamespacedString() {
        return this._namespaceURI != null ? "{" + this._namespaceURI + "}" + this._localName : this._localName;
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

    public boolean equals(String ns, String localPart) {
        String thisnamespace = getNamespaceURI();
        if (!getLocalName().equals(localPart)) {
            return false;
        }
        if (thisnamespace == null || ns == null) {
            return thisnamespace == null && ns == null;
        }
        return thisnamespace.equals(ns);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof QName)) {
            return false;
        }
        String thisnamespace = getNamespaceURI();
        String thatnamespace = obj.getNamespaceURI();
        if (!getLocalName().equals(obj.getLocalName())) {
            return false;
        }
        if (thisnamespace == null || thatnamespace == null) {
            return thisnamespace == null && thatnamespace == null;
        }
        return thisnamespace.equals(thatnamespace);
    }

    public static QName getQNameFromString(String name) {
        StringTokenizer tokenizer = new StringTokenizer(name, "{}", false);
        String s1 = tokenizer.nextToken();
        String s2 = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        if (s2 == null) {
            QName qname = new QName((String) null, s1);
            return qname;
        }
        QName qname2 = new QName(s1, s2);
        return qname2;
    }

    public static boolean isXMLNSDecl(String attRawName) {
        if (!attRawName.startsWith("xmlns")) {
            return false;
        }
        if (attRawName.equals("xmlns")) {
            return true;
        }
        return attRawName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS);
    }

    public static String getPrefixFromXMLNSDecl(String attRawName) {
        int index = attRawName.indexOf(58);
        return index >= 0 ? attRawName.substring(index + 1) : "";
    }

    public static String getLocalPart(String qname) {
        int index = qname.indexOf(58);
        return index < 0 ? qname : qname.substring(index + 1);
    }

    public static String getPrefixPart(String qname) {
        int index = qname.indexOf(58);
        return index >= 0 ? qname.substring(0, index) : "";
    }
}
