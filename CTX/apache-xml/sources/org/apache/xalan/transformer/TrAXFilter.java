package org.apache.xalan.transformer;

import java.io.IOException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

public class TrAXFilter extends XMLFilterImpl {
    private Templates m_templates;
    private TransformerImpl m_transformer;

    public TrAXFilter(Templates templates) throws TransformerConfigurationException {
        this.m_templates = templates;
        this.m_transformer = (TransformerImpl) templates.newTransformer();
    }

    public TransformerImpl getTransformer() {
        return this.m_transformer;
    }

    @Override
    public void setParent(XMLReader xMLReader) {
        super.setParent(xMLReader);
        if (xMLReader.getContentHandler() != null) {
            setContentHandler(xMLReader.getContentHandler());
        }
        setupParse();
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        XMLReader xMLReaderCreateXMLReader;
        if (getParent() == null) {
            try {
                SAXParserFactory sAXParserFactoryNewInstance = SAXParserFactory.newInstance();
                sAXParserFactoryNewInstance.setNamespaceAware(true);
                if (this.m_transformer.getStylesheet().isSecureProcessing()) {
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
            try {
                xMLReaderCreateXMLReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            } catch (SAXException e6) {
            }
            setParent(xMLReaderCreateXMLReader);
        } else {
            setupParse();
        }
        if (this.m_transformer.getContentHandler() == null) {
            throw new SAXException(XSLMessages.createMessage(XSLTErrorResources.ER_CANNOT_CALL_PARSE, null));
        }
        getParent().parse(inputSource);
        Exception exceptionThrown = this.m_transformer.getExceptionThrown();
        if (exceptionThrown != null) {
            if (exceptionThrown instanceof SAXException) {
                throw ((SAXException) exceptionThrown);
            }
            throw new SAXException(exceptionThrown);
        }
    }

    @Override
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
    }

    private void setupParse() {
        XMLReader parent = getParent();
        if (parent == null) {
            throw new NullPointerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_PARENT_FOR_FILTER, null));
        }
        parent.setContentHandler(this.m_transformer.getInputContentHandler());
        parent.setEntityResolver(this);
        parent.setDTDHandler(this);
        parent.setErrorHandler(this);
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.m_transformer.setContentHandler(contentHandler);
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.m_transformer.setErrorListener(errorListener);
    }
}
