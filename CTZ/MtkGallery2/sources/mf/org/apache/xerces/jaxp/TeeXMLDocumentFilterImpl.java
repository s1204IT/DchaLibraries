package mf.org.apache.xerces.jaxp;

import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLDocumentFilter;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;

class TeeXMLDocumentFilterImpl implements XMLDocumentFilter {
    private XMLDocumentHandler next;
    private XMLDocumentHandler side;
    private XMLDocumentSource source;

    TeeXMLDocumentFilterImpl() {
    }

    public XMLDocumentHandler getSide() {
        return this.side;
    }

    public void setSide(XMLDocumentHandler side) {
        this.side = side;
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return this.source;
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
        this.source = source;
    }

    @Override
    public XMLDocumentHandler getDocumentHandler() {
        return this.next;
    }

    @Override
    public void setDocumentHandler(XMLDocumentHandler handler) {
        this.next = handler;
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        this.side.characters(text, augs);
        this.next.characters(text, augs);
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        this.side.comment(text, augs);
        this.next.comment(text, augs);
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
        this.side.doctypeDecl(rootElement, publicId, systemId, augs);
        this.next.doctypeDecl(rootElement, publicId, systemId, augs);
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        this.side.emptyElement(element, attributes, augs);
        this.next.emptyElement(element, attributes, augs);
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        this.side.endCDATA(augs);
        this.next.endCDATA(augs);
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        this.side.endDocument(augs);
        this.next.endDocument(augs);
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        this.side.endElement(element, augs);
        this.next.endElement(element, augs);
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        this.side.endGeneralEntity(name, augs);
        this.next.endGeneralEntity(name, augs);
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        this.side.ignorableWhitespace(text, augs);
        this.next.ignorableWhitespace(text, augs);
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        this.side.processingInstruction(target, data, augs);
        this.next.processingInstruction(target, data, augs);
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        this.side.startCDATA(augs);
        this.next.startCDATA(augs);
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        this.side.startDocument(locator, encoding, namespaceContext, augs);
        this.next.startDocument(locator, encoding, namespaceContext, augs);
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        this.side.startElement(element, attributes, augs);
        this.next.startElement(element, attributes, augs);
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        this.side.startGeneralEntity(name, identifier, encoding, augs);
        this.next.startGeneralEntity(name, identifier, encoding, augs);
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        this.side.textDecl(version, encoding, augs);
        this.next.textDecl(version, encoding, augs);
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        this.side.xmlDecl(version, encoding, standalone, augs);
        this.next.xmlDecl(version, encoding, standalone, augs);
    }
}
