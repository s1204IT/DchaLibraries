package org.xmlpull.v1.sax2;

import android.icu.text.PluralRules;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class Driver implements Locator, XMLReader, Attributes {
    protected static final String APACHE_DYNAMIC_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/dynamic";
    protected static final String APACHE_SCHEMA_VALIDATION_FEATURE = "http://apache.org/xml/features/validation/schema";
    protected static final String DECLARATION_HANDLER_PROPERTY = "http://xml.org/sax/properties/declaration-handler";
    protected static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";
    protected static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    protected static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    protected static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
    protected ContentHandler contentHandler = new DefaultHandler();
    protected ErrorHandler errorHandler = new DefaultHandler();
    protected XmlPullParser pp;
    protected String systemId;

    public Driver() throws XmlPullParserException {
        XmlPullParserFactory xmlPullParserFactoryNewInstance = XmlPullParserFactory.newInstance();
        xmlPullParserFactoryNewInstance.setNamespaceAware(true);
        this.pp = xmlPullParserFactoryNewInstance.newPullParser();
    }

    public Driver(XmlPullParser xmlPullParser) throws XmlPullParserException {
        this.pp = xmlPullParser;
    }

    @Override
    public int getLength() {
        return this.pp.getAttributeCount();
    }

    @Override
    public String getURI(int i) {
        return this.pp.getAttributeNamespace(i);
    }

    @Override
    public String getLocalName(int i) {
        return this.pp.getAttributeName(i);
    }

    @Override
    public String getQName(int i) {
        String attributePrefix = this.pp.getAttributePrefix(i);
        if (attributePrefix != null) {
            return attributePrefix + ':' + this.pp.getAttributeName(i);
        }
        return this.pp.getAttributeName(i);
    }

    @Override
    public String getType(int i) {
        return this.pp.getAttributeType(i);
    }

    @Override
    public String getValue(int i) {
        return this.pp.getAttributeValue(i);
    }

    @Override
    public int getIndex(String str, String str2) {
        for (int i = 0; i < this.pp.getAttributeCount(); i++) {
            if (this.pp.getAttributeNamespace(i).equals(str) && this.pp.getAttributeName(i).equals(str2)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getIndex(String str) {
        for (int i = 0; i < this.pp.getAttributeCount(); i++) {
            if (this.pp.getAttributeName(i).equals(str)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getType(String str, String str2) {
        for (int i = 0; i < this.pp.getAttributeCount(); i++) {
            if (this.pp.getAttributeNamespace(i).equals(str) && this.pp.getAttributeName(i).equals(str2)) {
                return this.pp.getAttributeType(i);
            }
        }
        return null;
    }

    @Override
    public String getType(String str) {
        for (int i = 0; i < this.pp.getAttributeCount(); i++) {
            if (this.pp.getAttributeName(i).equals(str)) {
                return this.pp.getAttributeType(i);
            }
        }
        return null;
    }

    @Override
    public String getValue(String str, String str2) {
        return this.pp.getAttributeValue(str, str2);
    }

    @Override
    public String getValue(String str) {
        return this.pp.getAttributeValue(null, str);
    }

    @Override
    public String getPublicId() {
        return null;
    }

    @Override
    public String getSystemId() {
        return this.systemId;
    }

    @Override
    public int getLineNumber() {
        return this.pp.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return this.pp.getColumnNumber();
    }

    @Override
    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (NAMESPACES_FEATURE.equals(str)) {
            return this.pp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
        }
        if (NAMESPACE_PREFIXES_FEATURE.equals(str)) {
            return this.pp.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES);
        }
        if (VALIDATION_FEATURE.equals(str)) {
            return this.pp.getFeature(XmlPullParser.FEATURE_VALIDATION);
        }
        return this.pp.getFeature(str);
    }

    @Override
    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (NAMESPACES_FEATURE.equals(str)) {
                this.pp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, z);
            } else if (NAMESPACE_PREFIXES_FEATURE.equals(str)) {
                if (this.pp.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES) != z) {
                    this.pp.setFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, z);
                }
            } else if (VALIDATION_FEATURE.equals(str)) {
                this.pp.setFeature(XmlPullParser.FEATURE_VALIDATION, z);
            } else {
                this.pp.setFeature(str, z);
            }
        } catch (XmlPullParserException e) {
        }
    }

    @Override
    public Object getProperty(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (DECLARATION_HANDLER_PROPERTY.equals(str) || LEXICAL_HANDLER_PROPERTY.equals(str)) {
            return null;
        }
        return this.pp.getProperty(str);
    }

    @Override
    public void setProperty(String str, Object obj) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (DECLARATION_HANDLER_PROPERTY.equals(str)) {
            throw new SAXNotSupportedException("not supported setting property " + str);
        }
        if (LEXICAL_HANDLER_PROPERTY.equals(str)) {
            throw new SAXNotSupportedException("not supported setting property " + str);
        }
        try {
            this.pp.setProperty(str, obj);
        } catch (XmlPullParserException e) {
            throw new SAXNotSupportedException("not supported set property " + str + PluralRules.KEYWORD_RULE_SEPARATOR + e);
        }
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
    }

    @Override
    public EntityResolver getEntityResolver() {
        return null;
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
    }

    @Override
    public DTDHandler getDTDHandler() {
        return null;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.contentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        this.systemId = inputSource.getSystemId();
        this.contentHandler.setDocumentLocator(this);
        Reader characterStream = inputSource.getCharacterStream();
        try {
            if (characterStream == null) {
                InputStream byteStream = inputSource.getByteStream();
                String encoding = inputSource.getEncoding();
                if (byteStream == null) {
                    this.systemId = inputSource.getSystemId();
                    if (this.systemId == null) {
                        this.errorHandler.fatalError(new SAXParseException("null source systemId", this));
                        return;
                    }
                    try {
                        byteStream = new URL(this.systemId).openStream();
                    } catch (MalformedURLException e) {
                        try {
                            byteStream = new FileInputStream(this.systemId);
                        } catch (FileNotFoundException e2) {
                            this.errorHandler.fatalError(new SAXParseException("could not open file with systemId " + this.systemId, this, e2));
                            return;
                        }
                    }
                }
                this.pp.setInput(byteStream, encoding);
            } else {
                this.pp.setInput(characterStream);
            }
            try {
                this.contentHandler.startDocument();
                this.pp.next();
                if (this.pp.getEventType() != 2) {
                    this.errorHandler.fatalError(new SAXParseException("expected start tag not" + this.pp.getPositionDescription(), this));
                    return;
                }
                parseSubTree(this.pp);
                this.contentHandler.endDocument();
            } catch (XmlPullParserException e3) {
                this.errorHandler.fatalError(new SAXParseException("parsing initialization error: " + e3, this, e3));
            }
        } catch (XmlPullParserException e4) {
            this.errorHandler.fatalError(new SAXParseException("parsing initialization error: " + e4, this, e4));
        }
    }

    @Override
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
    }

    public void parseSubTree(XmlPullParser xmlPullParser) throws SAXException, IOException {
        String string;
        String string2;
        this.pp = xmlPullParser;
        boolean feature = xmlPullParser.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
        try {
            int next = 2;
            if (xmlPullParser.getEventType() != 2) {
                throw new SAXException("start tag must be read before skiping subtree" + xmlPullParser.getPositionDescription());
            }
            int[] iArr = new int[2];
            StringBuilder sb = new StringBuilder(16);
            int depth = xmlPullParser.getDepth() - 1;
            do {
                switch (next) {
                    case 1:
                        break;
                    case 2:
                        if (feature) {
                            int depth2 = xmlPullParser.getDepth() - 1;
                            int namespaceCount = xmlPullParser.getNamespaceCount(depth2 + 1);
                            for (int namespaceCount2 = depth > depth2 ? xmlPullParser.getNamespaceCount(depth2) : 0; namespaceCount2 < namespaceCount; namespaceCount2++) {
                                this.contentHandler.startPrefixMapping(xmlPullParser.getNamespacePrefix(namespaceCount2), xmlPullParser.getNamespaceUri(namespaceCount2));
                            }
                            String name = xmlPullParser.getName();
                            String prefix = xmlPullParser.getPrefix();
                            if (prefix != null) {
                                sb.setLength(0);
                                sb.append(prefix);
                                sb.append(':');
                                sb.append(name);
                            }
                            String namespace = xmlPullParser.getNamespace();
                            if (prefix == null) {
                                string = name;
                            } else {
                                string = sb.toString();
                            }
                            startElement(namespace, name, string);
                        } else {
                            startElement(xmlPullParser.getNamespace(), xmlPullParser.getName(), xmlPullParser.getName());
                        }
                        next = xmlPullParser.next();
                        break;
                    case 3:
                        if (feature) {
                            String name2 = xmlPullParser.getName();
                            String prefix2 = xmlPullParser.getPrefix();
                            if (prefix2 != null) {
                                sb.setLength(0);
                                sb.append(prefix2);
                                sb.append(':');
                                sb.append(name2);
                            }
                            ContentHandler contentHandler = this.contentHandler;
                            String namespace2 = xmlPullParser.getNamespace();
                            if (prefix2 != null) {
                                string2 = name2;
                            } else {
                                string2 = sb.toString();
                            }
                            contentHandler.endElement(namespace2, name2, string2);
                            int namespaceCount3 = depth > xmlPullParser.getDepth() ? xmlPullParser.getNamespaceCount(xmlPullParser.getDepth()) : 0;
                            for (int namespaceCount4 = xmlPullParser.getNamespaceCount(xmlPullParser.getDepth() - 1) - 1; namespaceCount4 >= namespaceCount3; namespaceCount4--) {
                                this.contentHandler.endPrefixMapping(xmlPullParser.getNamespacePrefix(namespaceCount4));
                            }
                        } else {
                            this.contentHandler.endElement(xmlPullParser.getNamespace(), xmlPullParser.getName(), xmlPullParser.getName());
                        }
                        next = xmlPullParser.next();
                        break;
                    case 4:
                        this.contentHandler.characters(xmlPullParser.getTextCharacters(iArr), iArr[0], iArr[1]);
                        next = xmlPullParser.next();
                        break;
                    default:
                        next = xmlPullParser.next();
                        break;
                }
            } while (xmlPullParser.getDepth() > depth);
        } catch (XmlPullParserException e) {
            SAXParseException sAXParseException = new SAXParseException("parsing error: " + e, this, e);
            e.printStackTrace();
            this.errorHandler.fatalError(sAXParseException);
        }
    }

    protected void startElement(String str, String str2, String str3) throws SAXException {
        this.contentHandler.startElement(str, str2, str3, this);
    }
}
