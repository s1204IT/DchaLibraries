package org.apache.harmony.xml.parsers;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import libcore.io.IoUtils;
import org.apache.harmony.xml.dom.CDATASectionImpl;
import org.apache.harmony.xml.dom.DOMImplementationImpl;
import org.apache.harmony.xml.dom.DocumentImpl;
import org.apache.harmony.xml.dom.DocumentTypeImpl;
import org.apache.harmony.xml.dom.TextImpl;
import org.kxml2.io.KXmlParser;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    public Document parse(InputSource source) throws SAXException, IOException {
        if (source == null) {
            throw new IllegalArgumentException("source == null");
        }
        String inputEncoding = source.getEncoding();
        String systemId = source.getSystemId();
        DocumentImpl document = new DocumentImpl(dom, null, null, null, inputEncoding);
        document.setDocumentURI(systemId);
        KXmlParser parser = new KXmlParser();
        try {
            try {
                parser.keepNamespaceAttributes();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, this.namespaceAware);
                if (source.getByteStream() != null) {
                    parser.setInput(source.getByteStream(), inputEncoding);
                } else if (source.getCharacterStream() != null) {
                    parser.setInput(source.getCharacterStream());
                } else if (systemId != null) {
                    URL url = new URL(systemId);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.connect();
                    parser.setInput(urlConnection.getInputStream(), inputEncoding);
                } else {
                    throw new SAXParseException("InputSource needs a stream, reader or URI", null);
                }
                if (parser.nextToken() == 1) {
                    throw new SAXParseException("Unexpected end of document", null);
                }
                parse(parser, document, document, 1);
                parser.require(1, null, null);
                return document;
            } catch (XmlPullParserException ex) {
                if (ex.getDetail() instanceof IOException) {
                    throw ((IOException) ex.getDetail());
                }
                if (ex.getDetail() instanceof RuntimeException) {
                    throw ((RuntimeException) ex.getDetail());
                }
                LocatorImpl locator = new LocatorImpl();
                locator.setPublicId(source.getPublicId());
                locator.setSystemId(systemId);
                locator.setLineNumber(ex.getLineNumber());
                locator.setColumnNumber(ex.getColumnNumber());
                SAXParseException newEx = new SAXParseException(ex.getMessage(), locator);
                if (this.errorHandler != null) {
                    this.errorHandler.error(newEx);
                    throw newEx;
                }
                throw newEx;
            }
        } finally {
            IoUtils.closeQuietly(parser);
        }
    }

    private void parse(KXmlParser parser, DocumentImpl document, Node node, int endToken) throws XmlPullParserException, IOException {
        int token = parser.getEventType();
        while (token != endToken && token != 1) {
            if (token == 8) {
                String text = parser.getText();
                int dot = text.indexOf(32);
                String target = dot != -1 ? text.substring(0, dot) : text;
                String data = dot != -1 ? text.substring(dot + 1) : "";
                node.appendChild(document.createProcessingInstruction(target, data));
            } else if (token == 10) {
                String name = parser.getRootElementName();
                String publicId = parser.getPublicId();
                String systemId = parser.getSystemId();
                document.appendChild(new DocumentTypeImpl(document, name, publicId, systemId));
            } else if (token == 9) {
                if (!this.ignoreComments) {
                    node.appendChild(document.createComment(parser.getText()));
                }
            } else if (token == 7) {
                if (!this.ignoreElementContentWhitespace && document != node) {
                    appendText(document, node, token, parser.getText());
                }
            } else if (token == 4 || token == 5) {
                appendText(document, node, token, parser.getText());
            } else if (token == 6) {
                String entity = parser.getName();
                if (this.entityResolver != null) {
                }
                String resolved = resolvePredefinedOrCharacterEntity(entity);
                if (resolved != null) {
                    appendText(document, node, token, resolved);
                } else {
                    node.appendChild(document.createEntityReference(entity));
                }
            } else if (token == 2) {
                if (this.namespaceAware) {
                    String namespace = parser.getNamespace();
                    String name2 = parser.getName();
                    String prefix = parser.getPrefix();
                    if ("".equals(namespace)) {
                        namespace = null;
                    }
                    Element element = document.createElementNS(namespace, name2);
                    element.setPrefix(prefix);
                    node.appendChild(element);
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attrNamespace = parser.getAttributeNamespace(i);
                        String attrPrefix = parser.getAttributePrefix(i);
                        String attrName = parser.getAttributeName(i);
                        String attrValue = parser.getAttributeValue(i);
                        if ("".equals(attrNamespace)) {
                            attrNamespace = null;
                        }
                        Attr attr = document.createAttributeNS(attrNamespace, attrName);
                        attr.setPrefix(attrPrefix);
                        attr.setValue(attrValue);
                        element.setAttributeNodeNS(attr);
                    }
                    parser.nextToken();
                    parse(parser, document, element, 3);
                    parser.require(3, namespace, name2);
                } else {
                    String name3 = parser.getName();
                    Element element2 = document.createElement(name3);
                    node.appendChild(element2);
                    for (int i2 = 0; i2 < parser.getAttributeCount(); i2++) {
                        String attrName2 = parser.getAttributeName(i2);
                        String attrValue2 = parser.getAttributeValue(i2);
                        Attr attr2 = document.createAttribute(attrName2);
                        attr2.setValue(attrValue2);
                        element2.setAttributeNode(attr2);
                    }
                    parser.nextToken();
                    parse(parser, document, element2, 3);
                    parser.require(3, "", name3);
                }
            }
            token = parser.nextToken();
        }
    }

    private void appendText(DocumentImpl document, Node parent, int token, String text) {
        Node lastChild;
        if (!text.isEmpty()) {
            if ((this.coalescing || token != 5) && (lastChild = parent.getLastChild()) != null && lastChild.getNodeType() == 3) {
                Text textNode = (Text) lastChild;
                textNode.appendData(text);
            } else {
                parent.appendChild(token == 5 ? new CDATASectionImpl(document, text) : new TextImpl(document, text));
            }
        }
    }

    @Override
    public void setEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
    }

    public void setIgnoreComments(boolean value) {
        this.ignoreComments = value;
    }

    public void setCoalescing(boolean value) {
        this.coalescing = value;
    }

    public void setIgnoreElementContentWhitespace(boolean value) {
        this.ignoreElementContentWhitespace = value;
    }

    public void setNamespaceAware(boolean value) {
        this.namespaceAware = value;
    }

    private String resolvePredefinedOrCharacterEntity(String entityName) {
        if (entityName.startsWith("#x")) {
            return resolveCharacterReference(entityName.substring(2), 16);
        }
        if (entityName.startsWith("#")) {
            return resolveCharacterReference(entityName.substring(1), 10);
        }
        if ("lt".equals(entityName)) {
            return "<";
        }
        if ("gt".equals(entityName)) {
            return ">";
        }
        if ("amp".equals(entityName)) {
            return "&";
        }
        if ("apos".equals(entityName)) {
            return "'";
        }
        if ("quot".equals(entityName)) {
            return "\"";
        }
        return null;
    }

    private String resolveCharacterReference(String value, int base) {
        String str;
        try {
            int codePoint = Integer.parseInt(value, base);
            if (Character.isBmpCodePoint(codePoint)) {
                str = String.valueOf((char) codePoint);
            } else {
                char[] surrogatePair = Character.toChars(codePoint);
                str = new String(surrogatePair);
            }
            return str;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
