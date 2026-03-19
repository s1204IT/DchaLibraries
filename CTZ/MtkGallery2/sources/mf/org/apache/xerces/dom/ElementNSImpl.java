package mf.org.apache.xerces.dom;

import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.DOMException;

public class ElementNSImpl extends ElementImpl {
    static final long serialVersionUID = -9142310625494392642L;
    static final String xmlURI = "http://www.w3.org/XML/1998/namespace";
    protected String localName;
    protected String namespaceURI;
    transient XSTypeDefinition type;

    protected ElementNSImpl() {
    }

    protected ElementNSImpl(CoreDocumentImpl ownerDocument, String namespaceURI, String qualifiedName) throws DOMException {
        super(ownerDocument, qualifiedName);
        setName(namespaceURI, qualifiedName);
    }

    private void setName(String namespaceURI, String qname) {
        this.namespaceURI = namespaceURI;
        if (namespaceURI != null) {
            this.namespaceURI = namespaceURI.length() == 0 ? null : namespaceURI;
        }
        if (qname == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
            throw new DOMException((short) 14, msg);
        }
        int colon1 = qname.indexOf(58);
        int colon2 = qname.lastIndexOf(58);
        this.ownerDocument.checkNamespaceWF(qname, colon1, colon2);
        if (colon1 < 0) {
            this.localName = qname;
            if (this.ownerDocument.errorChecking) {
                this.ownerDocument.checkQName(null, this.localName);
                if ((qname.equals("xmlns") && (namespaceURI == null || !namespaceURI.equals(NamespaceContext.XMLNS_URI))) || (namespaceURI != null && namespaceURI.equals(NamespaceContext.XMLNS_URI) && !qname.equals("xmlns"))) {
                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                    throw new DOMException((short) 14, msg2);
                }
                return;
            }
            return;
        }
        String prefix = qname.substring(0, colon1);
        this.localName = qname.substring(colon2 + 1);
        if (this.ownerDocument.errorChecking) {
            if (namespaceURI == null || (prefix.equals("xml") && !namespaceURI.equals(NamespaceContext.XML_URI))) {
                String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                throw new DOMException((short) 14, msg3);
            }
            this.ownerDocument.checkQName(prefix, this.localName);
            this.ownerDocument.checkDOMNSErr(prefix, namespaceURI);
        }
    }

    protected ElementNSImpl(CoreDocumentImpl ownerDocument, String namespaceURI, String qualifiedName, String localName) throws DOMException {
        super(ownerDocument, qualifiedName);
        this.localName = localName;
        this.namespaceURI = namespaceURI;
    }

    protected ElementNSImpl(CoreDocumentImpl ownerDocument, String value) {
        super(ownerDocument, value);
    }

    void rename(String namespaceURI, String qualifiedName) {
        if (needsSyncData()) {
            synchronizeData();
        }
        this.name = qualifiedName;
        setName(namespaceURI, qualifiedName);
        reconcileDefaultAttributes();
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
        if (this.ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (prefix != null && prefix.length() != 0) {
                if (!CoreDocumentImpl.isXMLName(prefix, this.ownerDocument.isXML11Version())) {
                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
                    throw new DOMException((short) 5, msg2);
                }
                if (this.namespaceURI == null || prefix.indexOf(58) >= 0) {
                    String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                    throw new DOMException((short) 14, msg3);
                }
                if (prefix.equals("xml") && !this.namespaceURI.equals(xmlURI)) {
                    String msg4 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                    throw new DOMException((short) 14, msg4);
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
    protected Attr getXMLBaseAttribute() {
        return (Attr) this.attributes.getNamedItemNS(xmlURI, "base");
    }

    @Override
    public String getTypeName() {
        if (this.type != null) {
            if (this.type instanceof XSSimpleTypeDecl) {
                return ((XSSimpleTypeDecl) this.type).getTypeName();
            }
            if (this.type instanceof XSComplexTypeDecl) {
                return ((XSComplexTypeDecl) this.type).getTypeName();
            }
            return null;
        }
        return null;
    }

    @Override
    public String getTypeNamespace() {
        if (this.type != null) {
            return this.type.getNamespace();
        }
        return null;
    }

    @Override
    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.type != null) {
            if (this.type instanceof XSSimpleTypeDecl) {
                return ((XSSimpleTypeDecl) this.type).isDOMDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
            }
            if (this.type instanceof XSComplexTypeDecl) {
                return ((XSComplexTypeDecl) this.type).isDOMDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
            }
            return false;
        }
        return false;
    }

    public void setType(XSTypeDefinition type) {
        this.type = type;
    }
}
