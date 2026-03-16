package org.xml.sax;

@Deprecated
public class HandlerBase implements EntityResolver, DTDHandler, DocumentHandler, ErrorHandler {
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        return null;
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) {
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) {
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String name, AttributeList attributes) throws SAXException {
    }

    @Override
    public void endElement(String name) throws SAXException {
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
