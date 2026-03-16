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
import javax.xml.transform.SourceLocator;
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
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.SerializerTrace;
import org.apache.xml.serializer.ToSAXHandler;
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

    public TransformerImpl(StylesheetRoot stylesheet) {
        this.m_optimizer = true;
        this.m_incremental = false;
        this.m_source_location = false;
        this.m_optimizer = stylesheet.getOptimizer();
        this.m_incremental = stylesheet.getIncremental();
        this.m_source_location = stylesheet.getSource_location();
        setStylesheet(stylesheet);
        XPathContext xPath = new XPathContext(this);
        xPath.setIncremental(this.m_incremental);
        xPath.getDTMManager().setIncremental(this.m_incremental);
        xPath.setSource_location(this.m_source_location);
        xPath.getDTMManager().setSource_location(this.m_source_location);
        if (stylesheet.isSecureProcessing()) {
            xPath.setSecureProcessing(true);
        }
        setXPathContext(xPath);
        getXPathContext().setNamespaceContext(stylesheet);
    }

    public ExtensionsTable getExtensionsTable() {
        return this.m_extensionsTable;
    }

    void setExtensionsTable(StylesheetRoot sroot) throws TransformerException {
        try {
            if (sroot.getExtensions() != null) {
                this.m_extensionsTable = new ExtensionsTable(sroot);
            }
        } catch (TransformerException te) {
            te.printStackTrace();
        }
    }

    @Override
    public boolean functionAvailable(String ns, String funcName) throws TransformerException {
        return getExtensionsTable().functionAvailable(ns, funcName);
    }

    @Override
    public boolean elementAvailable(String ns, String elemName) throws TransformerException {
        return getExtensionsTable().elementAvailable(ns, elemName);
    }

    @Override
    public Object extFunction(String ns, String funcName, Vector argVec, Object methodKey) throws TransformerException {
        return getExtensionsTable().extFunction(ns, funcName, argVec, methodKey, getXPathContext().getExpressionContext());
    }

    @Override
    public Object extFunction(FuncExtFunction extFunction, Vector argVec) throws TransformerException {
        return getExtensionsTable().extFunction(extFunction, argVec, getXPathContext().getExpressionContext());
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

    public void setTransformThread(Thread t) {
        this.m_transformThread = t;
    }

    public boolean hasTransformThreadErrorCatcher() {
        return this.m_hasTransformThreadErrorCatcher;
    }

    public void transform(Source source) throws Throwable {
        transform(source, true);
    }

    public void transform(Source source, boolean shouldRelease) throws Throwable {
        try {
            try {
                if (getXPathContext().getNamespaceContext() == null) {
                    getXPathContext().setNamespaceContext(getStylesheet());
                }
                String base = source.getSystemId();
                if (base == null) {
                    base = this.m_stylesheetRoot.getBaseIdentifier();
                }
                if (base == null) {
                    String currentDir = "";
                    try {
                        currentDir = System.getProperty("user.dir");
                    } catch (SecurityException e) {
                    }
                    base = (currentDir.startsWith(File.separator) ? "file://" + currentDir : "file:///" + currentDir) + File.separatorChar + source.getClass().getName();
                }
                setBaseURLOfSource(base);
                DTMManager mgr = this.m_xcontext.getDTMManager();
                if (((source instanceof StreamSource) && source.getSystemId() == null && ((StreamSource) source).getInputStream() == null && ((StreamSource) source).getReader() == null) || (((source instanceof SAXSource) && ((SAXSource) source).getInputSource() == null && ((SAXSource) source).getXMLReader() == null) || ((source instanceof DOMSource) && ((DOMSource) source).getNode() == null))) {
                    try {
                        DocumentBuilderFactory builderF = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = builderF.newDocumentBuilder();
                        String systemID = source.getSystemId();
                        Source source2 = new DOMSource(builder.newDocument());
                        if (systemID != null) {
                            try {
                                source2.setSystemId(systemID);
                            } catch (ParserConfigurationException e2) {
                                e = e2;
                                source = source2;
                                fatalError(e);
                            } catch (WrappedRuntimeException e3) {
                                wre = e3;
                                Throwable throwable = wre.getException();
                                while (throwable instanceof WrappedRuntimeException) {
                                    throwable = ((WrappedRuntimeException) throwable).getException();
                                }
                                fatalError(throwable);
                                this.m_hasTransformThreadErrorCatcher = false;
                                reset();
                                return;
                            } catch (SAXParseException e4) {
                                spe = e4;
                                fatalError(spe);
                                this.m_hasTransformThreadErrorCatcher = false;
                                reset();
                                return;
                            } catch (SAXException e5) {
                                se = e5;
                                this.m_errorHandler.fatalError(new TransformerException(se));
                                this.m_hasTransformThreadErrorCatcher = false;
                                reset();
                                return;
                            } catch (Throwable th) {
                                th = th;
                                this.m_hasTransformThreadErrorCatcher = false;
                                reset();
                                throw th;
                            }
                        }
                        source = source2;
                    } catch (ParserConfigurationException e6) {
                        e = e6;
                    }
                }
                DTM dtm = mgr.getDTM(source, false, this, true, true);
                dtm.setDocumentBaseURI(base);
                try {
                    transformNode(dtm.getDocument());
                    Exception e7 = getExceptionThrown();
                    if (e7 != null) {
                        if (e7 instanceof TransformerException) {
                            throw ((TransformerException) e7);
                        }
                        if (!(e7 instanceof WrappedRuntimeException)) {
                            throw new TransformerException(e7);
                        }
                        fatalError(((WrappedRuntimeException) e7).getException());
                    } else if (this.m_serializationHandler != null) {
                        this.m_serializationHandler.endDocument();
                    }
                    this.m_hasTransformThreadErrorCatcher = false;
                    reset();
                } finally {
                    if (shouldRelease) {
                        mgr.release(dtm, true);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (WrappedRuntimeException e8) {
            wre = e8;
        } catch (SAXParseException e9) {
            spe = e9;
        } catch (SAXException e10) {
            se = e10;
        }
    }

    private void fatalError(Throwable throwable) throws TransformerException {
        if (throwable instanceof SAXParseException) {
            this.m_errorHandler.fatalError(new TransformerException(throwable.getMessage(), new SAXSourceLocator((SAXParseException) throwable)));
        } else {
            this.m_errorHandler.fatalError(new TransformerException(throwable));
        }
    }

    public void setBaseURLOfSource(String base) {
        this.m_urlOfSource = base;
    }

    @Override
    public String getOutputProperty(String qnameString) throws IllegalArgumentException {
        OutputProperties props = getOutputFormat();
        String value = props.getProperty(qnameString);
        if (value == null && !OutputProperties.isLegalPropertyKey(qnameString)) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{qnameString}));
        }
        return value;
    }

    public String getOutputPropertyNoDefault(String qnameString) throws IllegalArgumentException {
        OutputProperties props = getOutputFormat();
        String value = (String) props.getProperties().get(qnameString);
        if (value == null && !OutputProperties.isLegalPropertyKey(qnameString)) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{qnameString}));
        }
        return value;
    }

    @Override
    public void setOutputProperty(String name, String value) throws IllegalArgumentException {
        synchronized (this.m_reentryGuard) {
            if (this.m_outputFormat == null) {
                this.m_outputFormat = (OutputProperties) getStylesheet().getOutputComposed().clone();
            }
            if (!OutputProperties.isLegalPropertyKey(name)) {
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{name}));
            }
            this.m_outputFormat.setProperty(name, value);
        }
    }

    @Override
    public void setOutputProperties(Properties oformat) throws IllegalArgumentException {
        synchronized (this.m_reentryGuard) {
            if (oformat != null) {
                String method = (String) oformat.get(Constants.ATTRNAME_OUTPUT_METHOD);
                if (method != null) {
                    this.m_outputFormat = new OutputProperties(method);
                } else if (this.m_outputFormat == null) {
                    this.m_outputFormat = new OutputProperties();
                }
                this.m_outputFormat.copyFrom(oformat);
                this.m_outputFormat.copyFrom(this.m_stylesheetRoot.getOutputProperties());
            } else {
                this.m_outputFormat = null;
            }
        }
    }

    @Override
    public Properties getOutputProperties() {
        return (Properties) getOutputFormat().getProperties().clone();
    }

    public SerializationHandler createSerializationHandler(Result outputTarget) throws TransformerException {
        SerializationHandler xoh = createSerializationHandler(outputTarget, getOutputFormat());
        return xoh;
    }

    public SerializationHandler createSerializationHandler(Result outputTarget, OutputProperties format) throws TransformerException {
        SerializationHandler xoh;
        LexicalHandler lexHandler;
        Document doc;
        short type;
        if (outputTarget instanceof DOMResult) {
            Node outputNode = ((DOMResult) outputTarget).getNode();
            Node nextSibling = ((DOMResult) outputTarget).getNextSibling();
            if (outputNode != null) {
                type = outputNode.getNodeType();
                doc = 9 == type ? (Document) outputNode : outputNode.getOwnerDocument();
            } else {
                boolean isSecureProcessing = this.m_stylesheetRoot.isSecureProcessing();
                doc = DOMHelper.createDocument(isSecureProcessing);
                outputNode = doc;
                type = outputNode.getNodeType();
                ((DOMResult) outputTarget).setNode(outputNode);
            }
            DOMBuilder handler = 11 == type ? new DOMBuilder(doc, (DocumentFragment) outputNode) : new DOMBuilder(doc, outputNode);
            if (nextSibling != null) {
                handler.setNextSibling(nextSibling);
            }
            String encoding = format.getProperty("encoding");
            xoh = new ToXMLSAXHandler(handler, handler, encoding);
        } else if (outputTarget instanceof SAXResult) {
            ContentHandler handler2 = ((SAXResult) outputTarget).getHandler();
            if (handler2 == null) {
                throw new IllegalArgumentException("handler can not be null for a SAXResult");
            }
            if (handler2 instanceof LexicalHandler) {
                lexHandler = (LexicalHandler) handler2;
            } else {
                lexHandler = null;
            }
            String encoding2 = format.getProperty("encoding");
            format.getProperty(Constants.ATTRNAME_OUTPUT_METHOD);
            ToXMLSAXHandler toXMLSAXHandler = new ToXMLSAXHandler(handler2, lexHandler, encoding2);
            toXMLSAXHandler.setShouldOutputNSAttr(false);
            xoh = toXMLSAXHandler;
            String publicID = format.getProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC);
            String systemID = format.getProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM);
            if (systemID != null) {
                xoh.setDoctypeSystem(systemID);
            }
            if (publicID != null) {
                xoh.setDoctypePublic(publicID);
            }
            if (handler2 instanceof TransformerClient) {
                XalanTransformState state = new XalanTransformState();
                ((TransformerClient) handler2).setTransformState(state);
                ((ToSAXHandler) xoh).setTransformState(state);
            }
        } else if (outputTarget instanceof StreamResult) {
            StreamResult sresult = (StreamResult) outputTarget;
            try {
                SerializationHandler serializer = (SerializationHandler) SerializerFactory.getSerializer(format.getProperties());
                if (sresult.getWriter() != null) {
                    serializer.setWriter(sresult.getWriter());
                } else if (sresult.getOutputStream() != null) {
                    serializer.setOutputStream(sresult.getOutputStream());
                } else if (sresult.getSystemId() != null) {
                    String fileURL = sresult.getSystemId();
                    if (fileURL.startsWith("file:///")) {
                        if (fileURL.substring(8).indexOf(":") > 0) {
                            fileURL = fileURL.substring(8);
                        } else {
                            fileURL = fileURL.substring(7);
                        }
                    } else if (fileURL.startsWith("file:/")) {
                        if (fileURL.substring(6).indexOf(":") > 0) {
                            fileURL = fileURL.substring(6);
                        } else {
                            fileURL = fileURL.substring(5);
                        }
                    }
                    this.m_outputStream = new FileOutputStream(fileURL);
                    serializer.setOutputStream(this.m_outputStream);
                } else {
                    throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_OUTPUT_SPECIFIED, null));
                }
                xoh = serializer;
            } catch (IOException ioe) {
                throw new TransformerException(ioe);
            }
        } else {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_CANNOT_TRANSFORM_TO_RESULT_TYPE, new Object[]{outputTarget.getClass().getName()}));
        }
        xoh.setTransformer(this);
        SourceLocator srcLocator = getStylesheet();
        xoh.setSourceLocator(srcLocator);
        return xoh;
    }

    @Override
    public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
        transform(xmlSource, outputTarget, true);
    }

    public void transform(Source xmlSource, Result outputTarget, boolean shouldRelease) throws TransformerException {
        synchronized (this.m_reentryGuard) {
            SerializationHandler xoh = createSerializationHandler(outputTarget);
            setSerializationHandler(xoh);
            this.m_outputTarget = outputTarget;
            transform(xmlSource, shouldRelease);
        }
    }

    public void transformNode(int node, Result outputTarget) throws TransformerException {
        SerializationHandler xoh = createSerializationHandler(outputTarget);
        setSerializationHandler(xoh);
        this.m_outputTarget = outputTarget;
        transformNode(node);
    }

    public void transformNode(int node) throws TransformerException {
        setExtensionsTable(getStylesheet());
        synchronized (this.m_serializationHandler) {
            try {
                this.m_hasBeenReset = false;
                XPathContext xctxt = getXPathContext();
                xctxt.getDTM(node);
                try {
                    pushGlobalVars(node);
                    StylesheetRoot stylesheet = getStylesheet();
                    int n = stylesheet.getGlobalImportCount();
                    for (int i = 0; i < n; i++) {
                        StylesheetComposed imported = stylesheet.getGlobalImport(i);
                        int includedCount = imported.getIncludeCountComposed();
                        for (int j = -1; j < includedCount; j++) {
                            Stylesheet included = imported.getIncludeComposed(j);
                            included.runtimeInit(this);
                            for (ElemTemplateElement child = included.getFirstChildElem(); child != null; child = child.getNextSiblingElem()) {
                                child.runtimeInit(this);
                            }
                        }
                    }
                    DTMIterator dtmIter = new SelfIteratorNoPredicate();
                    dtmIter.setRoot(node, xctxt);
                    xctxt.pushContextNodeList(dtmIter);
                } catch (Exception e) {
                    se = e;
                    while (se instanceof WrappedRuntimeException) {
                        Exception e2 = ((WrappedRuntimeException) se).getException();
                        if (e2 != null) {
                            se = e2;
                        }
                    }
                    if (this.m_serializationHandler != null) {
                        try {
                            if (se instanceof SAXParseException) {
                                this.m_serializationHandler.fatalError((SAXParseException) se);
                            } else if (se instanceof TransformerException) {
                                TransformerException te = (TransformerException) se;
                                SAXSourceLocator sl = new SAXSourceLocator(te.getLocator());
                                this.m_serializationHandler.fatalError(new SAXParseException(te.getMessage(), sl, te));
                            } else {
                                this.m_serializationHandler.fatalError(new SAXParseException(se.getMessage(), new SAXSourceLocator(), se));
                            }
                        } catch (Exception e3) {
                        }
                    }
                    if (se instanceof TransformerException) {
                        this.m_errorHandler.fatalError((TransformerException) se);
                    } else if (se instanceof SAXParseException) {
                        this.m_errorHandler.fatalError(new TransformerException(se.getMessage(), new SAXSourceLocator((SAXParseException) se), se));
                    } else {
                        this.m_errorHandler.fatalError(new TransformerException(se));
                    }
                    reset();
                }
                try {
                    applyTemplateToNode(null, null, node);
                    xctxt.popContextNodeList();
                    if (this.m_serializationHandler != null) {
                        this.m_serializationHandler.endDocument();
                    }
                } catch (Throwable th) {
                    xctxt.popContextNodeList();
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

    public ContentHandler getInputContentHandler(boolean doDocFrag) {
        if (this.m_inputContentHandler == null) {
            this.m_inputContentHandler = new TransformerHandlerImpl(this, doDocFrag, this.m_urlOfSource);
        }
        return this.m_inputContentHandler;
    }

    public void setOutputFormat(OutputProperties oformat) {
        this.m_outputFormat = oformat;
    }

    public OutputProperties getOutputFormat() {
        if (this.m_outputFormat != null) {
            OutputProperties format = this.m_outputFormat;
            return format;
        }
        OutputProperties format2 = getStylesheet().getOutputComposed();
        return format2;
    }

    public void setParameter(String name, String namespace, Object value) {
        VariableStack varstack = getXPathContext().getVarStack();
        QName qname = new QName(namespace, name);
        XObject xobject = XObject.create(value, getXPathContext());
        StylesheetRoot sroot = this.m_stylesheetRoot;
        Vector vars = sroot.getVariablesAndParamsComposed();
        int i = vars.size();
        while (true) {
            i--;
            if (i >= 0) {
                ElemVariable variable = (ElemVariable) vars.elementAt(i);
                if (variable.getXSLToken() == 41 && variable.getName().equals(qname)) {
                    varstack.setGlobalVariable(i, xobject);
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_SET_PARAM_VALUE, new Object[]{name}));
        }
        StringTokenizer tokenizer = new StringTokenizer(name, "{}", false);
        try {
            String s1 = tokenizer.nextToken();
            String s2 = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
            if (this.m_userParams == null) {
                this.m_userParams = new Vector();
            }
            if (s2 == null) {
                replaceOrPushUserParam(new QName(s1), XObject.create(value, getXPathContext()));
                setParameter(s1, null, value);
            } else {
                replaceOrPushUserParam(new QName(s1, s2), XObject.create(value, getXPathContext()));
                setParameter(s2, s1, value);
            }
        } catch (NoSuchElementException e) {
        }
    }

    private void replaceOrPushUserParam(QName qname, XObject xval) {
        int n = this.m_userParams.size();
        for (int i = n - 1; i >= 0; i--) {
            Arg arg = (Arg) this.m_userParams.elementAt(i);
            if (arg.getQName().equals(qname)) {
                this.m_userParams.setElementAt(new Arg(qname, xval, true), i);
                return;
            }
        }
        this.m_userParams.addElement(new Arg(qname, xval, true));
    }

    @Override
    public Object getParameter(String name) {
        try {
            QName qname = QName.getQNameFromString(name);
            if (this.m_userParams == null) {
                return null;
            }
            int n = this.m_userParams.size();
            for (int i = n - 1; i >= 0; i--) {
                Arg arg = (Arg) this.m_userParams.elementAt(i);
                if (arg.getQName().equals(qname)) {
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
            if (this.m_userParams != null) {
                int n = this.m_userParams.size();
                for (int i = n - 1; i >= 0; i--) {
                    Arg arg = (Arg) this.m_userParams.elementAt(i);
                    QName name = arg.getQName();
                    String s1 = name.getNamespace();
                    String s2 = name.getLocalPart();
                    setParameter(s2, s1, arg.getVal().object());
                }
            }
        } catch (NoSuchElementException e) {
        }
    }

    public void setParameters(Properties params) {
        clearParameters();
        Enumeration<?> enumerationPropertyNames = params.propertyNames();
        while (enumerationPropertyNames.hasMoreElements()) {
            String name = params.getProperty((String) enumerationPropertyNames.nextElement());
            StringTokenizer tokenizer = new StringTokenizer(name, "{}", false);
            try {
                String s1 = tokenizer.nextToken();
                String s2 = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
                if (s2 == null) {
                    setParameter(s1, null, params.getProperty(name));
                } else {
                    setParameter(s2, s1, params.getProperty(name));
                }
            } catch (NoSuchElementException e) {
            }
        }
    }

    @Override
    public void clearParameters() {
        synchronized (this.m_reentryGuard) {
            VariableStack varstack = new VariableStack();
            this.m_xcontext.setVarStack(varstack);
            this.m_userParams = null;
        }
    }

    protected void pushGlobalVars(int contextNode) throws TransformerException {
        XPathContext xctxt = this.m_xcontext;
        VariableStack vs = xctxt.getVarStack();
        StylesheetRoot sr = getStylesheet();
        Vector vars = sr.getVariablesAndParamsComposed();
        int i = vars.size();
        vs.link(i);
        while (true) {
            i--;
            if (i >= 0) {
                ElemVariable v = (ElemVariable) vars.elementAt(i);
                XObject xobj = new XUnresolvedVariable(v, contextNode, this, vs.getStackFrame(), 0, true);
                if (vs.elementAt(i) == null) {
                    vs.setGlobalVariable(i, xobj);
                }
            } else {
                return;
            }
        }
    }

    @Override
    public void setURIResolver(URIResolver resolver) {
        synchronized (this.m_reentryGuard) {
            this.m_xcontext.getSourceTreeManager().setURIResolver(resolver);
        }
    }

    @Override
    public URIResolver getURIResolver() {
        return this.m_xcontext.getSourceTreeManager().getURIResolver();
    }

    public void setContentHandler(ContentHandler handler) {
        if (handler == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_NULL_CONTENT_HANDLER, null));
        }
        this.m_outputContentHandler = handler;
        if (this.m_serializationHandler == null) {
            ToXMLSAXHandler h = new ToXMLSAXHandler();
            h.setContentHandler(handler);
            h.setTransformer(this);
            this.m_serializationHandler = h;
            return;
        }
        this.m_serializationHandler.setContentHandler(handler);
    }

    public ContentHandler getContentHandler() {
        return this.m_outputContentHandler;
    }

    public int transformToRTF(ElemTemplateElement templateParent) throws TransformerException {
        DTM dtmFrag = this.m_xcontext.getRTFDTM();
        return transformToRTF(templateParent, dtmFrag);
    }

    public int transformToGlobalRTF(ElemTemplateElement templateParent) throws TransformerException {
        DTM dtmFrag = this.m_xcontext.getGlobalRTFDTM();
        return transformToRTF(templateParent, dtmFrag);
    }

    private int transformToRTF(ElemTemplateElement templateParent, DTM dtmFrag) throws TransformerException {
        XPathContext xPathContext = this.m_xcontext;
        ContentHandler rtfHandler = dtmFrag.getContentHandler();
        SerializationHandler savedRTreeHandler = this.m_serializationHandler;
        ToSAXHandler h = new ToXMLSAXHandler();
        h.setContentHandler(rtfHandler);
        h.setTransformer(this);
        this.m_serializationHandler = h;
        SerializationHandler rth = this.m_serializationHandler;
        try {
            try {
                rth.startDocument();
                rth.flushPending();
                try {
                    executeChildTemplates(templateParent, true);
                    rth.flushPending();
                    int resultFragment = dtmFrag.getDocument();
                    return resultFragment;
                } finally {
                    rth.endDocument();
                }
            } catch (SAXException se) {
                throw new TransformerException(se);
            }
        } finally {
            this.m_serializationHandler = savedRTreeHandler;
        }
    }

    public String transformToString(ElemTemplateElement elem) throws TransformerException {
        ElemTemplateElement firstChild = elem.getFirstChildElem();
        if (firstChild == null) {
            return "";
        }
        if (elem.hasTextLitOnly() && this.m_optimizer) {
            return ((ElemTextLiteral) firstChild).getNodeValue();
        }
        SerializationHandler savedRTreeHandler = this.m_serializationHandler;
        StringWriter sw = (StringWriter) this.m_stringWriterObjectPool.getInstance();
        this.m_serializationHandler = (ToTextStream) this.m_textResultHandlerObjectPool.getInstance();
        if (this.m_serializationHandler == null) {
            Serializer serializer = SerializerFactory.getSerializer(this.m_textformat.getProperties());
            this.m_serializationHandler = (SerializationHandler) serializer;
        }
        this.m_serializationHandler.setTransformer(this);
        this.m_serializationHandler.setWriter(sw);
        try {
            try {
                executeChildTemplates(elem, true);
                this.m_serializationHandler.endDocument();
                return sw.toString();
            } catch (SAXException se) {
                throw new TransformerException(se);
            }
        } finally {
            sw.getBuffer().setLength(0);
            try {
                sw.close();
            } catch (Exception e) {
            }
            this.m_stringWriterObjectPool.freeInstance(sw);
            this.m_serializationHandler.reset();
            this.m_textResultHandlerObjectPool.freeInstance(this.m_serializationHandler);
            this.m_serializationHandler = savedRTreeHandler;
        }
    }

    public boolean applyTemplateToNode(ElemTemplateElement xslInstruction, ElemTemplate template, int child) throws TransformerException {
        int maxImportLevel;
        DTM dtm = this.m_xcontext.getDTM(child);
        short nodeType = dtm.getNodeType(child);
        boolean isDefaultTextRule = false;
        boolean isApplyImports = xslInstruction != null && xslInstruction.getXSLToken() == 72;
        if (template == null || isApplyImports) {
            int endImportLevel = 0;
            if (isApplyImports) {
                maxImportLevel = template.getStylesheetComposed().getImportCountComposed() - 1;
                endImportLevel = template.getStylesheetComposed().getEndImportCountComposed();
            } else {
                maxImportLevel = -1;
            }
            if (isApplyImports && maxImportLevel == -1) {
                template = null;
            } else {
                XPathContext xctxt = this.m_xcontext;
                try {
                    xctxt.pushNamespaceContext(xslInstruction);
                    QName mode = getMode();
                    if (isApplyImports) {
                        template = this.m_stylesheetRoot.getTemplateComposed(xctxt, child, mode, maxImportLevel, endImportLevel, this.m_quietConflictWarnings, dtm);
                    } else {
                        template = this.m_stylesheetRoot.getTemplateComposed(xctxt, child, mode, this.m_quietConflictWarnings, dtm);
                    }
                } finally {
                    xctxt.popNamespaceContext();
                }
            }
            if (template == null) {
                switch (nodeType) {
                    case 1:
                    case 11:
                        template = this.m_stylesheetRoot.getDefaultRule();
                        break;
                    case 2:
                    case 3:
                    case 4:
                        template = this.m_stylesheetRoot.getDefaultTextRule();
                        isDefaultTextRule = true;
                        break;
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 10:
                    default:
                        return false;
                    case 9:
                        template = this.m_stylesheetRoot.getDefaultRootRule();
                        break;
                }
            }
        }
        try {
            try {
                pushElemTemplateElement(template);
                this.m_xcontext.pushCurrentNode(child);
                pushPairCurrentMatched(template, child);
                if (!isApplyImports) {
                    DTMIterator cnl = new NodeSetDTM(child, this.m_xcontext.getDTMManager());
                    this.m_xcontext.pushContextNodeList(cnl);
                }
                if (isDefaultTextRule) {
                    switch (nodeType) {
                        case 2:
                            dtm.dispatchCharactersEvents(child, getResultTreeHandler(), false);
                            break;
                        case 3:
                        case 4:
                            ClonerToResultTree.cloneToResultTree(child, nodeType, dtm, getResultTreeHandler(), false);
                            break;
                    }
                } else {
                    this.m_xcontext.setSAXLocator(template);
                    this.m_xcontext.getVarStack().link(template.m_frameSize);
                    executeChildTemplates((ElemTemplateElement) template, true);
                }
                if (!isDefaultTextRule) {
                    this.m_xcontext.getVarStack().unlink();
                }
                this.m_xcontext.popCurrentNode();
                if (!isApplyImports) {
                    this.m_xcontext.popContextNodeList();
                }
                popCurrentMatched();
                popElemTemplateElement();
                return true;
            } catch (SAXException se) {
                throw new TransformerException(se);
            }
        } catch (Throwable th) {
            if (!isDefaultTextRule) {
                this.m_xcontext.getVarStack().unlink();
            }
            this.m_xcontext.popCurrentNode();
            if (!isApplyImports) {
                this.m_xcontext.popContextNodeList();
            }
            popCurrentMatched();
            popElemTemplateElement();
            throw th;
        }
    }

    public void executeChildTemplates(ElemTemplateElement elem, Node context, QName mode, ContentHandler handler) throws TransformerException {
        XPathContext xctxt = this.m_xcontext;
        if (mode != null) {
            try {
                pushMode(mode);
            } finally {
                xctxt.popCurrentNode();
                if (mode != null) {
                    popMode();
                }
            }
        }
        xctxt.pushCurrentNode(xctxt.getDTMHandleFromNode(context));
        executeChildTemplates(elem, handler);
    }

    public void executeChildTemplates(ElemTemplateElement elem, boolean shouldAddAttrs) throws TransformerException {
        ElemTemplateElement t = elem.getFirstChildElem();
        if (t != null) {
            if (elem.hasTextLitOnly() && this.m_optimizer) {
                char[] chars = ((ElemTextLiteral) t).getChars();
                try {
                    try {
                        pushElemTemplateElement(t);
                        this.m_serializationHandler.characters(chars, 0, chars.length);
                        return;
                    } catch (SAXException se) {
                        throw new TransformerException(se);
                    }
                } finally {
                    popElemTemplateElement();
                }
            }
            XPathContext xctxt = this.m_xcontext;
            xctxt.pushSAXLocatorNull();
            int currentTemplateElementsTop = this.m_currentTemplateElements.size();
            this.m_currentTemplateElements.push(null);
            while (t != null) {
                if (!shouldAddAttrs) {
                    try {
                        try {
                            if (t.getXSLToken() != 48) {
                                xctxt.setSAXLocator(t);
                                this.m_currentTemplateElements.setElementAt(t, currentTemplateElementsTop);
                                t.execute(this);
                            }
                        } catch (RuntimeException re) {
                            TransformerException te = new TransformerException(re);
                            te.setLocator(t);
                            throw te;
                        }
                    } finally {
                        this.m_currentTemplateElements.pop();
                        xctxt.popSAXLocator();
                    }
                }
                t = t.getNextSiblingElem();
            }
        }
    }

    public void executeChildTemplates(ElemTemplateElement elem, ContentHandler handler) throws TransformerException {
        SerializationHandler xoh = getSerializationHandler();
        try {
            try {
                try {
                    xoh.flushPending();
                    LexicalHandler lex = null;
                    if (handler instanceof LexicalHandler) {
                        lex = (LexicalHandler) handler;
                    }
                    this.m_serializationHandler = new ToXMLSAXHandler(handler, lex, xoh.getEncoding());
                    this.m_serializationHandler.setTransformer(this);
                    executeChildTemplates(elem, true);
                } catch (TransformerException e) {
                    throw e;
                }
            } catch (SAXException se) {
                throw new TransformerException(se);
            }
        } finally {
            this.m_serializationHandler = xoh;
        }
    }

    public Vector processSortKeys(ElemForEach foreach, int sourceNodeContext) throws TransformerException {
        boolean caseOrderUpper;
        Vector keys = null;
        XPathContext xctxt = this.m_xcontext;
        int nElems = foreach.getSortElemCount();
        if (nElems > 0) {
            keys = new Vector();
        }
        for (int i = 0; i < nElems; i++) {
            ElemSort sort = foreach.getSortElem(i);
            String langString = sort.getLang() != null ? sort.getLang().evaluate(xctxt, sourceNodeContext, foreach) : null;
            String dataTypeString = sort.getDataType().evaluate(xctxt, sourceNodeContext, foreach);
            if (dataTypeString.indexOf(":") >= 0) {
                System.out.println("TODO: Need to write the hooks for QNAME sort data type");
            } else if (!dataTypeString.equalsIgnoreCase("text") && !dataTypeString.equalsIgnoreCase("number")) {
                foreach.error(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{Constants.ATTRNAME_DATATYPE, dataTypeString});
            }
            boolean treatAsNumbers = dataTypeString != null && dataTypeString.equals("number");
            String orderString = sort.getOrder().evaluate(xctxt, sourceNodeContext, foreach);
            if (!orderString.equalsIgnoreCase(Constants.ATTRVAL_ORDER_ASCENDING) && !orderString.equalsIgnoreCase(Constants.ATTRVAL_ORDER_DESCENDING)) {
                foreach.error(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{Constants.ATTRNAME_ORDER, orderString});
            }
            boolean descending = orderString != null && orderString.equals(Constants.ATTRVAL_ORDER_DESCENDING);
            AVT caseOrder = sort.getCaseOrder();
            if (caseOrder != null) {
                String caseOrderString = caseOrder.evaluate(xctxt, sourceNodeContext, foreach);
                if (!caseOrderString.equalsIgnoreCase(Constants.ATTRVAL_CASEORDER_UPPER) && !caseOrderString.equalsIgnoreCase(Constants.ATTRVAL_CASEORDER_LOWER)) {
                    foreach.error(XSLTErrorResources.ER_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{Constants.ATTRNAME_CASEORDER, caseOrderString});
                }
                caseOrderUpper = caseOrderString != null && caseOrderString.equals(Constants.ATTRVAL_CASEORDER_UPPER);
            } else {
                caseOrderUpper = false;
            }
            keys.addElement(new NodeSortKey(this, sort.getSelect(), treatAsNumbers, descending, langString, caseOrderUpper, foreach));
        }
        return keys;
    }

    public int getCurrentTemplateElementsCount() {
        return this.m_currentTemplateElements.size();
    }

    public ObjectStack getCurrentTemplateElements() {
        return this.m_currentTemplateElements;
    }

    public void pushElemTemplateElement(ElemTemplateElement elem) {
        this.m_currentTemplateElements.push(elem);
    }

    public void popElemTemplateElement() {
        this.m_currentTemplateElements.pop();
    }

    public void setCurrentElement(ElemTemplateElement e) {
        this.m_currentTemplateElements.setTop(e);
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
        ElemTemplateElement elem = getCurrentElement();
        while (elem != null && elem.getXSLToken() != 19) {
            elem = elem.getParentElem();
        }
        return (ElemTemplate) elem;
    }

    public void pushPairCurrentMatched(ElemTemplateElement template, int child) {
        this.m_currentMatchTemplates.push(template);
        this.m_currentMatchedNodes.push(child);
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
            DTMIterator cnl = this.m_xcontext.getContextNodeList();
            if (cnl == null) {
                return null;
            }
            return cnl.cloneWithReset();
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

    public void setXPathContext(XPathContext xcontext) {
        this.m_xcontext = xcontext;
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

    public boolean isRecursiveAttrSet(ElemAttributeSet attrSet) {
        if (this.m_attrSetStack == null) {
            this.m_attrSetStack = new Stack();
        }
        if (!this.m_attrSetStack.empty()) {
            int loc = this.m_attrSetStack.search(attrSet);
            if (loc > -1) {
                return true;
            }
        }
        return false;
    }

    public void pushElemAttributeSet(ElemAttributeSet attrSet) {
        this.m_attrSetStack.push(attrSet);
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

    public void pushCurrentTemplateRuleIsNull(boolean b) {
        this.m_currentTemplateRuleIsNull.push(b);
    }

    public void popCurrentTemplateRuleIsNull() {
        this.m_currentTemplateRuleIsNull.pop();
    }

    public void pushCurrentFuncResult(Object val) {
        this.m_currentFuncResult.push(val);
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
    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
        synchronized (this.m_reentryGuard) {
            if (listener == null) {
                throw new IllegalArgumentException(XSLMessages.createMessage("ER_NULL_ERROR_HANDLER", null));
            }
            this.m_errorHandler = listener;
        }
    }

    @Override
    public ErrorListener getErrorListener() {
        return this.m_errorHandler;
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/trax/features/sax/input".equals(name) || "http://xml.org/trax/features/dom/input".equals(name)) {
            return true;
        }
        throw new SAXNotRecognizedException(name);
    }

    public QName getMode() {
        if (this.m_modes.isEmpty()) {
            return null;
        }
        return (QName) this.m_modes.peek();
    }

    public void pushMode(QName mode) {
        this.m_modes.push(mode);
    }

    public void popMode() {
        this.m_modes.pop();
    }

    public void runTransformThread(int priority) {
        Thread t = ThreadControllerWrapper.runThread(this, priority);
        setTransformThread(t);
    }

    public void runTransformThread() {
        ThreadControllerWrapper.runThread(this, -1);
    }

    public static void runTransformThread(Runnable runnable) {
        ThreadControllerWrapper.runThread(runnable, -1);
    }

    public void waitTransformThread() throws SAXException {
        Exception e;
        Thread transformThread = getTransformThread();
        if (transformThread != null) {
            try {
                ThreadControllerWrapper.waitThread(transformThread, this);
                if (!hasTransformThreadErrorCatcher() && (e = getExceptionThrown()) != null) {
                    e.printStackTrace();
                    throw new SAXException(e);
                }
                setTransformThread(null);
            } catch (InterruptedException e2) {
            }
        }
    }

    public Exception getExceptionThrown() {
        return this.m_exceptionThrown;
    }

    public void setExceptionThrown(Exception e) {
        this.m_exceptionThrown = e;
    }

    public void setSourceTreeDocForThread(int doc) {
        this.m_doc = doc;
    }

    void postExceptionFromThread(Exception e) {
        this.m_exceptionThrown = e;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void run() {
        this.m_hasBeenReset = false;
        try {
            try {
                try {
                    transformNode(this.m_doc);
                    if (this.m_inputContentHandler instanceof TransformerHandlerImpl) {
                        ((TransformerHandlerImpl) this.m_inputContentHandler).clearCoRoutine();
                    }
                } catch (Exception e) {
                    if (this.m_transformThread != null) {
                        postExceptionFromThread(e);
                        if (this.m_inputContentHandler instanceof TransformerHandlerImpl) {
                            ((TransformerHandlerImpl) this.m_inputContentHandler).clearCoRoutine();
                            return;
                        }
                        return;
                    }
                    throw new RuntimeException(e.getMessage());
                }
            } finally {
            }
        } catch (Exception e2) {
            if (this.m_transformThread != null) {
                postExceptionFromThread(e2);
                return;
            }
            throw new RuntimeException(e2.getMessage());
        }
    }

    @Override
    public short getShouldStripSpace(int elementHandle, DTM dtm) {
        try {
            WhiteSpaceInfo info = this.m_stylesheetRoot.getWhiteSpaceInfo(this.m_xcontext, elementHandle, dtm);
            if (info == null) {
                return (short) 3;
            }
            return info.getShouldStripSpace() ? (short) 2 : (short) 1;
        } catch (TransformerException e) {
            return (short) 3;
        }
    }

    public void init(ToXMLSAXHandler h, Transformer transformer, ContentHandler realHandler) {
        h.setTransformer(transformer);
        h.setContentHandler(realHandler);
    }

    public void setSerializationHandler(SerializationHandler xoh) {
        this.m_serializationHandler = xoh;
    }

    @Override
    public void fireGenerateEvent(int eventType, char[] ch, int start, int length) {
    }

    @Override
    public void fireGenerateEvent(int eventType, String name, Attributes atts) {
    }

    @Override
    public void fireGenerateEvent(int eventType, String name, String data) {
    }

    @Override
    public void fireGenerateEvent(int eventType, String data) {
    }

    @Override
    public void fireGenerateEvent(int eventType) {
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
