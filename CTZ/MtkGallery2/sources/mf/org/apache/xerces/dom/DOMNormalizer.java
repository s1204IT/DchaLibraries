package mf.org.apache.xerces.dom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.RevalidationHandler;
import mf.org.apache.xerces.impl.dtd.XMLDTDLoader;
import mf.org.apache.xerces.impl.dtd.XMLDTDValidator;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.util.SimpleLocator;
import mf.org.apache.xerces.jaxp.JAXPConstants;
import mf.org.apache.xerces.util.AugmentationsImpl;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Comment;
import mf.org.w3c.dom.DOMErrorHandler;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Entity;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.Text;

public class DOMNormalizer implements XMLDocumentHandler {
    protected static final boolean DEBUG = false;
    protected static final boolean DEBUG_EVENTS = false;
    protected static final boolean DEBUG_ND = false;
    protected static final String PREFIX = "NS";
    protected DOMErrorHandler fErrorHandler;
    protected SymbolTable fSymbolTable;
    protected RevalidationHandler fValidationHandler;
    public static final RuntimeException abort = new RuntimeException();
    public static final XMLString EMPTY_STRING = new XMLString();
    protected DOMConfigurationImpl fConfiguration = null;
    protected CoreDocumentImpl fDocument = null;
    protected final XMLAttributesProxy fAttrProxy = new XMLAttributesProxy();
    protected final QName fQName = new QName();
    private final DOMErrorImpl fError = new DOMErrorImpl();
    protected boolean fNamespaceValidation = false;
    protected boolean fPSVI = false;
    protected final NamespaceContext fNamespaceContext = new NamespaceSupport();
    protected final NamespaceContext fLocalNSBinder = new NamespaceSupport();
    protected final ArrayList fAttributeList = new ArrayList(5);
    protected final DOMLocatorImpl fLocator = new DOMLocatorImpl();
    protected Node fCurrentNode = null;
    private final QName fAttrQName = new QName();
    final XMLString fNormalizedValue = new XMLString(new char[16], 0, 0);
    private boolean fAllWhitespace = false;

    protected void normalizeDocument(CoreDocumentImpl document, DOMConfigurationImpl config) {
        String str;
        boolean z;
        this.fDocument = document;
        this.fConfiguration = config;
        this.fAllWhitespace = false;
        this.fNamespaceValidation = false;
        String xmlVersion = this.fDocument.getXmlVersion();
        String schemaType = null;
        String[] schemaLocations = null;
        this.fSymbolTable = (SymbolTable) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        this.fNamespaceContext.reset();
        this.fNamespaceContext.declarePrefix(XMLSymbols.EMPTY_STRING, null);
        if ((this.fConfiguration.features & 64) != 0) {
            String schemaLang = (String) this.fConfiguration.getProperty(JAXPConstants.JAXP_SCHEMA_LANGUAGE);
            if (schemaLang != null && schemaLang.equals(Constants.NS_XMLSCHEMA)) {
                schemaType = "http://www.w3.org/2001/XMLSchema";
                this.fValidationHandler = CoreDOMImplementationImpl.singleton.getValidator("http://www.w3.org/2001/XMLSchema", xmlVersion);
                this.fConfiguration.setFeature("http://apache.org/xml/features/validation/schema", true);
                this.fConfiguration.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
                this.fNamespaceValidation = true;
                if ((this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MINEXCLUSIVE) == 0) {
                    z = false;
                } else {
                    z = true;
                }
                this.fPSVI = z;
            } else {
                schemaType = XMLGrammarDescription.XML_DTD;
                if (schemaLang != null) {
                    schemaLocations = (String[]) this.fConfiguration.getProperty(JAXPConstants.JAXP_SCHEMA_SOURCE);
                }
                this.fConfiguration.setDTDValidatorFactory(xmlVersion);
                this.fValidationHandler = CoreDOMImplementationImpl.singleton.getValidator(XMLGrammarDescription.XML_DTD, xmlVersion);
                this.fPSVI = false;
            }
            this.fConfiguration.setFeature("http://xml.org/sax/features/validation", true);
            this.fDocument.clearIdentifiers();
            if (this.fValidationHandler != null) {
                ((XMLComponent) this.fValidationHandler).reset(this.fConfiguration);
            }
        } else {
            this.fValidationHandler = null;
        }
        this.fErrorHandler = (DOMErrorHandler) this.fConfiguration.getParameter(Constants.DOM_ERROR_HANDLER);
        if (this.fValidationHandler != null) {
            this.fValidationHandler.setDocumentHandler(this);
            this.fValidationHandler.startDocument(new SimpleLocator(this.fDocument.fDocumentURI, this.fDocument.fDocumentURI, -1, -1), this.fDocument.encoding, this.fNamespaceContext, null);
            this.fValidationHandler.xmlDecl(this.fDocument.getXmlVersion(), this.fDocument.getXmlEncoding(), this.fDocument.getXmlStandalone() ? "yes" : "no", null);
        }
        if (schemaType == XMLGrammarDescription.XML_DTD) {
            if (schemaLocations != null) {
                try {
                    str = schemaLocations[0];
                } catch (RuntimeException e) {
                    if (this.fValidationHandler != null) {
                        this.fValidationHandler.setDocumentHandler(null);
                        CoreDOMImplementationImpl.singleton.releaseValidator(schemaType, xmlVersion, this.fValidationHandler);
                        this.fValidationHandler = null;
                    }
                    if (e == abort) {
                        return;
                    } else {
                        throw e;
                    }
                }
            } else {
                str = null;
            }
            processDTD(xmlVersion, str);
        }
        Node kid = this.fDocument.getFirstChild();
        while (kid != null) {
            Node next = kid.getNextSibling();
            Node kid2 = normalizeNode(kid);
            if (kid2 != null) {
                next = kid2;
            }
            kid = next;
        }
        if (this.fValidationHandler != null) {
            this.fValidationHandler.endDocument(null);
            this.fValidationHandler.setDocumentHandler(null);
            CoreDOMImplementationImpl.singleton.releaseValidator(schemaType, xmlVersion, this.fValidationHandler);
            this.fValidationHandler = null;
        }
    }

    protected Node normalizeNode(Node node) {
        boolean wellformed;
        boolean wellformed2;
        boolean wellformed3;
        Node nextSibling;
        Node node2 = node;
        short type = node.getNodeType();
        this.fLocator.fRelatedNode = node2;
        switch (type) {
            case 1:
                if (this.fDocument.errorChecking && (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && this.fDocument.isXMLVersionChanged()) {
                    if (this.fNamespaceValidation) {
                        wellformed2 = CoreDocumentImpl.isValidQName(node.getPrefix(), node.getLocalName(), this.fDocument.isXML11Version());
                    } else {
                        wellformed2 = CoreDocumentImpl.isXMLName(node.getNodeName(), this.fDocument.isXML11Version());
                    }
                    if (!wellformed2) {
                        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Element", node.getNodeName()});
                        reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg, (short) 2, "wf-invalid-character-in-node-name");
                    }
                }
                this.fNamespaceContext.pushContext();
                this.fLocalNSBinder.reset();
                ElementImpl elem = (ElementImpl) node2;
                if (elem.needsSyncChildren()) {
                    elem.synchronizeChildren();
                }
                AttributeMap attributes = elem.hasAttributes() ? (AttributeMap) elem.getAttributes() : null;
                if ((this.fConfiguration.features & 1) != 0) {
                    namespaceFixUp(elem, attributes);
                    if ((this.fConfiguration.features & XSSimpleTypeDefinition.FACET_TOTALDIGITS) == 0 && attributes != null) {
                        int i = 0;
                        while (i < attributes.getLength()) {
                            Attr att = (Attr) attributes.getItem(i);
                            if (XMLSymbols.PREFIX_XMLNS.equals(att.getPrefix()) || XMLSymbols.PREFIX_XMLNS.equals(att.getName())) {
                                elem.removeAttributeNode(att);
                                i--;
                            }
                            i++;
                        }
                    }
                } else if (attributes != null) {
                    for (int i2 = 0; i2 < attributes.getLength(); i2++) {
                        Attr attr = (Attr) attributes.item(i2);
                        attr.normalize();
                        if (this.fDocument.errorChecking && (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                            isAttrValueWF(this.fErrorHandler, this.fError, this.fLocator, attributes, attr, attr.getValue(), this.fDocument.isXML11Version());
                            if (this.fDocument.isXMLVersionChanged()) {
                                if (this.fNamespaceValidation) {
                                    wellformed = CoreDocumentImpl.isValidQName(node.getPrefix(), node.getLocalName(), this.fDocument.isXML11Version());
                                } else {
                                    wellformed = CoreDocumentImpl.isXMLName(node.getNodeName(), this.fDocument.isXML11Version());
                                }
                                if (!wellformed) {
                                    String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Attr", node.getNodeName()});
                                    reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg2, (short) 2, "wf-invalid-character-in-node-name");
                                }
                            }
                        }
                    }
                }
                if (this.fValidationHandler != null) {
                    this.fAttrProxy.setAttributes(attributes, this.fDocument, elem);
                    updateQName(elem, this.fQName);
                    this.fConfiguration.fErrorHandlerWrapper.fCurrentNode = node2;
                    this.fCurrentNode = node2;
                    this.fValidationHandler.startElement(this.fQName, this.fAttrProxy, null);
                }
                Node kid = elem.getFirstChild();
                while (kid != null) {
                    Node next = kid.getNextSibling();
                    Node kid2 = normalizeNode(kid);
                    if (kid2 != null) {
                        next = kid2;
                    }
                    kid = next;
                }
                if (this.fValidationHandler != null) {
                    updateQName(elem, this.fQName);
                    this.fConfiguration.fErrorHandlerWrapper.fCurrentNode = node2;
                    this.fCurrentNode = node2;
                    this.fValidationHandler.endElement(this.fQName, null);
                }
                this.fNamespaceContext.popContext();
                return null;
            case 2:
            case 6:
            case 9:
            case 10:
            default:
                return null;
            case 3:
                Node next2 = node.getNextSibling();
                if (next2 != null && next2.getNodeType() == 3) {
                    ((Text) node2).appendData(next2.getNodeValue());
                    node.getParentNode().removeChild(next2);
                    return node2;
                }
                if (node.getNodeValue().length() == 0) {
                    node.getParentNode().removeChild(node2);
                } else {
                    short nextType = next2 != null ? next2.getNodeType() : (short) -1;
                    if (nextType == -1 || (((this.fConfiguration.features & 4) != 0 || nextType != 6) && (((this.fConfiguration.features & 32) != 0 || nextType != 8) && ((8 & this.fConfiguration.features) != 0 || nextType != 4)))) {
                        if (this.fDocument.errorChecking && (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                            isXMLCharWF(this.fErrorHandler, this.fError, this.fLocator, node.getNodeValue(), this.fDocument.isXML11Version());
                        }
                        if (this.fValidationHandler != null) {
                            this.fConfiguration.fErrorHandlerWrapper.fCurrentNode = node2;
                            this.fCurrentNode = node2;
                            this.fValidationHandler.characterData(node.getNodeValue(), null);
                            if (!this.fNamespaceValidation) {
                                if (this.fAllWhitespace) {
                                    this.fAllWhitespace = false;
                                    ((TextImpl) node2).setIgnorableWhitespace(true);
                                } else {
                                    ((TextImpl) node2).setIgnorableWhitespace(false);
                                }
                            }
                        }
                    }
                }
                return null;
            case 4:
                if ((8 & this.fConfiguration.features) == 0) {
                    Node prevSibling = node.getPreviousSibling();
                    if (prevSibling != null && prevSibling.getNodeType() == 3) {
                        ((Text) prevSibling).appendData(node.getNodeValue());
                        node.getParentNode().removeChild(node2);
                        return prevSibling;
                    }
                    Node text = this.fDocument.createTextNode(node.getNodeValue());
                    Node parent = node.getParentNode();
                    parent.replaceChild(text, node2);
                    return text;
                }
                if (this.fValidationHandler != null) {
                    this.fConfiguration.fErrorHandlerWrapper.fCurrentNode = node2;
                    this.fCurrentNode = node2;
                    this.fValidationHandler.startCDATA(null);
                    this.fValidationHandler.characterData(node.getNodeValue(), null);
                    this.fValidationHandler.endCDATA(null);
                }
                String value = node.getNodeValue();
                if ((this.fConfiguration.features & 16) != 0) {
                    Node parent2 = node.getParentNode();
                    if (this.fDocument.errorChecking) {
                        isXMLCharWF(this.fErrorHandler, this.fError, this.fLocator, node.getNodeValue(), this.fDocument.isXML11Version());
                    }
                    while (true) {
                        int index = value.indexOf("]]>");
                        if (index >= 0) {
                            node2.setNodeValue(value.substring(0, index + 2));
                            value = value.substring(index + 2);
                            Node firstSplitNode = node2;
                            Node newChild = this.fDocument.createCDATASection(value);
                            parent2.insertBefore(newChild, node2.getNextSibling());
                            node2 = newChild;
                            this.fLocator.fRelatedNode = firstSplitNode;
                            String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "cdata-sections-splitted", null);
                            reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg3, (short) 1, "cdata-sections-splitted");
                        }
                    }
                } else if (this.fDocument.errorChecking) {
                    isCDataWF(this.fErrorHandler, this.fError, this.fLocator, value, this.fDocument.isXML11Version());
                }
                return null;
            case 5:
                if ((this.fConfiguration.features & 4) == 0) {
                    Node prevSibling2 = node.getPreviousSibling();
                    Node parent3 = node.getParentNode();
                    ((EntityReferenceImpl) node2).setReadOnly(false, true);
                    expandEntityRef(parent3, node2);
                    parent3.removeChild(node2);
                    Node next3 = prevSibling2 != null ? prevSibling2.getNextSibling() : parent3.getFirstChild();
                    if (prevSibling2 != null && next3 != null && prevSibling2.getNodeType() == 3 && next3.getNodeType() == 3) {
                        return prevSibling2;
                    }
                    return next3;
                }
                if (this.fDocument.errorChecking && (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0 && this.fDocument.isXMLVersionChanged()) {
                    CoreDocumentImpl.isXMLName(node.getNodeName(), this.fDocument.isXML11Version());
                }
                return null;
            case 7:
                if (this.fDocument.errorChecking && (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                    ProcessingInstruction pinode = (ProcessingInstruction) node2;
                    String target = pinode.getTarget();
                    if (this.fDocument.isXML11Version()) {
                        wellformed3 = XML11Char.isXML11ValidName(target);
                    } else {
                        wellformed3 = XMLChar.isValidName(target);
                    }
                    if (!wellformed3) {
                        String msg4 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Element", node.getNodeName()});
                        reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg4, (short) 2, "wf-invalid-character-in-node-name");
                    }
                    isXMLCharWF(this.fErrorHandler, this.fError, this.fLocator, pinode.getData(), this.fDocument.isXML11Version());
                }
                if (this.fValidationHandler != null) {
                    this.fValidationHandler.processingInstruction(((ProcessingInstruction) node2).getTarget(), EMPTY_STRING, null);
                }
                return null;
            case 8:
                if ((this.fConfiguration.features & 32) == 0) {
                    Node prevSibling3 = node.getPreviousSibling();
                    Node parent4 = node.getParentNode();
                    parent4.removeChild(node2);
                    if (prevSibling3 != null && prevSibling3.getNodeType() == 3 && (nextSibling = prevSibling3.getNextSibling()) != null && nextSibling.getNodeType() == 3) {
                        ((TextImpl) nextSibling).insertData(0, prevSibling3.getNodeValue());
                        parent4.removeChild(prevSibling3);
                        return nextSibling;
                    }
                } else {
                    if (this.fDocument.errorChecking && (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) != 0) {
                        String commentdata = ((Comment) node2).getData();
                        isCommentWF(this.fErrorHandler, this.fError, this.fLocator, commentdata, this.fDocument.isXML11Version());
                    }
                    if (this.fValidationHandler != null) {
                        this.fValidationHandler.comment(EMPTY_STRING, null);
                    }
                }
                return null;
        }
    }

    private void processDTD(String xmlVersion, String schemaLocation) throws Throwable {
        String rootName;
        XMLDTDLoader loader;
        XMLDTDLoader loader2;
        String publicId = null;
        String systemId = schemaLocation;
        String baseSystemId = this.fDocument.getDocumentURI();
        String internalSubset = null;
        DocumentType docType = this.fDocument.getDoctype();
        if (docType != null) {
            rootName = docType.getName();
            publicId = docType.getPublicId();
            if (systemId == null || systemId.length() == 0) {
                systemId = docType.getSystemId();
            }
            internalSubset = docType.getInternalSubset();
        } else {
            Element elem = this.fDocument.getDocumentElement();
            if (elem == null) {
                return;
            }
            rootName = elem.getNodeName();
            if (systemId == null || systemId.length() == 0) {
                return;
            }
        }
        String systemId2 = systemId;
        String internalSubset2 = internalSubset;
        String publicId2 = publicId;
        String rootName2 = rootName;
        try {
            this.fValidationHandler.doctypeDecl(rootName2, publicId2, systemId2, null);
            loader2 = CoreDOMImplementationImpl.singleton.getDTDLoader(xmlVersion);
            try {
                loader2.setFeature("http://xml.org/sax/features/validation", true);
                loader2.setEntityResolver(this.fConfiguration.getEntityResolver());
                loader2.setErrorHandler(this.fConfiguration.getErrorHandler());
                loader = loader2;
            } catch (IOException e) {
            } catch (Throwable th) {
                th = th;
                loader = loader2;
            }
        } catch (IOException e2) {
            loader2 = null;
        } catch (Throwable th2) {
            th = th2;
            loader = null;
        }
        try {
            loader2.loadGrammarWithContext((XMLDTDValidator) this.fValidationHandler, rootName2, publicId2, systemId2, baseSystemId, internalSubset2);
            if (loader != null) {
                CoreDOMImplementationImpl.singleton.releaseDTDLoader(xmlVersion, loader);
            }
        } catch (IOException e3) {
            loader2 = loader;
            if (loader2 != null) {
                CoreDOMImplementationImpl.singleton.releaseDTDLoader(xmlVersion, loader2);
            }
        } catch (Throwable th3) {
            th = th3;
            if (loader != null) {
                CoreDOMImplementationImpl.singleton.releaseDTDLoader(xmlVersion, loader);
            }
            throw th;
        }
    }

    protected final void expandEntityRef(Node parent, Node reference) {
        Node kid = reference.getFirstChild();
        while (kid != null) {
            Node next = kid.getNextSibling();
            parent.insertBefore(kid, reference);
            kid = next;
        }
    }

    protected final void namespaceFixUp(ElementImpl element, AttributeMap attributes) {
        String value;
        String value2;
        if (attributes != null) {
            for (int k = 0; k < attributes.getLength(); k++) {
                Attr attr = (Attr) attributes.getItem(k);
                String uri = attr.getNamespaceURI();
                if (uri != null && uri.equals(NamespaceContext.XMLNS_URI)) {
                    String value3 = attr.getNodeValue();
                    if (value3 == null) {
                        value3 = XMLSymbols.EMPTY_STRING;
                    }
                    if (this.fDocument.errorChecking && value3.equals(NamespaceContext.XMLNS_URI)) {
                        this.fLocator.fRelatedNode = attr;
                        String msg = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "CantBindXMLNS", null);
                        reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg, (short) 2, "CantBindXMLNS");
                    } else {
                        String prefix = attr.getPrefix();
                        String prefix2 = (prefix == null || prefix.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix);
                        String localpart = this.fSymbolTable.addSymbol(attr.getLocalName());
                        if (prefix2 == XMLSymbols.PREFIX_XMLNS) {
                            String value4 = this.fSymbolTable.addSymbol(value3);
                            if (value4.length() != 0) {
                                this.fNamespaceContext.declarePrefix(localpart, value4);
                            }
                        } else {
                            String value5 = this.fSymbolTable.addSymbol(value3);
                            this.fNamespaceContext.declarePrefix(XMLSymbols.EMPTY_STRING, value5.length() != 0 ? value5 : null);
                        }
                    }
                }
            }
        }
        String uri2 = element.getNamespaceURI();
        String prefix3 = element.getPrefix();
        if (uri2 != null) {
            String uri3 = this.fSymbolTable.addSymbol(uri2);
            prefix3 = (prefix3 == null || prefix3.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix3);
            if (this.fNamespaceContext.getURI(prefix3) != uri3) {
                addNamespaceDecl(prefix3, uri3, element);
                this.fLocalNSBinder.declarePrefix(prefix3, uri3);
                this.fNamespaceContext.declarePrefix(prefix3, uri3);
            }
        } else if (element.getLocalName() != null) {
            String uri4 = this.fNamespaceContext.getURI(XMLSymbols.EMPTY_STRING);
            if (uri4 != null && uri4.length() > 0) {
                addNamespaceDecl(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING, element);
                this.fLocalNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, null);
                this.fNamespaceContext.declarePrefix(XMLSymbols.EMPTY_STRING, null);
            }
        } else if (this.fNamespaceValidation) {
            String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NullLocalElementName", new Object[]{element.getNodeName()});
            reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg2, (short) 3, "NullLocalElementName");
        } else {
            String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NullLocalElementName", new Object[]{element.getNodeName()});
            reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg3, (short) 2, "NullLocalElementName");
        }
        if (attributes != null) {
            attributes.cloneMap(this.fAttributeList);
            for (int i = 0; i < this.fAttributeList.size(); i++) {
                Attr attr2 = (Attr) this.fAttributeList.get(i);
                this.fLocator.fRelatedNode = attr2;
                attr2.normalize();
                String value6 = attr2.getValue();
                String uri5 = attr2.getNamespaceURI();
                if (value6 == null) {
                    value6 = XMLSymbols.EMPTY_STRING;
                }
                String value7 = value6;
                if (!this.fDocument.errorChecking || (this.fConfiguration.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) == 0) {
                    value = value7;
                } else {
                    value = value7;
                    isAttrValueWF(this.fErrorHandler, this.fError, this.fLocator, attributes, attr2, value7, this.fDocument.isXML11Version());
                    if (this.fDocument.isXMLVersionChanged()) {
                        boolean wellformed = this.fNamespaceValidation ? CoreDocumentImpl.isValidQName(attr2.getPrefix(), attr2.getLocalName(), this.fDocument.isXML11Version()) : CoreDocumentImpl.isXMLName(attr2.getNodeName(), this.fDocument.isXML11Version());
                        if (!wellformed) {
                            String msg4 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Attr", attr2.getNodeName()});
                            reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg4, (short) 2, "wf-invalid-character-in-node-name");
                        }
                    }
                }
                if (uri5 != null) {
                    String prefix4 = attr2.getPrefix();
                    String prefix5 = (prefix4 == null || prefix4.length() == 0) ? XMLSymbols.EMPTY_STRING : this.fSymbolTable.addSymbol(prefix4);
                    this.fSymbolTable.addSymbol(attr2.getLocalName());
                    if (uri5 == null || !uri5.equals(NamespaceContext.XMLNS_URI)) {
                        ((AttrImpl) attr2).setIdAttribute(false);
                        String uri6 = this.fSymbolTable.addSymbol(uri5);
                        String declaredURI = this.fNamespaceContext.getURI(prefix5);
                        if (prefix5 == XMLSymbols.EMPTY_STRING || declaredURI != uri6) {
                            String declaredPrefix = this.fNamespaceContext.getPrefix(uri6);
                            if (declaredPrefix == null || declaredPrefix == XMLSymbols.EMPTY_STRING) {
                                if (prefix5 == XMLSymbols.EMPTY_STRING || this.fLocalNSBinder.getURI(prefix5) != null) {
                                    SymbolTable symbolTable = this.fSymbolTable;
                                    StringBuilder sb = new StringBuilder(PREFIX);
                                    int counter = 1 + 1;
                                    sb.append(1);
                                    prefix5 = symbolTable.addSymbol(sb.toString());
                                    while (this.fLocalNSBinder.getURI(prefix5) != null) {
                                        prefix5 = this.fSymbolTable.addSymbol(PREFIX + counter);
                                        counter++;
                                    }
                                }
                                addNamespaceDecl(prefix5, uri6, element);
                                String value8 = this.fSymbolTable.addSymbol(value);
                                this.fLocalNSBinder.declarePrefix(prefix5, value8);
                                this.fNamespaceContext.declarePrefix(prefix5, uri6);
                                value2 = value8;
                            } else {
                                prefix5 = declaredPrefix;
                                value2 = value;
                            }
                            attr2.setPrefix(prefix5);
                        }
                    }
                } else {
                    ((AttrImpl) attr2).setIdAttribute(false);
                    if (attr2.getLocalName() == null) {
                        if (this.fNamespaceValidation) {
                            String msg5 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NullLocalAttrName", new Object[]{attr2.getNodeName()});
                            reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg5, (short) 3, "NullLocalAttrName");
                        } else {
                            String msg6 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NullLocalAttrName", new Object[]{attr2.getNodeName()});
                            reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg6, (short) 2, "NullLocalAttrName");
                        }
                    }
                }
            }
        }
    }

    protected final void addNamespaceDecl(String prefix, String uri, ElementImpl element) {
        if (prefix == XMLSymbols.EMPTY_STRING) {
            element.setAttributeNS(NamespaceContext.XMLNS_URI, XMLSymbols.PREFIX_XMLNS, uri);
            return;
        }
        element.setAttributeNS(NamespaceContext.XMLNS_URI, "xmlns:" + prefix, uri);
    }

    public static final void isCDataWF(DOMErrorHandler errorHandler, DOMErrorImpl error, DOMLocatorImpl locator, String datavalue, boolean isXML11Version) {
        int count;
        int count2;
        if (datavalue == null || datavalue.length() == 0) {
            return;
        }
        char[] dataarray = datavalue.toCharArray();
        int datalength = dataarray.length;
        if (isXML11Version) {
            int i = 0;
            while (i < datalength) {
                int i2 = i + 1;
                char c = dataarray[i];
                if (XML11Char.isXML11Invalid(c)) {
                    if (XMLChar.isHighSurrogate(c) && i2 < datalength) {
                        int i3 = i2 + 1;
                        char c2 = dataarray[i2];
                        if (XMLChar.isLowSurrogate(c2) && XMLChar.isSupplemental(XMLChar.supplemental(c, c2))) {
                            i = i3;
                        } else {
                            i2 = i3;
                        }
                    }
                    String msg = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInCDSect", new Object[]{Integer.toString(c, 16)});
                    reportDOMError(errorHandler, error, locator, msg, (short) 2, "wf-invalid-character");
                } else if (c == ']' && (count2 = i2) < datalength && dataarray[count2] == ']') {
                    do {
                        count2++;
                        if (count2 >= datalength) {
                            break;
                        }
                    } while (dataarray[count2] == ']');
                    if (count2 < datalength && dataarray[count2] == '>') {
                        String msg2 = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "CDEndInContent", null);
                        reportDOMError(errorHandler, error, locator, msg2, (short) 2, "wf-invalid-character");
                    }
                }
                i = i2;
            }
            return;
        }
        int i4 = 0;
        while (i4 < datalength) {
            int i5 = i4 + 1;
            char c3 = dataarray[i4];
            if (XMLChar.isInvalid(c3)) {
                if (XMLChar.isHighSurrogate(c3) && i5 < datalength) {
                    int i6 = i5 + 1;
                    char c22 = dataarray[i5];
                    if (XMLChar.isLowSurrogate(c22) && XMLChar.isSupplemental(XMLChar.supplemental(c3, c22))) {
                        i4 = i6;
                    } else {
                        i5 = i6;
                    }
                }
                String msg3 = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInCDSect", new Object[]{Integer.toString(c3, 16)});
                reportDOMError(errorHandler, error, locator, msg3, (short) 2, "wf-invalid-character");
            } else if (c3 == ']' && (count = i5) < datalength && dataarray[count] == ']') {
                do {
                    count++;
                    if (count >= datalength) {
                        break;
                    }
                } while (dataarray[count] == ']');
                if (count < datalength && dataarray[count] == '>') {
                    String msg4 = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "CDEndInContent", null);
                    reportDOMError(errorHandler, error, locator, msg4, (short) 2, "wf-invalid-character");
                }
            }
            i4 = i5;
        }
    }

    public static final void isXMLCharWF(DOMErrorHandler errorHandler, DOMErrorImpl error, DOMLocatorImpl locator, String datavalue, boolean isXML11Version) {
        int i;
        int i2;
        if (datavalue == null || datavalue.length() == 0) {
            return;
        }
        char[] dataarray = datavalue.toCharArray();
        int datalength = dataarray.length;
        if (isXML11Version) {
            int i3 = 0;
            while (i3 < datalength) {
                int i4 = i3 + 1;
                if (XML11Char.isXML11Invalid(dataarray[i3])) {
                    char ch = dataarray[i4 - 1];
                    if (XMLChar.isHighSurrogate(ch) && i4 < datalength) {
                        i2 = i4 + 1;
                        char ch2 = dataarray[i4];
                        if (!XMLChar.isLowSurrogate(ch2) || !XMLChar.isSupplemental(XMLChar.supplemental(ch, ch2))) {
                        }
                        i3 = i2;
                    } else {
                        i2 = i4;
                    }
                    String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "InvalidXMLCharInDOM", new Object[]{Integer.toString(dataarray[i2 - 1], 16)});
                    reportDOMError(errorHandler, error, locator, msg, (short) 2, "wf-invalid-character");
                    i3 = i2;
                } else {
                    i3 = i4;
                }
            }
            return;
        }
        int i5 = 0;
        while (i5 < datalength) {
            int i6 = i5 + 1;
            if (XMLChar.isInvalid(dataarray[i5])) {
                char ch3 = dataarray[i6 - 1];
                if (XMLChar.isHighSurrogate(ch3) && i6 < datalength) {
                    i = i6 + 1;
                    char ch22 = dataarray[i6];
                    if (!XMLChar.isLowSurrogate(ch22) || !XMLChar.isSupplemental(XMLChar.supplemental(ch3, ch22))) {
                    }
                    i5 = i;
                } else {
                    i = i6;
                }
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "InvalidXMLCharInDOM", new Object[]{Integer.toString(dataarray[i - 1], 16)});
                reportDOMError(errorHandler, error, locator, msg2, (short) 2, "wf-invalid-character");
                i5 = i;
            } else {
                i5 = i6;
            }
        }
    }

    public static final void isCommentWF(DOMErrorHandler errorHandler, DOMErrorImpl error, DOMLocatorImpl locator, String datavalue, boolean isXML11Version) {
        if (datavalue == null || datavalue.length() == 0) {
            return;
        }
        char[] dataarray = datavalue.toCharArray();
        int datalength = dataarray.length;
        if (isXML11Version) {
            int i = 0;
            while (i < datalength) {
                int i2 = i + 1;
                char c = dataarray[i];
                if (XML11Char.isXML11Invalid(c)) {
                    if (XMLChar.isHighSurrogate(c) && i2 < datalength) {
                        int i3 = i2 + 1;
                        char c2 = dataarray[i2];
                        if (XMLChar.isLowSurrogate(c2) && XMLChar.isSupplemental(XMLChar.supplemental(c, c2))) {
                            i = i3;
                        } else {
                            i2 = i3;
                        }
                    }
                    String msg = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInComment", new Object[]{Integer.toString(dataarray[i2 - 1], 16)});
                    reportDOMError(errorHandler, error, locator, msg, (short) 2, "wf-invalid-character");
                } else if (c == '-' && i2 < datalength && dataarray[i2] == '-') {
                    String msg2 = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "DashDashInComment", null);
                    reportDOMError(errorHandler, error, locator, msg2, (short) 2, "wf-invalid-character");
                }
                i = i2;
            }
            return;
        }
        int i4 = 0;
        while (i4 < datalength) {
            int i5 = i4 + 1;
            char c3 = dataarray[i4];
            if (XMLChar.isInvalid(c3)) {
                if (XMLChar.isHighSurrogate(c3) && i5 < datalength) {
                    int i6 = i5 + 1;
                    char c22 = dataarray[i5];
                    if (XMLChar.isLowSurrogate(c22) && XMLChar.isSupplemental(XMLChar.supplemental(c3, c22))) {
                        i4 = i6;
                    } else {
                        i5 = i6;
                    }
                }
                String msg3 = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidCharInComment", new Object[]{Integer.toString(dataarray[i5 - 1], 16)});
                reportDOMError(errorHandler, error, locator, msg3, (short) 2, "wf-invalid-character");
            } else if (c3 == '-' && i5 < datalength && dataarray[i5] == '-') {
                String msg4 = DOMMessageFormatter.formatMessage("http://www.w3.org/TR/1998/REC-xml-19980210", "DashDashInComment", null);
                reportDOMError(errorHandler, error, locator, msg4, (short) 2, "wf-invalid-character");
            }
            i4 = i5;
        }
    }

    public static final void isAttrValueWF(DOMErrorHandler errorHandler, DOMErrorImpl error, DOMLocatorImpl locator, NamedNodeMap attributes, Attr attr, String value, boolean xml11Version) {
        DocumentType docType;
        if ((attr instanceof AttrImpl) && attr.hasStringValue()) {
            isXMLCharWF(errorHandler, error, locator, value, xml11Version);
            return;
        }
        NodeList children = attr.getChildNodes();
        for (int j = 0; j < j; j++) {
            Node child = children.item(j);
            if (child.getNodeType() != 5) {
                isXMLCharWF(errorHandler, error, locator, child.getNodeValue(), xml11Version);
            } else {
                Document owner = attr.getOwnerDocument();
                Entity ent = null;
                if (owner != null && (docType = owner.getDoctype()) != null) {
                    NamedNodeMap entities = docType.getEntities();
                    ent = (Entity) entities.getNamedItemNS("*", child.getNodeName());
                }
                if (ent == null) {
                    String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "UndeclaredEntRefInAttrValue", new Object[]{attr.getNodeName()});
                    reportDOMError(errorHandler, error, locator, msg, (short) 2, "UndeclaredEntRefInAttrValue");
                }
            }
        }
    }

    public static final void reportDOMError(DOMErrorHandler errorHandler, DOMErrorImpl error, DOMLocatorImpl locator, String message, short severity, String type) {
        if (errorHandler != null) {
            error.reset();
            error.fMessage = message;
            error.fSeverity = severity;
            error.fLocator = locator;
            error.fType = type;
            error.fRelatedData = locator.fRelatedNode;
            if (!errorHandler.handleError(error)) {
                throw abort;
            }
        }
        if (severity == 3) {
            throw abort;
        }
    }

    protected final void updateQName(Node node, QName qname) {
        String prefix = node.getPrefix();
        String namespace = node.getNamespaceURI();
        String localName = node.getLocalName();
        qname.prefix = (prefix == null || prefix.length() == 0) ? null : this.fSymbolTable.addSymbol(prefix);
        qname.localpart = localName != null ? this.fSymbolTable.addSymbol(localName) : null;
        qname.rawname = this.fSymbolTable.addSymbol(node.getNodeName());
        qname.uri = namespace != null ? this.fSymbolTable.addSymbol(namespace) : null;
    }

    final String normalizeAttributeValue(String value, Attr attr) {
        if (!attr.getSpecified()) {
            return value;
        }
        int end = value.length();
        if (this.fNormalizedValue.ch.length < end) {
            this.fNormalizedValue.ch = new char[end];
        }
        this.fNormalizedValue.length = 0;
        boolean normalized = false;
        int i = 0;
        while (i < end) {
            char c = value.charAt(i);
            if (c == '\t' || c == '\n') {
                char[] cArr = this.fNormalizedValue.ch;
                XMLString xMLString = this.fNormalizedValue;
                int i2 = xMLString.length;
                xMLString.length = i2 + 1;
                cArr[i2] = ' ';
                normalized = true;
            } else if (c == '\r') {
                normalized = true;
                char[] cArr2 = this.fNormalizedValue.ch;
                XMLString xMLString2 = this.fNormalizedValue;
                int i3 = xMLString2.length;
                xMLString2.length = i3 + 1;
                cArr2[i3] = ' ';
                int next = i + 1;
                if (next < end && value.charAt(next) == '\n') {
                    i = next;
                }
            } else {
                char[] cArr3 = this.fNormalizedValue.ch;
                XMLString xMLString3 = this.fNormalizedValue;
                int i4 = xMLString3.length;
                xMLString3.length = i4 + 1;
                cArr3[i4] = c;
            }
            i++;
        }
        if (normalized) {
            String value2 = this.fNormalizedValue.toString();
            attr.setValue(value2);
            return value2;
        }
        return value;
    }

    protected final class XMLAttributesProxy implements XMLAttributes {
        protected AttributeMap fAttributes;
        protected CoreDocumentImpl fDocument;
        protected ElementImpl fElement;
        protected final Vector fDTDTypes = new Vector(5);
        protected final Vector fAugmentations = new Vector(5);

        protected XMLAttributesProxy() {
        }

        public void setAttributes(AttributeMap attributes, CoreDocumentImpl doc, ElementImpl elem) {
            this.fDocument = doc;
            this.fAttributes = attributes;
            this.fElement = elem;
            if (attributes != null) {
                int length = attributes.getLength();
                this.fDTDTypes.setSize(length);
                this.fAugmentations.setSize(length);
                for (int i = 0; i < length; i++) {
                    this.fAugmentations.setElementAt(new AugmentationsImpl(), i);
                }
                return;
            }
            this.fDTDTypes.setSize(0);
            this.fAugmentations.setSize(0);
        }

        @Override
        public int addAttribute(QName qname, String attrType, String attrValue) {
            int index = this.fElement.getXercesAttribute(qname.uri, qname.localpart);
            if (index < 0) {
                AttrImpl attr = (AttrImpl) ((CoreDocumentImpl) this.fElement.getOwnerDocument()).createAttributeNS(qname.uri, qname.rawname, qname.localpart);
                attr.setNodeValue(attrValue);
                int index2 = this.fElement.setXercesAttributeNode(attr);
                this.fDTDTypes.insertElementAt(attrType, index2);
                this.fAugmentations.insertElementAt(new AugmentationsImpl(), index2);
                attr.setSpecified(false);
                return index2;
            }
            return index;
        }

        @Override
        public void removeAllAttributes() {
        }

        @Override
        public void removeAttributeAt(int attrIndex) {
        }

        @Override
        public int getLength() {
            if (this.fAttributes != null) {
                return this.fAttributes.getLength();
            }
            return 0;
        }

        @Override
        public int getIndex(String qName) {
            return -1;
        }

        @Override
        public int getIndex(String uri, String localPart) {
            return -1;
        }

        @Override
        public void setName(int attrIndex, QName attrName) {
        }

        @Override
        public void getName(int attrIndex, QName attrName) {
            if (this.fAttributes != null) {
                DOMNormalizer.this.updateQName((Node) this.fAttributes.getItem(attrIndex), attrName);
            }
        }

        @Override
        public String getPrefix(int index) {
            if (this.fAttributes == null) {
                return null;
            }
            Node node = (Node) this.fAttributes.getItem(index);
            String prefix = node.getPrefix();
            if (prefix == null || prefix.length() == 0) {
                return null;
            }
            return DOMNormalizer.this.fSymbolTable.addSymbol(prefix);
        }

        @Override
        public String getURI(int index) {
            if (this.fAttributes == null) {
                return null;
            }
            Node node = (Node) this.fAttributes.getItem(index);
            String namespace = node.getNamespaceURI();
            if (namespace != null) {
                return DOMNormalizer.this.fSymbolTable.addSymbol(namespace);
            }
            return null;
        }

        @Override
        public String getLocalName(int index) {
            if (this.fAttributes == null) {
                return null;
            }
            Node node = (Node) this.fAttributes.getItem(index);
            String localName = node.getLocalName();
            if (localName != null) {
                return DOMNormalizer.this.fSymbolTable.addSymbol(localName);
            }
            return null;
        }

        @Override
        public String getQName(int index) {
            if (this.fAttributes != null) {
                Node node = (Node) this.fAttributes.getItem(index);
                String rawname = DOMNormalizer.this.fSymbolTable.addSymbol(node.getNodeName());
                return rawname;
            }
            return null;
        }

        @Override
        public void setType(int attrIndex, String attrType) {
            this.fDTDTypes.setElementAt(attrType, attrIndex);
        }

        @Override
        public String getType(int index) {
            String type = (String) this.fDTDTypes.elementAt(index);
            return type != null ? getReportableType(type) : "CDATA";
        }

        @Override
        public String getType(String qName) {
            return "CDATA";
        }

        @Override
        public String getType(String uri, String localName) {
            return "CDATA";
        }

        private String getReportableType(String type) {
            if (type.charAt(0) == '(') {
                return SchemaSymbols.ATTVAL_NMTOKEN;
            }
            return type;
        }

        @Override
        public void setValue(int attrIndex, String attrValue) {
            if (this.fAttributes != null) {
                AttrImpl attr = (AttrImpl) this.fAttributes.getItem(attrIndex);
                boolean specified = attr.getSpecified();
                attr.setValue(attrValue);
                attr.setSpecified(specified);
            }
        }

        @Override
        public String getValue(int index) {
            return this.fAttributes != null ? this.fAttributes.item(index).getNodeValue() : "";
        }

        @Override
        public String getValue(String qName) {
            return null;
        }

        @Override
        public String getValue(String uri, String localName) {
            Node node;
            if (this.fAttributes == null || (node = this.fAttributes.getNamedItemNS(uri, localName)) == null) {
                return null;
            }
            return node.getNodeValue();
        }

        @Override
        public void setNonNormalizedValue(int attrIndex, String attrValue) {
        }

        @Override
        public String getNonNormalizedValue(int attrIndex) {
            return null;
        }

        @Override
        public void setSpecified(int attrIndex, boolean specified) {
            AttrImpl attr = (AttrImpl) this.fAttributes.getItem(attrIndex);
            attr.setSpecified(specified);
        }

        @Override
        public boolean isSpecified(int attrIndex) {
            return ((Attr) this.fAttributes.getItem(attrIndex)).getSpecified();
        }

        @Override
        public Augmentations getAugmentations(int attributeIndex) {
            return (Augmentations) this.fAugmentations.elementAt(attributeIndex);
        }

        @Override
        public Augmentations getAugmentations(String uri, String localPart) {
            return null;
        }

        @Override
        public Augmentations getAugmentations(String qName) {
            return null;
        }

        @Override
        public void setAugmentations(int attrIndex, Augmentations augs) {
            this.fAugmentations.setElementAt(augs, attrIndex);
        }
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
        String normalizedValue;
        Element currentElement = (Element) this.fCurrentNode;
        int attrCount = attributes.getLength();
        for (int i = 0; i < attrCount; i++) {
            attributes.getName(i, this.fAttrQName);
            Attr attr = currentElement.getAttributeNodeNS(this.fAttrQName.uri, this.fAttrQName.localpart);
            if (attr == null) {
                attr = currentElement.getAttributeNode(this.fAttrQName.rawname);
            }
            AttributePSVI attrPSVI = (AttributePSVI) attributes.getAugmentations(i).getItem(Constants.ATTRIBUTE_PSVI);
            if (attrPSVI != null) {
                XSTypeDefinition decl = attrPSVI.getMemberTypeDefinition();
                boolean id = false;
                if (decl != null) {
                    id = ((XSSimpleType) decl).isIDType();
                } else {
                    decl = attrPSVI.getTypeDefinition();
                    if (decl != null) {
                        id = ((XSSimpleType) decl).isIDType();
                    }
                }
                if (id) {
                    ((ElementImpl) currentElement).setIdAttributeNode(attr, true);
                }
                if (this.fPSVI) {
                    ((PSVIAttrNSImpl) attr).setPSVI(attrPSVI);
                }
                ((AttrImpl) attr).setType(decl);
                if ((this.fConfiguration.features & 2) != 0 && (normalizedValue = attrPSVI.getSchemaNormalizedValue()) != null) {
                    boolean specified = attr.getSpecified();
                    attr.setValue(normalizedValue);
                    if (!specified) {
                        ((AttrImpl) attr).setSpecified(specified);
                    }
                }
            } else {
                String type = null;
                boolean isDeclared = Boolean.TRUE.equals(attributes.getAugmentations(i).getItem(Constants.ATTRIBUTE_DECLARED));
                if (isDeclared) {
                    type = attributes.getType(i);
                    if (SchemaSymbols.ATTVAL_ID.equals(type)) {
                        ((ElementImpl) currentElement).setIdAttributeNode(attr, true);
                    }
                }
                ((AttrImpl) attr).setType(type);
            }
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
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        this.fAllWhitespace = true;
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        ElementPSVI elementPSVI;
        if (augs != null && (elementPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI)) != null) {
            ?? r1 = (ElementImpl) this.fCurrentNode;
            if (this.fPSVI) {
                ((PSVIElementNSImpl) this.fCurrentNode).setPSVI(elementPSVI);
            }
            if (r1 instanceof ElementNSImpl) {
                XSTypeDefinition type = elementPSVI.getMemberTypeDefinition();
                if (type == null) {
                    type = elementPSVI.getTypeDefinition();
                }
                r1.setType(type);
            }
            String normalizedValue = elementPSVI.getSchemaNormalizedValue();
            if ((this.fConfiguration.features & 2) != 0) {
                if (normalizedValue != null) {
                    r1.setTextContent(normalizedValue);
                    return;
                }
                return;
            } else {
                String text = r1.getTextContent();
                if (text.length() == 0 && normalizedValue != null) {
                    r1.setTextContent(normalizedValue);
                    return;
                }
                return;
            }
        }
        if (this.fCurrentNode instanceof ElementNSImpl) {
            ((ElementNSImpl) this.fCurrentNode).setType(null);
        }
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
    public void setDocumentSource(XMLDocumentSource source) {
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return null;
    }
}
