package mf.org.apache.xerces.stax;

import java.util.Iterator;
import mf.javax.xml.namespace.NamespaceContext;
import mf.javax.xml.namespace.QName;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLEventFactory;
import mf.javax.xml.stream.events.Attribute;
import mf.javax.xml.stream.events.Characters;
import mf.javax.xml.stream.events.Comment;
import mf.javax.xml.stream.events.DTD;
import mf.javax.xml.stream.events.EndDocument;
import mf.javax.xml.stream.events.EndElement;
import mf.javax.xml.stream.events.EntityDeclaration;
import mf.javax.xml.stream.events.EntityReference;
import mf.javax.xml.stream.events.Namespace;
import mf.javax.xml.stream.events.ProcessingInstruction;
import mf.javax.xml.stream.events.StartDocument;
import mf.javax.xml.stream.events.StartElement;
import mf.org.apache.xerces.stax.events.AttributeImpl;
import mf.org.apache.xerces.stax.events.CharactersImpl;
import mf.org.apache.xerces.stax.events.CommentImpl;
import mf.org.apache.xerces.stax.events.DTDImpl;
import mf.org.apache.xerces.stax.events.EndDocumentImpl;
import mf.org.apache.xerces.stax.events.EndElementImpl;
import mf.org.apache.xerces.stax.events.EntityReferenceImpl;
import mf.org.apache.xerces.stax.events.NamespaceImpl;
import mf.org.apache.xerces.stax.events.ProcessingInstructionImpl;
import mf.org.apache.xerces.stax.events.StartDocumentImpl;
import mf.org.apache.xerces.stax.events.StartElementImpl;

public final class XMLEventFactoryImpl extends XMLEventFactory {
    private Location fLocation;

    @Override
    public void setLocation(Location location) {
        this.fLocation = location;
    }

    @Override
    public Attribute createAttribute(String prefix, String namespaceURI, String localName, String value) {
        return createAttribute(new QName(namespaceURI, localName, prefix), value);
    }

    @Override
    public Attribute createAttribute(String localName, String value) {
        return createAttribute(new QName(localName), value);
    }

    @Override
    public Attribute createAttribute(QName name, String value) {
        return new AttributeImpl(name, value, "CDATA", true, this.fLocation);
    }

    @Override
    public Namespace createNamespace(String namespaceURI) {
        return createNamespace("", namespaceURI);
    }

    @Override
    public Namespace createNamespace(String prefix, String namespaceUri) {
        return new NamespaceImpl(prefix, namespaceUri, this.fLocation);
    }

    @Override
    public StartElement createStartElement(QName name, Iterator attributes, Iterator namespaces) {
        return createStartElement(name, attributes, namespaces, null);
    }

    @Override
    public StartElement createStartElement(String prefix, String namespaceUri, String localName) {
        return createStartElement(new QName(namespaceUri, localName, prefix), (Iterator) null, (Iterator) null);
    }

    @Override
    public StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes, Iterator namespaces) {
        return createStartElement(new QName(namespaceUri, localName, prefix), attributes, namespaces);
    }

    @Override
    public StartElement createStartElement(String prefix, String namespaceUri, String localName, Iterator attributes, Iterator namespaces, NamespaceContext context) {
        return createStartElement(new QName(namespaceUri, localName, prefix), attributes, namespaces, context);
    }

    private StartElement createStartElement(QName name, Iterator attributes, Iterator namespaces, NamespaceContext context) {
        return new StartElementImpl(name, attributes, namespaces, context, this.fLocation);
    }

    @Override
    public EndElement createEndElement(QName name, Iterator namespaces) {
        return new EndElementImpl(name, namespaces, this.fLocation);
    }

    @Override
    public EndElement createEndElement(String prefix, String namespaceUri, String localName) {
        return createEndElement(new QName(namespaceUri, localName, prefix), null);
    }

    @Override
    public EndElement createEndElement(String prefix, String namespaceUri, String localName, Iterator namespaces) {
        return createEndElement(new QName(namespaceUri, localName, prefix), namespaces);
    }

    @Override
    public Characters createCharacters(String content) {
        return new CharactersImpl(content, 4, this.fLocation);
    }

    @Override
    public Characters createCData(String content) {
        return new CharactersImpl(content, 12, this.fLocation);
    }

    @Override
    public Characters createSpace(String content) {
        return createCharacters(content);
    }

    @Override
    public Characters createIgnorableSpace(String content) {
        return new CharactersImpl(content, 6, this.fLocation);
    }

    @Override
    public StartDocument createStartDocument() {
        return createStartDocument(null, null);
    }

    @Override
    public StartDocument createStartDocument(String encoding, String version, boolean standalone) {
        return new StartDocumentImpl(encoding, encoding != null, standalone, true, version, this.fLocation);
    }

    @Override
    public StartDocument createStartDocument(String encoding, String version) {
        return new StartDocumentImpl(encoding, encoding != null, false, false, version, this.fLocation);
    }

    @Override
    public StartDocument createStartDocument(String encoding) {
        return createStartDocument(encoding, null);
    }

    @Override
    public EndDocument createEndDocument() {
        return new EndDocumentImpl(this.fLocation);
    }

    @Override
    public EntityReference createEntityReference(String name, EntityDeclaration declaration) {
        return new EntityReferenceImpl(name, declaration, this.fLocation);
    }

    @Override
    public Comment createComment(String text) {
        return new CommentImpl(text, this.fLocation);
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data) {
        return new ProcessingInstructionImpl(target, data, this.fLocation);
    }

    @Override
    public DTD createDTD(String dtd) {
        return new DTDImpl(dtd, this.fLocation);
    }
}
