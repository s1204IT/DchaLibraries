package org.apache.xalan.templates;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.StringVector;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.SAXException;

public class ElemLiteralResult extends ElemUse {
    private static final String EMPTYSTRING = "";
    static final long serialVersionUID = -8703409074421657260L;
    private StringVector m_ExtensionElementURIs;
    private StringVector m_excludeResultPrefixes;
    private String m_localName;
    private String m_namespace;
    private String m_rawName;
    private String m_version;
    private boolean isLiteralResultAsStylesheet = false;
    private List m_avts = null;
    private List m_xslAttr = null;

    public void setIsLiteralResultAsStylesheet(boolean z) {
        this.isLiteralResultAsStylesheet = z;
    }

    public boolean getIsLiteralResultAsStylesheet() {
        return this.isLiteralResultAsStylesheet;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        Vector variableNames = composeState.getVariableNames();
        if (this.m_avts != null) {
            for (int size = this.m_avts.size() - 1; size >= 0; size--) {
                ((AVT) this.m_avts.get(size)).fixupVariables(variableNames, composeState.getGlobalsSize());
            }
        }
    }

    public void addLiteralResultAttribute(AVT avt) {
        if (this.m_avts == null) {
            this.m_avts = new ArrayList();
        }
        this.m_avts.add(avt);
    }

    public void addLiteralResultAttribute(String str) {
        if (this.m_xslAttr == null) {
            this.m_xslAttr = new ArrayList();
        }
        this.m_xslAttr.add(str);
    }

    public void setXmlSpace(AVT avt) {
        addLiteralResultAttribute(avt);
        String simpleString = avt.getSimpleString();
        if (simpleString.equals(Constants.ATTRNAME_DEFAULT)) {
            super.setXmlSpace(2);
        } else if (simpleString.equals("preserve")) {
            super.setXmlSpace(1);
        }
    }

    public AVT getLiteralResultAttributeNS(String str, String str2) {
        if (this.m_avts != null) {
            for (int size = this.m_avts.size() - 1; size >= 0; size--) {
                AVT avt = (AVT) this.m_avts.get(size);
                if (avt.getName().equals(str2) && avt.getURI().equals(str)) {
                    return avt;
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public String getAttributeNS(String str, String str2) {
        AVT literalResultAttributeNS = getLiteralResultAttributeNS(str, str2);
        if (literalResultAttributeNS != null) {
            return literalResultAttributeNS.getSimpleString();
        }
        return "";
    }

    public AVT getLiteralResultAttribute(String str) {
        if (this.m_avts != null) {
            for (int size = this.m_avts.size() - 1; size >= 0; size--) {
                AVT avt = (AVT) this.m_avts.get(size);
                String uri = avt.getURI();
                if (uri != null && !uri.equals("")) {
                    if (!(uri + ":" + avt.getName()).equals(str)) {
                    }
                } else if ((uri != null && !uri.equals("")) || !avt.getRawName().equals(str)) {
                }
                return avt;
            }
            return null;
        }
        return null;
    }

    @Override
    public String getAttribute(String str) {
        AVT literalResultAttribute = getLiteralResultAttribute(str);
        if (literalResultAttribute != null) {
            return literalResultAttribute.getSimpleString();
        }
        return "";
    }

    @Override
    public boolean containsExcludeResultPrefix(String str, String str2) {
        if (str2 == null || (this.m_excludeResultPrefixes == null && this.m_ExtensionElementURIs == null)) {
            return super.containsExcludeResultPrefix(str, str2);
        }
        if (str.length() == 0) {
            str = "#default";
        }
        if (this.m_excludeResultPrefixes != null) {
            for (int i = 0; i < this.m_excludeResultPrefixes.size(); i++) {
                if (str2.equals(getNamespaceForPrefix(this.m_excludeResultPrefixes.elementAt(i)))) {
                    return true;
                }
            }
        }
        if (this.m_ExtensionElementURIs == null || !this.m_ExtensionElementURIs.contains(str2)) {
            return super.containsExcludeResultPrefix(str, str2);
        }
        return true;
    }

    @Override
    public void resolvePrefixTables() throws TransformerException {
        NamespaceAlias namespaceAliasComposed;
        NamespaceAlias namespaceAliasComposed2;
        super.resolvePrefixTables();
        StylesheetRoot stylesheetRoot = getStylesheetRoot();
        if (this.m_namespace != null && this.m_namespace.length() > 0 && (namespaceAliasComposed2 = stylesheetRoot.getNamespaceAliasComposed(this.m_namespace)) != null) {
            this.m_namespace = namespaceAliasComposed2.getResultNamespace();
            String stylesheetPrefix = namespaceAliasComposed2.getStylesheetPrefix();
            if (stylesheetPrefix != null && stylesheetPrefix.length() > 0) {
                this.m_rawName = stylesheetPrefix + ":" + this.m_localName;
            } else {
                this.m_rawName = this.m_localName;
            }
        }
        if (this.m_avts != null) {
            int size = this.m_avts.size();
            for (int i = 0; i < size; i++) {
                AVT avt = (AVT) this.m_avts.get(i);
                String uri = avt.getURI();
                if (uri != null && uri.length() > 0 && (namespaceAliasComposed = stylesheetRoot.getNamespaceAliasComposed(this.m_namespace)) != null) {
                    String resultNamespace = namespaceAliasComposed.getResultNamespace();
                    String stylesheetPrefix2 = namespaceAliasComposed.getStylesheetPrefix();
                    String name = avt.getName();
                    if (stylesheetPrefix2 != null && stylesheetPrefix2.length() > 0) {
                        name = stylesheetPrefix2 + ":" + name;
                    }
                    avt.setURI(resultNamespace);
                    avt.setRawName(name);
                }
            }
        }
    }

    @Override
    boolean needToCheckExclude() {
        if (this.m_excludeResultPrefixes == null && getPrefixTable() == null && this.m_ExtensionElementURIs == null) {
            return false;
        }
        if (getPrefixTable() == null) {
            setPrefixTable(new ArrayList());
            return true;
        }
        return true;
    }

    public void setNamespace(String str) {
        if (str == null) {
            str = "";
        }
        this.m_namespace = str;
    }

    public String getNamespace() {
        return this.m_namespace;
    }

    public void setLocalName(String str) {
        this.m_localName = str;
    }

    @Override
    public String getLocalName() {
        return this.m_localName;
    }

    public void setRawName(String str) {
        this.m_rawName = str;
    }

    public String getRawName() {
        return this.m_rawName;
    }

    @Override
    public String getPrefix() {
        int length = (this.m_rawName.length() - this.m_localName.length()) - 1;
        if (length > 0) {
            return this.m_rawName.substring(0, length);
        }
        return "";
    }

    public void setExtensionElementPrefixes(StringVector stringVector) {
        this.m_ExtensionElementURIs = stringVector;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return new LiteralElementAttributes();
    }

    public class LiteralElementAttributes implements NamedNodeMap {
        private int m_count = -1;

        public LiteralElementAttributes() {
        }

        @Override
        public int getLength() {
            if (this.m_count == -1) {
                if (ElemLiteralResult.this.m_avts != null) {
                    this.m_count = ElemLiteralResult.this.m_avts.size();
                } else {
                    this.m_count = 0;
                }
            }
            return this.m_count;
        }

        @Override
        public Node getNamedItem(String str) {
            String strSubstring;
            if (getLength() == 0) {
                return null;
            }
            int iIndexOf = str.indexOf(":");
            if (-1 != iIndexOf) {
                strSubstring = str.substring(0, iIndexOf);
                str = str.substring(iIndexOf + 1);
            } else {
                strSubstring = null;
            }
            for (AVT avt : ElemLiteralResult.this.m_avts) {
                if (str.equals(avt.getName())) {
                    String uri = avt.getURI();
                    if ((strSubstring == null && uri == null) || (strSubstring != null && strSubstring.equals(uri))) {
                        return ElemLiteralResult.this.new Attribute(avt, ElemLiteralResult.this);
                    }
                }
            }
            return null;
        }

        @Override
        public Node getNamedItemNS(String str, String str2) {
            if (getLength() == 0) {
                return null;
            }
            for (AVT avt : ElemLiteralResult.this.m_avts) {
                if (str2.equals(avt.getName())) {
                    String uri = avt.getURI();
                    if ((str == null && uri == null) || (str != null && str.equals(uri))) {
                        return ElemLiteralResult.this.new Attribute(avt, ElemLiteralResult.this);
                    }
                }
            }
            return null;
        }

        @Override
        public Node item(int i) {
            if (getLength() == 0 || i >= ElemLiteralResult.this.m_avts.size()) {
                return null;
            }
            return ElemLiteralResult.this.new Attribute((AVT) ElemLiteralResult.this.m_avts.get(i), ElemLiteralResult.this);
        }

        @Override
        public Node removeNamedItem(String str) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public Node removeNamedItemNS(String str, String str2) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public Node setNamedItem(Node node) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public Node setNamedItemNS(Node node) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }
    }

    public class Attribute implements Attr {
        private AVT m_attribute;
        private Element m_owner;

        public Attribute(AVT avt, Element element) {
            this.m_owner = null;
            this.m_attribute = avt;
            this.m_owner = element;
        }

        @Override
        public Node appendChild(Node node) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public Node cloneNode(boolean z) {
            return ElemLiteralResult.this.new Attribute(this.m_attribute, this.m_owner);
        }

        @Override
        public NamedNodeMap getAttributes() {
            return null;
        }

        @Override
        public NodeList getChildNodes() {
            return new NodeList() {
                @Override
                public int getLength() {
                    return 0;
                }

                @Override
                public Node item(int i) {
                    return null;
                }
            };
        }

        @Override
        public Node getFirstChild() {
            return null;
        }

        @Override
        public Node getLastChild() {
            return null;
        }

        @Override
        public String getLocalName() {
            return this.m_attribute.getName();
        }

        @Override
        public String getNamespaceURI() {
            String uri = this.m_attribute.getURI();
            if (uri.equals("")) {
                return null;
            }
            return uri;
        }

        @Override
        public Node getNextSibling() {
            return null;
        }

        @Override
        public String getNodeName() {
            String uri = this.m_attribute.getURI();
            String localName = getLocalName();
            if (uri.equals("")) {
                return localName;
            }
            return uri + ":" + localName;
        }

        @Override
        public short getNodeType() {
            return (short) 2;
        }

        @Override
        public String getNodeValue() throws DOMException {
            return this.m_attribute.getSimpleString();
        }

        @Override
        public Document getOwnerDocument() {
            return this.m_owner.getOwnerDocument();
        }

        @Override
        public Node getParentNode() {
            return this.m_owner;
        }

        @Override
        public String getPrefix() {
            String uri = this.m_attribute.getURI();
            String rawName = this.m_attribute.getRawName();
            if (uri.equals("")) {
                return null;
            }
            return rawName.substring(0, rawName.indexOf(":"));
        }

        @Override
        public Node getPreviousSibling() {
            return null;
        }

        @Override
        public boolean hasAttributes() {
            return false;
        }

        @Override
        public boolean hasChildNodes() {
            return false;
        }

        @Override
        public Node insertBefore(Node node, Node node2) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public boolean isSupported(String str, String str2) {
            return false;
        }

        @Override
        public void normalize() {
        }

        @Override
        public Node removeChild(Node node) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public Node replaceChild(Node node, Node node2) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
            return null;
        }

        @Override
        public void setNodeValue(String str) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
        }

        @Override
        public void setPrefix(String str) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
        }

        @Override
        public String getName() {
            return this.m_attribute.getName();
        }

        @Override
        public String getValue() {
            return this.m_attribute.getSimpleString();
        }

        @Override
        public Element getOwnerElement() {
            return this.m_owner;
        }

        @Override
        public boolean getSpecified() {
            return true;
        }

        @Override
        public void setValue(String str) throws DOMException {
            ElemLiteralResult.this.throwDOMException((short) 7, XSLTErrorResources.NO_MODIFICATION_ALLOWED_ERR);
        }

        @Override
        public TypeInfo getSchemaTypeInfo() {
            return null;
        }

        @Override
        public boolean isId() {
            return false;
        }

        @Override
        public Object setUserData(String str, Object obj, UserDataHandler userDataHandler) {
            return getOwnerDocument().setUserData(str, obj, userDataHandler);
        }

        @Override
        public Object getUserData(String str) {
            return getOwnerDocument().getUserData(str);
        }

        @Override
        public Object getFeature(String str, String str2) {
            if (isSupported(str, str2)) {
                return this;
            }
            return null;
        }

        @Override
        public boolean isEqualNode(Node node) {
            return node == this;
        }

        @Override
        public String lookupNamespaceURI(String str) {
            return null;
        }

        @Override
        public boolean isDefaultNamespace(String str) {
            return false;
        }

        @Override
        public String lookupPrefix(String str) {
            return null;
        }

        @Override
        public boolean isSameNode(Node node) {
            return this == node;
        }

        @Override
        public void setTextContent(String str) throws DOMException {
            setNodeValue(str);
        }

        @Override
        public String getTextContent() throws DOMException {
            return getNodeValue();
        }

        @Override
        public short compareDocumentPosition(Node node) throws DOMException {
            return (short) 0;
        }

        @Override
        public String getBaseURI() {
            return null;
        }
    }

    public String getExtensionElementPrefix(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_ExtensionElementURIs == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.m_ExtensionElementURIs.elementAt(i);
    }

    public int getExtensionElementPrefixCount() {
        if (this.m_ExtensionElementURIs != null) {
            return this.m_ExtensionElementURIs.size();
        }
        return 0;
    }

    public boolean containsExtensionElementURI(String str) {
        if (this.m_ExtensionElementURIs == null) {
            return false;
        }
        return this.m_ExtensionElementURIs.contains(str);
    }

    @Override
    public int getXSLToken() {
        return 77;
    }

    @Override
    public String getNodeName() {
        return this.m_rawName;
    }

    public void setVersion(String str) {
        this.m_version = str;
    }

    public String getVersion() {
        return this.m_version;
    }

    public void setExcludeResultPrefixes(StringVector stringVector) {
        this.m_excludeResultPrefixes = stringVector;
    }

    private boolean excludeResultNSDecl(String str, String str2) throws TransformerException {
        if (this.m_excludeResultPrefixes != null) {
            return containsExcludeResultPrefix(str, str2);
        }
        return false;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        SerializationHandler serializationHandler = transformerImpl.getSerializationHandler();
        try {
            serializationHandler.startPrefixMapping(getPrefix(), getNamespace());
            executeNSDecls(transformerImpl);
            serializationHandler.startElement(getNamespace(), getLocalName(), getRawName());
            TransformerException transformerException = null;
            try {
                super.execute(transformerImpl);
                if (this.m_avts != null) {
                    for (int size = this.m_avts.size() - 1; size >= 0; size--) {
                        AVT avt = (AVT) this.m_avts.get(size);
                        XPathContext xPathContext = transformerImpl.getXPathContext();
                        String strEvaluate = avt.evaluate(xPathContext, xPathContext.getCurrentNode(), this);
                        if (strEvaluate != null) {
                            serializationHandler.addAttribute(avt.getURI(), avt.getName(), avt.getRawName(), "CDATA", strEvaluate, false);
                        }
                    }
                }
                transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
            } catch (TransformerException e) {
                transformerException = e;
            } catch (SAXException e2) {
                transformerException = new TransformerException(e2);
            }
            try {
                serializationHandler.endElement(getNamespace(), getLocalName(), getRawName());
                if (transformerException != null) {
                    throw transformerException;
                }
                unexecuteNSDecls(transformerImpl);
                try {
                    serializationHandler.endPrefixMapping(getPrefix());
                } catch (SAXException e3) {
                    throw new TransformerException(e3);
                }
            } catch (SAXException e4) {
                if (transformerException != null) {
                    throw transformerException;
                }
                throw new TransformerException(e4);
            }
        } catch (SAXException e5) {
            throw new TransformerException(e5);
        }
    }

    public Iterator enumerateLiteralResultAttributes() {
        if (this.m_avts == null) {
            return null;
        }
        return this.m_avts.iterator();
    }

    @Override
    protected boolean accept(XSLTVisitor xSLTVisitor) {
        return xSLTVisitor.visitLiteralResultElement(this);
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z && this.m_avts != null) {
            for (int size = this.m_avts.size() - 1; size >= 0; size--) {
                ((AVT) this.m_avts.get(size)).callVisitors(xSLTVisitor);
            }
        }
        super.callChildVisitors(xSLTVisitor, z);
    }

    public void throwDOMException(short s, String str) {
        throw new DOMException(s, XSLMessages.createMessage(str, null));
    }
}
