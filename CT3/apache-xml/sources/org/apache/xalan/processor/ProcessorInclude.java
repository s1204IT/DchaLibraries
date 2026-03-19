package org.apache.xalan.processor;

import java.io.IOException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.DOM2Helper;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.TreeWalker;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ProcessorInclude extends XSLTElementProcessor {
    static final long serialVersionUID = -4570078731972673481L;
    private String m_href = null;

    public String getHref() {
        return this.m_href;
    }

    public void setHref(String baseIdent) {
        this.m_href = baseIdent;
    }

    protected int getStylesheetType() {
        return 2;
    }

    protected String getStylesheetInclErr() {
        return XSLTErrorResources.ER_STYLESHEET_INCLUDES_ITSELF;
    }

    @Override
    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        setPropertiesFromAttributes(handler, rawName, attributes, this);
        try {
            Source sourceFromURIResolver = getSourceFromUriResolver(handler);
            String hrefUrl = getBaseURIOfIncludedStylesheet(handler, sourceFromURIResolver);
            if (handler.importStackContains(hrefUrl)) {
                throw new SAXException(XSLMessages.createMessage(getStylesheetInclErr(), new Object[]{hrefUrl}));
            }
            handler.pushImportURL(hrefUrl);
            handler.pushImportSource(sourceFromURIResolver);
            int savedStylesheetType = handler.getStylesheetType();
            handler.setStylesheetType(getStylesheetType());
            handler.pushNewNamespaceSupport();
            try {
                parse(handler, uri, localName, rawName, attributes);
                handler.setStylesheetType(savedStylesheetType);
                handler.popImportURL();
                handler.popImportSource();
                handler.popNamespaceSupport();
            } catch (Throwable th) {
                handler.setStylesheetType(savedStylesheetType);
                handler.popImportURL();
                handler.popImportSource();
                handler.popNamespaceSupport();
                throw th;
            }
        } catch (TransformerException te) {
            handler.error(te.getMessage(), te);
        }
    }

    protected void parse(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        Source source;
        Source source2;
        Source source3;
        XMLReader reader;
        TransformerFactoryImpl processor = handler.getStylesheetProcessor();
        URIResolver uriresolver = processor.getURIResolver();
        Source source4 = null;
        if (uriresolver != null) {
            try {
                source4 = handler.peekSourceFromURIResolver();
                if (source4 == null) {
                    source = source4;
                    if (source != null) {
                        try {
                            String absURL = SystemIDResolver.getAbsoluteURI(getHref(), handler.getBaseIdentifier());
                            source2 = new StreamSource(absURL);
                        } catch (IOException e) {
                            ioe = e;
                            handler.error(XSLTErrorResources.ER_IOEXCEPTION, new Object[]{getHref()}, ioe);
                            return;
                        } catch (TransformerException e2) {
                            te = e2;
                            handler.error(te.getMessage(), te);
                            return;
                        }
                    } else {
                        source2 = source;
                    }
                    source3 = processSource(handler, source2);
                    reader = null;
                    if (source3 instanceof SAXSource) {
                        SAXSource saxSource = (SAXSource) source3;
                        reader = saxSource.getXMLReader();
                    }
                    InputSource inputSource = SAXSource.sourceToInputSource(source3);
                    if (reader == null) {
                        try {
                            SAXParserFactory factory = SAXParserFactory.newInstance();
                            factory.setNamespaceAware(true);
                            if (handler.getStylesheetProcessor().isSecureProcessing()) {
                                try {
                                    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                                } catch (SAXException e3) {
                                }
                            }
                            SAXParser jaxpParser = factory.newSAXParser();
                            reader = jaxpParser.getXMLReader();
                        } catch (AbstractMethodError e4) {
                        } catch (NoSuchMethodError e5) {
                        } catch (FactoryConfigurationError ex1) {
                            throw new SAXException(ex1.toString());
                        } catch (ParserConfigurationException ex) {
                            throw new SAXException(ex);
                        }
                    }
                    if (reader == null) {
                        reader = XMLReaderFactory.createXMLReader();
                    }
                    if (reader != null) {
                        return;
                    }
                    reader.setContentHandler(handler);
                    handler.pushBaseIndentifier(inputSource.getSystemId());
                    try {
                        reader.parse(inputSource);
                        return;
                    } finally {
                        handler.popBaseIndentifier();
                    }
                }
                if (source4 instanceof DOMSource) {
                    Node node = ((DOMSource) source4).getNode();
                    String systemId = handler.peekImportURL();
                    if (systemId != null) {
                        handler.pushBaseIndentifier(systemId);
                    }
                    TreeWalker walker = new TreeWalker(handler, new DOM2Helper(), systemId);
                    try {
                        walker.traverse(node);
                        if (systemId != null) {
                            return;
                        } else {
                            return;
                        }
                    } catch (SAXException se) {
                        throw new TransformerException(se);
                    }
                }
            } catch (IOException e6) {
                ioe = e6;
                handler.error(XSLTErrorResources.ER_IOEXCEPTION, new Object[]{getHref()}, ioe);
                return;
            } catch (TransformerException e7) {
                te = e7;
                handler.error(te.getMessage(), te);
                return;
            }
        }
        source = source4;
        if (source != null) {
        }
        source3 = processSource(handler, source2);
        reader = null;
        if (source3 instanceof SAXSource) {
        }
        InputSource inputSource2 = SAXSource.sourceToInputSource(source3);
        if (reader == null) {
        }
        if (reader == null) {
        }
        if (reader != null) {
        }
    }

    protected Source processSource(StylesheetHandler handler, Source source) {
        return source;
    }

    private Source getSourceFromUriResolver(StylesheetHandler handler) throws TransformerException {
        TransformerFactoryImpl processor = handler.getStylesheetProcessor();
        URIResolver uriresolver = processor.getURIResolver();
        if (uriresolver == null) {
            return null;
        }
        String href = getHref();
        String base = handler.getBaseIdentifier();
        Source s = uriresolver.resolve(href, base);
        return s;
    }

    private String getBaseURIOfIncludedStylesheet(StylesheetHandler handler, Source s) throws TransformerException {
        String idFromUriResolverSource;
        if (s != null && (idFromUriResolverSource = s.getSystemId()) != null) {
            return idFromUriResolverSource;
        }
        String baseURI = SystemIDResolver.getAbsoluteURI(getHref(), handler.getBaseIdentifier());
        return baseURI;
    }
}
