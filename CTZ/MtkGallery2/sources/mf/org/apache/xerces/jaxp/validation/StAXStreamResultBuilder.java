package mf.org.apache.xerces.jaxp.validation;

import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.XMLStreamReader;
import mf.javax.xml.stream.XMLStreamWriter;
import mf.javax.xml.stream.events.Characters;
import mf.javax.xml.stream.events.Comment;
import mf.javax.xml.stream.events.DTD;
import mf.javax.xml.stream.events.EndDocument;
import mf.javax.xml.stream.events.EntityReference;
import mf.javax.xml.stream.events.ProcessingInstruction;
import mf.javax.xml.stream.events.StartDocument;
import mf.javax.xml.transform.stax.StAXResult;
import mf.org.apache.xerces.util.JAXPNamespaceContextWrapper;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;

final class StAXStreamResultBuilder implements StAXDocumentHandler {
    private final QName fAttrName = new QName();
    private boolean fIgnoreChars;
    private boolean fInCDATA;
    private final JAXPNamespaceContextWrapper fNamespaceContext;
    private XMLStreamWriter fStreamWriter;

    public StAXStreamResultBuilder(JAXPNamespaceContextWrapper context) {
        this.fNamespaceContext = context;
    }

    @Override
    public void setStAXResult(StAXResult result) {
        this.fIgnoreChars = false;
        this.fInCDATA = false;
        this.fAttrName.clear();
        this.fStreamWriter = result != null ? result.getXMLStreamWriter() : null;
    }

    @Override
    public void startDocument(XMLStreamReader reader) throws XMLStreamException {
        String version = reader.getVersion();
        String encoding = reader.getCharacterEncodingScheme();
        this.fStreamWriter.writeStartDocument(encoding != null ? encoding : "UTF-8", version != null ? version : "1.0");
    }

    @Override
    public void endDocument(XMLStreamReader reader) throws XMLStreamException {
        this.fStreamWriter.writeEndDocument();
        this.fStreamWriter.flush();
    }

    @Override
    public void comment(XMLStreamReader reader) throws XMLStreamException {
        this.fStreamWriter.writeComment(reader.getText());
    }

    @Override
    public void processingInstruction(XMLStreamReader reader) throws XMLStreamException {
        String data = reader.getPIData();
        if (data != null && data.length() > 0) {
            this.fStreamWriter.writeProcessingInstruction(reader.getPITarget(), data);
        } else {
            this.fStreamWriter.writeProcessingInstruction(reader.getPITarget());
        }
    }

    @Override
    public void entityReference(XMLStreamReader reader) throws XMLStreamException {
        this.fStreamWriter.writeEntityRef(reader.getLocalName());
    }

    @Override
    public void startDocument(StartDocument event) throws XMLStreamException {
        String version = event.getVersion();
        String encoding = event.getCharacterEncodingScheme();
        this.fStreamWriter.writeStartDocument(encoding != null ? encoding : "UTF-8", version != null ? version : "1.0");
    }

    @Override
    public void endDocument(EndDocument event) throws XMLStreamException {
        this.fStreamWriter.writeEndDocument();
        this.fStreamWriter.flush();
    }

    @Override
    public void doctypeDecl(DTD event) throws XMLStreamException {
        this.fStreamWriter.writeDTD(event.getDocumentTypeDeclaration());
    }

    @Override
    public void characters(Characters event) throws XMLStreamException {
        this.fStreamWriter.writeCharacters(event.getData());
    }

    @Override
    public void cdata(Characters event) throws XMLStreamException {
        this.fStreamWriter.writeCData(event.getData());
    }

    @Override
    public void comment(Comment event) throws XMLStreamException {
        this.fStreamWriter.writeComment(event.getText());
    }

    @Override
    public void processingInstruction(ProcessingInstruction event) throws XMLStreamException {
        String data = event.getData();
        if (data != null && data.length() > 0) {
            this.fStreamWriter.writeProcessingInstruction(event.getTarget(), data);
        } else {
            this.fStreamWriter.writeProcessingInstruction(event.getTarget());
        }
    }

    @Override
    public void entityReference(EntityReference event) throws XMLStreamException {
        this.fStreamWriter.writeEntityRef(event.getName());
    }

    @Override
    public void setIgnoringCharacters(boolean ignore) {
        this.fIgnoreChars = ignore;
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
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

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        try {
            if (element.prefix.length() > 0) {
                this.fStreamWriter.writeStartElement(element.prefix, element.localpart, element.uri != null ? element.uri : "");
            } else if (element.uri != null) {
                this.fStreamWriter.writeStartElement(element.uri, element.localpart);
            } else {
                this.fStreamWriter.writeStartElement(element.localpart);
            }
            int size = this.fNamespaceContext.getDeclaredPrefixCount();
            mf.javax.xml.namespace.NamespaceContext nc = this.fNamespaceContext.getNamespaceContext();
            for (int i = 0; i < size; i++) {
                String prefix = this.fNamespaceContext.getDeclaredPrefixAt(i);
                String uri = nc.getNamespaceURI(prefix);
                if (prefix.length() == 0) {
                    this.fStreamWriter.writeDefaultNamespace(uri != null ? uri : "");
                } else {
                    this.fStreamWriter.writeNamespace(prefix, uri != null ? uri : "");
                }
            }
            int size2 = attributes.getLength();
            for (int i2 = 0; i2 < size2; i2++) {
                attributes.getName(i2, this.fAttrName);
                if (this.fAttrName.prefix.length() > 0) {
                    this.fStreamWriter.writeAttribute(this.fAttrName.prefix, this.fAttrName.uri != null ? this.fAttrName.uri : "", this.fAttrName.localpart, attributes.getValue(i2));
                } else if (this.fAttrName.uri != null) {
                    this.fStreamWriter.writeAttribute(this.fAttrName.uri, this.fAttrName.localpart, attributes.getValue(i2));
                } else {
                    this.fStreamWriter.writeAttribute(this.fAttrName.localpart, attributes.getValue(i2));
                }
            }
        } catch (XMLStreamException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
        endElement(element, augs);
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
        if (!this.fIgnoreChars) {
            try {
                if (!this.fInCDATA) {
                    this.fStreamWriter.writeCharacters(text.ch, text.offset, text.length);
                } else {
                    this.fStreamWriter.writeCData(text.toString());
                }
            } catch (XMLStreamException e) {
                throw new XNIException(e);
            }
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        characters(text, augs);
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        try {
            this.fStreamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XNIException(e);
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        this.fInCDATA = true;
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        this.fInCDATA = false;
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return null;
    }
}
