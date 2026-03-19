package org.apache.xalan.processor;

import java.io.IOException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
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

    public void setHref(String str) {
        this.m_href = str;
    }

    protected int getStylesheetType() {
        return 2;
    }

    protected String getStylesheetInclErr() {
        return XSLTErrorResources.ER_STYLESHEET_INCLUDES_ITSELF;
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, this);
        try {
            Source sourceFromUriResolver = getSourceFromUriResolver(stylesheetHandler);
            String baseURIOfIncludedStylesheet = getBaseURIOfIncludedStylesheet(stylesheetHandler, sourceFromUriResolver);
            if (stylesheetHandler.importStackContains(baseURIOfIncludedStylesheet)) {
                throw new SAXException(XSLMessages.createMessage(getStylesheetInclErr(), new Object[]{baseURIOfIncludedStylesheet}));
            }
            stylesheetHandler.pushImportURL(baseURIOfIncludedStylesheet);
            stylesheetHandler.pushImportSource(sourceFromUriResolver);
            int stylesheetType = stylesheetHandler.getStylesheetType();
            stylesheetHandler.setStylesheetType(getStylesheetType());
            stylesheetHandler.pushNewNamespaceSupport();
            try {
                parse(stylesheetHandler, str, str2, str3, attributes);
                stylesheetHandler.setStylesheetType(stylesheetType);
                stylesheetHandler.popImportURL();
                stylesheetHandler.popImportSource();
                stylesheetHandler.popNamespaceSupport();
            } catch (Throwable th) {
                stylesheetHandler.setStylesheetType(stylesheetType);
                stylesheetHandler.popImportURL();
                stylesheetHandler.popImportSource();
                stylesheetHandler.popNamespaceSupport();
                throw th;
            }
        } catch (TransformerException e) {
            stylesheetHandler.error(e.getMessage(), e);
        }
    }

    protected void parse(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        Source sourcePeekSourceFromURIResolver;
        XMLReader xMLReader = null;
        if (stylesheetHandler.getStylesheetProcessor().getURIResolver() != null) {
            try {
                sourcePeekSourceFromURIResolver = stylesheetHandler.peekSourceFromURIResolver();
                if (sourcePeekSourceFromURIResolver != null && (sourcePeekSourceFromURIResolver instanceof DOMSource)) {
                    Node node = ((DOMSource) sourcePeekSourceFromURIResolver).getNode();
                    String strPeekImportURL = stylesheetHandler.peekImportURL();
                    if (strPeekImportURL != null) {
                        stylesheetHandler.pushBaseIndentifier(strPeekImportURL);
                    }
                    try {
                        new TreeWalker(stylesheetHandler, new DOM2Helper(), strPeekImportURL).traverse(node);
                        if (strPeekImportURL != null) {
                            return;
                        } else {
                            return;
                        }
                    } catch (SAXException e) {
                        throw new TransformerException(e);
                    }
                }
            } catch (IOException e2) {
                stylesheetHandler.error(XSLTErrorResources.ER_IOEXCEPTION, new Object[]{getHref()}, e2);
                return;
            } catch (TransformerException e3) {
                stylesheetHandler.error(e3.getMessage(), e3);
                return;
            }
        } else {
            sourcePeekSourceFromURIResolver = null;
        }
        if (sourcePeekSourceFromURIResolver == null) {
            sourcePeekSourceFromURIResolver = new StreamSource(SystemIDResolver.getAbsoluteURI(getHref(), stylesheetHandler.getBaseIdentifier()));
        }
        Source sourceProcessSource = processSource(stylesheetHandler, sourcePeekSourceFromURIResolver);
        if (sourceProcessSource instanceof SAXSource) {
            xMLReader = ((SAXSource) sourceProcessSource).getXMLReader();
        }
        InputSource inputSourceSourceToInputSource = SAXSource.sourceToInputSource(sourceProcessSource);
        if (xMLReader == null) {
            try {
                try {
                    SAXParserFactory sAXParserFactoryNewInstance = SAXParserFactory.newInstance();
                    sAXParserFactoryNewInstance.setNamespaceAware(true);
                    if (stylesheetHandler.getStylesheetProcessor().isSecureProcessing()) {
                        try {
                            sAXParserFactoryNewInstance.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
                        } catch (SAXException e4) {
                        }
                    }
                    xMLReader = sAXParserFactoryNewInstance.newSAXParser().getXMLReader();
                } catch (NoSuchMethodError e5) {
                } catch (FactoryConfigurationError e6) {
                    throw new SAXException(e6.toString());
                }
            } catch (AbstractMethodError e7) {
            } catch (ParserConfigurationException e8) {
                throw new SAXException(e8);
            }
        }
        if (xMLReader == null) {
            xMLReader = XMLReaderFactory.createXMLReader();
        }
        if (xMLReader != null) {
            xMLReader.setContentHandler(stylesheetHandler);
            stylesheetHandler.pushBaseIndentifier(inputSourceSourceToInputSource.getSystemId());
            try {
                xMLReader.parse(inputSourceSourceToInputSource);
                stylesheetHandler.popBaseIndentifier();
            } finally {
                stylesheetHandler.popBaseIndentifier();
            }
        }
    }

    protected Source processSource(StylesheetHandler stylesheetHandler, Source source) {
        return source;
    }

    private Source getSourceFromUriResolver(StylesheetHandler stylesheetHandler) throws TransformerException {
        URIResolver uRIResolver = stylesheetHandler.getStylesheetProcessor().getURIResolver();
        if (uRIResolver != null) {
            return uRIResolver.resolve(getHref(), stylesheetHandler.getBaseIdentifier());
        }
        return null;
    }

    private String getBaseURIOfIncludedStylesheet(StylesheetHandler stylesheetHandler, Source source) throws TransformerException {
        String systemId;
        if (source == null || (systemId = source.getSystemId()) == null) {
            return SystemIDResolver.getAbsoluteURI(getHref(), stylesheetHandler.getBaseIdentifier());
        }
        return systemId;
    }
}
