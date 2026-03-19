package org.apache.xalan.templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.UnImplNode;
import org.apache.xpath.ExpressionNode;
import org.apache.xpath.WhitespaceStrippingElementMatcher;
import org.apache.xpath.XPathContext;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

public class ElemTemplateElement extends UnImplNode implements PrefixResolver, Serializable, ExpressionNode, WhitespaceStrippingElementMatcher, XSLTVisitable {
    static final long serialVersionUID = 4440018597841834447L;
    private transient Node m_DOMBackPointer;
    private int m_columnNumber;
    private List m_declaredPrefixes;
    private int m_endColumnNumber;
    private int m_endLineNumber;
    ElemTemplateElement m_firstChild;
    private int m_lineNumber;
    ElemTemplateElement m_nextSibling;
    protected ElemTemplateElement m_parentNode;
    private List m_prefixTable;
    private boolean m_defaultSpace = true;
    private boolean m_hasTextLitOnly = false;
    protected boolean m_hasVariableDecl = false;
    protected int m_docOrderNumber = -1;

    public boolean isCompiledTemplate() {
        return false;
    }

    public int getXSLToken() {
        return -1;
    }

    @Override
    public String getNodeName() {
        return "Unknown XSLT Element";
    }

    @Override
    public String getLocalName() {
        return getNodeName();
    }

    public void runtimeInit(TransformerImpl transformerImpl) throws TransformerException {
    }

    public void execute(TransformerImpl transformerImpl) throws TransformerException {
    }

    public StylesheetComposed getStylesheetComposed() {
        return this.m_parentNode.getStylesheetComposed();
    }

    public Stylesheet getStylesheet() {
        if (this.m_parentNode == null) {
            return null;
        }
        return this.m_parentNode.getStylesheet();
    }

    public StylesheetRoot getStylesheetRoot() {
        return this.m_parentNode.getStylesheetRoot();
    }

    public void recompose(StylesheetRoot stylesheetRoot) throws TransformerException {
    }

    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        resolvePrefixTables();
        ElemTemplateElement firstChildElem = getFirstChildElem();
        this.m_hasTextLitOnly = firstChildElem != null && firstChildElem.getXSLToken() == 78 && firstChildElem.getNextSiblingElem() == null;
        stylesheetRoot.getComposeState().pushStackMark();
    }

    public void endCompose(StylesheetRoot stylesheetRoot) throws TransformerException {
        stylesheetRoot.getComposeState().popStackMark();
    }

    @Override
    public void error(String str, Object[] objArr) {
        throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_ELEMTEMPLATEELEM_ERR, new Object[]{XSLMessages.createMessage(str, objArr)}));
    }

    @Override
    public void error(String str) {
        error(str, null);
    }

    @Override
    public Node appendChild(Node node) throws DOMException {
        if (node == null) {
            error(XSLTErrorResources.ER_NULL_CHILD, null);
        }
        ElemTemplateElement elemTemplateElement = (ElemTemplateElement) node;
        if (this.m_firstChild == null) {
            this.m_firstChild = elemTemplateElement;
        } else {
            ((ElemTemplateElement) getLastChild()).m_nextSibling = elemTemplateElement;
        }
        elemTemplateElement.m_parentNode = this;
        return node;
    }

    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        if (elemTemplateElement == null) {
            error(XSLTErrorResources.ER_NULL_CHILD, null);
        }
        if (this.m_firstChild == null) {
            this.m_firstChild = elemTemplateElement;
        } else {
            getLastChildElem().m_nextSibling = elemTemplateElement;
        }
        elemTemplateElement.setParentElem(this);
        return elemTemplateElement;
    }

    @Override
    public boolean hasChildNodes() {
        return this.m_firstChild != null;
    }

    @Override
    public short getNodeType() {
        return (short) 1;
    }

    @Override
    public NodeList getChildNodes() {
        return this;
    }

    public ElemTemplateElement removeChild(ElemTemplateElement elemTemplateElement) {
        if (elemTemplateElement == null || elemTemplateElement.m_parentNode != this) {
            return null;
        }
        if (elemTemplateElement == this.m_firstChild) {
            this.m_firstChild = elemTemplateElement.m_nextSibling;
        } else {
            elemTemplateElement.getPreviousSiblingElem().m_nextSibling = elemTemplateElement.m_nextSibling;
        }
        elemTemplateElement.m_parentNode = null;
        elemTemplateElement.m_nextSibling = null;
        return elemTemplateElement;
    }

    @Override
    public Node replaceChild(Node node, Node node2) throws DOMException {
        if (node2 == null || node2.getParentNode() != this) {
            return null;
        }
        ElemTemplateElement elemTemplateElement = (ElemTemplateElement) node;
        ElemTemplateElement elemTemplateElement2 = (ElemTemplateElement) node2;
        ElemTemplateElement elemTemplateElement3 = (ElemTemplateElement) elemTemplateElement2.getPreviousSibling();
        if (elemTemplateElement3 != null) {
            elemTemplateElement3.m_nextSibling = elemTemplateElement;
        }
        if (this.m_firstChild == elemTemplateElement2) {
            this.m_firstChild = elemTemplateElement;
        }
        elemTemplateElement.m_parentNode = this;
        elemTemplateElement2.m_parentNode = null;
        elemTemplateElement.m_nextSibling = elemTemplateElement2.m_nextSibling;
        elemTemplateElement2.m_nextSibling = null;
        return elemTemplateElement;
    }

    @Override
    public Node insertBefore(Node node, Node node2) throws DOMException {
        if (node2 == null) {
            appendChild(node);
            return node;
        }
        if (node == node2) {
            return node;
        }
        Node nextSibling = this.m_firstChild;
        Node node3 = null;
        boolean z = false;
        while (nextSibling != null) {
            if (node == nextSibling) {
                if (node3 != null) {
                    ((ElemTemplateElement) node3).m_nextSibling = (ElemTemplateElement) nextSibling.getNextSibling();
                } else {
                    this.m_firstChild = (ElemTemplateElement) nextSibling.getNextSibling();
                }
                nextSibling = nextSibling.getNextSibling();
            } else if (node2 == nextSibling) {
                if (node3 != null) {
                    ((ElemTemplateElement) node3).m_nextSibling = (ElemTemplateElement) node;
                } else {
                    this.m_firstChild = (ElemTemplateElement) node;
                }
                ElemTemplateElement elemTemplateElement = (ElemTemplateElement) node;
                elemTemplateElement.m_nextSibling = (ElemTemplateElement) node2;
                elemTemplateElement.setParentElem(this);
                nextSibling = nextSibling.getNextSibling();
                z = true;
                node3 = node;
            } else {
                node3 = nextSibling;
                nextSibling = nextSibling.getNextSibling();
            }
        }
        if (!z) {
            throw new DOMException((short) 8, "refChild was not found in insertBefore method!");
        }
        return node;
    }

    public ElemTemplateElement replaceChild(ElemTemplateElement elemTemplateElement, ElemTemplateElement elemTemplateElement2) {
        if (elemTemplateElement2 == null || elemTemplateElement2.getParentElem() != this) {
            return null;
        }
        ElemTemplateElement previousSiblingElem = elemTemplateElement2.getPreviousSiblingElem();
        if (previousSiblingElem != null) {
            previousSiblingElem.m_nextSibling = elemTemplateElement;
        }
        if (this.m_firstChild == elemTemplateElement2) {
            this.m_firstChild = elemTemplateElement;
        }
        elemTemplateElement.m_parentNode = this;
        elemTemplateElement2.m_parentNode = null;
        elemTemplateElement.m_nextSibling = elemTemplateElement2.m_nextSibling;
        elemTemplateElement2.m_nextSibling = null;
        return elemTemplateElement;
    }

    @Override
    public int getLength() {
        int i = 0;
        for (ElemTemplateElement elemTemplateElement = this.m_firstChild; elemTemplateElement != null; elemTemplateElement = elemTemplateElement.m_nextSibling) {
            i++;
        }
        return i;
    }

    @Override
    public Node item(int i) {
        ElemTemplateElement elemTemplateElement = this.m_firstChild;
        for (int i2 = 0; i2 < i && elemTemplateElement != null; i2++) {
            elemTemplateElement = elemTemplateElement.m_nextSibling;
        }
        return elemTemplateElement;
    }

    @Override
    public Document getOwnerDocument() {
        return getStylesheet();
    }

    public ElemTemplate getOwnerXSLTemplate() {
        int xSLToken = getXSLToken();
        ElemTemplateElement parentElem = this;
        while (parentElem != null && xSLToken != 19) {
            parentElem = parentElem.getParentElem();
            if (parentElem != null) {
                xSLToken = parentElem.getXSLToken();
            }
        }
        return (ElemTemplate) parentElem;
    }

    @Override
    public String getTagName() {
        return getNodeName();
    }

    public boolean hasTextLitOnly() {
        return this.m_hasTextLitOnly;
    }

    @Override
    public String getBaseIdentifier() {
        return getSystemId();
    }

    public int getEndLineNumber() {
        return this.m_endLineNumber;
    }

    @Override
    public int getLineNumber() {
        return this.m_lineNumber;
    }

    public int getEndColumnNumber() {
        return this.m_endColumnNumber;
    }

    @Override
    public int getColumnNumber() {
        return this.m_columnNumber;
    }

    public String getPublicId() {
        if (this.m_parentNode != null) {
            return this.m_parentNode.getPublicId();
        }
        return null;
    }

    public String getSystemId() {
        Stylesheet stylesheet = getStylesheet();
        if (stylesheet == null) {
            return null;
        }
        return stylesheet.getHref();
    }

    public void setLocaterInfo(SourceLocator sourceLocator) {
        this.m_lineNumber = sourceLocator.getLineNumber();
        this.m_columnNumber = sourceLocator.getColumnNumber();
    }

    public void setEndLocaterInfo(SourceLocator sourceLocator) {
        this.m_endLineNumber = sourceLocator.getLineNumber();
        this.m_endColumnNumber = sourceLocator.getColumnNumber();
    }

    public boolean hasVariableDecl() {
        return this.m_hasVariableDecl;
    }

    public void setXmlSpace(int i) {
        this.m_defaultSpace = 2 == i;
    }

    public boolean getXmlSpace() {
        return this.m_defaultSpace;
    }

    public List getDeclaredPrefixes() {
        return this.m_declaredPrefixes;
    }

    public void setPrefixes(NamespaceSupport namespaceSupport) throws TransformerException {
        setPrefixes(namespaceSupport, false);
    }

    public void setPrefixes(NamespaceSupport namespaceSupport, boolean z) throws TransformerException {
        Enumeration declaredPrefixes = namespaceSupport.getDeclaredPrefixes();
        while (declaredPrefixes.hasMoreElements()) {
            String str = (String) declaredPrefixes.nextElement();
            if (this.m_declaredPrefixes == null) {
                this.m_declaredPrefixes = new ArrayList();
            }
            String uri = namespaceSupport.getURI(str);
            if (!z || !uri.equals(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL)) {
                this.m_declaredPrefixes.add(new XMLNSDecl(str, uri, false));
            }
        }
    }

    @Override
    public String getNamespaceForPrefix(String str, Node node) {
        error(XSLTErrorResources.ER_CANT_RESOLVE_NSPREFIX, null);
        return null;
    }

    @Override
    public String getNamespaceForPrefix(String str) {
        List list = this.m_declaredPrefixes;
        if (list != null) {
            int size = list.size();
            if (str.equals("#default")) {
                str = "";
            }
            for (int i = 0; i < size; i++) {
                XMLNSDecl xMLNSDecl = (XMLNSDecl) list.get(i);
                if (str.equals(xMLNSDecl.getPrefix())) {
                    return xMLNSDecl.getURI();
                }
            }
        }
        if (this.m_parentNode != null) {
            return this.m_parentNode.getNamespaceForPrefix(str);
        }
        if ("xml".equals(str)) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        return null;
    }

    List getPrefixTable() {
        return this.m_prefixTable;
    }

    void setPrefixTable(List list) {
        this.m_prefixTable = list;
    }

    public boolean containsExcludeResultPrefix(String str, String str2) {
        ElemTemplateElement parentElem = getParentElem();
        if (parentElem != null) {
            return parentElem.containsExcludeResultPrefix(str, str2);
        }
        return false;
    }

    private boolean excludeResultNSDecl(String str, String str2) throws TransformerException {
        if (str2 != null) {
            return str2.equals(org.apache.xml.utils.Constants.S_XSLNAMESPACEURL) || getStylesheet().containsExtensionElementURI(str2) || containsExcludeResultPrefix(str, str2);
        }
        return false;
    }

    public void resolvePrefixTables() throws TransformerException {
        XMLNSDecl xMLNSDecl;
        setPrefixTable(null);
        if (this.m_declaredPrefixes != null) {
            StylesheetRoot stylesheetRoot = getStylesheetRoot();
            int size = this.m_declaredPrefixes.size();
            for (int i = 0; i < size; i++) {
                XMLNSDecl xMLNSDecl2 = (XMLNSDecl) this.m_declaredPrefixes.get(i);
                String prefix = xMLNSDecl2.getPrefix();
                String uri = xMLNSDecl2.getURI();
                if (uri == null) {
                    uri = "";
                }
                boolean zExcludeResultNSDecl = excludeResultNSDecl(prefix, uri);
                if (this.m_prefixTable == null) {
                    setPrefixTable(new ArrayList());
                }
                NamespaceAlias namespaceAliasComposed = stylesheetRoot.getNamespaceAliasComposed(uri);
                if (namespaceAliasComposed != null) {
                    xMLNSDecl = new XMLNSDecl(namespaceAliasComposed.getStylesheetPrefix(), namespaceAliasComposed.getResultNamespace(), zExcludeResultNSDecl);
                } else {
                    xMLNSDecl = new XMLNSDecl(prefix, uri, zExcludeResultNSDecl);
                }
                this.m_prefixTable.add(xMLNSDecl);
            }
        }
        ElemTemplateElement parentNodeElem = getParentNodeElem();
        if (parentNodeElem != null) {
            List list = parentNodeElem.m_prefixTable;
            if (this.m_prefixTable == null && !needToCheckExclude()) {
                setPrefixTable(parentNodeElem.m_prefixTable);
                return;
            }
            int size2 = list.size();
            for (int i2 = 0; i2 < size2; i2++) {
                XMLNSDecl xMLNSDecl3 = (XMLNSDecl) list.get(i2);
                boolean zExcludeResultNSDecl2 = excludeResultNSDecl(xMLNSDecl3.getPrefix(), xMLNSDecl3.getURI());
                if (zExcludeResultNSDecl2 != xMLNSDecl3.getIsExcluded()) {
                    xMLNSDecl3 = new XMLNSDecl(xMLNSDecl3.getPrefix(), xMLNSDecl3.getURI(), zExcludeResultNSDecl2);
                }
                addOrReplaceDecls(xMLNSDecl3);
            }
            return;
        }
        if (this.m_prefixTable == null) {
            setPrefixTable(new ArrayList());
        }
    }

    void addOrReplaceDecls(XMLNSDecl xMLNSDecl) {
        for (int size = this.m_prefixTable.size() - 1; size >= 0; size--) {
            if (((XMLNSDecl) this.m_prefixTable.get(size)).getPrefix().equals(xMLNSDecl.getPrefix())) {
                return;
            }
        }
        this.m_prefixTable.add(xMLNSDecl);
    }

    boolean needToCheckExclude() {
        return false;
    }

    void executeNSDecls(TransformerImpl transformerImpl) throws TransformerException {
        executeNSDecls(transformerImpl, null);
    }

    void executeNSDecls(TransformerImpl transformerImpl, String str) throws TransformerException {
        try {
            if (this.m_prefixTable != null) {
                SerializationHandler resultTreeHandler = transformerImpl.getResultTreeHandler();
                for (int size = this.m_prefixTable.size() - 1; size >= 0; size--) {
                    XMLNSDecl xMLNSDecl = (XMLNSDecl) this.m_prefixTable.get(size);
                    if (!xMLNSDecl.getIsExcluded() && (str == null || !xMLNSDecl.getPrefix().equals(str))) {
                        resultTreeHandler.startPrefixMapping(xMLNSDecl.getPrefix(), xMLNSDecl.getURI(), true);
                    }
                }
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    void unexecuteNSDecls(TransformerImpl transformerImpl) throws TransformerException {
        unexecuteNSDecls(transformerImpl, null);
    }

    void unexecuteNSDecls(TransformerImpl transformerImpl, String str) throws TransformerException {
        try {
            if (this.m_prefixTable != null) {
                SerializationHandler resultTreeHandler = transformerImpl.getResultTreeHandler();
                int size = this.m_prefixTable.size();
                for (int i = 0; i < size; i++) {
                    XMLNSDecl xMLNSDecl = (XMLNSDecl) this.m_prefixTable.get(i);
                    if (!xMLNSDecl.getIsExcluded() && (str == null || !xMLNSDecl.getPrefix().equals(str))) {
                        resultTreeHandler.endPrefixMapping(xMLNSDecl.getPrefix());
                    }
                }
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    public void setUid(int i) {
        this.m_docOrderNumber = i;
    }

    public int getUid() {
        return this.m_docOrderNumber;
    }

    @Override
    public Node getParentNode() {
        return this.m_parentNode;
    }

    public ElemTemplateElement getParentElem() {
        return this.m_parentNode;
    }

    public void setParentElem(ElemTemplateElement elemTemplateElement) {
        this.m_parentNode = elemTemplateElement;
    }

    @Override
    public Node getNextSibling() {
        return this.m_nextSibling;
    }

    @Override
    public Node getPreviousSibling() {
        Node parentNode = getParentNode();
        if (parentNode != null) {
            Node node = null;
            for (Node firstChild = parentNode.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
                if (firstChild != this) {
                    node = firstChild;
                } else {
                    return node;
                }
            }
        }
        return null;
    }

    public ElemTemplateElement getPreviousSiblingElem() {
        ElemTemplateElement parentNodeElem = getParentNodeElem();
        if (parentNodeElem != null) {
            ElemTemplateElement elemTemplateElement = null;
            for (ElemTemplateElement firstChildElem = parentNodeElem.getFirstChildElem(); firstChildElem != null; firstChildElem = firstChildElem.getNextSiblingElem()) {
                if (firstChildElem != this) {
                    elemTemplateElement = firstChildElem;
                } else {
                    return elemTemplateElement;
                }
            }
        }
        return null;
    }

    public ElemTemplateElement getNextSiblingElem() {
        return this.m_nextSibling;
    }

    public ElemTemplateElement getParentNodeElem() {
        return this.m_parentNode;
    }

    @Override
    public Node getFirstChild() {
        return this.m_firstChild;
    }

    public ElemTemplateElement getFirstChildElem() {
        return this.m_firstChild;
    }

    @Override
    public Node getLastChild() {
        ElemTemplateElement elemTemplateElement = this.m_firstChild;
        ElemTemplateElement elemTemplateElement2 = null;
        while (true) {
            ElemTemplateElement elemTemplateElement3 = elemTemplateElement2;
            elemTemplateElement2 = elemTemplateElement;
            if (elemTemplateElement2 == null) {
                return elemTemplateElement3;
            }
            elemTemplateElement = elemTemplateElement2.m_nextSibling;
        }
    }

    public ElemTemplateElement getLastChildElem() {
        ElemTemplateElement elemTemplateElement = this.m_firstChild;
        ElemTemplateElement elemTemplateElement2 = null;
        while (true) {
            ElemTemplateElement elemTemplateElement3 = elemTemplateElement2;
            elemTemplateElement2 = elemTemplateElement;
            if (elemTemplateElement2 == null) {
                return elemTemplateElement3;
            }
            elemTemplateElement = elemTemplateElement2.m_nextSibling;
        }
    }

    public Node getDOMBackPointer() {
        return this.m_DOMBackPointer;
    }

    public void setDOMBackPointer(Node node) {
        this.m_DOMBackPointer = node;
    }

    public int compareTo(Object obj) throws ClassCastException {
        ElemTemplateElement elemTemplateElement = (ElemTemplateElement) obj;
        int importCountComposed = elemTemplateElement.getStylesheetComposed().getImportCountComposed();
        int importCountComposed2 = getStylesheetComposed().getImportCountComposed();
        if (importCountComposed2 < importCountComposed) {
            return -1;
        }
        if (importCountComposed2 > importCountComposed) {
            return 1;
        }
        return getUid() - elemTemplateElement.getUid();
    }

    @Override
    public boolean shouldStripWhiteSpace(XPathContext xPathContext, Element element) throws TransformerException {
        StylesheetRoot stylesheetRoot = getStylesheetRoot();
        if (stylesheetRoot != null) {
            return stylesheetRoot.shouldStripWhiteSpace(xPathContext, element);
        }
        return false;
    }

    @Override
    public boolean canStripWhiteSpace() {
        StylesheetRoot stylesheetRoot = getStylesheetRoot();
        if (stylesheetRoot != null) {
            return stylesheetRoot.canStripWhiteSpace();
        }
        return false;
    }

    public boolean canAcceptVariables() {
        return true;
    }

    @Override
    public void exprSetParent(ExpressionNode expressionNode) {
        setParentElem((ElemTemplateElement) expressionNode);
    }

    @Override
    public ExpressionNode exprGetParent() {
        return getParentElem();
    }

    @Override
    public void exprAddChild(ExpressionNode expressionNode, int i) {
        appendChild((ElemTemplateElement) expressionNode);
    }

    @Override
    public ExpressionNode exprGetChild(int i) {
        return (ExpressionNode) item(i);
    }

    @Override
    public int exprGetNumChildren() {
        return getLength();
    }

    protected boolean accept(XSLTVisitor xSLTVisitor) {
        return xSLTVisitor.visitInstruction(this);
    }

    @Override
    public void callVisitors(XSLTVisitor xSLTVisitor) {
        if (accept(xSLTVisitor)) {
            callChildVisitors(xSLTVisitor);
        }
    }

    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        for (ElemTemplateElement elemTemplateElement = this.m_firstChild; elemTemplateElement != null; elemTemplateElement = elemTemplateElement.m_nextSibling) {
            elemTemplateElement.callVisitors(xSLTVisitor);
        }
    }

    protected void callChildVisitors(XSLTVisitor xSLTVisitor) {
        callChildVisitors(xSLTVisitor, true);
    }

    @Override
    public boolean handlesNullPrefixes() {
        return false;
    }
}
