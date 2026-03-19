package mf.org.apache.xerces.impl.xs.traversers;

import java.util.Stack;
import java.util.Vector;
import mf.org.apache.xerces.impl.validation.ValidationState;
import mf.org.apache.xerces.impl.xs.SchemaNamespaceSupport;
import mf.org.apache.xerces.impl.xs.XMLSchemaException;
import mf.org.apache.xerces.impl.xs.opti.SchemaDOM;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.Element;

class XSDocumentInfo {
    protected boolean fAreLocalAttributesQualified;
    protected boolean fAreLocalElementsQualified;
    protected XSAttributeChecker fAttrChecker;
    protected short fBlockDefault;
    protected short fFinalDefault;
    protected boolean fIsChameleonSchema;
    protected SchemaNamespaceSupport fNamespaceSupport;
    protected SchemaNamespaceSupport fNamespaceSupportRoot;
    protected Object[] fSchemaAttrs;
    protected Element fSchemaElement;
    SymbolTable fSymbolTable;
    String fTargetNamespace;
    protected Stack SchemaNamespaceSupportStack = new Stack();
    Vector fImportedNS = new Vector();
    protected ValidationState fValidationContext = new ValidationState();
    protected XSAnnotationInfo fAnnotations = null;
    private Vector fReportedTNS = null;

    XSDocumentInfo(Element schemaRoot, XSAttributeChecker attrChecker, SymbolTable symbolTable) throws XMLSchemaException {
        this.fSymbolTable = null;
        this.fSchemaElement = schemaRoot;
        this.fNamespaceSupport = new SchemaNamespaceSupport(schemaRoot, symbolTable);
        this.fNamespaceSupport.reset();
        this.fIsChameleonSchema = false;
        this.fSymbolTable = symbolTable;
        this.fAttrChecker = attrChecker;
        if (schemaRoot != null) {
            this.fSchemaAttrs = attrChecker.checkAttributes(schemaRoot, true, this);
            if (this.fSchemaAttrs == null) {
                throw new XMLSchemaException(null, null);
            }
            this.fAreLocalAttributesQualified = ((XInt) this.fSchemaAttrs[XSAttributeChecker.ATTIDX_AFORMDEFAULT]).intValue() == 1;
            this.fAreLocalElementsQualified = ((XInt) this.fSchemaAttrs[XSAttributeChecker.ATTIDX_EFORMDEFAULT]).intValue() == 1;
            this.fBlockDefault = ((XInt) this.fSchemaAttrs[XSAttributeChecker.ATTIDX_BLOCKDEFAULT]).shortValue();
            this.fFinalDefault = ((XInt) this.fSchemaAttrs[XSAttributeChecker.ATTIDX_FINALDEFAULT]).shortValue();
            this.fTargetNamespace = (String) this.fSchemaAttrs[XSAttributeChecker.ATTIDX_TARGETNAMESPACE];
            if (this.fTargetNamespace != null) {
                this.fTargetNamespace = symbolTable.addSymbol(this.fTargetNamespace);
            }
            this.fNamespaceSupportRoot = new SchemaNamespaceSupport(this.fNamespaceSupport);
            this.fValidationContext.setNamespaceSupport(this.fNamespaceSupport);
            this.fValidationContext.setSymbolTable(symbolTable);
        }
    }

    void backupNSSupport(SchemaNamespaceSupport nsSupport) {
        this.SchemaNamespaceSupportStack.push(this.fNamespaceSupport);
        if (nsSupport == null) {
            nsSupport = this.fNamespaceSupportRoot;
        }
        this.fNamespaceSupport = new SchemaNamespaceSupport(nsSupport);
        this.fValidationContext.setNamespaceSupport(this.fNamespaceSupport);
    }

    void restoreNSSupport() {
        this.fNamespaceSupport = (SchemaNamespaceSupport) this.SchemaNamespaceSupportStack.pop();
        this.fValidationContext.setNamespaceSupport(this.fNamespaceSupport);
    }

    public String toString() {
        String documentURI;
        StringBuffer buf = new StringBuffer();
        if (this.fTargetNamespace == null) {
            buf.append("no targetNamspace");
        } else {
            buf.append("targetNamespace is ");
            buf.append(this.fTargetNamespace);
        }
        Document doc = this.fSchemaElement != null ? this.fSchemaElement.getOwnerDocument() : null;
        if ((doc instanceof SchemaDOM) && (documentURI = doc.getDocumentURI()) != null && documentURI.length() > 0) {
            buf.append(" :: schemaLocation is ");
            buf.append(documentURI);
        }
        return buf.toString();
    }

    public void addAllowedNS(String namespace) {
        this.fImportedNS.addElement(namespace == null ? "" : namespace);
    }

    public boolean isAllowedNS(String namespace) {
        return this.fImportedNS.contains(namespace == null ? "" : namespace);
    }

    final boolean needReportTNSError(String uri) {
        if (this.fReportedTNS == null) {
            this.fReportedTNS = new Vector();
        } else if (this.fReportedTNS.contains(uri)) {
            return false;
        }
        this.fReportedTNS.addElement(uri);
        return true;
    }

    Object[] getSchemaAttrs() {
        return this.fSchemaAttrs;
    }

    void returnSchemaAttrs() {
        this.fAttrChecker.returnAttrArray(this.fSchemaAttrs, null);
        this.fSchemaAttrs = null;
    }

    void addAnnotation(XSAnnotationInfo info) {
        info.next = this.fAnnotations;
        this.fAnnotations = info;
    }

    XSAnnotationInfo getAnnotations() {
        return this.fAnnotations;
    }

    void removeAnnotations() {
        this.fAnnotations = null;
    }
}
