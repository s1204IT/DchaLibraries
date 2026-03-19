package org.xml.sax;

@Deprecated
public class HandlerBase implements EntityResolver, DTDHandler, DocumentHandler, ErrorHandler {
    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException {
        return null;
    }

    @Override
    public void notationDecl(String str, String str2, String str3) {
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) {
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
    public void startElement(String str, AttributeList attributeList) throws SAXException {
    }

    @Override
    public void endElement(String str) throws SAXException {
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        throw sAXParseException;
    }
}
