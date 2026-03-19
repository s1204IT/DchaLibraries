package org.apache.xalan.processor;

import java.io.IOException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
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
            TemplatesHandler builder = newTemplatesHandler();
            TreeWalker walker = new TreeWalker(builder, new DOM2Helper(), builder.getSystemId());
            walker.traverse(node);
            return builder.getTemplates();
        } catch (TransformerConfigurationException tce) {
            throw tce;
        } catch (SAXException se) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(se));
                    return null;
                } catch (TransformerConfigurationException ex) {
                    throw ex;
                } catch (TransformerException ex2) {
                    throw new TransformerConfigurationException(ex2);
                }
            }
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSFROMNODE_FAILED, null), se);
        } catch (Exception e) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(e));
                    return null;
                } catch (TransformerConfigurationException ex3) {
                    throw ex3;
                } catch (TransformerException ex4) {
                    throw new TransformerConfigurationException(ex4);
                }
            }
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSFROMNODE_FAILED, null), e);
        }
    }

    String getDOMsystemID() {
        return this.m_DOMsystemID;
    }

    Templates processFromNode(Node node, String systemID) throws TransformerConfigurationException {
        this.m_DOMsystemID = systemID;
        return processFromNode(node);
    }

    @Override
    public Source getAssociatedStylesheet(Source source, String media, String title, String charset) throws TransformerConfigurationException {
        String baseID;
        InputSource isource = null;
        Node node = null;
        XMLReader reader = null;
        if (source instanceof DOMSource) {
            DOMSource dsource = (DOMSource) source;
            node = dsource.getNode();
            baseID = dsource.getSystemId();
        } else {
            isource = SAXSource.sourceToInputSource(source);
            baseID = isource.getSystemId();
        }
        StylesheetPIHandler handler = new StylesheetPIHandler(baseID, media, title, charset);
        if (this.m_uriResolver != null) {
            handler.setURIResolver(this.m_uriResolver);
        }
        try {
            try {
                if (node != null) {
                    TreeWalker walker = new TreeWalker(handler, new DOM2Helper(), baseID);
                    walker.traverse(node);
                } else {
                    try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        if (this.m_isSecureProcessing) {
                            try {
                                factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                            } catch (SAXException e) {
                            }
                        }
                        SAXParser jaxpParser = factory.newSAXParser();
                        reader = jaxpParser.getXMLReader();
                    } catch (AbstractMethodError e2) {
                    } catch (NoSuchMethodError e3) {
                    } catch (FactoryConfigurationError ex1) {
                        throw new SAXException(ex1.toString());
                    } catch (ParserConfigurationException ex) {
                        throw new SAXException(ex);
                    }
                    if (reader == null) {
                        reader = XMLReaderFactory.createXMLReader();
                    }
                    if (this.m_isSecureProcessing) {
                        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    }
                    reader.setContentHandler(handler);
                    reader.parse(isource);
                }
            } catch (SAXException se) {
                throw new TransformerConfigurationException("getAssociatedStylesheets failed", se);
            }
        } catch (IOException ioe) {
            throw new TransformerConfigurationException("getAssociatedStylesheets failed", ioe);
        } catch (StopParseException e4) {
        }
        return handler.getAssociatedStylesheet();
    }

    @Override
    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        return new StylesheetHandler(this);
    }

    @Override
    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        if (name == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_SET_FEATURE_NULL_NAME, null));
        }
        if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            this.m_isSecureProcessing = value;
            return;
        }
        throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_UNSUPPORTED_FEATURE, new Object[]{name}));
    }

    @Override
    public boolean getFeature(String name) {
        if (name == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_GET_FEATURE_NULL_NAME, null));
        }
        if ("http://javax.xml.transform.dom.DOMResult/feature" == name || "http://javax.xml.transform.dom.DOMSource/feature" == name || "http://javax.xml.transform.sax.SAXResult/feature" == name || "http://javax.xml.transform.sax.SAXSource/feature" == name || "http://javax.xml.transform.stream.StreamResult/feature" == name || "http://javax.xml.transform.stream.StreamSource/feature" == name || "http://javax.xml.transform.sax.SAXTransformerFactory/feature" == name || "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter" == name || "http://javax.xml.transform.dom.DOMResult/feature".equals(name) || "http://javax.xml.transform.dom.DOMSource/feature".equals(name) || "http://javax.xml.transform.sax.SAXResult/feature".equals(name) || "http://javax.xml.transform.sax.SAXSource/feature".equals(name) || "http://javax.xml.transform.stream.StreamResult/feature".equals(name) || "http://javax.xml.transform.stream.StreamSource/feature".equals(name) || "http://javax.xml.transform.sax.SAXTransformerFactory/feature".equals(name) || "http://javax.xml.transform.sax.SAXTransformerFactory/feature/xmlfilter".equals(name)) {
            return true;
        }
        if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            return this.m_isSecureProcessing;
        }
        return false;
    }

    @Override
    public void setAttribute(String name, Object obj) throws IllegalArgumentException {
        if (name.equals(FEATURE_INCREMENTAL)) {
            if (obj instanceof Boolean) {
                this.m_incremental = obj.booleanValue();
                return;
            } else {
                if (obj instanceof String) {
                    this.m_incremental = new Boolean((String) obj).booleanValue();
                    return;
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{name, obj}));
            }
        }
        if (name.equals(FEATURE_OPTIMIZE)) {
            if (obj instanceof Boolean) {
                this.m_optimize = obj.booleanValue();
                return;
            } else {
                if (obj instanceof String) {
                    this.m_optimize = new Boolean((String) obj).booleanValue();
                    return;
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{name, obj}));
            }
        }
        if (name.equals("http://xml.apache.org/xalan/properties/source-location")) {
            if (obj instanceof Boolean) {
                this.m_source_location = obj.booleanValue();
                return;
            } else {
                if (obj instanceof String) {
                    this.m_source_location = new Boolean((String) obj).booleanValue();
                    return;
                }
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_BAD_VALUE, new Object[]{name, obj}));
            }
        }
        throw new IllegalArgumentException(XSLMessages.createMessage("ER_NOT_SUPPORTED", new Object[]{name}));
    }

    @Override
    public Object getAttribute(String name) throws IllegalArgumentException {
        if (name.equals(FEATURE_INCREMENTAL)) {
            return new Boolean(this.m_incremental);
        }
        if (name.equals(FEATURE_OPTIMIZE)) {
            return new Boolean(this.m_optimize);
        }
        if (name.equals("http://xml.apache.org/xalan/properties/source-location")) {
            return new Boolean(this.m_source_location);
        }
        throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ATTRIB_VALUE_NOT_RECOGNIZED, new Object[]{name}));
    }

    @Override
    public XMLFilter newXMLFilter(Source src) throws TransformerConfigurationException {
        Templates templates = newTemplates(src);
        if (templates == null) {
            return null;
        }
        return newXMLFilter(templates);
    }

    @Override
    public XMLFilter newXMLFilter(Templates templates) throws TransformerConfigurationException {
        try {
            return new TrAXFilter(templates);
        } catch (TransformerConfigurationException ex) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(ex);
                    return null;
                } catch (TransformerConfigurationException ex1) {
                    throw ex1;
                } catch (TransformerException ex12) {
                    throw new TransformerConfigurationException(ex12);
                }
            }
            throw ex;
        }
    }

    @Override
    public TransformerHandler newTransformerHandler(Source src) throws TransformerConfigurationException {
        Templates templates = newTemplates(src);
        if (templates == null) {
            return null;
        }
        return newTransformerHandler(templates);
    }

    @Override
    public TransformerHandler newTransformerHandler(Templates templates) throws TransformerConfigurationException {
        try {
            TransformerImpl transformer = (TransformerImpl) templates.newTransformer();
            transformer.setURIResolver(this.m_uriResolver);
            TransformerHandler th = (TransformerHandler) transformer.getInputContentHandler(true);
            return th;
        } catch (TransformerConfigurationException ex) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(ex);
                    return null;
                } catch (TransformerConfigurationException ex1) {
                    throw ex1;
                } catch (TransformerException ex12) {
                    throw new TransformerConfigurationException(ex12);
                }
            }
            throw ex;
        }
    }

    @Override
    public TransformerHandler newTransformerHandler() throws TransformerConfigurationException {
        return new TransformerIdentityImpl(this.m_isSecureProcessing);
    }

    @Override
    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        try {
            Templates tmpl = newTemplates(source);
            if (tmpl == null) {
                return null;
            }
            Transformer transformer = tmpl.newTransformer();
            transformer.setURIResolver(this.m_uriResolver);
            return transformer;
        } catch (TransformerConfigurationException ex) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(ex);
                    return null;
                } catch (TransformerConfigurationException ex1) {
                    throw ex1;
                } catch (TransformerException ex12) {
                    throw new TransformerConfigurationException(ex12);
                }
            }
            throw ex;
        }
    }

    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
        return new TransformerIdentityImpl(this.m_isSecureProcessing);
    }

    @Override
    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        String baseID = source.getSystemId();
        if (baseID != null) {
            baseID = SystemIDResolver.getAbsoluteURI(baseID);
        }
        if (source instanceof DOMSource) {
            DOMSource dsource = (DOMSource) source;
            Node node = dsource.getNode();
            if (node != null) {
                return processFromNode(node, baseID);
            }
            String messageStr = XSLMessages.createMessage(XSLTErrorResources.ER_ILLEGAL_DOMSOURCE_INPUT, null);
            throw new IllegalArgumentException(messageStr);
        }
        TemplatesHandler builder = newTemplatesHandler();
        builder.setSystemId(baseID);
        try {
            try {
                InputSource isource = SAXSource.sourceToInputSource(source);
                isource.setSystemId(baseID);
                XMLReader reader = null;
                if (source instanceof SAXSource) {
                    reader = ((SAXSource) source).getXMLReader();
                }
                if (reader == null) {
                    try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        if (this.m_isSecureProcessing) {
                            try {
                                factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                            } catch (SAXException e) {
                            }
                        }
                        SAXParser jaxpParser = factory.newSAXParser();
                        reader = jaxpParser.getXMLReader();
                    } catch (AbstractMethodError e2) {
                    } catch (NoSuchMethodError e3) {
                    } catch (FactoryConfigurationError ex1) {
                        throw new SAXException(ex1.toString());
                    } catch (ParserConfigurationException ex) {
                        throw new SAXException(ex);
                    }
                }
                if (reader == null) {
                    reader = XMLReaderFactory.createXMLReader();
                }
                reader.setContentHandler(builder);
                reader.parse(isource);
            } catch (Exception e4) {
                if (this.m_errorListener != null) {
                    try {
                        this.m_errorListener.fatalError(new TransformerException(e4));
                        return null;
                    } catch (TransformerConfigurationException ex12) {
                        throw ex12;
                    } catch (TransformerException ex13) {
                        throw new TransformerConfigurationException(ex13);
                    }
                }
                throw new TransformerConfigurationException(e4.getMessage(), e4);
            }
        } catch (SAXException se) {
            if (this.m_errorListener != null) {
                try {
                    this.m_errorListener.fatalError(new TransformerException(se));
                } catch (TransformerConfigurationException ex14) {
                    throw ex14;
                } catch (TransformerException ex15) {
                    throw new TransformerConfigurationException(ex15);
                }
            } else {
                throw new TransformerConfigurationException(se.getMessage(), se);
            }
        }
        return builder.getTemplates();
    }

    @Override
    public void setURIResolver(URIResolver resolver) {
        this.m_uriResolver = resolver;
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
    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
        if (listener == null) {
            throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_ERRORLISTENER, null));
        }
        this.m_errorListener = listener;
    }

    public boolean isSecureProcessing() {
        return this.m_isSecureProcessing;
    }
}
