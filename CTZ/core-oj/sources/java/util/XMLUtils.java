package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class XMLUtils {
    static final boolean $assertionsDisabled = false;
    private static final String EXTERNAL_XML_VERSION = "1.0";
    private static final String PROPS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- DTD for properties --><!ELEMENT properties ( comment?, entry* ) ><!ATTLIST properties version CDATA #FIXED \"1.0\"><!ELEMENT comment (#PCDATA) ><!ELEMENT entry (#PCDATA) ><!ATTLIST entry  key CDATA #REQUIRED>";
    private static final String PROPS_DTD_URI = "http://java.sun.com/dtd/properties.dtd";

    XMLUtils() {
    }

    static void load(Properties properties, InputStream inputStream) throws IOException {
        try {
            Element documentElement = getLoadingDoc(inputStream).getDocumentElement();
            String attribute = documentElement.getAttribute("version");
            if (attribute.compareTo(EXTERNAL_XML_VERSION) > 0) {
                throw new InvalidPropertiesFormatException("Exported Properties file format version " + attribute + " is not supported. This java installation can read versions " + EXTERNAL_XML_VERSION + " or older. You may need to install a newer version of JDK.");
            }
            importProperties(properties, documentElement);
        } catch (SAXException e) {
            throw new InvalidPropertiesFormatException(e);
        }
    }

    static Document getLoadingDoc(InputStream inputStream) throws SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
        documentBuilderFactoryNewInstance.setIgnoringElementContentWhitespace(true);
        documentBuilderFactoryNewInstance.setCoalescing(true);
        documentBuilderFactoryNewInstance.setIgnoringComments(true);
        try {
            DocumentBuilder documentBuilderNewDocumentBuilder = documentBuilderFactoryNewInstance.newDocumentBuilder();
            documentBuilderNewDocumentBuilder.setEntityResolver(new Resolver());
            documentBuilderNewDocumentBuilder.setErrorHandler(new EH());
            return documentBuilderNewDocumentBuilder.parse(new InputSource(inputStream));
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }
    }

    static void importProperties(Properties properties, Element element) {
        NodeList childNodes = element.getChildNodes();
        int length = childNodes.getLength();
        int i = 0;
        if (length > 0 && childNodes.item(0).getNodeName().equals("comment")) {
            i = 1;
        }
        while (i < length) {
            if (childNodes.item(i) instanceof Element) {
                Element element2 = (Element) childNodes.item(i);
                if (element2.hasAttribute("key")) {
                    Node firstChild = element2.getFirstChild();
                    properties.setProperty(element2.getAttribute("key"), firstChild == null ? "" : firstChild.getNodeValue());
                }
            }
            i++;
        }
    }

    static void save(Properties properties, OutputStream outputStream, String str, String str2) throws IOException {
        DocumentBuilder documentBuilderNewDocumentBuilder;
        try {
            documentBuilderNewDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            documentBuilderNewDocumentBuilder = null;
        }
        Document documentNewDocument = documentBuilderNewDocumentBuilder.newDocument();
        Element element = (Element) documentNewDocument.appendChild(documentNewDocument.createElement("properties"));
        if (str != null) {
            ((Element) element.appendChild(documentNewDocument.createElement("comment"))).appendChild(documentNewDocument.createTextNode(str));
        }
        synchronized (properties) {
            for (String str3 : properties.stringPropertyNames()) {
                Element element2 = (Element) element.appendChild(documentNewDocument.createElement("entry"));
                element2.setAttribute("key", str3);
                element2.appendChild(documentNewDocument.createTextNode(properties.getProperty(str3)));
            }
        }
        emitDocument(documentNewDocument, outputStream, str2);
    }

    static void emitDocument(Document document, OutputStream outputStream, String str) throws IOException {
        Transformer transformerNewTransformer;
        try {
            transformerNewTransformer = TransformerFactory.newInstance().newTransformer();
            try {
                transformerNewTransformer.setOutputProperty("doctype-system", PROPS_DTD_URI);
                transformerNewTransformer.setOutputProperty("indent", "yes");
                transformerNewTransformer.setOutputProperty("method", "xml");
                transformerNewTransformer.setOutputProperty("encoding", str);
            } catch (TransformerConfigurationException e) {
            }
        } catch (TransformerConfigurationException e2) {
            transformerNewTransformer = null;
        }
        try {
            transformerNewTransformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (TransformerException e3) {
            IOException iOException = new IOException();
            iOException.initCause(e3);
            throw iOException;
        }
    }

    private static class Resolver implements EntityResolver {
        private Resolver() {
        }

        @Override
        public InputSource resolveEntity(String str, String str2) throws SAXException {
            if (str2.equals(XMLUtils.PROPS_DTD_URI)) {
                InputSource inputSource = new InputSource(new StringReader(XMLUtils.PROPS_DTD));
                inputSource.setSystemId(XMLUtils.PROPS_DTD_URI);
                return inputSource;
            }
            throw new SAXException("Invalid system identifier: " + str2);
        }
    }

    private static class EH implements ErrorHandler {
        private EH() {
        }

        @Override
        public void error(SAXParseException sAXParseException) throws SAXException {
            throw sAXParseException;
        }

        @Override
        public void fatalError(SAXParseException sAXParseException) throws SAXException {
            throw sAXParseException;
        }

        @Override
        public void warning(SAXParseException sAXParseException) throws SAXException {
            throw sAXParseException;
        }
    }
}
