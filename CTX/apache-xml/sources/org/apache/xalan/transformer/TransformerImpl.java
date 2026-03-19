package org.apache.xalan.transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.xalan.extensions.ExtensionsTable;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.AVT;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemAttributeSet;
import org.apache.xalan.templates.ElemForEach;
import org.apache.xalan.templates.ElemSort;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemTextLiteral;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.OutputProperties;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.StylesheetComposed;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.templates.WhiteSpaceInfo;
import org.apache.xalan.templates.XUnresolvedVariable;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.SerializerTrace;
import org.apache.xml.serializer.ToTextStream;
import org.apache.xml.serializer.ToXMLSAXHandler;
import org.apache.xml.utils.BoolStack;
import org.apache.xml.utils.DOMBuilder;
import org.apache.xml.utils.DOMHelper;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.NodeVector;
import org.apache.xml.utils.ObjectPool;
import org.apache.xml.utils.ObjectStack;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xml.utils.ThreadControllerWrapper;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.Arg;
import org.apache.xpath.ExtensionsProvider;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.SelfIteratorNoPredicate;
import org.apache.xpath.functions.FuncExtFunction;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

public class TransformerImpl extends Transformer implements Runnable, DTMWSFilter, ExtensionsProvider, SerializerTrace {
    private int m_doc;
    private boolean m_incremental;
    ContentHandler m_inputContentHandler;
    private MsgMgr m_msgMgr;
    private boolean m_optimizer;
    private OutputProperties m_outputFormat;
    private SerializationHandler m_serializationHandler;
    private boolean m_source_location;
    private Thread m_transformThread;
    Vector m_userParams;
    private XPathContext m_xcontext;
    private Boolean m_reentryGuard = new Boolean(true);
    private FileOutputStream m_outputStream = null;
    private String m_urlOfSource = null;
    private Result m_outputTarget = null;
    private ContentHandler m_outputContentHandler = null;
    private ObjectPool m_textResultHandlerObjectPool = new ObjectPool(ToTextStream.class);
    private ObjectPool m_stringWriterObjectPool = new ObjectPool(StringWriter.class);
    private OutputProperties m_textformat = new OutputProperties("text");
    ObjectStack m_currentTemplateElements = new ObjectStack(4096);
    Stack m_currentMatchTemplates = new Stack();
    NodeVector m_currentMatchedNodes = new NodeVector();
    private StylesheetRoot m_stylesheetRoot = null;
    private boolean m_quietConflictWarnings = true;
    private KeyManager m_keyManager = new KeyManager();
    Stack m_attrSetStack = null;
    CountersTable m_countersTable = null;
    BoolStack m_currentTemplateRuleIsNull = new BoolStack();
    ObjectStack m_currentFuncResult = new ObjectStack();
    private ErrorListener m_errorHandler = new DefaultErrorHandler(false);
    private Exception m_exceptionThrown = null;
    private boolean m_hasBeenReset = false;
    private boolean m_shouldReset = true;
    private Stack m_modes = new Stack();
    private ExtensionsTable m_extensionsTable = null;
    private boolean m_hasTransformThreadErrorCatcher = false;

    public TransformerImpl(StylesheetRoot stylesheetRoot) {
        this.m_optimizer = true;
        this.m_incremental = false;
        this.m_source_location = false;
        this.m_optimizer = stylesheetRoot.getOptimizer();
        this.m_incremental = stylesheetRoot.getIncremental();
        this.m_source_location = stylesheetRoot.getSource_location();
        setStylesheet(stylesheetRoot);
        XPathContext xPathContext = new XPathContext(this);
        xPathContext.setIncremental(this.m_incremental);
        xPathContext.getDTMManager().setIncremental(this.m_incremental);
        xPathContext.setSource_location(this.m_source_location);
        xPathContext.getDTMManager().setSource_location(this.m_source_location);
        if (stylesheetRoot.isSecureProcessing()) {
            xPathContext.setSecureProcessing(true);
        }
        setXPathContext(xPathContext);
        getXPathContext().setNamespaceContext(stylesheetRoot);
    }

    public ExtensionsTable getExtensionsTable() {
        return this.m_extensionsTable;
    }

    void setExtensionsTable(StylesheetRoot stylesheetRoot) throws TransformerException {
        try {
            if (stylesheetRoot.getExtensions() != null && !stylesheetRoot.isSecureProcessing()) {
                this.m_extensionsTable = new ExtensionsTable(stylesheetRoot);
            }
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean functionAvailable(String str, String str2) throws TransformerException {
        return getExtensionsTable().functionAvailable(str, str2);
    }

    @Override
    public boolean elementAvailable(String str, String str2) throws TransformerException {
        return getExtensionsTable().elementAvailable(str, str2);
    }

    @Override
    public Object extFunction(String str, String str2, Vector vector, Object obj) throws TransformerException {
        return getExtensionsTable().extFunction(str, str2, vector, obj, getXPathContext().getExpressionContext());
    }

    @Override
    public Object extFunction(FuncExtFunction funcExtFunction, Vector vector) throws TransformerException {
        return getExtensionsTable().extFunction(funcExtFunction, vector, getXPathContext().getExpressionContext());
    }

    @Override
    public void reset() {
        if (!this.m_hasBeenReset && this.m_shouldReset) {
            this.m_hasBeenReset = true;
            if (this.m_outputStream != null) {
                try {
                    this.m_outputStream.close();
                } catch (IOException e) {
                }
            }
            this.m_outputStream = null;
            this.m_countersTable = null;
            this.m_xcontext.reset();
            this.m_xcontext.getVarStack().reset();
            resetUserParameters();
            this.m_currentTemplateElements.removeAllElements();
            this.m_currentMatchTemplates.removeAllElements();
            this.m_currentMatchedNodes.removeAllElements();
            this.m_serializationHandler = null;
            this.m_outputTarget = null;
            this.m_keyManager = new KeyManager();
            this.m_attrSetStack = null;
            this.m_countersTable = null;
            this.m_currentTemplateRuleIsNull = new BoolStack();
            this.m_doc = -1;
            this.m_transformThread = null;
            this.m_xcontext.getSourceTreeManager().reset();
        }
    }

    public Thread getTransformThread() {
        return this.m_transformThread;
    }

    public void setTransformThread(Thread thread) {
        this.m_transformThread = thread;
    }

    public boolean hasTransformThreadErrorCatcher() {
        return this.m_hasTransformThreadErrorCatcher;
    }

    public void transform(Source source) throws TransformerException {
        transform(source, true);
    }

    public void transform(Source source, boolean z) throws TransformerException {
        Source source2;
        DTM dtm;
        Exception exceptionThrown;
        String str;
        try {
            try {
                try {
                    try {
                        if (getXPathContext().getNamespaceContext() == null) {
                            getXPathContext().setNamespaceContext(getStylesheet());
                        }
                        String systemId = source.getSystemId();
                        if (systemId == null) {
                            systemId = this.m_stylesheetRoot.getBaseIdentifier();
                        }
                        if (systemId == null) {
                            String property = "";
                            try {
                                property = System.getProperty("user.dir");
                            } catch (SecurityException e) {
                            }
                            if (property.startsWith(File.separator)) {
                                str = "file://" + property;
                            } else {
                                str = "file:///" + property;
                            }
                            systemId = str + File.separatorChar + source.getClass().getName();
                        }
                        setBaseURLOfSource(systemId);
                        DTMManager dTMManager = this.m_xcontext.getDTMManager();
                        if (((source instanceof StreamSource) && source.getSystemId() == null && ((StreamSource) source).getInputStream() == null && ((StreamSource) source).getReader() == null) || (((source instanceof SAXSource) && ((SAXSource) source).getInputSource() == null && ((SAXSource) source).getXMLReader() == null) || ((source instanceof DOMSource) && ((DOMSource) source).getNode() == null))) {
                            try {
                                DocumentBuilder documentBuilderNewDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                                String systemId2 = source.getSystemId();
                                DOMSource dOMSource = new DOMSource(documentBuilderNewDocumentBuilder.newDocument());
                                if (systemId2 != null) {
                                    try {
                                        dOMSource.setSystemId(systemId2);
                                    } catch (ParserConfigurationException e2) {
                                        e = e2;
                                        source = dOMSource;
                                        fatalError(e);
                                        source2 = source;
                                    }
                                }
                                source2 = dOMSource;
                            } catch (ParserConfigurationException e3) {
                                e = e3;
                            }
                            dtm = dTMManager.getDTM(source2, false, this, true, true);
                            dtm.setDocumentBaseURI(systemId);
                            transformNode(dtm.getDocument());
                            exceptionThrown = getExceptionThrown();
                            if (exceptionThrown == null) {
                            }
                        } else {
                            source2 = source;
                            dtm = dTMManager.getDTM(source2, false, this, true, true);
                            dtm.setDocumentBaseURI(systemId);
                            try {
                                transformNode(dtm.getDocument());
                                exceptionThrown = getExceptionThrown();
                                if (exceptionThrown == null) {
                                    if (exceptionThrown instanceof TransformerException) {
                                        throw ((TransformerException) exceptionThrown);
                                    }
                                    if (exceptionThrown instanceof WrappedRuntimeException) {
                                        fatalError(((WrappedRuntimeException) exceptionThrown).getException());
                                    } else {
                                        throw new TransformerException(exceptionThrown);
                                    }
                                } else if (this.m_serializationHandler != null) {
                                    this.m_serializationHandler.endDocument();
                                }
                            } finally {
                                if (z) {
                                    dTMManager.release(dtm, true);
                                }
                            }
                        }
                    } catch (SAXException e4) {
                        this.m_errorHandler.fatalError(new TransformerException(e4));
                    }
                } catch (WrappedRuntimeException e5) {
                    Exception exception = e5.getException();
                    while (exception instanceof WrappedRuntimeException) {
                        exception = ((WrappedRuntimeException) exception).getException();
                    }
                    fatalError(exception);
                }
            } catch (SAXParseException e6) {
                fatalError(e6);
            }
        } finally {
            this.m_hasTransformThreadErrorCatcher = false;
            reset();
        }
    }

    private void fatalError(Throwable th) throws TransformerException {
        if (th instanceof SAXParseException) {
            this.m_errorHandler.fatalError(new TransformerException(th.getMessage(), new SAXSourceLocator((SAXParseException) th)));
        } else {
            this.m_errorHandler.fatalError(new TransformerException(th));
        }
    }

    public void setBaseURLOfSource(String str) {
        this.m_urlOfSource = str;
    }

    @Override
    public String getOutputProperty(String str) throws IllegalArgumentException {
        String property = getOutputFormat().getProperty(str);
        if (property == null && !OutputProperties.isLegalPropertyKey(str)) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{str}));
        }
        return property;
    }

    public String getOutputPropertyNoDefault(String str) throws IllegalArgumentException {
        String str2 = (String) getOutputFormat().getProperties().get(str);
        if (str2 == null && !OutputProperties.isLegalPropertyKey(str)) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{str}));
        }
        return str2;
    }

    @Override
    public void setOutputProperty(String str, String str2) throws IllegalArgumentException {
        synchronized (this.m_reentryGuard) {
            if (this.m_outputFormat == null) {
                this.m_outputFormat = (OutputProperties) getStylesheet().getOutputComposed().clone();
            }
            if (!OutputProperties.isLegalPropertyKey(str)) {
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{str}));
            }
            this.m_outputFormat.setProperty(str, str2);
        }
    }

    @Override
    public void setOutputProperties(Properties properties) throws IllegalArgumentException {
        synchronized (this.m_reentryGuard) {
            try {
                if (properties != null) {
                    String str = (String) properties.get(Constants.ATTRNAME_OUTPUT_METHOD);
                    if (str != null) {
                        this.m_outputFormat = new OutputProperties(str);
                    } else if (this.m_outputFormat == null) {
                        this.m_outputFormat = new OutputProperties();
                    }
                    this.m_outputFormat.copyFrom(properties);
                    this.m_outputFormat.copyFrom(this.m_stylesheetRoot.getOutputProperties());
                } else {
                    this.m_outputFormat = null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override
    public Properties getOutputProperties() {
        return (Properties) getOutputFormat().getProperties().clone();
    }

    public SerializationHandler createSerializationHandler(Result result) throws TransformerException {
        return createSerializationHandler(result, getOutputFormat());
    }

    public SerializationHandler createSerializationHandler(Result result, OutputProperties outputProperties) throws TransformerException {
        SerializationHandler toXMLSAXHandler;
        Document documentCreateDocument;
        short nodeType;
        Node node;
        DOMBuilder dOMBuilder;
        Document ownerDocument;
        if (result instanceof DOMResult) {
            DOMResult dOMResult = (DOMResult) result;
            Node node2 = dOMResult.getNode();
            Node nextSibling = dOMResult.getNextSibling();
            if (node2 != null) {
                nodeType = node2.getNodeType();
                if (9 == nodeType) {
                    ownerDocument = (Document) node2;
                } else {
                    ownerDocument = node2.getOwnerDocument();
                }
                Document document = ownerDocument;
                node = node2;
                documentCreateDocument = document;
            } else {
                documentCreateDocument = DOMHelper.createDocument(this.m_stylesheetRoot.isSecureProcessing());
                short nodeType2 = documentCreateDocument.getNodeType();
                dOMResult.setNode(documentCreateDocument);
                nodeType = nodeType2;
                node = documentCreateDocument;
            }
            if (11 == nodeType) {
                dOMBuilder = new DOMBuilder(documentCreateDocument, (DocumentFragment) node);
            } else {
                dOMBuilder = new DOMBuilder(documentCreateDocument, node);
            }
            if (nextSibling != null) {
                dOMBuilder.setNextSibling(nextSibling);
            }
            toXMLSAXHandler = new ToXMLSAXHandler(dOMBuilder, dOMBuilder, outputProperties.getProperty("encoding"));
        } else {
            if (result instanceof SAXResult) {
                ContentHandler handler = ((SAXResult) result).getHandler();
                if (handler == null) {
                    throw new IllegalArgumentException("handler can not be null for a SAXResult");
                }
                LexicalHandler lexicalHandler = handler instanceof LexicalHandler ? (LexicalHandler) handler : null;
                String property = outputProperties.getProperty("encoding");
                outputProperties.getProperty(Constants.ATTRNAME_OUTPUT_METHOD);
                ToXMLSAXHandler toXMLSAXHandler2 = new ToXMLSAXHandler(handler, lexicalHandler, property);
                toXMLSAXHandler2.setShouldOutputNSAttr(false);
                String property2 = outputProperties.getProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC);
                String property3 = outputProperties.getProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM);
                if (property3 != null) {
                    toXMLSAXHandler2.setDoctypeSystem(property3);
                }
                if (property2 != null) {
                    toXMLSAXHandler2.setDoctypePublic(property2);
                }
                if (handler instanceof TransformerClient) {
                    XalanTransformState xalanTransformState = new XalanTransformState();
                    ((TransformerClient) handler).setTransformState(xalanTransformState);
                    toXMLSAXHandler2.setTransformState(xalanTransformState);
                }
                toXMLSAXHandler = toXMLSAXHandler2;
            } else if (result instanceof StreamResult) {
                StreamResult streamResult = (StreamResult) result;
                try {
                    toXMLSAXHandler = (SerializationHandler) SerializerFactory.getSerializer(outputProperties.getProperties());
                    if (streamResult.getWriter() != null) {
                        toXMLSAXHandler.setWriter(streamResult.getWriter());
                    } else if (streamResult.getOutputStream() != null) {
                        toXMLSAXHandler.setOutputStream(streamResult.getOutputStream());
                    } else if (streamResult.getSystemId() != null) {
                        String systemId = streamResult.getSystemId();
                        if (systemId.startsWith("file:///")) {
                            if (systemId.substring(8).indexOf(":") > 0) {
                                systemId = systemId.substring(8);
                            } else {
                                systemId = systemId.substring(7);
                            }
                        } else if (systemId.startsWith("file:/")) {
                            if (systemId.substring(6).indexOf(":") > 0) {
                                systemId = systemId.substring(6);
                            } else {
                                systemId = systemId.substring(5);
                            }
                        }
                        this.m_outputStream = new FileOutputStream(systemId);
                        toXMLSAXHandler.setOutputStream(this.m_outputStream);
                    } else {
                        throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_OUTPUT_SPECIFIED, null));
                    }
                } catch (IOException e) {
                    throw new TransformerException(e);
                }
            } else {
                throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_CANNOT_TRANSFORM_TO_RESULT_TYPE, new Object[]{result.getClass().getName()}));
            }
        }
        toXMLSAXHandler.setTransformer(this);
        toXMLSAXHandler.setSourceLocator(getStylesheet());
        return toXMLSAXHandler;
    }

    @Override
    public void transform(Source source, Result result) throws TransformerException {
        transform(source, result, true);
    }

    public void transform(Source source, Result result, boolean z) throws TransformerException {
        synchronized (this.m_reentryGuard) {
            setSerializationHandler(createSerializationHandler(result));
            this.m_outputTarget = result;
            transform(source, z);
        }
    }

    public void transformNode(int i, Result result) throws TransformerException {
        setSerializationHandler(createSerializationHandler(result));
        this.m_outputTarget = result;
        transformNode(i);
    }

    public void transformNode(int i) throws TransformerException {
        setExtensionsTable(getStylesheet());
        synchronized (this.m_serializationHandler) {
            try {
                this.m_hasBeenReset = false;
                XPathContext xPathContext = getXPathContext();
                xPathContext.getDTM(i);
                try {
                    pushGlobalVars(i);
                    StylesheetRoot stylesheet = getStylesheet();
                    int globalImportCount = stylesheet.getGlobalImportCount();
                    for (int i2 = 0; i2 < globalImportCount; i2++) {
                        StylesheetComposed globalImport = stylesheet.getGlobalImport(i2);
                        int includeCountComposed = globalImport.getIncludeCountComposed();
                        for (int i3 = -1; i3 < includeCountComposed; i3++) {
                            Stylesheet includeComposed = globalImport.getIncludeComposed(i3);
                            includeComposed.runtimeInit(this);
                            for (ElemTemplateElement firstChildElem = includeComposed.getFirstChildElem(); firstChildElem != null; firstChildElem = firstChildElem.getNextSiblingElem()) {
                                firstChildElem.runtimeInit(this);
                            }
                        }
                    }
                    SelfIteratorNoPredicate selfIteratorNoPredicate = new SelfIteratorNoPredicate();
                    selfIteratorNoPredicate.setRoot(i, xPathContext);
                    xPathContext.pushContextNodeList(selfIteratorNoPredicate);
                } catch (Exception e) {
                    e = e;
                    while (e instanceof WrappedRuntimeException) {
                        Exception exception = ((WrappedRuntimeException) e).getException();
                        if (exception != null) {
                            e = exception;
                        }
                    }
                    if (this.m_serializationHandler != null) {
                        try {
                            if (e instanceof SAXParseException) {
                                this.m_serializationHandler.fatalError((SAXParseException) e);
                            } else if (e instanceof TransformerException) {
                                TransformerException transformerException = (TransformerException) e;
                                this.m_serializationHandler.fatalError(new SAXParseException(transformerException.getMessage(), new SAXSourceLocator(transformerException.getLocator()), transformerException));
                            } else {
                                this.m_serializationHandler.fatalError(new SAXParseException(e.getMessage(), new SAXSourceLocator(), e));
                            }
                        } catch (Exception e2) {
                        }
                    }
                    if (e instanceof TransformerException) {
                        this.m_errorHandler.fatalError((TransformerException) e);
                    } else if (e instanceof SAXParseException) {
                        this.m_errorHandler.fatalError(new TransformerException(e.getMessage(), new SAXSourceLocator((SAXParseException) e), e));
                    } else {
                        this.m_errorHandler.fatalError(new TransformerException(e));
                    }
                }
                try {
                    applyTemplateToNode(null, null, i);
                    xPathContext.popContextNodeList();
                    if (this.m_serializationHandler != null) {
                        this.m_serializationHandler.endDocument();
                    }
                } catch (Throwable th) {
                    xPathContext.popContextNodeList();
                    throw th;
                }
            } finally {
                reset();
            }
        }
    }

    public ContentHandler getInputContentHandler() {
        return getInputContentHandler(false);
    }

    public ContentHandler getInputContentHandler(boolean z) {
        if (this.m_inputContentHandler == null) {
            this.m_inputContentHandler = new TransformerHandlerImpl(this, z, this.m_urlOfSource);
        }
        return this.m_inputContentHandler;
    }

    public void setOutputFormat(OutputProperties outputProperties) {
        this.m_outputFormat = outputProperties;
    }

    public OutputProperties getOutputFormat() {
        if (this.m_outputFormat == null) {
            return getStylesheet().getOutputComposed();
        }
        return this.m_outputFormat;
    }

    public void setParameter(String str, String str2, Object obj) {
        VariableStack varStack = getXPathContext().getVarStack();
        QName qName = new QName(str2, str);
        XObject xObjectCreate = XObject.create(obj, getXPathContext());
        Vector variablesAndParamsComposed = this.m_stylesheetRoot.getVariablesAndParamsComposed();
        int size = variablesAndParamsComposed.size();
        while (true) {
            size--;
            if (size >= 0) {
                ElemVariable elemVariable = (ElemVariable) variablesAndParamsComposed.elementAt(size);
                if (elemVariable.getXSLToken() == 41 && elemVariable.getName().equals(qName)) {
                    varStack.setGlobalVariable(size, xObjectCreate);
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void setParameter(String str, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_SET_PARAM_VALUE, new Object[]{str}));
        }
        StringTokenizer stringTokenizer = new StringTokenizer(str, "{}", false);
        try {
            String strNextToken = stringTokenizer.nextToken();
            String strNextToken2 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
            if (this.m_userParams == null) {
                this.m_userParams = new Vector();
            }
            if (strNextToken2 == null) {
                replaceOrPushUserParam(new QName(strNextToken), XObject.create(obj, getXPathContext()));
                setParameter(strNextToken, null, obj);
            } else {
                replaceOrPushUserParam(new QName(strNextToken, strNextToken2), XObject.create(obj, getXPathContext()));
                setParameter(strNextToken2, strNextToken, obj);
            }
        } catch (NoSuchElementException e) {
        }
    }

    private void replaceOrPushUserParam(QName qName, XObject xObject) {
        for (int size = this.m_userParams.size() - 1; size >= 0; size--) {
            if (((Arg) this.m_userParams.elementAt(size)).getQName().equals(qName)) {
                this.m_userParams.setElementAt(new Arg(qName, xObject, true), size);
                return;
            }
        }
        this.m_userParams.addElement(new Arg(qName, xObject, true));
    }

    @Override
    public Object getParameter(String str) {
        try {
            QName qNameFromString = QName.getQNameFromString(str);
            if (this.m_userParams == null) {
                return null;
            }
            for (int size = this.m_userParams.size() - 1; size >= 0; size--) {
                Arg arg = (Arg) this.m_userParams.elementAt(size);
                if (arg.getQName().equals(qNameFromString)) {
                    return arg.getVal().object();
                }
            }
            return null;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private void resetUserParameters() {
        try {
            if (this.m_userParams == null) {
                return;
            }
            for (int size = this.m_userParams.size() - 1; size >= 0; size--) {
                Arg arg = (Arg) this.m_userParams.elementAt(size);
                QName qName = arg.getQName();
                setParameter(qName.getLocalPart(), qName.getNamespace(), arg.getVal().object());
            }
        } catch (NoSuchElementException e) {
        }
    }

    public void setParameters(Properties properties) {
        clearParameters();
        Enumeration<?> enumerationPropertyNames = properties.propertyNames();
        while (enumerationPropertyNames.hasMoreElements()) {
            String property = properties.getProperty((String) enumerationPropertyNames.nextElement());
            StringTokenizer stringTokenizer = new StringTokenizer(property, "{}", false);
            try {
                String strNextToken = stringTokenizer.nextToken();
                String strNextToken2 = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;
                if (strNextToken2 == null) {
                    setParameter(strNextToken, null, properties.getProperty(property));
                } else {
                    setParameter(strNextToken2, strNextToken, properties.getProperty(property));
                }
            } catch (NoSuchElementException e) {
            }
        }
    }

    @Override
    public void clearParameters() {
        synchronized (this.m_reentryGuard) {
            this.m_xcontext.setVarStack(new VariableStack());
            this.m_userParams = null;
        }
    }

    protected void pushGlobalVars(int i) throws TransformerException {
        VariableStack varStack = this.m_xcontext.getVarStack();
        Vector variablesAndParamsComposed = getStylesheet().getVariablesAndParamsComposed();
        int size = variablesAndParamsComposed.size();
        varStack.link(size);
        while (true) {
            size--;
            if (size >= 0) {
                XUnresolvedVariable xUnresolvedVariable = new XUnresolvedVariable((ElemVariable) variablesAndParamsComposed.elementAt(size), i, this, varStack.getStackFrame(), 0, true);
                if (varStack.elementAt(size) == null) {
                    varStack.setGlobalVariable(size, xUnresolvedVariable);
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void setURIResolver(URIResolver uRIResolver) {
        synchronized (this.m_reentryGuard) {
            this.m_xcontext.getSourceTreeManager().setURIResolver(uRIResolver);
        }
    }

    @Override
    public URIResolver getURIResolver() {
        return this.m_xcontext.getSourceTreeManager().getURIResolver();
    }

    public void setContentHandler(ContentHandler contentHandler) {
        if (contentHandler == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_NULL_CONTENT_HANDLER, null));
        }
        this.m_outputContentHandler = contentHandler;
        if (this.m_serializationHandler == null) {
            ToXMLSAXHandler toXMLSAXHandler = new ToXMLSAXHandler();
            toXMLSAXHandler.setContentHandler(contentHandler);
            toXMLSAXHandler.setTransformer(this);
            this.m_serializationHandler = toXMLSAXHandler;
            return;
        }
        this.m_serializationHandler.setContentHandler(contentHandler);
    }

    public ContentHandler getContentHandler() {
        return this.m_outputContentHandler;
    }

    public int transformToRTF(ElemTemplateElement elemTemplateElement) throws TransformerException {
        return transformToRTF(elemTemplateElement, this.m_xcontext.getRTFDTM());
    }

    public int transformToGlobalRTF(ElemTemplateElement elemTemplateElement) throws TransformerException {
        return transformToRTF(elemTemplateElement, this.m_xcontext.getGlobalRTFDTM());
    }

    private int transformToRTF(ElemTemplateElement elemTemplateElement, DTM dtm) throws TransformerException {
        XPathContext xPathContext = this.m_xcontext;
        ContentHandler contentHandler = dtm.getContentHandler();
        SerializationHandler serializationHandler = this.m_serializationHandler;
        ToXMLSAXHandler toXMLSAXHandler = new ToXMLSAXHandler();
        toXMLSAXHandler.setContentHandler(contentHandler);
        toXMLSAXHandler.setTransformer(this);
        this.m_serializationHandler = toXMLSAXHandler;
        SerializationHandler serializationHandler2 = this.m_serializationHandler;
        try {
            try {
                serializationHandler2.startDocument();
                serializationHandler2.flushPending();
                try {
                    executeChildTemplates(elemTemplateElement, true);
                    serializationHandler2.flushPending();
                    return dtm.getDocument();
                } finally {
                    serializationHandler2.endDocument();
                }
            } catch (SAXException e) {
                throw new TransformerException(e);
            }
        } finally {
            this.m_serializationHandler = serializationHandler;
        }
    }

    public String transformToString(ElemTemplateElement elemTemplateElement) throws TransformerException {
        ElemTemplateElement firstChildElem = elemTemplateElement.getFirstChildElem();
        if (firstChildElem == null) {
            return "";
        }
        if (elemTemplateElement.hasTextLitOnly() && this.m_optimizer) {
            return ((ElemTextLiteral) firstChildElem).getNodeValue();
        }
        SerializationHandler serializationHandler = this.m_serializationHandler;
        StringWriter stringWriter = (StringWriter) this.m_stringWriterObjectPool.getInstance();
        this.m_serializationHandler = (ToTextStream) this.m_textResultHandlerObjectPool.getInstance();
        if (this.m_serializationHandler == null) {
            this.m_serializationHandler = (SerializationHandler) SerializerFactory.getSerializer(this.m_textformat.getProperties());
        }
        this.m_serializationHandler.setTransformer(this);
        this.m_serializationHandler.setWriter(stringWriter);
        try {
            try {
                executeChildTemplates(elemTemplateElement, true);
                this.m_serializationHandler.endDocument();
                return stringWriter.toString();
            } catch (SAXException e) {
                throw new TransformerException(e);
            }
        } finally {
            stringWriter.getBuffer().setLength(0);
            try {
                stringWriter.close();
            } catch (Exception e2) {
            }
            this.m_stringWriterObjectPool.freeInstance(stringWriter);
            this.m_serializationHandler.reset();
            this.m_textResultHandlerObjectPool.freeInstance(this.m_serializationHandler);
            this.m_serializationHandler = serializationHandler;
        }
    }

    public boolean applyTemplateToNode(ElemTemplateElement elemTemplateElement, ElemTemplate elemTemplate, int i) throws Throwable {
        int i2;
        int endImportCountComposed;
        XPathContext xPathContext;
        XPathContext xPathContext2;
        ElemTemplate templateComposed;
        boolean z;
        DTM dtm = this.m_xcontext.getDTM(i);
        short nodeType = dtm.getNodeType(i);
        boolean z2 = elemTemplateElement != null && elemTemplateElement.getXSLToken() == 72;
        try {
            try {
                if (elemTemplate == null || z2) {
                    if (z2) {
                        int importCountComposed = elemTemplate.getStylesheetComposed().getImportCountComposed() - 1;
                        endImportCountComposed = elemTemplate.getStylesheetComposed().getEndImportCountComposed();
                        i2 = importCountComposed;
                    } else {
                        i2 = -1;
                        endImportCountComposed = 0;
                    }
                    if (z2 && i2 == -1) {
                        templateComposed = null;
                    } else {
                        XPathContext xPathContext3 = this.m_xcontext;
                        try {
                            xPathContext3.pushNamespaceContext(elemTemplateElement);
                            QName mode = getMode();
                            try {
                                if (z2) {
                                    xPathContext2 = xPathContext3;
                                    templateComposed = this.m_stylesheetRoot.getTemplateComposed(xPathContext3, i, mode, i2, endImportCountComposed, this.m_quietConflictWarnings, dtm);
                                } else {
                                    xPathContext2 = xPathContext3;
                                    templateComposed = this.m_stylesheetRoot.getTemplateComposed(xPathContext2, i, mode, this.m_quietConflictWarnings, dtm);
                                }
                                xPathContext2.popNamespaceContext();
                            } catch (Throwable th) {
                                th = th;
                                xPathContext = xPathContext2;
                                xPathContext.popNamespaceContext();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            xPathContext = xPathContext3;
                        }
                    }
                    if (templateComposed == null) {
                        if (nodeType != 9) {
                            if (nodeType != 11) {
                                switch (nodeType) {
                                    case 1:
                                        break;
                                    case 2:
                                    case 3:
                                    case 4:
                                        templateComposed = this.m_stylesheetRoot.getDefaultTextRule();
                                        z = true;
                                        break;
                                    default:
                                        return false;
                                }
                            }
                            templateComposed = this.m_stylesheetRoot.getDefaultRule();
                        } else {
                            templateComposed = this.m_stylesheetRoot.getDefaultRootRule();
                        }
                    }
                    pushElemTemplateElement(templateComposed);
                    this.m_xcontext.pushCurrentNode(i);
                    pushPairCurrentMatched(templateComposed, i);
                    if (!z2) {
                        this.m_xcontext.pushContextNodeList(new NodeSetDTM(i, this.m_xcontext.getDTMManager()));
                    }
                    if (!z) {
                        switch (nodeType) {
                            case 2:
                                dtm.dispatchCharactersEvents(i, getResultTreeHandler(), false);
                                break;
                            case 3:
                            case 4:
                                ClonerToResultTree.cloneToResultTree(i, nodeType, dtm, getResultTreeHandler(), false);
                                break;
                        }
                    } else {
                        this.m_xcontext.setSAXLocator(templateComposed);
                        this.m_xcontext.getVarStack().link(templateComposed.m_frameSize);
                        executeChildTemplates((ElemTemplateElement) templateComposed, true);
                    }
                    return true;
                }
                templateComposed = elemTemplate;
                pushElemTemplateElement(templateComposed);
                this.m_xcontext.pushCurrentNode(i);
                pushPairCurrentMatched(templateComposed, i);
                if (!z2) {
                }
                if (!z) {
                }
                return true;
            } catch (SAXException e) {
                throw new TransformerException(e);
            }
        } finally {
            if (!z) {
                this.m_xcontext.getVarStack().unlink();
            }
            this.m_xcontext.popCurrentNode();
            if (!z2) {
                this.m_xcontext.popContextNodeList();
            }
            popCurrentMatched();
            popElemTemplateElement();
        }
        z = false;
    }

    public void executeChildTemplates(ElemTemplateElement elemTemplateElement, Node node, QName qName, ContentHandler contentHandler) throws TransformerException {
        XPathContext xPathContext = this.m_xcontext;
        if (qName != null) {
            try {
                pushMode(qName);
            } catch (Throwable th) {
                xPathContext.popCurrentNode();
                if (qName != null) {
                    popMode();
                }
                throw th;
            }
        }
        xPathContext.pushCurrentNode(xPathContext.getDTMHandleFromNode(node));
        executeChildTemplates(elemTemplateElement, contentHandler);
        xPathContext.popCurrentNode();
        if (qName != null) {
            popMode();
        }
    }

    public void executeChildTemplates(ElemTemplateElement elemTemplateElement, boolean z) throws TransformerException {
        ElemTemplateElement firstChildElem = elemTemplateElement.getFirstChildElem();
        if (firstChildElem == null) {
            return;
        }
        if (elemTemplateElement.hasTextLitOnly() && this.m_optimizer) {
            char[] chars = ((ElemTextLiteral) firstChildElem).getChars();
            try {
                try {
                    pushElemTemplateElement(firstChildElem);
                    this.m_serializationHandler.characters(chars, 0, chars.length);
                    return;
                } catch (SAXException e) {
                    throw new TransformerException(e);
                }
            } finally {
                popElemTemplateElement();
            }
        }
        XPathContext xPathContext = this.m_xcontext;
        xPathContext.pushSAXLocatorNull();
        int size = this.m_currentTemplateElements.size();
        this.m_currentTemplateElements.push(null);
        while (firstChildElem != null) {
            if (!z) {
                try {
                    try {
                        if (firstChildElem.getXSLToken() != 48) {
                            xPathContext.setSAXLocator(firstChildElem);
                            this.m_currentTemplateElements.setElementAt(firstChildElem, size);
                            firstChildElem.execute(this);
                        }
                    } catch (RuntimeException e2) {
                        TransformerException transformerException = new TransformerException(e2);
                        transformerException.setLocator(firstChildElem);
                        throw transformerException;
                    }
                } catch (Throwable th) {
                    this.m_currentTemplateElements.pop();
                    xPathContext.popSAXLocator();
                    throw th;
                }
            }
            firstChildElem = firstChildElem.getNextSiblingElem();
        }
        this.m_currentTemplateElements.pop();
        xPathContext.popSAXLocator();
    }

    public void executeChildTemplates(ElemTemplateElement elemTemplateElement, ContentHandler contentHandler) throws TransformerException {
        SerializationHandler serializationHandler = getSerializationHandler();
        try {
            try {
                serializationHandler.flushPending();
                LexicalHandler lexicalHandler = null;
                if (contentHandler instanceof LexicalHandler) {
                    lexicalHandler = (LexicalHandler) contentHandler;
                }
                this.m_serializationHandler = new ToXMLSAXHandler(contentHandler, lexicalHandler, serializationHandler.getEncoding());
                this.m_serializationHandler.setTransformer(this);
                executeChildTemplates(elemTemplateElement, true);
            } catch (TransformerException e) {
                throw e;
            } catch (SAXException e2) {
                throw new TransformerException(e2);
            }
        } finally {
            this.m_serializationHandler = serializationHandler;
        }
    }

    public Vector processSortKeys(ElemForEach elemForEach, int i) throws TransformerException {
        String strEvaluate;
        boolean z;
        XPathContext xPathContext = this.m_xcontext;
        int sortElemCount = elemForEach.getSortElemCount();
        Vector vector = sortElemCount > 0 ? new Vector() : null;
        for (int i2 = 0; i2 < sortElemCount; i2++) {
            ElemSort sortElem = elemForEach.getSortElem(i2);
            if (sortElem.getLang() != null) {
                strEvaluate = sortElem.getLang().evaluate(xPathContext, i, elemForEach);
            } else {
                strEvaluate = null;
            }
            String strEvaluate2 = sortElem.getDataType().evaluate(xPathContext, i, elemForEach);
            if (strEvaluate2.indexOf(":") >= 0) {
                System.out.println("TODO: Need to write the hooks for QNAME sort data type");
            } else if (!strEvaluate2.equalsIgnoreCase("text") && !strEvaluate2.equalsIgnoreCase("number")) {
                elemForEach.error(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{Constants.ATTRNAME_DATATYPE, strEvaluate2});
            }
            boolean z2 = strEvaluate2 != null && strEvaluate2.equals("number");
            String strEvaluate3 = sortElem.getOrder().evaluate(xPathContext, i, elemForEach);
            if (!strEvaluate3.equalsIgnoreCase(Constants.ATTRVAL_ORDER_ASCENDING) && !strEvaluate3.equalsIgnoreCase(Constants.ATTRVAL_ORDER_DESCENDING)) {
                elemForEach.error(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{Constants.ATTRNAME_ORDER, strEvaluate3});
            }
            boolean z3 = strEvaluate3 != null && strEvaluate3.equals(Constants.ATTRVAL_ORDER_DESCENDING);
            AVT caseOrder = sortElem.getCaseOrder();
            if (caseOrder != null) {
                String strEvaluate4 = caseOrder.evaluate(xPathContext, i, elemForEach);
                if (!strEvaluate4.equalsIgnoreCase(Constants.ATTRVAL_CASEORDER_UPPER) && !strEvaluate4.equalsIgnoreCase(Constants.ATTRVAL_CASEORDER_LOWER)) {
                    elemForEach.error(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{Constants.ATTRNAME_CASEORDER, strEvaluate4});
                }
                z = strEvaluate4 != null && strEvaluate4.equals(Constants.ATTRVAL_CASEORDER_UPPER);
            } else {
                z = false;
            }
            vector.addElement(new NodeSortKey(this, sortElem.getSelect(), z2, z3, strEvaluate, z, elemForEach));
        }
        return vector;
    }

    public int getCurrentTemplateElementsCount() {
        return this.m_currentTemplateElements.size();
    }

    public ObjectStack getCurrentTemplateElements() {
        return this.m_currentTemplateElements;
    }

    public void pushElemTemplateElement(ElemTemplateElement elemTemplateElement) {
        this.m_currentTemplateElements.push(elemTemplateElement);
    }

    public void popElemTemplateElement() {
        this.m_currentTemplateElements.pop();
    }

    public void setCurrentElement(ElemTemplateElement elemTemplateElement) {
        this.m_currentTemplateElements.setTop(elemTemplateElement);
    }

    public ElemTemplateElement getCurrentElement() {
        if (this.m_currentTemplateElements.size() > 0) {
            return (ElemTemplateElement) this.m_currentTemplateElements.peek();
        }
        return null;
    }

    public int getCurrentNode() {
        return this.m_xcontext.getCurrentNode();
    }

    public ElemTemplate getCurrentTemplate() {
        ElemTemplateElement currentElement = getCurrentElement();
        while (currentElement != null && currentElement.getXSLToken() != 19) {
            currentElement = currentElement.getParentElem();
        }
        return (ElemTemplate) currentElement;
    }

    public void pushPairCurrentMatched(ElemTemplateElement elemTemplateElement, int i) {
        this.m_currentMatchTemplates.push(elemTemplateElement);
        this.m_currentMatchedNodes.push(i);
    }

    public void popCurrentMatched() {
        this.m_currentMatchTemplates.pop();
        this.m_currentMatchedNodes.pop();
    }

    public ElemTemplate getMatchedTemplate() {
        return (ElemTemplate) this.m_currentMatchTemplates.peek();
    }

    public int getMatchedNode() {
        return this.m_currentMatchedNodes.peepTail();
    }

    public DTMIterator getContextNodeList() {
        try {
            DTMIterator contextNodeList = this.m_xcontext.getContextNodeList();
            if (contextNodeList == null) {
                return null;
            }
            return contextNodeList.cloneWithReset();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public Transformer getTransformer() {
        return this;
    }

    public void setStylesheet(StylesheetRoot stylesheetRoot) {
        this.m_stylesheetRoot = stylesheetRoot;
    }

    public final StylesheetRoot getStylesheet() {
        return this.m_stylesheetRoot;
    }

    public boolean getQuietConflictWarnings() {
        return this.m_quietConflictWarnings;
    }

    public void setXPathContext(XPathContext xPathContext) {
        this.m_xcontext = xPathContext;
    }

    public final XPathContext getXPathContext() {
        return this.m_xcontext;
    }

    public SerializationHandler getResultTreeHandler() {
        return this.m_serializationHandler;
    }

    public SerializationHandler getSerializationHandler() {
        return this.m_serializationHandler;
    }

    public KeyManager getKeyManager() {
        return this.m_keyManager;
    }

    public boolean isRecursiveAttrSet(ElemAttributeSet elemAttributeSet) {
        if (this.m_attrSetStack == null) {
            this.m_attrSetStack = new Stack();
        }
        if (!this.m_attrSetStack.empty() && this.m_attrSetStack.search(elemAttributeSet) > -1) {
            return true;
        }
        return false;
    }

    public void pushElemAttributeSet(ElemAttributeSet elemAttributeSet) {
        this.m_attrSetStack.push(elemAttributeSet);
    }

    public void popElemAttributeSet() {
        this.m_attrSetStack.pop();
    }

    public CountersTable getCountersTable() {
        if (this.m_countersTable == null) {
            this.m_countersTable = new CountersTable();
        }
        return this.m_countersTable;
    }

    public boolean currentTemplateRuleIsNull() {
        return !this.m_currentTemplateRuleIsNull.isEmpty() && this.m_currentTemplateRuleIsNull.peek();
    }

    public void pushCurrentTemplateRuleIsNull(boolean z) {
        this.m_currentTemplateRuleIsNull.push(z);
    }

    public void popCurrentTemplateRuleIsNull() {
        this.m_currentTemplateRuleIsNull.pop();
    }

    public void pushCurrentFuncResult(Object obj) {
        this.m_currentFuncResult.push(obj);
    }

    public Object popCurrentFuncResult() {
        return this.m_currentFuncResult.pop();
    }

    public boolean currentFuncResultSeen() {
        return (this.m_currentFuncResult.empty() || this.m_currentFuncResult.peek() == null) ? false : true;
    }

    public MsgMgr getMsgMgr() {
        if (this.m_msgMgr == null) {
            this.m_msgMgr = new MsgMgr(this);
        }
        return this.m_msgMgr;
    }

    @Override
    public void setErrorListener(ErrorListener errorListener) throws IllegalArgumentException {
        synchronized (this.m_reentryGuard) {
            if (errorListener == null) {
                throw new IllegalArgumentException(XSLMessages.createMessage("ER_NULL_ERROR_HANDLER", null));
            }
            this.m_errorHandler = errorListener;
        }
    }

    @Override
    public ErrorListener getErrorListener() {
        return this.m_errorHandler;
    }

    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/trax/features/sax/input".equals(str) || "http://xml.org/trax/features/dom/input".equals(str)) {
            return true;
        }
        throw new SAXNotRecognizedException(str);
    }

    public QName getMode() {
        if (this.m_modes.isEmpty()) {
            return null;
        }
        return (QName) this.m_modes.peek();
    }

    public void pushMode(QName qName) {
        this.m_modes.push(qName);
    }

    public void popMode() {
        this.m_modes.pop();
    }

    public void runTransformThread(int i) {
        setTransformThread(ThreadControllerWrapper.runThread(this, i));
    }

    public void runTransformThread() {
        ThreadControllerWrapper.runThread(this, -1);
    }

    public static void runTransformThread(Runnable runnable) {
        ThreadControllerWrapper.runThread(runnable, -1);
    }

    public void waitTransformThread() throws SAXException {
        Exception exceptionThrown;
        Thread transformThread = getTransformThread();
        if (transformThread != null) {
            try {
                ThreadControllerWrapper.waitThread(transformThread, this);
                if (!hasTransformThreadErrorCatcher() && (exceptionThrown = getExceptionThrown()) != null) {
                    exceptionThrown.printStackTrace();
                    throw new SAXException(exceptionThrown);
                }
                setTransformThread(null);
            } catch (InterruptedException e) {
            }
        }
    }

    public Exception getExceptionThrown() {
        return this.m_exceptionThrown;
    }

    public void setExceptionThrown(Exception exc) {
        this.m_exceptionThrown = exc;
    }

    public void setSourceTreeDocForThread(int i) {
        this.m_doc = i;
    }

    void postExceptionFromThread(Exception exc) {
        this.m_exceptionThrown = exc;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void run() {
        TransformerHandlerImpl transformerHandlerImpl;
        this.m_hasBeenReset = false;
        try {
            try {
                try {
                    transformNode(this.m_doc);
                } catch (Exception e) {
                    if (this.m_transformThread == null) {
                        throw new RuntimeException(e.getMessage());
                    }
                    postExceptionFromThread(e);
                    if (!(this.m_inputContentHandler instanceof TransformerHandlerImpl)) {
                        return;
                    } else {
                        transformerHandlerImpl = (TransformerHandlerImpl) this.m_inputContentHandler;
                    }
                }
                if (this.m_inputContentHandler instanceof TransformerHandlerImpl) {
                    transformerHandlerImpl = (TransformerHandlerImpl) this.m_inputContentHandler;
                    transformerHandlerImpl.clearCoRoutine();
                }
            } catch (Throwable th) {
                if (this.m_inputContentHandler instanceof TransformerHandlerImpl) {
                    ((TransformerHandlerImpl) this.m_inputContentHandler).clearCoRoutine();
                }
                throw th;
            }
        } catch (Exception e2) {
            if (this.m_transformThread == null) {
                throw new RuntimeException(e2.getMessage());
            }
            postExceptionFromThread(e2);
        }
    }

    @Override
    public short getShouldStripSpace(int i, DTM dtm) {
        try {
            WhiteSpaceInfo whiteSpaceInfo = this.m_stylesheetRoot.getWhiteSpaceInfo(this.m_xcontext, i, dtm);
            if (whiteSpaceInfo == null) {
                return (short) 3;
            }
            return whiteSpaceInfo.getShouldStripSpace() ? (short) 2 : (short) 1;
        } catch (TransformerException e) {
            return (short) 3;
        }
    }

    public void init(ToXMLSAXHandler toXMLSAXHandler, Transformer transformer, ContentHandler contentHandler) {
        toXMLSAXHandler.setTransformer(transformer);
        toXMLSAXHandler.setContentHandler(contentHandler);
    }

    public void setSerializationHandler(SerializationHandler serializationHandler) {
        this.m_serializationHandler = serializationHandler;
    }

    @Override
    public void fireGenerateEvent(int i, char[] cArr, int i2, int i3) {
    }

    @Override
    public void fireGenerateEvent(int i, String str, Attributes attributes) {
    }

    @Override
    public void fireGenerateEvent(int i, String str, String str2) {
    }

    @Override
    public void fireGenerateEvent(int i, String str) {
    }

    @Override
    public void fireGenerateEvent(int i) {
    }

    @Override
    public boolean hasTraceListeners() {
        return false;
    }

    public boolean getIncremental() {
        return this.m_incremental;
    }

    public boolean getOptimize() {
        return this.m_optimizer;
    }

    public boolean getSource_location() {
        return this.m_source_location;
    }
}
