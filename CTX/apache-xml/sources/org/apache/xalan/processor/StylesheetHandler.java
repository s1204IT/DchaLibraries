package org.apache.xalan.processor;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TemplatesHandler;
import org.apache.xalan.extensions.ExpressionVisitor;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemForEach;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.FuncDocument;
import org.apache.xalan.templates.FuncFormatNumb;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xml.utils.BoolStack;
import org.apache.xml.utils.Constants;
import org.apache.xml.utils.NamespaceSupport2;
import org.apache.xml.utils.NodeConsumer;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.apache.xpath.XPath;
import org.apache.xpath.compiler.FunctionTable;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class StylesheetHandler extends DefaultHandler implements TemplatesHandler, PrefixResolver, NodeConsumer {
    public static final int STYPE_IMPORT = 3;
    public static final int STYPE_INCLUDE = 2;
    public static final int STYPE_ROOT = 1;
    private String m_fragmentIDString;
    private boolean m_incremental;
    Stylesheet m_lastPoppedStylesheet;
    private boolean m_optimize;
    private Node m_originatingNode;
    private boolean m_source_location;
    private TransformerFactoryImpl m_stylesheetProcessor;
    StylesheetRoot m_stylesheetRoot;
    private FunctionTable m_funcTable = new FunctionTable();
    private int m_stylesheetLevel = -1;
    private boolean m_parsingComplete = false;
    private Vector m_prefixMappings = new Vector();
    private boolean m_shouldProcess = true;
    private int m_elementID = 0;
    private int m_fragmentID = 0;
    private int m_stylesheetType = 1;
    private Stack m_stylesheets = new Stack();
    private Stack m_processors = new Stack();
    private XSLTSchema m_schema = new XSLTSchema();
    private Stack m_elems = new Stack();
    private int m_docOrderCount = 0;
    Stack m_baseIdentifiers = new Stack();
    private Stack m_stylesheetLocatorStack = new Stack();
    private Stack m_importStack = new Stack();
    private Stack m_importSourceStack = new Stack();
    private boolean warnedAboutOldXSLTNamespace = false;
    Stack m_nsSupportStack = new Stack();
    private BoolStack m_spacePreserveStack = new BoolStack();

    public StylesheetHandler(TransformerFactoryImpl transformerFactoryImpl) throws TransformerConfigurationException {
        this.m_optimize = true;
        this.m_incremental = false;
        this.m_source_location = false;
        this.m_funcTable.installFunction("document", FuncDocument.class);
        this.m_funcTable.installFunction("format-number", FuncFormatNumb.class);
        this.m_optimize = ((Boolean) transformerFactoryImpl.getAttribute(TransformerFactoryImpl.FEATURE_OPTIMIZE)).booleanValue();
        this.m_incremental = ((Boolean) transformerFactoryImpl.getAttribute(TransformerFactoryImpl.FEATURE_INCREMENTAL)).booleanValue();
        this.m_source_location = ((Boolean) transformerFactoryImpl.getAttribute("http://xml.apache.org/xalan/properties/source-location")).booleanValue();
        init(transformerFactoryImpl);
    }

    void init(TransformerFactoryImpl transformerFactoryImpl) {
        this.m_stylesheetProcessor = transformerFactoryImpl;
        this.m_processors.push(this.m_schema.getElementProcessor());
        pushNewNamespaceSupport();
    }

    public XPath createXPath(String str, ElemTemplateElement elemTemplateElement) throws TransformerException {
        XPath xPath = new XPath(str, elemTemplateElement, this, 0, this.m_stylesheetProcessor.getErrorListener(), this.m_funcTable);
        xPath.callVisitors(xPath, new ExpressionVisitor(getStylesheetRoot()));
        return xPath;
    }

    XPath createMatchPatternXPath(String str, ElemTemplateElement elemTemplateElement) throws TransformerException {
        XPath xPath = new XPath(str, elemTemplateElement, this, 1, this.m_stylesheetProcessor.getErrorListener(), this.m_funcTable);
        xPath.callVisitors(xPath, new ExpressionVisitor(getStylesheetRoot()));
        return xPath;
    }

    @Override
    public String getNamespaceForPrefix(String str) {
        return getNamespaceSupport().getURI(str);
    }

    @Override
    public String getNamespaceForPrefix(String str, Node node) {
        assertion(true, "can't process a context node in StylesheetHandler!");
        return null;
    }

    private boolean stackContains(Stack stack, String str) {
        int size = stack.size();
        for (int i = 0; i < size; i++) {
            if (((String) stack.elementAt(i)).equals(str)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Templates getTemplates() {
        return getStylesheetRoot();
    }

    @Override
    public void setSystemId(String str) {
        pushBaseIndentifier(str);
    }

    @Override
    public String getSystemId() {
        return getBaseIdentifier();
    }

    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException {
        return getCurrentProcessor().resolveEntity(this, str, str2);
    }

    @Override
    public void notationDecl(String str, String str2, String str3) {
        getCurrentProcessor().notationDecl(this, str, str2, str3);
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) {
        getCurrentProcessor().unparsedEntityDecl(this, str, str2, str3, str4);
    }

    XSLTElementProcessor getProcessorFor(String str, String str2, String str3) throws SAXException {
        XSLTElementProcessor currentProcessor = getCurrentProcessor();
        XSLTElementDef elemDef = currentProcessor.getElemDef();
        XSLTElementProcessor processorFor = elemDef.getProcessorFor(str, str2);
        if (processorFor == null && !(currentProcessor instanceof ProcessorStylesheetDoc) && (getStylesheet() == null || Double.valueOf(getStylesheet().getVersion()).doubleValue() > 1.0d || ((!str.equals(Constants.S_XSLNAMESPACEURL) && (currentProcessor instanceof ProcessorStylesheetElement)) || getElemVersion() > 1.0d))) {
            processorFor = elemDef.getProcessorForUnknown(str, str2);
        }
        if (processorFor == null) {
            error(XSLMessages.createMessage(XSLTErrorResources.ER_NOT_ALLOWED_IN_POSITION, new Object[]{str3}), null);
        }
        return processorFor;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.m_stylesheetLocatorStack.push(new SAXSourceLocator(locator));
    }

    @Override
    public void startDocument() throws SAXException {
        this.m_stylesheetLevel++;
        pushSpaceHandling(false);
    }

    public boolean isStylesheetParsingComplete() {
        return this.m_parsingComplete;
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            if (getStylesheetRoot() != null) {
                if (this.m_stylesheetLevel == 0) {
                    getStylesheetRoot().recompose();
                }
                XSLTElementProcessor currentProcessor = getCurrentProcessor();
                if (currentProcessor != null) {
                    currentProcessor.startNonText(this);
                }
                boolean z = true;
                this.m_stylesheetLevel--;
                popSpaceHandling();
                if (this.m_stylesheetLevel >= 0) {
                    z = false;
                }
                this.m_parsingComplete = z;
                return;
            }
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_STYLESHEETROOT, null));
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        this.m_prefixMappings.addElement(str);
        this.m_prefixMappings.addElement(str2);
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    private void flushCharacters() throws SAXException {
        XSLTElementProcessor currentProcessor = getCurrentProcessor();
        if (currentProcessor != null) {
            currentProcessor.startNonText(this);
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        NamespaceSupport namespaceSupport = getNamespaceSupport();
        namespaceSupport.pushContext();
        int size = this.m_prefixMappings.size();
        int i = 0;
        while (i < size) {
            int i2 = i + 1;
            namespaceSupport.declarePrefix((String) this.m_prefixMappings.elementAt(i), (String) this.m_prefixMappings.elementAt(i2));
            i = i2 + 1;
        }
        this.m_prefixMappings.removeAllElements();
        this.m_elementID++;
        checkForFragmentID(attributes);
        if (!this.m_shouldProcess) {
            return;
        }
        flushCharacters();
        pushSpaceHandling(attributes);
        XSLTElementProcessor processorFor = getProcessorFor(str, str2, str3);
        if (processorFor != null) {
            pushProcessor(processorFor);
            processorFor.startElement(this, str, str2, str3, attributes);
        } else {
            this.m_shouldProcess = false;
            popSpaceHandling();
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        this.m_elementID--;
        if (!this.m_shouldProcess) {
            return;
        }
        if (this.m_elementID + 1 == this.m_fragmentID) {
            this.m_shouldProcess = false;
        }
        flushCharacters();
        popSpaceHandling();
        getCurrentProcessor().endElement(this, str, str2, str3);
        popProcessor();
        getNamespaceSupport().popContext();
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (!this.m_shouldProcess) {
            return;
        }
        XSLTElementProcessor currentProcessor = getCurrentProcessor();
        XSLTElementDef elemDef = currentProcessor.getElemDef();
        if (elemDef.getType() != 2) {
            currentProcessor = elemDef.getProcessorFor(null, "text()");
        }
        if (currentProcessor == null) {
            if (!XMLCharacterRecognizer.isWhiteSpace(cArr, i, i2)) {
                error(XSLMessages.createMessage(XSLTErrorResources.ER_NONWHITESPACE_NOT_ALLOWED_IN_POSITION, null), null);
                return;
            }
            return;
        }
        currentProcessor.characters(this, cArr, i, i2);
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (!this.m_shouldProcess) {
            return;
        }
        getCurrentProcessor().ignorableWhitespace(this, cArr, i, i2);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        String strSubstring;
        if (!this.m_shouldProcess) {
            return;
        }
        String namespaceForPrefix = "";
        int iIndexOf = str.indexOf(58);
        if (iIndexOf >= 0) {
            namespaceForPrefix = getNamespaceForPrefix(str.substring(0, iIndexOf));
            strSubstring = str.substring(iIndexOf + 1);
        } else {
            strSubstring = str;
        }
        try {
            if ("xalan-doc-cache-off".equals(str) || "xalan:doc-cache-off".equals(str) || ("doc-cache-off".equals(strSubstring) && namespaceForPrefix.equals("org.apache.xalan.xslt.extensions.Redirect"))) {
                if (!(this.m_elems.peek() instanceof ElemForEach)) {
                    throw new TransformerException("xalan:doc-cache-off not allowed here!", getLocator());
                }
                ((ElemForEach) this.m_elems.peek()).m_doc_cache_off = true;
            }
        } catch (Exception e) {
        }
        flushCharacters();
        getCurrentProcessor().processingInstruction(this, str, str2);
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        if (!this.m_shouldProcess) {
            return;
        }
        getCurrentProcessor().skippedEntity(this, str);
    }

    public void warn(String str, Object[] objArr) throws SAXException {
        String strCreateWarning = XSLMessages.createWarning(str, objArr);
        SAXSourceLocator locator = getLocator();
        ErrorListener errorListener = this.m_stylesheetProcessor.getErrorListener();
        if (errorListener != null) {
            try {
                errorListener.warning(new TransformerException(strCreateWarning, locator));
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
    }

    private void assertion(boolean z, String str) throws RuntimeException {
        if (!z) {
            throw new RuntimeException(str);
        }
    }

    protected void error(String str, Exception exc) throws SAXException {
        TransformerException transformerException;
        SAXSourceLocator locator = getLocator();
        ErrorListener errorListener = this.m_stylesheetProcessor.getErrorListener();
        if (!(exc instanceof TransformerException)) {
            if (exc == null) {
                transformerException = new TransformerException(str, locator);
            } else {
                transformerException = new TransformerException(str, locator, exc);
            }
        } else {
            transformerException = (TransformerException) exc;
        }
        if (errorListener != null) {
            try {
                errorListener.error(transformerException);
                return;
            } catch (TransformerException e) {
                throw new SAXException(e);
            }
        }
        throw new SAXException(transformerException);
    }

    protected void error(String str, Object[] objArr, Exception exc) throws SAXException {
        error(XSLMessages.createMessage(str, objArr), exc);
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        String message = sAXParseException.getMessage();
        SAXSourceLocator locator = getLocator();
        try {
            this.m_stylesheetProcessor.getErrorListener().warning(new TransformerException(message, locator));
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        String message = sAXParseException.getMessage();
        SAXSourceLocator locator = getLocator();
        try {
            this.m_stylesheetProcessor.getErrorListener().error(new TransformerException(message, locator));
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        String message = sAXParseException.getMessage();
        SAXSourceLocator locator = getLocator();
        try {
            this.m_stylesheetProcessor.getErrorListener().fatalError(new TransformerException(message, locator));
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    private void checkForFragmentID(Attributes attributes) {
        if (!this.m_shouldProcess && attributes != null && this.m_fragmentIDString != null) {
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                if (attributes.getQName(i).equals("id") && attributes.getValue(i).equalsIgnoreCase(this.m_fragmentIDString)) {
                    this.m_shouldProcess = true;
                    this.m_fragmentID = this.m_elementID;
                }
            }
        }
    }

    public TransformerFactoryImpl getStylesheetProcessor() {
        return this.m_stylesheetProcessor;
    }

    int getStylesheetType() {
        return this.m_stylesheetType;
    }

    void setStylesheetType(int i) {
        this.m_stylesheetType = i;
    }

    Stylesheet getStylesheet() {
        if (this.m_stylesheets.size() == 0) {
            return null;
        }
        return (Stylesheet) this.m_stylesheets.peek();
    }

    Stylesheet getLastPoppedStylesheet() {
        return this.m_lastPoppedStylesheet;
    }

    public StylesheetRoot getStylesheetRoot() {
        if (this.m_stylesheetRoot != null) {
            this.m_stylesheetRoot.setOptimizer(this.m_optimize);
            this.m_stylesheetRoot.setIncremental(this.m_incremental);
            this.m_stylesheetRoot.setSource_location(this.m_source_location);
        }
        return this.m_stylesheetRoot;
    }

    public void pushStylesheet(Stylesheet stylesheet) {
        if (this.m_stylesheets.size() == 0) {
            this.m_stylesheetRoot = (StylesheetRoot) stylesheet;
        }
        this.m_stylesheets.push(stylesheet);
    }

    Stylesheet popStylesheet() {
        if (!this.m_stylesheetLocatorStack.isEmpty()) {
            this.m_stylesheetLocatorStack.pop();
        }
        if (!this.m_stylesheets.isEmpty()) {
            this.m_lastPoppedStylesheet = (Stylesheet) this.m_stylesheets.pop();
        }
        return this.m_lastPoppedStylesheet;
    }

    XSLTElementProcessor getCurrentProcessor() {
        return (XSLTElementProcessor) this.m_processors.peek();
    }

    void pushProcessor(XSLTElementProcessor xSLTElementProcessor) {
        this.m_processors.push(xSLTElementProcessor);
    }

    XSLTElementProcessor popProcessor() {
        return (XSLTElementProcessor) this.m_processors.pop();
    }

    public XSLTSchema getSchema() {
        return this.m_schema;
    }

    ElemTemplateElement getElemTemplateElement() {
        try {
            return (ElemTemplateElement) this.m_elems.peek();
        } catch (EmptyStackException e) {
            return null;
        }
    }

    int nextUid() {
        int i = this.m_docOrderCount;
        this.m_docOrderCount = i + 1;
        return i;
    }

    void pushElemTemplateElement(ElemTemplateElement elemTemplateElement) {
        if (elemTemplateElement.getUid() == -1) {
            elemTemplateElement.setUid(nextUid());
        }
        this.m_elems.push(elemTemplateElement);
    }

    ElemTemplateElement popElemTemplateElement() {
        return (ElemTemplateElement) this.m_elems.pop();
    }

    void pushBaseIndentifier(String str) {
        int iIndexOf;
        if (str != null && (iIndexOf = str.indexOf(35)) > -1) {
            this.m_fragmentIDString = str.substring(iIndexOf + 1);
            this.m_shouldProcess = false;
        } else {
            this.m_shouldProcess = true;
        }
        this.m_baseIdentifiers.push(str);
    }

    String popBaseIndentifier() {
        return (String) this.m_baseIdentifiers.pop();
    }

    @Override
    public String getBaseIdentifier() {
        String str = (String) (this.m_baseIdentifiers.isEmpty() ? null : this.m_baseIdentifiers.peek());
        if (str == null) {
            SAXSourceLocator locator = getLocator();
            return locator == null ? "" : locator.getSystemId();
        }
        return str;
    }

    public SAXSourceLocator getLocator() {
        if (this.m_stylesheetLocatorStack.isEmpty()) {
            SAXSourceLocator sAXSourceLocator = new SAXSourceLocator();
            sAXSourceLocator.setSystemId(getStylesheetProcessor().getDOMsystemID());
            return sAXSourceLocator;
        }
        return (SAXSourceLocator) this.m_stylesheetLocatorStack.peek();
    }

    void pushImportURL(String str) {
        this.m_importStack.push(str);
    }

    void pushImportSource(Source source) {
        this.m_importSourceStack.push(source);
    }

    boolean importStackContains(String str) {
        return stackContains(this.m_importStack, str);
    }

    String popImportURL() {
        return (String) this.m_importStack.pop();
    }

    String peekImportURL() {
        return (String) this.m_importStack.peek();
    }

    Source peekSourceFromURIResolver() {
        return (Source) this.m_importSourceStack.peek();
    }

    Source popImportSource() {
        return (Source) this.m_importSourceStack.pop();
    }

    void pushNewNamespaceSupport() {
        this.m_nsSupportStack.push(new NamespaceSupport2());
    }

    void popNamespaceSupport() {
        this.m_nsSupportStack.pop();
    }

    NamespaceSupport getNamespaceSupport() {
        return (NamespaceSupport) this.m_nsSupportStack.peek();
    }

    @Override
    public void setOriginatingNode(Node node) {
        this.m_originatingNode = node;
    }

    public Node getOriginatingNode() {
        return this.m_originatingNode;
    }

    boolean isSpacePreserve() {
        return this.m_spacePreserveStack.peek();
    }

    void popSpaceHandling() {
        this.m_spacePreserveStack.pop();
    }

    void pushSpaceHandling(boolean z) throws SAXParseException {
        this.m_spacePreserveStack.push(z);
    }

    void pushSpaceHandling(Attributes attributes) throws SAXParseException {
        String value = attributes.getValue(org.apache.xalan.templates.Constants.ATTRNAME_XMLSPACE);
        if (value == null) {
            this.m_spacePreserveStack.push(this.m_spacePreserveStack.peekOrFalse());
            return;
        }
        if (value.equals("preserve")) {
            this.m_spacePreserveStack.push(true);
            return;
        }
        if (value.equals(org.apache.xalan.templates.Constants.ATTRNAME_DEFAULT)) {
            this.m_spacePreserveStack.push(false);
            return;
        }
        SAXSourceLocator locator = getLocator();
        try {
            this.m_stylesheetProcessor.getErrorListener().error(new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_XMLSPACE_VALUE, null), locator));
            this.m_spacePreserveStack.push(this.m_spacePreserveStack.peek());
        } catch (TransformerException e) {
            throw new SAXParseException(e.getMessage(), locator, e);
        }
    }

    private double getElemVersion() {
        ElemTemplateElement elemTemplateElement = getElemTemplateElement();
        double dDoubleValue = -1.0d;
        while (true) {
            if ((dDoubleValue != -1.0d && dDoubleValue != 1.0d) || elemTemplateElement == null) {
                break;
            }
            try {
                dDoubleValue = Double.valueOf(elemTemplateElement.getXmlVersion()).doubleValue();
            } catch (Exception e) {
                dDoubleValue = -1.0d;
            }
            elemTemplateElement = elemTemplateElement.getParentElem();
        }
        if (dDoubleValue == -1.0d) {
            return 1.0d;
        }
        return dDoubleValue;
    }

    @Override
    public boolean handlesNullPrefixes() {
        return false;
    }

    public boolean getOptimize() {
        return this.m_optimize;
    }

    public boolean getIncremental() {
        return this.m_incremental;
    }

    public boolean getSource_location() {
        return this.m_source_location;
    }
}
