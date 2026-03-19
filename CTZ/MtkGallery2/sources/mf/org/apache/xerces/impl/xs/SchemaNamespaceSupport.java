package mf.org.apache.xerces.impl.xs;

import mf.org.apache.xerces.impl.xs.opti.ElementImpl;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;

public class SchemaNamespaceSupport extends NamespaceSupport {
    private SchemaRootContext fSchemaRootContext;

    public SchemaNamespaceSupport(Element schemaRoot, SymbolTable symbolTable) {
        Document ownerDocument;
        this.fSchemaRootContext = null;
        if (schemaRoot != null && !(schemaRoot instanceof ElementImpl) && (ownerDocument = schemaRoot.getOwnerDocument()) != null && schemaRoot != ownerDocument.getDocumentElement()) {
            this.fSchemaRootContext = new SchemaRootContext(schemaRoot, symbolTable);
        }
    }

    public SchemaNamespaceSupport(SchemaNamespaceSupport nSupport) {
        this.fSchemaRootContext = null;
        this.fSchemaRootContext = nSupport.fSchemaRootContext;
        this.fNamespaceSize = nSupport.fNamespaceSize;
        if (this.fNamespace.length < this.fNamespaceSize) {
            this.fNamespace = new String[this.fNamespaceSize];
        }
        System.arraycopy(nSupport.fNamespace, 0, this.fNamespace, 0, this.fNamespaceSize);
        this.fCurrentContext = nSupport.fCurrentContext;
        if (this.fContext.length <= this.fCurrentContext) {
            this.fContext = new int[this.fCurrentContext + 1];
        }
        System.arraycopy(nSupport.fContext, 0, this.fContext, 0, this.fCurrentContext + 1);
    }

    public void setEffectiveContext(String[] namespaceDecls) {
        if (namespaceDecls == null || namespaceDecls.length == 0) {
            return;
        }
        pushContext();
        int newSize = this.fNamespaceSize + namespaceDecls.length;
        if (this.fNamespace.length < newSize) {
            String[] tempNSArray = new String[newSize];
            System.arraycopy(this.fNamespace, 0, tempNSArray, 0, this.fNamespace.length);
            this.fNamespace = tempNSArray;
        }
        System.arraycopy(namespaceDecls, 0, this.fNamespace, this.fNamespaceSize, namespaceDecls.length);
        this.fNamespaceSize = newSize;
    }

    public String[] getEffectiveLocalContext() {
        int bottomLocalContext;
        int copyCount;
        if (this.fCurrentContext < 3 || (copyCount = this.fNamespaceSize - (bottomLocalContext = this.fContext[3])) <= 0) {
            return null;
        }
        String[] returnVal = new String[copyCount];
        System.arraycopy(this.fNamespace, bottomLocalContext, returnVal, 0, copyCount);
        return returnVal;
    }

    public void makeGlobal() {
        if (this.fCurrentContext >= 3) {
            this.fCurrentContext = 3;
            this.fNamespaceSize = this.fContext[3];
        }
    }

    @Override
    public String getURI(String prefix) {
        String uri = super.getURI(prefix);
        if (uri == null && this.fSchemaRootContext != null) {
            if (!this.fSchemaRootContext.fDOMContextBuilt) {
                this.fSchemaRootContext.fillNamespaceContext();
                this.fSchemaRootContext.fDOMContextBuilt = true;
            }
            if (this.fSchemaRootContext.fNamespaceSize > 0 && !containsPrefix(prefix)) {
                return this.fSchemaRootContext.getURI(prefix);
            }
            return uri;
        }
        return uri;
    }

    static final class SchemaRootContext {
        private final Element fSchemaRoot;
        private final SymbolTable fSymbolTable;
        String[] fNamespace = new String[32];
        int fNamespaceSize = 0;
        boolean fDOMContextBuilt = false;
        private final QName fAttributeQName = new QName();

        SchemaRootContext(Element schemaRoot, SymbolTable symbolTable) {
            this.fSchemaRoot = schemaRoot;
            this.fSymbolTable = symbolTable;
        }

        void fillNamespaceContext() {
            if (this.fSchemaRoot != null) {
                for (Node currentNode = this.fSchemaRoot.getParentNode(); currentNode != null; currentNode = currentNode.getParentNode()) {
                    if (1 == currentNode.getNodeType()) {
                        NamedNodeMap attributes = currentNode.getAttributes();
                        int attrCount = attributes.getLength();
                        for (int i = 0; i < attrCount; i++) {
                            Attr attr = (Attr) attributes.item(i);
                            String value = attr.getValue();
                            if (value == null) {
                                value = XMLSymbols.EMPTY_STRING;
                            }
                            fillQName(this.fAttributeQName, attr);
                            if (this.fAttributeQName.uri == NamespaceContext.XMLNS_URI) {
                                if (this.fAttributeQName.prefix == XMLSymbols.PREFIX_XMLNS) {
                                    declarePrefix(this.fAttributeQName.localpart, value.length() != 0 ? this.fSymbolTable.addSymbol(value) : null);
                                } else {
                                    declarePrefix(XMLSymbols.EMPTY_STRING, value.length() != 0 ? this.fSymbolTable.addSymbol(value) : null);
                                }
                            }
                        }
                    }
                }
            }
        }

        String getURI(String prefix) {
            for (int i = 0; i < this.fNamespaceSize; i += 2) {
                if (this.fNamespace[i] == prefix) {
                    return this.fNamespace[i + 1];
                }
            }
            return null;
        }

        private void declarePrefix(String prefix, String uri) {
            if (this.fNamespaceSize == this.fNamespace.length) {
                String[] namespacearray = new String[this.fNamespaceSize * 2];
                System.arraycopy(this.fNamespace, 0, namespacearray, 0, this.fNamespaceSize);
                this.fNamespace = namespacearray;
            }
            String[] namespacearray2 = this.fNamespace;
            int i = this.fNamespaceSize;
            this.fNamespaceSize = i + 1;
            namespacearray2[i] = prefix;
            String[] strArr = this.fNamespace;
            int i2 = this.fNamespaceSize;
            this.fNamespaceSize = i2 + 1;
            strArr[i2] = uri;
        }

        private void fillQName(QName toFill, Node node) {
            String prefix = node.getPrefix();
            String localName = node.getLocalName();
            String rawName = node.getNodeName();
            String namespace = node.getNamespaceURI();
            toFill.prefix = prefix != null ? this.fSymbolTable.addSymbol(prefix) : XMLSymbols.EMPTY_STRING;
            toFill.localpart = localName != null ? this.fSymbolTable.addSymbol(localName) : XMLSymbols.EMPTY_STRING;
            toFill.rawname = rawName != null ? this.fSymbolTable.addSymbol(rawName) : XMLSymbols.EMPTY_STRING;
            toFill.uri = (namespace == null || namespace.length() <= 0) ? null : this.fSymbolTable.addSymbol(namespace);
        }
    }
}
