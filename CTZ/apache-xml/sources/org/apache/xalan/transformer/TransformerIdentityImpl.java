package org.apache.xalan.transformer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
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
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.OutputProperties;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.TreeWalker;
import org.apache.xml.utils.DOMBuilder;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLReaderManager;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

public class TransformerIdentityImpl extends Transformer implements TransformerHandler, DeclHandler {
    URIResolver m_URIResolver;
    private ErrorListener m_errorListener;
    boolean m_flushedStartDoc;
    boolean m_foundFirstElement;
    private boolean m_isSecureProcessing;
    private OutputProperties m_outputFormat;
    private FileOutputStream m_outputStream;
    private Hashtable m_params;
    private Result m_result;
    private ContentHandler m_resultContentHandler;
    private DTDHandler m_resultDTDHandler;
    private DeclHandler m_resultDeclHandler;
    private LexicalHandler m_resultLexicalHandler;
    private Serializer m_serializer;
    private String m_systemID;

    public TransformerIdentityImpl(boolean z) {
        this.m_flushedStartDoc = false;
        this.m_outputStream = null;
        this.m_errorListener = new DefaultErrorHandler(false);
        this.m_isSecureProcessing = false;
        this.m_outputFormat = new OutputProperties("xml");
        this.m_isSecureProcessing = z;
    }

    public TransformerIdentityImpl() {
        this(false);
    }

    @Override
    public void setResult(Result result) throws IllegalArgumentException {
        if (result == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_RESULT_NULL, null));
        }
        this.m_result = result;
    }

    @Override
    public void setSystemId(String str) {
        this.m_systemID = str;
    }

    @Override
    public String getSystemId() {
        return this.m_systemID;
    }

    @Override
    public Transformer getTransformer() {
        return this;
    }

    @Override
    public void reset() {
        this.m_flushedStartDoc = false;
        this.m_foundFirstElement = false;
        this.m_outputStream = null;
        clearParameters();
        this.m_result = null;
        this.m_resultContentHandler = null;
        this.m_resultDeclHandler = null;
        this.m_resultDTDHandler = null;
        this.m_resultLexicalHandler = null;
        this.m_serializer = null;
        this.m_systemID = null;
        this.m_URIResolver = null;
        this.m_outputFormat = new OutputProperties("xml");
    }

    private void createResultContentHandler(Result result) throws TransformerException {
        Document documentNewDocument;
        short nodeType;
        Node node;
        DOMBuilder dOMBuilder;
        if (result instanceof SAXResult) {
            SAXResult sAXResult = (SAXResult) result;
            this.m_resultContentHandler = sAXResult.getHandler();
            this.m_resultLexicalHandler = sAXResult.getLexicalHandler();
            if (this.m_resultContentHandler instanceof Serializer) {
                this.m_serializer = (Serializer) this.m_resultContentHandler;
            }
        } else if (result instanceof DOMResult) {
            DOMResult dOMResult = (DOMResult) result;
            Node node2 = dOMResult.getNode();
            Node nextSibling = dOMResult.getNextSibling();
            if (node2 != null) {
                nodeType = node2.getNodeType();
                node = node2;
                documentNewDocument = 9 == nodeType ? (Document) node2 : node2.getOwnerDocument();
            } else {
                try {
                    DocumentBuilderFactory documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
                    documentBuilderFactoryNewInstance.setNamespaceAware(true);
                    if (this.m_isSecureProcessing) {
                        try {
                            documentBuilderFactoryNewInstance.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                        } catch (ParserConfigurationException e) {
                        }
                    }
                    documentNewDocument = documentBuilderFactoryNewInstance.newDocumentBuilder().newDocument();
                    short nodeType2 = documentNewDocument.getNodeType();
                    dOMResult.setNode(documentNewDocument);
                    nodeType = nodeType2;
                    node = documentNewDocument;
                } catch (ParserConfigurationException e2) {
                    throw new TransformerException(e2);
                }
            }
            if (11 == nodeType) {
                dOMBuilder = new DOMBuilder(documentNewDocument, (DocumentFragment) node);
            } else {
                dOMBuilder = new DOMBuilder(documentNewDocument, node);
            }
            if (nextSibling != null) {
                dOMBuilder.setNextSibling(nextSibling);
            }
            this.m_resultContentHandler = dOMBuilder;
            this.m_resultLexicalHandler = dOMBuilder;
        } else if (result instanceof StreamResult) {
            StreamResult streamResult = (StreamResult) result;
            try {
                Serializer serializer = SerializerFactory.getSerializer(this.m_outputFormat.getProperties());
                this.m_serializer = serializer;
                if (streamResult.getWriter() != null) {
                    serializer.setWriter(streamResult.getWriter());
                } else if (streamResult.getOutputStream() != null) {
                    serializer.setOutputStream(streamResult.getOutputStream());
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
                    serializer.setOutputStream(this.m_outputStream);
                } else {
                    throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_OUTPUT_SPECIFIED, null));
                }
                this.m_resultContentHandler = serializer.asContentHandler();
            } catch (IOException e3) {
                throw new TransformerException(e3);
            }
        } else {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_CANNOT_TRANSFORM_TO_RESULT_TYPE, new Object[]{result.getClass().getName()}));
        }
        if (this.m_resultContentHandler instanceof DTDHandler) {
            this.m_resultDTDHandler = (DTDHandler) this.m_resultContentHandler;
        }
        if (this.m_resultContentHandler instanceof DeclHandler) {
            this.m_resultDeclHandler = (DeclHandler) this.m_resultContentHandler;
        }
        if (this.m_resultContentHandler instanceof LexicalHandler) {
            this.m_resultLexicalHandler = (LexicalHandler) this.m_resultContentHandler;
        }
    }

    @Override
    public void transform(Source source, Result result) throws TransformerException, SAXException {
        ?? r5;
        Throwable th;
        SAXException e;
        WrappedRuntimeException e2;
        IOException e3;
        XMLReader xMLReader;
        createResultContentHandler(result);
        if ((source instanceof StreamSource) && source.getSystemId() == null) {
            StreamSource streamSource = (StreamSource) source;
            if (streamSource.getInputStream() != null || streamSource.getReader() != null) {
            }
        } else if (source instanceof SAXSource) {
            SAXSource sAXSource = (SAXSource) source;
            if (sAXSource.getInputSource() != null || sAXSource.getXMLReader() != null) {
                boolean z = source instanceof DOMSource;
                r5 = source;
                if (z) {
                    Node node = ((DOMSource) source).getNode();
                    r5 = source;
                    if (node == null) {
                        try {
                            DocumentBuilder documentBuilderNewDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                            String systemId = source.getSystemId();
                            DOMSource dOMSource = new DOMSource(documentBuilderNewDocumentBuilder.newDocument());
                            if (systemId != null) {
                                dOMSource.setSystemId(systemId);
                            }
                            r5 = dOMSource;
                        } catch (ParserConfigurationException e4) {
                            throw new TransformerException(e4.getMessage());
                        }
                    }
                }
            }
        }
        try {
            boolean z2 = false;
            if (r5 instanceof DOMSource) {
                DOMSource dOMSource2 = (DOMSource) r5;
                this.m_systemID = dOMSource2.getSystemId();
                Node node2 = dOMSource2.getNode();
                if (node2 != null) {
                    try {
                        if (node2.getNodeType() == 2) {
                            startDocument();
                        }
                        try {
                            if (node2.getNodeType() == 2) {
                                char[] charArray = node2.getNodeValue().toCharArray();
                                characters(charArray, 0, charArray.length);
                            } else {
                                new TreeWalker(this, this.m_systemID).traverse(node2);
                            }
                            if (this.m_outputStream == null) {
                                return;
                            }
                            try {
                                this.m_outputStream.close();
                            } catch (IOException e5) {
                            }
                            this.m_outputStream = null;
                            return;
                        } finally {
                            if (node2.getNodeType() == 2) {
                                endDocument();
                            }
                        }
                    } catch (SAXException e6) {
                        throw new TransformerException(e6);
                    }
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_DOMSOURCE_INPUT, null));
            }
            InputSource inputSourceSourceToInputSource = SAXSource.sourceToInputSource(r5);
            if (inputSourceSourceToInputSource == null) {
                throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_CANNOT_TRANSFORM_SOURCE_TYPE, new Object[]{r5.getClass().getName()}));
            }
            if (inputSourceSourceToInputSource.getSystemId() != null) {
                this.m_systemID = inputSourceSourceToInputSource.getSystemId();
            }
            try {
                try {
                    if (r5 instanceof SAXSource) {
                        xMLReader = ((SAXSource) r5).getXMLReader();
                    } else {
                        xMLReader = null;
                    }
                    try {
                        try {
                            if (xMLReader != null) {
                                try {
                                    xMLReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
                                } catch (SAXException e7) {
                                }
                            } else {
                                try {
                                    z2 = true;
                                    xMLReader = XMLReaderManager.getInstance().getXMLReader();
                                } catch (SAXException e8) {
                                    throw new TransformerException(e8);
                                }
                            }
                            xMLReader.setContentHandler(this);
                            if (this instanceof DTDHandler) {
                                xMLReader.setDTDHandler(this);
                            }
                            try {
                                if (this instanceof LexicalHandler) {
                                    xMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", this);
                                }
                                if (this instanceof DeclHandler) {
                                    xMLReader.setProperty("http://xml.org/sax/properties/declaration-handler", this);
                                }
                            } catch (SAXException e9) {
                            }
                            try {
                                if (this instanceof LexicalHandler) {
                                    xMLReader.setProperty("http://xml.org/sax/handlers/LexicalHandler", this);
                                }
                                if (this instanceof DeclHandler) {
                                    xMLReader.setProperty("http://xml.org/sax/handlers/DeclHandler", this);
                                }
                            } catch (SAXNotRecognizedException e10) {
                            }
                            xMLReader.parse(inputSourceSourceToInputSource);
                            if (z2) {
                                XMLReaderManager.getInstance().releaseXMLReader(xMLReader);
                            }
                            if (this.m_outputStream != null) {
                                try {
                                    this.m_outputStream.close();
                                } catch (IOException e11) {
                                }
                                this.m_outputStream = null;
                            }
                        } catch (SAXException e12) {
                            e = e12;
                            throw new TransformerException(e);
                        }
                    } catch (IOException e13) {
                        e3 = e13;
                        throw new TransformerException(e3);
                    } catch (WrappedRuntimeException e14) {
                        e2 = e14;
                        for (Exception exception = e2.getException(); exception instanceof WrappedRuntimeException; exception = ((WrappedRuntimeException) exception).getException()) {
                        }
                        throw new TransformerException(e2.getException());
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (0 != 0) {
                        XMLReaderManager.getInstance().releaseXMLReader(r5);
                    }
                    throw th;
                }
            } catch (IOException e15) {
                e3 = e15;
            } catch (WrappedRuntimeException e16) {
                e2 = e16;
            } catch (SAXException e17) {
                e = e17;
            } catch (Throwable th3) {
                th = th3;
                r5 = 0;
                if (0 != 0) {
                }
                throw th;
            }
        } catch (Throwable th4) {
            if (this.m_outputStream != null) {
                try {
                    this.m_outputStream.close();
                } catch (IOException e18) {
                }
                this.m_outputStream = null;
            }
            throw th4;
        }
    }

    @Override
    public void setParameter(String str, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_SET_PARAM_VALUE, new Object[]{str}));
        }
        if (this.m_params == null) {
            this.m_params = new Hashtable();
        }
        this.m_params.put(str, obj);
    }

    @Override
    public Object getParameter(String str) {
        if (this.m_params == null) {
            return null;
        }
        return this.m_params.get(str);
    }

    @Override
    public void clearParameters() {
        if (this.m_params == null) {
            return;
        }
        this.m_params.clear();
    }

    @Override
    public void setURIResolver(URIResolver uRIResolver) {
        this.m_URIResolver = uRIResolver;
    }

    @Override
    public URIResolver getURIResolver() {
        return this.m_URIResolver;
    }

    @Override
    public void setOutputProperties(Properties properties) throws IllegalArgumentException {
        if (properties != null) {
            String str = (String) properties.get(Constants.ATTRNAME_OUTPUT_METHOD);
            if (str != null) {
                this.m_outputFormat = new OutputProperties(str);
            } else {
                this.m_outputFormat = new OutputProperties();
            }
            this.m_outputFormat.copyFrom(properties);
            return;
        }
        this.m_outputFormat = null;
    }

    @Override
    public Properties getOutputProperties() {
        return (Properties) this.m_outputFormat.getProperties().clone();
    }

    @Override
    public void setOutputProperty(String str, String str2) throws IllegalArgumentException {
        if (!OutputProperties.isLegalPropertyKey(str)) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{str}));
        }
        this.m_outputFormat.setProperty(str, str2);
    }

    @Override
    public String getOutputProperty(String str) throws IllegalArgumentException {
        String property = this.m_outputFormat.getProperty(str);
        if (property == null && !OutputProperties.isLegalPropertyKey(str)) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{str}));
        }
        return property;
    }

    @Override
    public void setErrorListener(ErrorListener errorListener) throws IllegalArgumentException {
        if (errorListener == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage("ER_NULL_ERROR_HANDLER", null));
        }
        this.m_errorListener = errorListener;
    }

    @Override
    public ErrorListener getErrorListener() {
        return this.m_errorListener;
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
        if (this.m_resultDTDHandler != null) {
            this.m_resultDTDHandler.notationDecl(str, str2, str3);
        }
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        if (this.m_resultDTDHandler != null) {
            this.m_resultDTDHandler.unparsedEntityDecl(str, str2, str3, str4);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        try {
            if (this.m_resultContentHandler == null) {
                createResultContentHandler(this.m_result);
            }
            this.m_resultContentHandler.setDocumentLocator(locator);
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        try {
            if (this.m_resultContentHandler == null) {
                createResultContentHandler(this.m_result);
            }
            this.m_flushedStartDoc = false;
            this.m_foundFirstElement = false;
        } catch (TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    protected final void flushStartDoc() throws SAXException {
        if (!this.m_flushedStartDoc) {
            if (this.m_resultContentHandler == null) {
                try {
                    createResultContentHandler(this.m_result);
                } catch (TransformerException e) {
                    throw new SAXException(e);
                }
            }
            this.m_resultContentHandler.startDocument();
            this.m_flushedStartDoc = true;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        flushStartDoc();
        this.m_resultContentHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        flushStartDoc();
        this.m_resultContentHandler.startPrefixMapping(str, str2);
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
        flushStartDoc();
        this.m_resultContentHandler.endPrefixMapping(str);
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (!this.m_foundFirstElement && this.m_serializer != null) {
            this.m_foundFirstElement = true;
            try {
                Serializer serializerSwitchSerializerIfHTML = SerializerSwitcher.switchSerializerIfHTML(str, str2, this.m_outputFormat.getProperties(), this.m_serializer);
                if (serializerSwitchSerializerIfHTML != this.m_serializer) {
                    try {
                        this.m_resultContentHandler = serializerSwitchSerializerIfHTML.asContentHandler();
                        if (this.m_resultContentHandler instanceof DTDHandler) {
                            this.m_resultDTDHandler = (DTDHandler) this.m_resultContentHandler;
                        }
                        if (this.m_resultContentHandler instanceof LexicalHandler) {
                            this.m_resultLexicalHandler = (LexicalHandler) this.m_resultContentHandler;
                        }
                        this.m_serializer = serializerSwitchSerializerIfHTML;
                    } catch (IOException e) {
                        throw new SAXException(e);
                    }
                }
            } catch (TransformerException e2) {
                throw new SAXException(e2);
            }
        }
        flushStartDoc();
        this.m_resultContentHandler.startElement(str, str2, str3, attributes);
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        this.m_resultContentHandler.endElement(str, str2, str3);
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        flushStartDoc();
        this.m_resultContentHandler.characters(cArr, i, i2);
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        this.m_resultContentHandler.ignorableWhitespace(cArr, i, i2);
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        flushStartDoc();
        this.m_resultContentHandler.processingInstruction(str, str2);
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        flushStartDoc();
        this.m_resultContentHandler.skippedEntity(str);
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
        flushStartDoc();
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.startDTD(str, str2, str3);
        }
    }

    @Override
    public void endDTD() throws SAXException {
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.endDTD();
        }
    }

    @Override
    public void startEntity(String str) throws SAXException {
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.startEntity(str);
        }
    }

    @Override
    public void endEntity(String str) throws SAXException {
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.endEntity(str);
        }
    }

    @Override
    public void startCDATA() throws SAXException {
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.startCDATA();
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.endCDATA();
        }
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        flushStartDoc();
        if (this.m_resultLexicalHandler != null) {
            this.m_resultLexicalHandler.comment(cArr, i, i2);
        }
    }

    @Override
    public void elementDecl(String str, String str2) throws SAXException {
        if (this.m_resultDeclHandler != null) {
            this.m_resultDeclHandler.elementDecl(str, str2);
        }
    }

    @Override
    public void attributeDecl(String str, String str2, String str3, String str4, String str5) throws SAXException {
        if (this.m_resultDeclHandler != null) {
            this.m_resultDeclHandler.attributeDecl(str, str2, str3, str4, str5);
        }
    }

    @Override
    public void internalEntityDecl(String str, String str2) throws SAXException {
        if (this.m_resultDeclHandler != null) {
            this.m_resultDeclHandler.internalEntityDecl(str, str2);
        }
    }

    @Override
    public void externalEntityDecl(String str, String str2, String str3) throws SAXException {
        if (this.m_resultDeclHandler != null) {
            this.m_resultDeclHandler.externalEntityDecl(str, str2, str3);
        }
    }
}
