package mf.org.apache.xerces.jaxp.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLEventReader;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.XMLStreamReader;
import mf.javax.xml.stream.XMLStreamWriter;
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
import mf.javax.xml.stream.events.XMLEvent;
import mf.javax.xml.transform.Result;
import mf.javax.xml.transform.Source;
import mf.javax.xml.transform.stax.StAXResult;
import mf.javax.xml.transform.stax.StAXSource;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.validation.EntityState;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.impl.xs.XMLSchemaValidator;
import mf.org.apache.xerces.util.JAXPNamespaceContextWrapper;
import mf.org.apache.xerces.util.StAXLocationWrapper;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLAttributesImpl;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import org.xml.sax.SAXException;

final class StAXValidatorHelper implements EntityState, ValidatorHelper {
    private static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    private static final String SCHEMA_VALIDATOR = "http://apache.org/xml/properties/internal/validator/schema";
    private static final String STRING_INTERNING = "javax.xml.stream.isInterning";
    private static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    private static final String VALIDATION_MANAGER = "http://apache.org/xml/properties/internal/validation-manager";
    private final XMLSchemaValidatorComponentManager fComponentManager;
    private final XMLErrorReporter fErrorReporter;
    private EventHelper fEventHelper;
    private final JAXPNamespaceContextWrapper fNamespaceContext;
    private final XMLSchemaValidator fSchemaValidator;
    private StAXEventResultBuilder fStAXEventResultBuilder;
    private StAXStreamResultBuilder fStAXStreamResultBuilder;
    private StAXDocumentHandler fStAXValidatorHandler;
    private StreamHelper fStreamHelper;
    private final SymbolTable fSymbolTable;
    private final ValidationManager fValidationManager;
    private final StAXLocationWrapper fStAXLocationWrapper = new StAXLocationWrapper();
    private final XMLStreamReaderLocation fXMLStreamReaderLocation = new XMLStreamReaderLocation();
    private HashMap fEntities = null;
    private boolean fStringsInternalized = false;
    private int fDepth = 0;
    private XMLEvent fCurrentEvent = null;
    final QName fElementQName = new QName();
    final QName fAttributeQName = new QName();
    final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
    final ArrayList fDeclaredPrefixes = new ArrayList();
    final XMLString fTempString = new XMLString();
    final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    public StAXValidatorHelper(XMLSchemaValidatorComponentManager componentManager) {
        this.fComponentManager = componentManager;
        this.fErrorReporter = (XMLErrorReporter) this.fComponentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        this.fSchemaValidator = (XMLSchemaValidator) this.fComponentManager.getProperty(SCHEMA_VALIDATOR);
        this.fSymbolTable = (SymbolTable) this.fComponentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        this.fValidationManager = (ValidationManager) this.fComponentManager.getProperty(VALIDATION_MANAGER);
        this.fNamespaceContext = new JAXPNamespaceContextWrapper(this.fSymbolTable);
        this.fNamespaceContext.setDeclaredPrefixes(this.fDeclaredPrefixes);
    }

    @Override
    public void validate(Source source, Result result) throws SAXException, IOException {
        StAXDocumentHandler stAXDocumentHandler;
        if ((result instanceof StAXResult) || result == null) {
            StAXSource stAXSource = (StAXSource) source;
            StAXResult stAXResult = (StAXResult) result;
            try {
                try {
                    try {
                        XMLStreamReader xMLStreamReader = stAXSource.getXMLStreamReader();
                        if (xMLStreamReader != null) {
                            if (this.fStreamHelper == null) {
                                this.fStreamHelper = new StreamHelper();
                            }
                            this.fStreamHelper.validate(xMLStreamReader, stAXResult);
                        } else {
                            if (this.fEventHelper == null) {
                                this.fEventHelper = new EventHelper();
                            }
                            this.fEventHelper.validate(stAXSource.getXMLEventReader(), stAXResult);
                        }
                        if (stAXDocumentHandler != null) {
                            return;
                        } else {
                            return;
                        }
                    } catch (XMLParseException e) {
                        throw Util.toSAXParseException(e);
                    }
                } catch (XMLStreamException e2) {
                    throw new SAXException(e2);
                } catch (XNIException e3) {
                    throw Util.toSAXException(e3);
                }
            } finally {
                this.fCurrentEvent = null;
                this.fStAXLocationWrapper.setLocation(null);
                this.fXMLStreamReaderLocation.setXMLStreamReader(null);
                if (this.fStAXValidatorHandler != null) {
                    this.fStAXValidatorHandler.setStAXResult(null);
                }
            }
        }
        throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "SourceResultMismatch", new Object[]{source.getClass().getName(), result.getClass().getName()}));
    }

    @Override
    public boolean isEntityDeclared(String name) {
        if (this.fEntities != null) {
            return this.fEntities.containsKey(name);
        }
        return false;
    }

    @Override
    public boolean isEntityUnparsed(String name) {
        EntityDeclaration entityDecl;
        return (this.fEntities == null || (entityDecl = (EntityDeclaration) this.fEntities.get(name)) == null || entityDecl.getNotationName() == null) ? false : true;
    }

    final EntityDeclaration getEntityDeclaration(String name) {
        if (this.fEntities != null) {
            return (EntityDeclaration) this.fEntities.get(name);
        }
        return null;
    }

    final XMLEvent getCurrentEvent() {
        return this.fCurrentEvent;
    }

    final void fillQName(QName toFill, String uri, String localpart, String prefix) {
        if (!this.fStringsInternalized) {
            uri = (uri == null || uri.length() <= 0) ? null : this.fSymbolTable.addSymbol(uri);
            localpart = localpart != null ? this.fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
            prefix = (prefix == null || prefix.length() <= 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix);
        } else {
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
            if (localpart == null) {
                localpart = XMLSymbols.EMPTY_STRING;
            }
            if (prefix == null) {
                prefix = XMLSymbols.EMPTY_STRING;
            }
        }
        String raw = localpart;
        if (prefix != XMLSymbols.EMPTY_STRING) {
            this.fStringBuffer.clear();
            this.fStringBuffer.append(prefix);
            this.fStringBuffer.append(':');
            this.fStringBuffer.append(localpart);
            raw = this.fSymbolTable.addSymbol(this.fStringBuffer.ch, this.fStringBuffer.offset, this.fStringBuffer.length);
        }
        toFill.setValues(prefix, localpart, raw, uri);
    }

    final void setup(Location location, StAXResult result, boolean stringsInternalized) {
        this.fDepth = 0;
        this.fComponentManager.reset();
        setupStAXResultHandler(result);
        this.fValidationManager.setEntityState(this);
        if (this.fEntities != null && !this.fEntities.isEmpty()) {
            this.fEntities.clear();
        }
        this.fStAXLocationWrapper.setLocation(location);
        this.fErrorReporter.setDocumentLocator(this.fStAXLocationWrapper);
        this.fStringsInternalized = stringsInternalized;
    }

    final void processEntityDeclarations(List entityDecls) {
        int size = entityDecls != null ? entityDecls.size() : 0;
        if (size > 0) {
            if (this.fEntities == null) {
                this.fEntities = new HashMap();
            }
            for (int i = 0; i < size; i++) {
                EntityDeclaration decl = (EntityDeclaration) entityDecls.get(i);
                this.fEntities.put(decl.getName(), decl);
            }
        }
    }

    private void setupStAXResultHandler(StAXResult result) {
        if (result == null) {
            this.fStAXValidatorHandler = null;
            this.fSchemaValidator.setDocumentHandler(null);
            return;
        }
        XMLStreamWriter writer = result.getXMLStreamWriter();
        if (writer != null) {
            if (this.fStAXStreamResultBuilder == null) {
                this.fStAXStreamResultBuilder = new StAXStreamResultBuilder(this.fNamespaceContext);
            }
            this.fStAXValidatorHandler = this.fStAXStreamResultBuilder;
            this.fStAXStreamResultBuilder.setStAXResult(result);
        } else {
            if (this.fStAXEventResultBuilder == null) {
                this.fStAXEventResultBuilder = new StAXEventResultBuilder(this, this.fNamespaceContext);
            }
            this.fStAXValidatorHandler = this.fStAXEventResultBuilder;
            this.fStAXEventResultBuilder.setStAXResult(result);
        }
        this.fSchemaValidator.setDocumentHandler(this.fStAXValidatorHandler);
    }

    final class StreamHelper {
        StreamHelper() {
        }

        final void validate(XMLStreamReader reader, StAXResult result) throws XMLStreamException, InvalidDatatypeValueException, SAXException {
            if (reader.hasNext()) {
                int eventType = reader.getEventType();
                if (eventType == 7 || eventType == 1) {
                    StAXValidatorHelper.this.fXMLStreamReaderLocation.setXMLStreamReader(reader);
                    StAXValidatorHelper.this.setup(StAXValidatorHelper.this.fXMLStreamReaderLocation, result, Boolean.TRUE.equals(reader.getProperty(StAXValidatorHelper.STRING_INTERNING)));
                    StAXValidatorHelper.this.fSchemaValidator.startDocument(StAXValidatorHelper.this.fStAXLocationWrapper, null, StAXValidatorHelper.this.fNamespaceContext, null);
                    do {
                        switch (eventType) {
                            case 1:
                                StAXValidatorHelper.this.fDepth++;
                                StAXValidatorHelper.this.fillQName(StAXValidatorHelper.this.fElementQName, reader.getNamespaceURI(), reader.getLocalName(), reader.getPrefix());
                                fillXMLAttributes(reader);
                                fillDeclaredPrefixes(reader);
                                StAXValidatorHelper.this.fNamespaceContext.setNamespaceContext(reader.getNamespaceContext());
                                StAXValidatorHelper.this.fSchemaValidator.startElement(StAXValidatorHelper.this.fElementQName, StAXValidatorHelper.this.fAttributes, null);
                                break;
                            case 2:
                                StAXValidatorHelper.this.fillQName(StAXValidatorHelper.this.fElementQName, reader.getNamespaceURI(), reader.getLocalName(), reader.getPrefix());
                                fillDeclaredPrefixes(reader);
                                StAXValidatorHelper.this.fNamespaceContext.setNamespaceContext(reader.getNamespaceContext());
                                StAXValidatorHelper.this.fSchemaValidator.endElement(StAXValidatorHelper.this.fElementQName, null);
                                StAXValidatorHelper.this.fDepth--;
                                break;
                            case 3:
                                if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                    StAXValidatorHelper.this.fStAXValidatorHandler.processingInstruction(reader);
                                }
                                break;
                            case 4:
                            case 6:
                                StAXValidatorHelper.this.fTempString.setValues(reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());
                                StAXValidatorHelper.this.fSchemaValidator.characters(StAXValidatorHelper.this.fTempString, null);
                                break;
                            case 5:
                                if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                    StAXValidatorHelper.this.fStAXValidatorHandler.comment(reader);
                                }
                                break;
                            case 7:
                                StAXValidatorHelper.this.fDepth++;
                                if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                    StAXValidatorHelper.this.fStAXValidatorHandler.startDocument(reader);
                                }
                                break;
                            case 9:
                                if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                    StAXValidatorHelper.this.fStAXValidatorHandler.entityReference(reader);
                                }
                                break;
                            case 11:
                                StAXValidatorHelper.this.processEntityDeclarations((List) reader.getProperty("javax.xml.stream.entities"));
                                break;
                            case 12:
                                StAXValidatorHelper.this.fSchemaValidator.startCDATA(null);
                                StAXValidatorHelper.this.fTempString.setValues(reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());
                                StAXValidatorHelper.this.fSchemaValidator.characters(StAXValidatorHelper.this.fTempString, null);
                                StAXValidatorHelper.this.fSchemaValidator.endCDATA(null);
                                break;
                        }
                        eventType = reader.next();
                        if (reader.hasNext()) {
                        }
                        StAXValidatorHelper.this.fSchemaValidator.endDocument(null);
                        if (eventType != 8 && StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                            StAXValidatorHelper.this.fStAXValidatorHandler.endDocument(reader);
                            return;
                        }
                        return;
                    } while (StAXValidatorHelper.this.fDepth > 0);
                    StAXValidatorHelper.this.fSchemaValidator.endDocument(null);
                    if (eventType != 8) {
                        return;
                    } else {
                        return;
                    }
                }
                throw new SAXException(JAXPValidationMessageFormatter.formatMessage(StAXValidatorHelper.this.fComponentManager.getLocale(), "StAXIllegalInitialState", null));
            }
        }

        private void fillXMLAttributes(XMLStreamReader reader) {
            StAXValidatorHelper.this.fAttributes.removeAllAttributes();
            int len = reader.getAttributeCount();
            for (int i = 0; i < len; i++) {
                StAXValidatorHelper.this.fillQName(StAXValidatorHelper.this.fAttributeQName, reader.getAttributeNamespace(i), reader.getAttributeLocalName(i), reader.getAttributePrefix(i));
                String type = reader.getAttributeType(i);
                StAXValidatorHelper.this.fAttributes.addAttributeNS(StAXValidatorHelper.this.fAttributeQName, type != null ? type : XMLSymbols.fCDATASymbol, reader.getAttributeValue(i));
                StAXValidatorHelper.this.fAttributes.setSpecified(i, reader.isAttributeSpecified(i));
            }
        }

        private void fillDeclaredPrefixes(XMLStreamReader reader) {
            StAXValidatorHelper.this.fDeclaredPrefixes.clear();
            int len = reader.getNamespaceCount();
            for (int i = 0; i < len; i++) {
                String prefix = reader.getNamespacePrefix(i);
                StAXValidatorHelper.this.fDeclaredPrefixes.add(prefix != null ? prefix : "");
            }
        }
    }

    final class EventHelper {
        private static final int CHUNK_MASK = 1023;
        private static final int CHUNK_SIZE = 1024;
        private final char[] fCharBuffer = new char[1024];

        EventHelper() {
        }

        final void validate(XMLEventReader reader, StAXResult result) throws XMLStreamException, InvalidDatatypeValueException, SAXException {
            StAXValidatorHelper.this.fCurrentEvent = reader.peek();
            if (StAXValidatorHelper.this.fCurrentEvent != null) {
                int eventType = StAXValidatorHelper.this.fCurrentEvent.getEventType();
                if (eventType != 7 && eventType != 1) {
                    throw new SAXException(JAXPValidationMessageFormatter.formatMessage(StAXValidatorHelper.this.fComponentManager.getLocale(), "StAXIllegalInitialState", null));
                }
                StAXValidatorHelper.this.setup(null, result, false);
                StAXValidatorHelper.this.fSchemaValidator.startDocument(StAXValidatorHelper.this.fStAXLocationWrapper, null, StAXValidatorHelper.this.fNamespaceContext, null);
                while (reader.hasNext()) {
                    StAXValidatorHelper.this.fCurrentEvent = reader.nextEvent();
                    switch (StAXValidatorHelper.this.fCurrentEvent.getEventType()) {
                        case 1:
                            StAXValidatorHelper.this.fDepth++;
                            StartElement start = StAXValidatorHelper.this.fCurrentEvent.asStartElement();
                            fillQName(StAXValidatorHelper.this.fElementQName, start.getName());
                            fillXMLAttributes(start);
                            fillDeclaredPrefixes(start);
                            StAXValidatorHelper.this.fNamespaceContext.setNamespaceContext(start.getNamespaceContext());
                            StAXValidatorHelper.this.fStAXLocationWrapper.setLocation(start.getLocation());
                            StAXValidatorHelper.this.fSchemaValidator.startElement(StAXValidatorHelper.this.fElementQName, StAXValidatorHelper.this.fAttributes, null);
                            continue;
                        case 2:
                            EndElement end = StAXValidatorHelper.this.fCurrentEvent.asEndElement();
                            fillQName(StAXValidatorHelper.this.fElementQName, end.getName());
                            fillDeclaredPrefixes(end);
                            StAXValidatorHelper.this.fStAXLocationWrapper.setLocation(end.getLocation());
                            StAXValidatorHelper.this.fSchemaValidator.endElement(StAXValidatorHelper.this.fElementQName, null);
                            StAXValidatorHelper stAXValidatorHelper = StAXValidatorHelper.this;
                            int i = stAXValidatorHelper.fDepth - 1;
                            stAXValidatorHelper.fDepth = i;
                            if (i > 0) {
                            }
                            break;
                        case 3:
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                StAXValidatorHelper.this.fStAXValidatorHandler.processingInstruction((ProcessingInstruction) StAXValidatorHelper.this.fCurrentEvent);
                            } else {
                                continue;
                            }
                            break;
                        case 4:
                        case 6:
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                Characters chars = StAXValidatorHelper.this.fCurrentEvent.asCharacters();
                                StAXValidatorHelper.this.fStAXValidatorHandler.setIgnoringCharacters(true);
                                sendCharactersToValidator(chars.getData());
                                StAXValidatorHelper.this.fStAXValidatorHandler.setIgnoringCharacters(false);
                                StAXValidatorHelper.this.fStAXValidatorHandler.characters(chars);
                            } else {
                                sendCharactersToValidator(StAXValidatorHelper.this.fCurrentEvent.asCharacters().getData());
                                continue;
                            }
                            break;
                        case 5:
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                StAXValidatorHelper.this.fStAXValidatorHandler.comment((Comment) StAXValidatorHelper.this.fCurrentEvent);
                            } else {
                                continue;
                            }
                            break;
                        case 7:
                            StAXValidatorHelper.this.fDepth++;
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                StAXValidatorHelper.this.fStAXValidatorHandler.startDocument((StartDocument) StAXValidatorHelper.this.fCurrentEvent);
                            } else {
                                continue;
                            }
                            break;
                        case 8:
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                StAXValidatorHelper.this.fStAXValidatorHandler.endDocument((EndDocument) StAXValidatorHelper.this.fCurrentEvent);
                            } else {
                                continue;
                            }
                            break;
                        case 9:
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                StAXValidatorHelper.this.fStAXValidatorHandler.entityReference((EntityReference) StAXValidatorHelper.this.fCurrentEvent);
                            } else {
                                continue;
                            }
                            break;
                        case 11:
                            DTD dtd = (DTD) StAXValidatorHelper.this.fCurrentEvent;
                            StAXValidatorHelper.this.processEntityDeclarations(dtd.getEntities());
                            if (StAXValidatorHelper.this.fStAXValidatorHandler != null) {
                                StAXValidatorHelper.this.fStAXValidatorHandler.doctypeDecl(dtd);
                            } else {
                                continue;
                            }
                            break;
                        case 12:
                            if (StAXValidatorHelper.this.fStAXValidatorHandler == null) {
                                StAXValidatorHelper.this.fSchemaValidator.startCDATA(null);
                                sendCharactersToValidator(StAXValidatorHelper.this.fCurrentEvent.asCharacters().getData());
                                StAXValidatorHelper.this.fSchemaValidator.endCDATA(null);
                                continue;
                            } else {
                                Characters chars2 = StAXValidatorHelper.this.fCurrentEvent.asCharacters();
                                StAXValidatorHelper.this.fStAXValidatorHandler.setIgnoringCharacters(true);
                                StAXValidatorHelper.this.fSchemaValidator.startCDATA(null);
                                sendCharactersToValidator(StAXValidatorHelper.this.fCurrentEvent.asCharacters().getData());
                                StAXValidatorHelper.this.fSchemaValidator.endCDATA(null);
                                StAXValidatorHelper.this.fStAXValidatorHandler.setIgnoringCharacters(false);
                                StAXValidatorHelper.this.fStAXValidatorHandler.cdata(chars2);
                            }
                            break;
                    }
                    StAXValidatorHelper.this.fSchemaValidator.endDocument(null);
                }
                StAXValidatorHelper.this.fSchemaValidator.endDocument(null);
            }
        }

        private void fillQName(QName toFill, mf.javax.xml.namespace.QName toCopy) {
            StAXValidatorHelper.this.fillQName(toFill, toCopy.getNamespaceURI(), toCopy.getLocalPart(), toCopy.getPrefix());
        }

        private void fillXMLAttributes(StartElement event) {
            StAXValidatorHelper.this.fAttributes.removeAllAttributes();
            Iterator attrs = event.getAttributes();
            while (attrs.hasNext()) {
                Attribute attr = (Attribute) attrs.next();
                fillQName(StAXValidatorHelper.this.fAttributeQName, attr.getName());
                String type = attr.getDTDType();
                int idx = StAXValidatorHelper.this.fAttributes.getLength();
                StAXValidatorHelper.this.fAttributes.addAttributeNS(StAXValidatorHelper.this.fAttributeQName, type != null ? type : XMLSymbols.fCDATASymbol, attr.getValue());
                StAXValidatorHelper.this.fAttributes.setSpecified(idx, attr.isSpecified());
            }
        }

        private void fillDeclaredPrefixes(StartElement event) {
            fillDeclaredPrefixes(event.getNamespaces());
        }

        private void fillDeclaredPrefixes(EndElement event) {
            fillDeclaredPrefixes(event.getNamespaces());
        }

        private void fillDeclaredPrefixes(Iterator namespaces) {
            StAXValidatorHelper.this.fDeclaredPrefixes.clear();
            while (namespaces.hasNext()) {
                Namespace ns = (Namespace) namespaces.next();
                String prefix = ns.getPrefix();
                StAXValidatorHelper.this.fDeclaredPrefixes.add(prefix != null ? prefix : "");
            }
        }

        private void sendCharactersToValidator(String str) {
            if (str != null) {
                int length = str.length();
                int remainder = length & CHUNK_MASK;
                if (remainder > 0) {
                    str.getChars(0, remainder, this.fCharBuffer, 0);
                    StAXValidatorHelper.this.fTempString.setValues(this.fCharBuffer, 0, remainder);
                    StAXValidatorHelper.this.fSchemaValidator.characters(StAXValidatorHelper.this.fTempString, null);
                }
                int i = remainder;
                while (i < length) {
                    int i2 = i + 1024;
                    str.getChars(i, i2, this.fCharBuffer, 0);
                    StAXValidatorHelper.this.fTempString.setValues(this.fCharBuffer, 0, 1024);
                    StAXValidatorHelper.this.fSchemaValidator.characters(StAXValidatorHelper.this.fTempString, null);
                    i = i2;
                }
            }
        }
    }

    static final class XMLStreamReaderLocation implements Location {
        private XMLStreamReader reader;

        @Override
        public int getCharacterOffset() {
            Location loc = getLocation();
            if (loc != null) {
                return loc.getCharacterOffset();
            }
            return -1;
        }

        @Override
        public int getColumnNumber() {
            Location loc = getLocation();
            if (loc != null) {
                return loc.getColumnNumber();
            }
            return -1;
        }

        @Override
        public int getLineNumber() {
            Location loc = getLocation();
            if (loc != null) {
                return loc.getLineNumber();
            }
            return -1;
        }

        @Override
        public String getPublicId() {
            Location loc = getLocation();
            if (loc != null) {
                return loc.getPublicId();
            }
            return null;
        }

        @Override
        public String getSystemId() {
            Location loc = getLocation();
            if (loc != null) {
                return loc.getSystemId();
            }
            return null;
        }

        public void setXMLStreamReader(XMLStreamReader reader) {
            this.reader = reader;
        }

        private Location getLocation() {
            if (this.reader != null) {
                return this.reader.getLocation();
            }
            return null;
        }
    }
}
