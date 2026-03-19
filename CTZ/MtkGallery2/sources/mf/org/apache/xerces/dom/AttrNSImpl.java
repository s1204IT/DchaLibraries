package mf.org.apache.xerces.dom;

import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.w3c.dom.DOMException;

public class AttrNSImpl extends AttrImpl {
    static final long serialVersionUID = -781906615369795414L;
    static final String xmlURI = "http://www.w3.org/XML/1998/namespace";
    static final String xmlnsURI = "http://www.w3.org/2000/xmlns/";
    protected String localName;
    protected String namespaceURI;

    public AttrNSImpl() {
    }

    protected AttrNSImpl(CoreDocumentImpl ownerDocument, String namespaceURI, String qualifiedName) {
        super(ownerDocument, qualifiedName);
        setName(namespaceURI, qualifiedName);
    }

    private void setName(String namespaceURI, String qname) {
        String str;
        CoreDocumentImpl ownerDocument = ownerDocument();
        this.namespaceURI = namespaceURI;
        if (namespaceURI != null) {
            if (namespaceURI.length() == 0) {
                str = null;
            } else {
                str = namespaceURI;
            }
            this.namespaceURI = str;
        }
        int colon1 = qname.indexOf(58);
        int colon2 = qname.lastIndexOf(58);
        ownerDocument.checkNamespaceWF(qname, colon1, colon2);
        if (colon1 < 0) {
            this.localName = qname;
            if (ownerDocument.errorChecking) {
                ownerDocument.checkQName(null, this.localName);
                if ((qname.equals("xmlns") && (namespaceURI == null || !namespaceURI.equals(NamespaceContext.XMLNS_URI))) || (namespaceURI != null && namespaceURI.equals(NamespaceContext.XMLNS_URI) && !qname.equals("xmlns"))) {
                    String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                    throw new DOMException((short) 14, msg);
                }
                return;
            }
            return;
        }
        String prefix = qname.substring(0, colon1);
        this.localName = qname.substring(colon2 + 1);
        ownerDocument.checkQName(prefix, this.localName);
        ownerDocument.checkDOMNSErr(prefix, namespaceURI);
    }

    public AttrNSImpl(CoreDocumentImpl ownerDocument, String namespaceURI, String qualifiedName, String localName) {
        super(ownerDocument, qualifiedName);
        this.localName = localName;
        this.namespaceURI = namespaceURI;
    }

    protected AttrNSImpl(CoreDocumentImpl ownerDocument, String value) {
        super(ownerDocument, value);
    }

    void rename(String namespaceURI, String qualifiedName) {
        if (needsSyncData()) {
            synchronizeData();
        }
        this.name = qualifiedName;
        setName(namespaceURI, qualifiedName);
    }

    @Override
    public String getNamespaceURI() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.namespaceURI;
    }

    @Override
    public String getPrefix() {
        if (needsSyncData()) {
            synchronizeData();
        }
        int index = this.name.indexOf(58);
        if (index < 0) {
            return null;
        }
        return this.name.substring(0, index);
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (ownerDocument().errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (prefix != null && prefix.length() != 0) {
                if (!CoreDocumentImpl.isXMLName(prefix, ownerDocument().isXML11Version())) {
                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
                    throw new DOMException((short) 5, msg2);
                }
                if (this.namespaceURI == null || prefix.indexOf(58) >= 0) {
                    String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                    throw new DOMException((short) 14, msg3);
                }
                if (prefix.equals("xmlns")) {
                    if (!this.namespaceURI.equals(xmlnsURI)) {
                        String msg4 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                        throw new DOMException((short) 14, msg4);
                    }
                } else if (prefix.equals("xml")) {
                    if (!this.namespaceURI.equals(xmlURI)) {
                        String msg5 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                        throw new DOMException((short) 14, msg5);
                    }
                } else {
                    String msg6 = this.name;
                    if (msg6.equals("xmlns")) {
                        String msg7 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                        throw new DOMException((short) 14, msg7);
                    }
                }
            }
        }
        if (prefix != null && prefix.length() != 0) {
            this.name = String.valueOf(prefix) + ":" + this.localName;
            return;
        }
        this.name = this.localName;
    }

    @Override
    public String getLocalName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.localName;
    }

    @Override
    public String getTypeName() {
        if (this.type != null) {
            if (this.type instanceof XSSimpleTypeDecl) {
                return ((XSSimpleTypeDecl) this.type).getName();
            }
            return (String) this.type;
        }
        return null;
    }

    @Override
    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        if (this.type != null && (this.type instanceof XSSimpleTypeDecl)) {
            return ((XSSimpleTypeDecl) this.type).isDOMDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
        }
        return false;
    }

    @Override
    public String getTypeNamespace() {
        if (this.type != null) {
            if (this.type instanceof XSSimpleTypeDecl) {
                return ((XSSimpleTypeDecl) this.type).getNamespace();
            }
            return XMLGrammarDescription.XML_DTD;
        }
        return null;
    }
}
