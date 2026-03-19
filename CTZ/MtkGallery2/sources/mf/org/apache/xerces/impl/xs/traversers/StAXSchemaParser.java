package mf.org.apache.xerces.impl.xs.traversers;

import java.util.ArrayList;
import java.util.Iterator;
import mf.javax.xml.stream.XMLEventReader;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.XMLStreamReader;
import mf.javax.xml.stream.events.Attribute;
import mf.javax.xml.stream.events.EndElement;
import mf.javax.xml.stream.events.Namespace;
import mf.javax.xml.stream.events.ProcessingInstruction;
import mf.javax.xml.stream.events.StartElement;
import mf.javax.xml.stream.events.XMLEvent;
import mf.org.apache.xerces.impl.xs.opti.SchemaDOMParser;
import mf.org.apache.xerces.util.JAXPNamespaceContextWrapper;
import mf.org.apache.xerces.util.StAXLocationWrapper;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLAttributesImpl;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.w3c.dom.Document;

final class StAXSchemaParser {
    private static final int CHUNK_MASK = 1023;
    private static final int CHUNK_SIZE = 1024;
    private int fDepth;
    private SchemaDOMParser fSchemaDOMParser;
    private final char[] fCharBuffer = new char[1024];
    private final StAXLocationWrapper fLocationWrapper = new StAXLocationWrapper();
    private SymbolTable fSymbolTable;
    private final JAXPNamespaceContextWrapper fNamespaceContext = new JAXPNamespaceContextWrapper(this.fSymbolTable);
    private final QName fElementQName = new QName();
    private final QName fAttributeQName = new QName();
    private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
    private final XMLString fTempString = new XMLString();
    private final ArrayList fDeclaredPrefixes = new ArrayList();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    public StAXSchemaParser() {
        this.fNamespaceContext.setDeclaredPrefixes(this.fDeclaredPrefixes);
    }

    public void reset(SchemaDOMParser schemaDOMParser, SymbolTable symbolTable) {
        this.fSchemaDOMParser = schemaDOMParser;
        this.fSymbolTable = symbolTable;
        this.fNamespaceContext.setSymbolTable(this.fSymbolTable);
        this.fNamespaceContext.reset();
    }

    public Document getDocument() {
        return this.fSchemaDOMParser.getDocument();
    }

    public void parse(XMLEventReader input) throws XMLStreamException, XNIException {
        XMLEvent currentEvent = input.peek();
        if (currentEvent != null) {
            int eventType = currentEvent.getEventType();
            if (eventType != 7 && eventType != 1) {
                throw new XMLStreamException();
            }
            this.fLocationWrapper.setLocation(currentEvent.getLocation());
            this.fSchemaDOMParser.startDocument(this.fLocationWrapper, null, this.fNamespaceContext, null);
            while (input.hasNext()) {
                XMLEvent currentEvent2 = input.nextEvent();
                switch (currentEvent2.getEventType()) {
                    case 1:
                        this.fDepth++;
                        StartElement start = currentEvent2.asStartElement();
                        fillQName(this.fElementQName, start.getName());
                        this.fLocationWrapper.setLocation(start.getLocation());
                        this.fNamespaceContext.setNamespaceContext(start.getNamespaceContext());
                        fillXMLAttributes(start);
                        fillDeclaredPrefixes(start);
                        addNamespaceDeclarations();
                        this.fNamespaceContext.pushContext();
                        this.fSchemaDOMParser.startElement(this.fElementQName, this.fAttributes, null);
                        continue;
                    case 2:
                        EndElement end = currentEvent2.asEndElement();
                        fillQName(this.fElementQName, end.getName());
                        fillDeclaredPrefixes(end);
                        this.fLocationWrapper.setLocation(end.getLocation());
                        this.fSchemaDOMParser.endElement(this.fElementQName, null);
                        this.fNamespaceContext.popContext();
                        this.fDepth--;
                        if (this.fDepth > 0) {
                        }
                        break;
                    case 3:
                        ProcessingInstruction pi = (ProcessingInstruction) currentEvent2;
                        fillProcessingInstruction(pi.getData());
                        this.fSchemaDOMParser.processingInstruction(pi.getTarget(), this.fTempString, null);
                        continue;
                    case 4:
                        sendCharactersToSchemaParser(currentEvent2.asCharacters().getData(), false);
                        continue;
                    case 6:
                        sendCharactersToSchemaParser(currentEvent2.asCharacters().getData(), true);
                        continue;
                    case 7:
                        this.fDepth++;
                        continue;
                    case 12:
                        this.fSchemaDOMParser.startCDATA(null);
                        sendCharactersToSchemaParser(currentEvent2.asCharacters().getData(), false);
                        this.fSchemaDOMParser.endCDATA(null);
                        continue;
                }
                this.fLocationWrapper.setLocation(null);
                this.fNamespaceContext.setNamespaceContext(null);
                this.fSchemaDOMParser.endDocument(null);
            }
            this.fLocationWrapper.setLocation(null);
            this.fNamespaceContext.setNamespaceContext(null);
            this.fSchemaDOMParser.endDocument(null);
        }
    }

    public void parse(XMLStreamReader input) throws XMLStreamException, XNIException {
        if (input.hasNext()) {
            int eventType = input.getEventType();
            if (eventType != 7 && eventType != 1) {
                throw new XMLStreamException();
            }
            this.fLocationWrapper.setLocation(input.getLocation());
            this.fSchemaDOMParser.startDocument(this.fLocationWrapper, null, this.fNamespaceContext, null);
            boolean first = true;
            while (input.hasNext()) {
                if (!first) {
                    eventType = input.next();
                } else {
                    first = false;
                }
                switch (eventType) {
                    case 1:
                        this.fDepth++;
                        this.fLocationWrapper.setLocation(input.getLocation());
                        this.fNamespaceContext.setNamespaceContext(input.getNamespaceContext());
                        fillQName(this.fElementQName, input.getNamespaceURI(), input.getLocalName(), input.getPrefix());
                        fillXMLAttributes(input);
                        fillDeclaredPrefixes(input);
                        addNamespaceDeclarations();
                        this.fNamespaceContext.pushContext();
                        this.fSchemaDOMParser.startElement(this.fElementQName, this.fAttributes, null);
                        continue;
                    case 2:
                        this.fLocationWrapper.setLocation(input.getLocation());
                        this.fNamespaceContext.setNamespaceContext(input.getNamespaceContext());
                        fillQName(this.fElementQName, input.getNamespaceURI(), input.getLocalName(), input.getPrefix());
                        fillDeclaredPrefixes(input);
                        this.fSchemaDOMParser.endElement(this.fElementQName, null);
                        this.fNamespaceContext.popContext();
                        this.fDepth--;
                        if (this.fDepth > 0) {
                        }
                        break;
                    case 3:
                        fillProcessingInstruction(input.getPIData());
                        this.fSchemaDOMParser.processingInstruction(input.getPITarget(), this.fTempString, null);
                        continue;
                    case 4:
                        this.fTempString.setValues(input.getTextCharacters(), input.getTextStart(), input.getTextLength());
                        this.fSchemaDOMParser.characters(this.fTempString, null);
                        continue;
                    case 6:
                        this.fTempString.setValues(input.getTextCharacters(), input.getTextStart(), input.getTextLength());
                        this.fSchemaDOMParser.ignorableWhitespace(this.fTempString, null);
                        continue;
                    case 7:
                        this.fDepth++;
                        continue;
                    case 12:
                        this.fSchemaDOMParser.startCDATA(null);
                        this.fTempString.setValues(input.getTextCharacters(), input.getTextStart(), input.getTextLength());
                        this.fSchemaDOMParser.characters(this.fTempString, null);
                        this.fSchemaDOMParser.endCDATA(null);
                        continue;
                }
                this.fLocationWrapper.setLocation(null);
                this.fNamespaceContext.setNamespaceContext(null);
                this.fSchemaDOMParser.endDocument(null);
            }
            this.fLocationWrapper.setLocation(null);
            this.fNamespaceContext.setNamespaceContext(null);
            this.fSchemaDOMParser.endDocument(null);
        }
    }

    private void sendCharactersToSchemaParser(String str, boolean whitespace) {
        if (str != null) {
            int length = str.length();
            int remainder = length & CHUNK_MASK;
            if (remainder > 0) {
                str.getChars(0, remainder, this.fCharBuffer, 0);
                this.fTempString.setValues(this.fCharBuffer, 0, remainder);
                if (whitespace) {
                    this.fSchemaDOMParser.ignorableWhitespace(this.fTempString, null);
                } else {
                    this.fSchemaDOMParser.characters(this.fTempString, null);
                }
            }
            int i = remainder;
            while (i < length) {
                int i2 = i + 1024;
                str.getChars(i, i2, this.fCharBuffer, 0);
                this.fTempString.setValues(this.fCharBuffer, 0, 1024);
                if (whitespace) {
                    this.fSchemaDOMParser.ignorableWhitespace(this.fTempString, null);
                } else {
                    this.fSchemaDOMParser.characters(this.fTempString, null);
                }
                i = i2;
            }
        }
    }

    private void fillProcessingInstruction(String data) {
        int dataLength = data.length();
        char[] charBuffer = this.fCharBuffer;
        if (charBuffer.length < dataLength) {
            charBuffer = data.toCharArray();
        } else {
            data.getChars(0, dataLength, charBuffer, 0);
        }
        this.fTempString.setValues(charBuffer, 0, dataLength);
    }

    private void fillXMLAttributes(StartElement event) {
        this.fAttributes.removeAllAttributes();
        Iterator attrs = event.getAttributes();
        while (attrs.hasNext()) {
            Attribute attr = (Attribute) attrs.next();
            fillQName(this.fAttributeQName, attr.getName());
            String type = attr.getDTDType();
            int idx = this.fAttributes.getLength();
            this.fAttributes.addAttributeNS(this.fAttributeQName, type != null ? type : XMLSymbols.fCDATASymbol, attr.getValue());
            this.fAttributes.setSpecified(idx, attr.isSpecified());
        }
    }

    private void fillXMLAttributes(XMLStreamReader input) {
        this.fAttributes.removeAllAttributes();
        int len = input.getAttributeCount();
        for (int i = 0; i < len; i++) {
            fillQName(this.fAttributeQName, input.getAttributeNamespace(i), input.getAttributeLocalName(i), input.getAttributePrefix(i));
            String type = input.getAttributeType(i);
            this.fAttributes.addAttributeNS(this.fAttributeQName, type != null ? type : XMLSymbols.fCDATASymbol, input.getAttributeValue(i));
            this.fAttributes.setSpecified(i, input.isAttributeSpecified(i));
        }
    }

    private void addNamespaceDeclarations() {
        String prefix;
        String localpart;
        String rawname;
        String str;
        for (String nsPrefix : this.fDeclaredPrefixes) {
            String nsURI = this.fNamespaceContext.getURI(nsPrefix);
            if (nsPrefix.length() > 0) {
                prefix = XMLSymbols.PREFIX_XMLNS;
                localpart = nsPrefix;
                this.fStringBuffer.clear();
                this.fStringBuffer.append(prefix);
                this.fStringBuffer.append(':');
                this.fStringBuffer.append(localpart);
                rawname = this.fSymbolTable.addSymbol(this.fStringBuffer.ch, this.fStringBuffer.offset, this.fStringBuffer.length);
            } else {
                prefix = XMLSymbols.EMPTY_STRING;
                localpart = XMLSymbols.PREFIX_XMLNS;
                rawname = XMLSymbols.PREFIX_XMLNS;
            }
            this.fAttributeQName.setValues(prefix, localpart, rawname, NamespaceContext.XMLNS_URI);
            XMLAttributesImpl xMLAttributesImpl = this.fAttributes;
            QName qName = this.fAttributeQName;
            String str2 = XMLSymbols.fCDATASymbol;
            if (nsURI != null) {
                str = nsURI;
            } else {
                str = XMLSymbols.EMPTY_STRING;
            }
            xMLAttributesImpl.addAttribute(qName, str2, str);
        }
    }

    private void fillDeclaredPrefixes(StartElement event) {
        fillDeclaredPrefixes(event.getNamespaces());
    }

    private void fillDeclaredPrefixes(EndElement event) {
        fillDeclaredPrefixes(event.getNamespaces());
    }

    private void fillDeclaredPrefixes(Iterator namespaces) {
        this.fDeclaredPrefixes.clear();
        while (namespaces.hasNext()) {
            Namespace ns = (Namespace) namespaces.next();
            String prefix = ns.getPrefix();
            this.fDeclaredPrefixes.add(prefix != null ? prefix : "");
        }
    }

    private void fillDeclaredPrefixes(XMLStreamReader reader) {
        this.fDeclaredPrefixes.clear();
        int len = reader.getNamespaceCount();
        for (int i = 0; i < len; i++) {
            String prefix = reader.getNamespacePrefix(i);
            this.fDeclaredPrefixes.add(prefix != null ? prefix : "");
        }
    }

    private void fillQName(QName toFill, mf.javax.xml.namespace.QName toCopy) {
        fillQName(toFill, toCopy.getNamespaceURI(), toCopy.getLocalPart(), toCopy.getPrefix());
    }

    final void fillQName(QName toFill, String uri, String localpart, String prefix) {
        String uri2 = (uri == null || uri.length() <= 0) ? null : this.fSymbolTable.addSymbol(uri);
        String localpart2 = localpart != null ? this.fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
        String prefix2 = (prefix == null || prefix.length() <= 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix);
        String raw = localpart2;
        if (prefix2 != XMLSymbols.EMPTY_STRING) {
            this.fStringBuffer.clear();
            this.fStringBuffer.append(prefix2);
            this.fStringBuffer.append(':');
            this.fStringBuffer.append(localpart2);
            raw = this.fSymbolTable.addSymbol(this.fStringBuffer.ch, this.fStringBuffer.offset, this.fStringBuffer.length);
        }
        toFill.setValues(prefix2, localpart2, raw, uri2);
    }
}
