package mf.org.apache.xerces.impl.xs.opti;

import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDTDContentModelHandler;
import mf.org.apache.xerces.xni.XMLDTDHandler;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLDTDContentModelSource;
import mf.org.apache.xerces.xni.parser.XMLDTDSource;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;

public class DefaultXMLDocumentHandler implements XMLDTDContentModelHandler, XMLDTDHandler, XMLDocumentHandler {
    private XMLDTDContentModelSource fCMSource;
    private XMLDTDSource fDTDSource;
    private XMLDocumentSource fDocumentSource;

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext context, Augmentations augs) throws XNIException {
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
    }

    public void startPrefixMapping(String prefix, String uri, Augmentations augs) throws XNIException {
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
    }

    public void endPrefixMapping(String prefix, Augmentations augs) throws XNIException {
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
    }

    @Override
    public void startDTD(XMLLocator locator, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void startParameterEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endParameterEntity(String name, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void startExternalSubset(XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endExternalSubset(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void elementDecl(String name, String contentModel, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void startAttlist(String elementName, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void attributeDecl(String elementName, String attributeName, String type, String[] enumeration, String defaultType, XMLString defaultValue, XMLString nonNormalizedDefaultValue, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endAttlist(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void internalEntityDecl(String name, XMLString text, XMLString nonNormalizedText, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier, String notation, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void notationDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void startConditional(short type, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void ignoredCharacters(XMLString text, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endConditional(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endDTD(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void startContentModel(String elementName, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void any(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void empty(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void startGroup(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void pcdata(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void element(String elementName, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void separator(short separator, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void occurrence(short occurrence, Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endGroup(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void endContentModel(Augmentations augmentations) throws XNIException {
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
        this.fDocumentSource = source;
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return this.fDocumentSource;
    }

    @Override
    public void setDTDSource(XMLDTDSource source) {
        this.fDTDSource = source;
    }

    @Override
    public XMLDTDSource getDTDSource() {
        return this.fDTDSource;
    }

    @Override
    public void setDTDContentModelSource(XMLDTDContentModelSource source) {
        this.fCMSource = source;
    }

    @Override
    public XMLDTDContentModelSource getDTDContentModelSource() {
        return this.fCMSource;
    }
}
