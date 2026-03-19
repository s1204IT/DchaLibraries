package org.apache.harmony.xml.parsers;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import libcore.io.IoUtils;
import org.apache.harmony.xml.dom.AttrImpl;
import org.apache.harmony.xml.dom.CDATASectionImpl;
import org.apache.harmony.xml.dom.DOMImplementationImpl;
import org.apache.harmony.xml.dom.DocumentImpl;
import org.apache.harmony.xml.dom.DocumentTypeImpl;
import org.apache.harmony.xml.dom.ElementImpl;
import org.apache.harmony.xml.dom.TextImpl;
import org.kxml2.io.KXmlParser;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class DocumentBuilderImpl extends DocumentBuilder {
    private static DOMImplementationImpl dom = DOMImplementationImpl.getInstance();
    private boolean coalescing;
    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;
    private boolean ignoreComments;
    private boolean ignoreElementContentWhitespace;
    private boolean namespaceAware;

    DocumentBuilderImpl() {
    }

    @Override
    public void reset() {
        this.coalescing = false;
        this.entityResolver = null;
        this.errorHandler = null;
        this.ignoreComments = false;
        this.ignoreElementContentWhitespace = false;
        this.namespaceAware = false;
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return dom;
    }

    @Override
    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    @Override
    public boolean isValidating() {
        return false;
    }

    @Override
    public Document newDocument() {
        return dom.createDocument(null, null, null);
    }

    @Override
    public Document parse(InputSource inputSource) throws SAXException, IOException {
        if (inputSource == null) {
            throw new IllegalArgumentException("source == null");
        }
        String encoding = inputSource.getEncoding();
        String systemId = inputSource.getSystemId();
        DocumentImpl documentImpl = new DocumentImpl(dom, null, null, null, encoding);
        documentImpl.setDocumentURI(systemId);
        KXmlParser kXmlParser = new KXmlParser();
        try {
            try {
                kXmlParser.keepNamespaceAttributes();
                kXmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, this.namespaceAware);
                if (inputSource.getByteStream() != null) {
                    kXmlParser.setInput(inputSource.getByteStream(), encoding);
                } else if (inputSource.getCharacterStream() != null) {
                    kXmlParser.setInput(inputSource.getCharacterStream());
                } else if (systemId != null) {
                    URLConnection uRLConnectionOpenConnection = new URL(systemId).openConnection();
                    uRLConnectionOpenConnection.connect();
                    kXmlParser.setInput(uRLConnectionOpenConnection.getInputStream(), encoding);
                } else {
                    throw new SAXParseException("InputSource needs a stream, reader or URI", null);
                }
                if (kXmlParser.nextToken() == 1) {
                    throw new SAXParseException("Unexpected end of document", null);
                }
                parse(kXmlParser, documentImpl, documentImpl, 1);
                kXmlParser.require(1, null, null);
                return documentImpl;
            } catch (XmlPullParserException e) {
                Throwable detail = e.getDetail();
                if (detail instanceof IOException) {
                    throw ((IOException) detail);
                }
                if (detail instanceof RuntimeException) {
                    throw ((RuntimeException) detail);
                }
                LocatorImpl locatorImpl = new LocatorImpl();
                locatorImpl.setPublicId(inputSource.getPublicId());
                locatorImpl.setSystemId(systemId);
                locatorImpl.setLineNumber(e.getLineNumber());
                locatorImpl.setColumnNumber(e.getColumnNumber());
                SAXParseException sAXParseException = new SAXParseException(e.getMessage(), locatorImpl);
                if (this.errorHandler != null) {
                    this.errorHandler.error(sAXParseException);
                    throw sAXParseException;
                }
                throw sAXParseException;
            }
        } finally {
            IoUtils.closeQuietly(kXmlParser);
        }
    }

    private void parse(KXmlParser kXmlParser, DocumentImpl documentImpl, Node node, int i) throws XmlPullParserException, IOException {
        int eventType = kXmlParser.getEventType();
        while (eventType != i && eventType != 1) {
            int i2 = 0;
            if (eventType == 8) {
                String text = kXmlParser.getText();
                int iIndexOf = text.indexOf(32);
                node.appendChild(documentImpl.createProcessingInstruction(iIndexOf != -1 ? text.substring(0, iIndexOf) : text, iIndexOf != -1 ? text.substring(iIndexOf + 1) : ""));
            } else if (eventType == 10) {
                documentImpl.appendChild(new DocumentTypeImpl(documentImpl, kXmlParser.getRootElementName(), kXmlParser.getPublicId(), kXmlParser.getSystemId()));
            } else if (eventType == 9) {
                if (!this.ignoreComments) {
                    node.appendChild(documentImpl.createComment(kXmlParser.getText()));
                }
            } else if (eventType == 7) {
                if (!this.ignoreElementContentWhitespace && documentImpl != node) {
                    appendText(documentImpl, node, eventType, kXmlParser.getText());
                }
            } else if (eventType == 4 || eventType == 5) {
                appendText(documentImpl, node, eventType, kXmlParser.getText());
            } else if (eventType == 6) {
                String name = kXmlParser.getName();
                EntityResolver entityResolver = this.entityResolver;
                String strResolvePredefinedOrCharacterEntity = resolvePredefinedOrCharacterEntity(name);
                if (strResolvePredefinedOrCharacterEntity != null) {
                    appendText(documentImpl, node, eventType, strResolvePredefinedOrCharacterEntity);
                } else {
                    node.appendChild(documentImpl.createEntityReference(name));
                }
            } else if (eventType == 2) {
                if (this.namespaceAware) {
                    String namespace = kXmlParser.getNamespace();
                    String name2 = kXmlParser.getName();
                    String prefix = kXmlParser.getPrefix();
                    if ("".equals(namespace)) {
                        namespace = null;
                    }
                    ElementImpl elementImplCreateElementNS = documentImpl.createElementNS(namespace, name2);
                    elementImplCreateElementNS.setPrefix(prefix);
                    node.appendChild(elementImplCreateElementNS);
                    while (i2 < kXmlParser.getAttributeCount()) {
                        String attributeNamespace = kXmlParser.getAttributeNamespace(i2);
                        String attributePrefix = kXmlParser.getAttributePrefix(i2);
                        String attributeName = kXmlParser.getAttributeName(i2);
                        String attributeValue = kXmlParser.getAttributeValue(i2);
                        if ("".equals(attributeNamespace)) {
                            attributeNamespace = null;
                        }
                        AttrImpl attrImplCreateAttributeNS = documentImpl.createAttributeNS(attributeNamespace, attributeName);
                        attrImplCreateAttributeNS.setPrefix(attributePrefix);
                        attrImplCreateAttributeNS.setValue(attributeValue);
                        elementImplCreateElementNS.setAttributeNodeNS(attrImplCreateAttributeNS);
                        i2++;
                    }
                    kXmlParser.nextToken();
                    parse(kXmlParser, documentImpl, elementImplCreateElementNS, 3);
                    kXmlParser.require(3, namespace, name2);
                } else {
                    String name3 = kXmlParser.getName();
                    ElementImpl elementImplCreateElement = documentImpl.createElement(name3);
                    node.appendChild(elementImplCreateElement);
                    while (i2 < kXmlParser.getAttributeCount()) {
                        String attributeName2 = kXmlParser.getAttributeName(i2);
                        String attributeValue2 = kXmlParser.getAttributeValue(i2);
                        AttrImpl attrImplCreateAttribute = documentImpl.createAttribute(attributeName2);
                        attrImplCreateAttribute.setValue(attributeValue2);
                        elementImplCreateElement.setAttributeNode(attrImplCreateAttribute);
                        i2++;
                    }
                    kXmlParser.nextToken();
                    parse(kXmlParser, documentImpl, elementImplCreateElement, 3);
                    kXmlParser.require(3, "", name3);
                }
            }
            eventType = kXmlParser.nextToken();
        }
    }

    private void appendText(DocumentImpl documentImpl, Node node, int i, String str) {
        Node lastChild;
        Node textImpl;
        if (str.isEmpty()) {
            return;
        }
        if ((this.coalescing || i != 5) && (lastChild = node.getLastChild()) != null && lastChild.getNodeType() == 3) {
            ((Text) lastChild).appendData(str);
            return;
        }
        if (i == 5) {
            textImpl = new CDATASectionImpl(documentImpl, str);
        } else {
            textImpl = new TextImpl(documentImpl, str);
        }
        node.appendChild(textImpl);
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setIgnoreComments(boolean z) {
        this.ignoreComments = z;
    }

    public void setCoalescing(boolean z) {
        this.coalescing = z;
    }

    public void setIgnoreElementContentWhitespace(boolean z) {
        this.ignoreElementContentWhitespace = z;
    }

    public void setNamespaceAware(boolean z) {
        this.namespaceAware = z;
    }

    private String resolvePredefinedOrCharacterEntity(String str) {
        if (str.startsWith("#x")) {
            return resolveCharacterReference(str.substring(2), 16);
        }
        if (str.startsWith("#")) {
            return resolveCharacterReference(str.substring(1), 10);
        }
        if ("lt".equals(str)) {
            return "<";
        }
        if ("gt".equals(str)) {
            return ">";
        }
        if ("amp".equals(str)) {
            return "&";
        }
        if ("apos".equals(str)) {
            return "'";
        }
        if ("quot".equals(str)) {
            return "\"";
        }
        return null;
    }

    private String resolveCharacterReference(String str, int i) {
        try {
            int i2 = Integer.parseInt(str, i);
            if (Character.isBmpCodePoint(i2)) {
                return String.valueOf((char) i2);
            }
            return new String(Character.toChars(i2));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
