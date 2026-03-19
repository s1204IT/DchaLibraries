package org.apache.xalan.processor;

import java.io.IOException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TrAXFilter;
import org.apache.xalan.transformer.TransformerIdentityImpl;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.DOM2Helper;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.StopParseException;
import org.apache.xml.utils.StylesheetPIHandler;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.TreeWalker;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class TransformerFactoryImpl extends SAXTransformerFactory {
    public static final String FEATURE_INCREMENTAL = "http://xml.apache.org/xalan/features/incremental";
    public static final String FEATURE_OPTIMIZE = "http://xml.apache.org/xalan/features/optimize";
    public static final String FEATURE_SOURCE_LOCATION = "http://xml.apache.org/xalan/properties/source-location";
    public static final String XSLT_PROPERTIES = "org/apache/xalan/res/XSLTInfo.properties";
    URIResolver m_uriResolver;
    private boolean m_isSecureProcessing = false;
    private String m_DOMsystemID = null;
    private boolean m_optimize = true;
    private boolean m_source_location = false;
    private boolean m_incremental = false;
    private ErrorListener m_errorListener = new DefaultErrorHandler(false);

    public Templates processFromNode(Node node) throws TransformerConfigurationException {
        try {
            TemplatesHandler templatesHandlerNewTemplatesHandler = newTemplatesHandler();
            new TreeWalker(templatesHandlerNewTemplatesHandler, new DOM2Helper(), templatesHandlerNewTemplatesHandler.getSystemId()).traverse(node);
            return templatesHandlerNewTemplatesHandler.getTemplates();
        } catch (TransformerConfigurationException e) {
            throw e;
        } catch (SAXException e2) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(e2));
                    return null;
                } catch (TransformerConfigurationException e3) {
                    throw e3;
                } catch (TransformerException e4) {
                    throw new TransformerConfigurationException(e4);
                }
            }
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSFROMNODE_FAILED, null), e2);
        } catch (Exception e5) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(e5));
                    return null;
                } catch (TransformerConfigurationException e6) {
                    throw e6;
                } catch (TransformerException e7) {
                    throw new TransformerConfigurationException(e7);
                }
            }
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSFROMNODE_FAILED, null), e5);
        }
    }

    String getDOMsystemID() {
        return this.m_DOMsystemID;
    }

    Templates processFromNode(Node node, String str) throws TransformerConfigurationException {
        this.m_DOMsystemID = str;
        return processFromNode(node);
    }

    @Override
    public Source getAssociatedStylesheet(Source source, String str, String str2, String str3) throws TransformerConfigurationException {
        InputSource inputSource;
        String systemId;
        Node node;
        XMLReader xMLReaderCreateXMLReader;
        if (source instanceof DOMSource) {
            DOMSource dOMSource = (DOMSource) source;
            node = dOMSource.getNode();
            systemId = dOMSource.getSystemId();
            inputSource = null;
        } else {
            InputSource inputSourceSourceToInputSource = SAXSource.sourceToInputSource(source);
            inputSource = inputSourceSourceToInputSource;
            systemId = inputSourceSourceToInputSource.getSystemId();
            node = null;
        }
        StylesheetPIHandler stylesheetPIHandler = new StylesheetPIHandler(systemId, str, str2, str3);
        if (this.m_uriResolver != null) {
            stylesheetPIHandler.setURIResolver(this.m_uriResolver);
        }
        try {
            try {
                if (node != null) {
                    new TreeWalker(stylesheetPIHandler, new DOM2Helper(), systemId).traverse(node);
                } else {
                    try {
                        SAXParserFactory sAXParserFactoryNewInstance = SAXParserFactory.newInstance();
                        sAXParserFactoryNewInstance.setNamespaceAware(true);
                        if (this.m_isSecureProcessing) {
                            try {
                                sAXParserFactoryNewInstance.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                            } catch (SAXException e) {
                            }
                        }
                        xMLReaderCreateXMLReader = sAXParserFactoryNewInstance.newSAXParser().getXMLReader();
                    } catch (AbstractMethodError e2) {
                        xMLReaderCreateXMLReader = null;
                    } catch (NoSuchMethodError e3) {
                        xMLReaderCreateXMLReader = null;
                    } catch (FactoryConfigurationError e4) {
                        throw new SAXException(e4.toString());
                    } catch (ParserConfigurationException e5) {
                        throw new SAXException(e5);
                    }
                    if (xMLReaderCreateXMLReader == null) {
                        xMLReaderCreateXMLReader = XMLReaderFactory.createXMLReader();
                    }
                    if (this.m_isSecureProcessing) {
                        xMLReaderCreateXMLReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    }
                    xMLReaderCreateXMLReader.setContentHandler(stylesheetPIHandler);
                    xMLReaderCreateXMLReader.parse(inputSource);
                }
            } catch (SAXException e6) {
                throw new TransformerConfigurationException("getAssociatedStylesheets failed", e6);
            }
        } catch (IOException e7) {
            throw new TransformerConfigurationException("getAssociatedStylesheets failed", e7);
        } catch (StopParseException e8) {
        }
        return stylesheetPIHandler.getAssociatedStylesheet();
    }

    @Override
    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        return new StylesheetHandler(this);
    }

    @Override
    public void setFeature(String str, boolean z) throws TransformerConfigurationException {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_SET_FEATURE_NULL_NAME, null));
        }
        if (str.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            this.m_isSecureProcessing = z;
            return;
        }
        throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_UNSUPPORTED_FEATURE, new Object[]{str}));
    }

    @Override
    public boolean getFeature(String str) {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_GET_FEATURE_NULL_NAME, null));
        }
        if ("http://javax.xml.transform.dom.DOMResult/feature" == str || "http://javax.xml.transform.dom.DOMSource/feature" == str || "http://javax.xml.transform.sax.SAXResult/feature" == str || "http://javax.xml.transform.sax.SAXSource/feature" == str || "http://javax.xml.transform.stream.StreamResult/feature" == str || "http://javax.xml.transform.stream.StreamSource/feature" == str || "http://javax.xml.transform.sax.SAXTransformerFactory/feature" == str || "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter" == str || "http://javax.xml.transform.dom.DOMResult/feature".equals(str) || "http://javax.xml.transform.dom.DOMSource/feature".equals(str) || "http://javax.xml.transform.sax.SAXResult/feature".equals(str) || "http://javax.xml.transform.sax.SAXSource/feature".equals(str) || "http://javax.xml.transform.stream.StreamResult/feature".equals(str) || "http://javax.xml.transform.stream.StreamSource/feature".equals(str) || "http://javax.xml.transform.sax.SAXTransformerFactory/feature".equals(str) || "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter".equals(str)) {
            return true;
        }
        if (str.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            return this.m_isSecureProcessing;
        }
        return false;
    }

    @Override
    public void setAttribute(String str, Object obj) throws IllegalArgumentException {
        if (str.equals(FEATURE_INCREMENTAL)) {
            if (obj instanceof Boolean) {
                this.m_incremental = ((Boolean) obj).booleanValue();
                return;
            } else {
                if (obj instanceof String) {
                    this.m_incremental = new Boolean((String) obj).booleanValue();
                    return;
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{str, obj}));
            }
        }
        if (str.equals(FEATURE_OPTIMIZE)) {
            if (obj instanceof Boolean) {
                this.m_optimize = ((Boolean) obj).booleanValue();
                return;
            } else {
                if (obj instanceof String) {
                    this.m_optimize = new Boolean((String) obj).booleanValue();
                    return;
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{str, obj}));
            }
        }
        if (str.equals("http://xml.apache.org/xalan/properties/source-location")) {
            if (obj instanceof Boolean) {
                this.m_source_location = ((Boolean) obj).booleanValue();
                return;
            } else {
                if (obj instanceof String) {
                    this.m_source_location = new Boolean((String) obj).booleanValue();
                    return;
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{str, obj}));
            }
        }
        throw new IllegalArgumentException(XSLMessages.createMessage("ER_NOT_SUPPORTED", new Object[]{str}));
    }

    @Override
    public Object getAttribute(String str) throws IllegalArgumentException {
        if (str.equals(FEATURE_INCREMENTAL)) {
            return new Boolean(this.m_incremental);
        }
        if (str.equals(FEATURE_OPTIMIZE)) {
            return new Boolean(this.m_optimize);
        }
        if (str.equals("http://xml.apache.org/xalan/properties/source-location")) {
            return new Boolean(this.m_source_location);
        }
        throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ATTRIB_VALUE_NOT_RECOGNIZED, new Object[]{str}));
    }

    @Override
    public XMLFilter newXMLFilter(Source source) throws TransformerConfigurationException, SAXException {
        Templates templatesNewTemplates = newTemplates(source);
        if (templatesNewTemplates == null) {
            return null;
        }
        return newXMLFilter(templatesNewTemplates);
    }

    @Override
    public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
        try {
            return new TrAXFilter(templates);
        } catch (TransformerConfigurationException e) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(e);
                    return null;
                } catch (TransformerConfigurationException e2) {
                    throw e2;
                } catch (TransformerException e3) {
                    throw new TransformerConfigurationException(e3);
                }
            }
            throw e;
        }
    }

    @Override
    public TransformerHandler newTransformerHandler(Source source) throws TransformerConfigurationException, SAXException {
        Templates templatesNewTemplates = newTemplates(source);
        if (templatesNewTemplates == null) {
            return null;
        }
        return newTransformerHandler(templatesNewTemplates);
    }

    @Override
    public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
        try {
            TransformerImpl transformerImpl = (TransformerImpl) templates.newTransformer();
            transformerImpl.setURIResolver(this.m_uriResolver);
            return (TransformerHandler) transformerImpl.getInputContentHandler(true);
        } catch (TransformerConfigurationException e) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(e);
                    return null;
                } catch (TransformerConfigurationException e2) {
                    throw e2;
                } catch (TransformerException e3) {
                    throw new TransformerConfigurationException(e3);
                }
            }
            throw e;
        }
    }

    @Override
    public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
        return new TransformerIdentityImpl(this.m_isSecureProcessing);
    }

    @Override
    public Transformer newTransformer(Source source) throws TransformerConfigurationException, SAXException {
        try {
            Templates templatesNewTemplates = newTemplates(source);
            if (templatesNewTemplates == null) {
                return null;
            }
            Transformer transformerNewTransformer = templatesNewTemplates.newTransformer();
            transformerNewTransformer.setURIResolver(this.m_uriResolver);
            return transformerNewTransformer;
        } catch (TransformerConfigurationException e) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(e);
                    return null;
                } catch (TransformerConfigurationException e2) {
                    throw e2;
                } catch (TransformerException e3) {
                    throw new TransformerConfigurationException(e3);
                }
            }
            throw e;
        }
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        return new TransformerIdentityImpl(this.m_isSecureProcessing);
    }

    @Override
    public Templates newTemplates(Source source) throws TransformerConfigurationException, SAXException {
        XMLReader xMLReader;
        String systemId = source.getSystemId();
        if (systemId != null) {
            systemId = SystemIDResolver.getAbsoluteURI(systemId);
        }
        if (source instanceof DOMSource) {
            Node node = ((DOMSource) source).getNode();
            if (node != null) {
                return processFromNode(node, systemId);
            }
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_DOMSOURCE_INPUT, null));
        }
        TemplatesHandler templatesHandlerNewTemplatesHandler = newTemplatesHandler();
        templatesHandlerNewTemplatesHandler.setSystemId(systemId);
        try {
            try {
                InputSource inputSourceSourceToInputSource = SAXSource.sourceToInputSource(source);
                inputSourceSourceToInputSource.setSystemId(systemId);
                if (source instanceof SAXSource) {
                    xMLReader = ((SAXSource) source).getXMLReader();
                } else {
                    xMLReader = null;
                }
                if (xMLReader == null) {
                    try {
                        SAXParserFactory sAXParserFactoryNewInstance = SAXParserFactory.newInstance();
                        sAXParserFactoryNewInstance.setNamespaceAware(true);
                        if (this.m_isSecureProcessing) {
                            try {
                                sAXParserFactoryNewInstance.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                            } catch (SAXException e) {
                            }
                        }
                        xMLReader = sAXParserFactoryNewInstance.newSAXParser().getXMLReader();
                    } catch (AbstractMethodError e2) {
                    } catch (NoSuchMethodError e3) {
                    } catch (FactoryConfigurationError e4) {
                        throw new SAXException(e4.toString());
                    } catch (ParserConfigurationException e5) {
                        throw new SAXException(e5);
                    }
                }
                if (xMLReader == null) {
                    xMLReader = XMLReaderFactory.createXMLReader();
                }
                xMLReader.setContentHandler(templatesHandlerNewTemplatesHandler);
                xMLReader.parse(inputSourceSourceToInputSource);
            } catch (SAXException e6) {
                if (this.m_errorListener != null) {
                    try {
                        this.m_errorListener.fatalError(new TransformerException(e6));
                    } catch (TransformerConfigurationException e7) {
                        throw e7;
                    } catch (TransformerException e8) {
                        throw new TransformerConfigurationException(e8);
                    }
                } else {
                    throw new TransformerConfigurationException(e6.getMessage(), e6);
                }
            }
            return templatesHandlerNewTemplatesHandler.getTemplates();
        } catch (Exception e9) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(e9));
                    return null;
                } catch (TransformerConfigurationException e10) {
                    throw e10;
                } catch (TransformerException e11) {
                    throw new TransformerConfigurationException(e11);
                }
            }
            throw new TransformerConfigurationException(e9.getMessage(), e9);
        }
    }

    @Override
    public void setURIResolver(URIResolver uRIResolver) {
        this.m_uriResolver = uRIResolver;
    }

    @Override
    public URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    @Override
    public ErrorListener getErrorListener() {
        return this.m_errorListener;
    }

    @Override
    public void setErrorListener(ErrorListener errorListener) throws IllegalArgumentException {
        if (errorListener == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ERRORLISTENER, null));
        }
        this.m_errorListener = errorListener;
    }

    public boolean isSecureProcessing() {
        return this.m_isSecureProcessing;
    }
}
