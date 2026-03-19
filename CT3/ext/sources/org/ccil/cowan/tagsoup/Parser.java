package org.ccil.cowan.tagsoup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public class Parser extends DefaultHandler implements ScanHandler, XMLReader, LexicalHandler {
    public static final String CDATAElementsFeature = "http://www.ccil.org/~cowan/tagsoup/features/cdata-elements";
    public static final String XML11Feature = "http://xml.org/sax/features/xml-1.1";
    public static final String autoDetectorProperty = "http://www.ccil.org/~cowan/tagsoup/properties/auto-detector";
    public static final String bogonsEmptyFeature = "http://www.ccil.org/~cowan/tagsoup/features/bogons-empty";
    public static final String defaultAttributesFeature = "http://www.ccil.org/~cowan/tagsoup/features/default-attributes";
    public static final String externalGeneralEntitiesFeature = "http://xml.org/sax/features/external-general-entities";
    public static final String externalParameterEntitiesFeature = "http://xml.org/sax/features/external-parameter-entities";
    public static final String ignorableWhitespaceFeature = "http://www.ccil.org/~cowan/tagsoup/features/ignorable-whitespace";
    public static final String ignoreBogonsFeature = "http://www.ccil.org/~cowan/tagsoup/features/ignore-bogons";
    public static final String isStandaloneFeature = "http://xml.org/sax/features/is-standalone";
    public static final String lexicalHandlerParameterEntitiesFeature = "http://xml.org/sax/features/lexical-handler/parameter-entities";
    public static final String lexicalHandlerProperty = "http://xml.org/sax/properties/lexical-handler";
    public static final String namespacePrefixesFeature = "http://xml.org/sax/features/namespace-prefixes";
    public static final String namespacesFeature = "http://xml.org/sax/features/namespaces";
    public static final String resolveDTDURIsFeature = "http://xml.org/sax/features/resolve-dtd-uris";
    public static final String restartElementsFeature = "http://www.ccil.org/~cowan/tagsoup/features/restart-elements";
    public static final String rootBogonsFeature = "http://www.ccil.org/~cowan/tagsoup/features/root-bogons";
    public static final String scannerProperty = "http://www.ccil.org/~cowan/tagsoup/properties/scanner";
    public static final String schemaProperty = "http://www.ccil.org/~cowan/tagsoup/properties/schema";
    public static final String stringInterningFeature = "http://xml.org/sax/features/string-interning";
    public static final String translateColonsFeature = "http://www.ccil.org/~cowan/tagsoup/features/translate-colons";
    public static final String unicodeNormalizationCheckingFeature = "http://xml.org/sax/features/unicode-normalization-checking";
    public static final String useAttributes2Feature = "http://xml.org/sax/features/use-attributes2";
    public static final String useEntityResolver2Feature = "http://xml.org/sax/features/use-entity-resolver2";
    public static final String useLocator2Feature = "http://xml.org/sax/features/use-locator2";
    public static final String validationFeature = "http://xml.org/sax/features/validation";
    public static final String xmlnsURIsFeature = "http://xml.org/sax/features/xmlns-uris";
    private String theAttributeName;
    private AutoDetector theAutoDetector;
    private char[] theCommentBuffer;
    private boolean theDoctypeIsPresent;
    private String theDoctypeName;
    private String theDoctypePublicId;
    private String theDoctypeSystemId;
    private int theEntity;
    private Element theNewElement;
    private Element thePCDATA;
    private String thePITarget;
    private Element theSaved;
    private Scanner theScanner;
    private Schema theSchema;
    private Element theStack;
    private boolean virginStack;
    private static boolean DEFAULT_NAMESPACES = true;
    private static boolean DEFAULT_IGNORE_BOGONS = false;
    private static boolean DEFAULT_BOGONS_EMPTY = false;
    private static boolean DEFAULT_ROOT_BOGONS = true;
    private static boolean DEFAULT_DEFAULT_ATTRIBUTES = true;
    private static boolean DEFAULT_TRANSLATE_COLONS = false;
    private static boolean DEFAULT_RESTART_ELEMENTS = true;
    private static boolean DEFAULT_IGNORABLE_WHITESPACE = false;
    private static boolean DEFAULT_CDATA_ELEMENTS = true;
    private static char[] etagchars = {'<', '/', '>'};
    private static String legal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-'()+,./:=?;!*#@$_%";
    private ContentHandler theContentHandler = this;
    private LexicalHandler theLexicalHandler = this;
    private DTDHandler theDTDHandler = this;
    private ErrorHandler theErrorHandler = this;
    private EntityResolver theEntityResolver = this;
    private boolean namespaces = DEFAULT_NAMESPACES;
    private boolean ignoreBogons = DEFAULT_IGNORE_BOGONS;
    private boolean bogonsEmpty = DEFAULT_BOGONS_EMPTY;
    private boolean rootBogons = DEFAULT_ROOT_BOGONS;
    private boolean defaultAttributes = DEFAULT_DEFAULT_ATTRIBUTES;
    private boolean translateColons = DEFAULT_TRANSLATE_COLONS;
    private boolean restartElements = DEFAULT_RESTART_ELEMENTS;
    private boolean ignorableWhitespace = DEFAULT_IGNORABLE_WHITESPACE;
    private boolean CDATAElements = DEFAULT_CDATA_ELEMENTS;
    private HashMap theFeatures = new HashMap();

    public Parser() {
        this.theFeatures.put(namespacesFeature, truthValue(DEFAULT_NAMESPACES));
        this.theFeatures.put(namespacePrefixesFeature, Boolean.FALSE);
        this.theFeatures.put(externalGeneralEntitiesFeature, Boolean.FALSE);
        this.theFeatures.put(externalParameterEntitiesFeature, Boolean.FALSE);
        this.theFeatures.put(isStandaloneFeature, Boolean.FALSE);
        this.theFeatures.put(lexicalHandlerParameterEntitiesFeature, Boolean.FALSE);
        this.theFeatures.put(resolveDTDURIsFeature, Boolean.TRUE);
        this.theFeatures.put(stringInterningFeature, Boolean.TRUE);
        this.theFeatures.put(useAttributes2Feature, Boolean.FALSE);
        this.theFeatures.put(useLocator2Feature, Boolean.FALSE);
        this.theFeatures.put(useEntityResolver2Feature, Boolean.FALSE);
        this.theFeatures.put(validationFeature, Boolean.FALSE);
        this.theFeatures.put(xmlnsURIsFeature, Boolean.FALSE);
        this.theFeatures.put(xmlnsURIsFeature, Boolean.FALSE);
        this.theFeatures.put(XML11Feature, Boolean.FALSE);
        this.theFeatures.put(ignoreBogonsFeature, truthValue(DEFAULT_IGNORE_BOGONS));
        this.theFeatures.put(bogonsEmptyFeature, truthValue(DEFAULT_BOGONS_EMPTY));
        this.theFeatures.put(rootBogonsFeature, truthValue(DEFAULT_ROOT_BOGONS));
        this.theFeatures.put(defaultAttributesFeature, truthValue(DEFAULT_DEFAULT_ATTRIBUTES));
        this.theFeatures.put(translateColonsFeature, truthValue(DEFAULT_TRANSLATE_COLONS));
        this.theFeatures.put(restartElementsFeature, truthValue(DEFAULT_RESTART_ELEMENTS));
        this.theFeatures.put(ignorableWhitespaceFeature, truthValue(DEFAULT_IGNORABLE_WHITESPACE));
        this.theFeatures.put(CDATAElementsFeature, truthValue(DEFAULT_CDATA_ELEMENTS));
        this.theNewElement = null;
        this.theAttributeName = null;
        this.theDoctypeIsPresent = false;
        this.theDoctypePublicId = null;
        this.theDoctypeSystemId = null;
        this.theDoctypeName = null;
        this.thePITarget = null;
        this.theStack = null;
        this.theSaved = null;
        this.thePCDATA = null;
        this.theEntity = 0;
        this.virginStack = true;
        this.theCommentBuffer = new char[2000];
    }

    private static Boolean truthValue(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        Boolean b = (Boolean) this.theFeatures.get(name);
        if (b == null) {
            throw new SAXNotRecognizedException("Unknown feature " + name);
        }
        return b.booleanValue();
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        Boolean b = (Boolean) this.theFeatures.get(name);
        if (b == null) {
            throw new SAXNotRecognizedException("Unknown feature " + name);
        }
        if (value) {
            this.theFeatures.put(name, Boolean.TRUE);
        } else {
            this.theFeatures.put(name, Boolean.FALSE);
        }
        if (name.equals(namespacesFeature)) {
            this.namespaces = value;
            return;
        }
        if (name.equals(ignoreBogonsFeature)) {
            this.ignoreBogons = value;
            return;
        }
        if (name.equals(bogonsEmptyFeature)) {
            this.bogonsEmpty = value;
            return;
        }
        if (name.equals(rootBogonsFeature)) {
            this.rootBogons = value;
            return;
        }
        if (name.equals(defaultAttributesFeature)) {
            this.defaultAttributes = value;
            return;
        }
        if (name.equals(translateColonsFeature)) {
            this.translateColons = value;
            return;
        }
        if (name.equals(restartElementsFeature)) {
            this.restartElements = value;
        } else if (name.equals(ignorableWhitespaceFeature)) {
            this.ignorableWhitespace = value;
        } else if (name.equals(CDATAElementsFeature)) {
            this.CDATAElements = value;
        }
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(lexicalHandlerProperty)) {
            if (this.theLexicalHandler == this) {
                return null;
            }
            return this.theLexicalHandler;
        }
        if (name.equals(scannerProperty)) {
            return this.theScanner;
        }
        if (name.equals(schemaProperty)) {
            return this.theSchema;
        }
        if (name.equals(autoDetectorProperty)) {
            return this.theAutoDetector;
        }
        throw new SAXNotRecognizedException("Unknown property " + name);
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(lexicalHandlerProperty)) {
            if (value == null) {
                this.theLexicalHandler = this;
                return;
            } else {
                if (value instanceof LexicalHandler) {
                    this.theLexicalHandler = (LexicalHandler) value;
                    return;
                }
                throw new SAXNotSupportedException("Your lexical handler is not a LexicalHandler");
            }
        }
        if (name.equals(scannerProperty)) {
            if (value instanceof Scanner) {
                this.theScanner = (Scanner) value;
                return;
            }
            throw new SAXNotSupportedException("Your scanner is not a Scanner");
        }
        if (name.equals(schemaProperty)) {
            if (value instanceof Schema) {
                this.theSchema = (Schema) value;
                return;
            }
            throw new SAXNotSupportedException("Your schema is not a Schema");
        }
        if (name.equals(autoDetectorProperty)) {
            if (value instanceof AutoDetector) {
                this.theAutoDetector = (AutoDetector) value;
                return;
            }
            throw new SAXNotSupportedException("Your auto-detector is not an AutoDetector");
        }
        throw new SAXNotRecognizedException("Unknown property " + name);
    }

    @Override
    public void setEntityResolver(EntityResolver resolver) {
        if (resolver == null) {
            resolver = this;
        }
        this.theEntityResolver = resolver;
    }

    @Override
    public EntityResolver getEntityResolver() {
        if (this.theEntityResolver == this) {
            return null;
        }
        return this.theEntityResolver;
    }

    @Override
    public void setDTDHandler(DTDHandler handler) {
        if (handler == null) {
            handler = this;
        }
        this.theDTDHandler = handler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        if (this.theDTDHandler == this) {
            return null;
        }
        return this.theDTDHandler;
    }

    @Override
    public void setContentHandler(ContentHandler handler) {
        if (handler == null) {
            handler = this;
        }
        this.theContentHandler = handler;
    }

    @Override
    public ContentHandler getContentHandler() {
        if (this.theContentHandler == this) {
            return null;
        }
        return this.theContentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        if (handler == null) {
            handler = this;
        }
        this.theErrorHandler = handler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        if (this.theErrorHandler == this) {
            return null;
        }
        return this.theErrorHandler;
    }

    @Override
    public void parse(InputSource input) throws SAXException, IOException {
        setup();
        Reader r = getReader(input);
        this.theContentHandler.startDocument();
        this.theScanner.resetDocumentLocator(input.getPublicId(), input.getSystemId());
        if (this.theScanner instanceof Locator) {
            this.theContentHandler.setDocumentLocator((Locator) this.theScanner);
        }
        if (!this.theSchema.getURI().equals("")) {
            this.theContentHandler.startPrefixMapping(this.theSchema.getPrefix(), this.theSchema.getURI());
        }
        this.theScanner.scan(r, this);
    }

    @Override
    public void parse(String systemid) throws SAXException, IOException {
        parse(new InputSource(systemid));
    }

    private void setup() {
        if (this.theSchema == null) {
            this.theSchema = new HTMLSchema();
        }
        if (this.theScanner == null) {
            this.theScanner = new HTMLScanner();
        }
        if (this.theAutoDetector == null) {
            this.theAutoDetector = new AutoDetector() {
                @Override
                public Reader autoDetectingReader(InputStream i) {
                    return new InputStreamReader(i);
                }
            };
        }
        this.theStack = new Element(this.theSchema.getElementType("<root>"), this.defaultAttributes);
        this.thePCDATA = new Element(this.theSchema.getElementType("<pcdata>"), this.defaultAttributes);
        this.theNewElement = null;
        this.theAttributeName = null;
        this.thePITarget = null;
        this.theSaved = null;
        this.theEntity = 0;
        this.virginStack = true;
        this.theDoctypeSystemId = null;
        this.theDoctypePublicId = null;
        this.theDoctypeName = null;
    }

    private Reader getReader(InputSource s) throws SAXException, IOException {
        Reader r = s.getCharacterStream();
        InputStream i = s.getByteStream();
        String encoding = s.getEncoding();
        String publicid = s.getPublicId();
        String systemid = s.getSystemId();
        if (r == null) {
            if (i == null) {
                i = getInputStream(publicid, systemid);
            }
            if (encoding == null) {
                return this.theAutoDetector.autoDetectingReader(i);
            }
            try {
                return new InputStreamReader(i, encoding);
            } catch (UnsupportedEncodingException e) {
                return new InputStreamReader(i);
            }
        }
        return r;
    }

    private InputStream getInputStream(String publicid, String systemid) throws SAXException, IOException {
        URL basis = new URL("file", "", System.getProperty("user.dir") + "/.");
        URL url = new URL(basis, systemid);
        URLConnection c = url.openConnection();
        return c.getInputStream();
    }

    @Override
    public void adup(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement == null || this.theAttributeName == null) {
            return;
        }
        this.theNewElement.setAttribute(this.theAttributeName, null, this.theAttributeName);
        this.theAttributeName = null;
    }

    @Override
    public void aname(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement == null) {
            return;
        }
        this.theAttributeName = makeName(buff, offset, length).toLowerCase(Locale.ROOT);
    }

    @Override
    public void aval(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement == null || this.theAttributeName == null) {
            return;
        }
        String value = new String(buff, offset, length);
        this.theNewElement.setAttribute(this.theAttributeName, null, expandEntities(value));
        this.theAttributeName = null;
    }

    private String expandEntities(String src) {
        int refStart = -1;
        int len = src.length();
        char[] dst = new char[len];
        int i = 0;
        int dstlen = 0;
        while (i < len) {
            char ch = src.charAt(i);
            int dstlen2 = dstlen + 1;
            dst[dstlen] = ch;
            if (ch == '&' && refStart == -1) {
                refStart = dstlen2;
            } else if (refStart != -1 && !Character.isLetter(ch) && !Character.isDigit(ch) && ch != '#') {
                if (ch == ';') {
                    int ent = lookupEntity(dst, refStart, (dstlen2 - refStart) - 1);
                    if (ent > 65535) {
                        int ent2 = ent - HTMLModels.M_OPTION;
                        dst[refStart - 1] = (char) ((ent2 >> 10) + 55296);
                        dst[refStart] = (char) ((ent2 & 1023) + 56320);
                        dstlen2 = refStart + 1;
                    } else if (ent != 0) {
                        dst[refStart - 1] = (char) ent;
                        dstlen2 = refStart;
                    }
                    refStart = -1;
                } else {
                    refStart = -1;
                }
            }
            i++;
            dstlen = dstlen2;
        }
        return new String(dst, 0, dstlen);
    }

    @Override
    public void entity(char[] buff, int offset, int length) throws SAXException {
        this.theEntity = lookupEntity(buff, offset, length);
    }

    private int lookupEntity(char[] buff, int offset, int length) {
        if (length < 1) {
            return 0;
        }
        if (buff[offset] == '#') {
            if (length > 1 && (buff[offset + 1] == 'x' || buff[offset + 1] == 'X')) {
                try {
                    return Integer.parseInt(new String(buff, offset + 2, length - 2), 16);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            try {
                return Integer.parseInt(new String(buff, offset + 1, length - 1), 10);
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
        return this.theSchema.getEntity(new String(buff, offset, length));
    }

    @Override
    public void eof(char[] buff, int offset, int length) throws SAXException {
        if (this.virginStack) {
            rectify(this.thePCDATA);
        }
        while (this.theStack.next() != null) {
            pop();
        }
        if (!this.theSchema.getURI().equals("")) {
            this.theContentHandler.endPrefixMapping(this.theSchema.getPrefix());
        }
        this.theContentHandler.endDocument();
    }

    @Override
    public void etag(char[] buff, int offset, int length) throws SAXException {
        if (etag_cdata(buff, offset, length)) {
            return;
        }
        etag_basic(buff, offset, length);
    }

    public boolean etag_cdata(char[] buff, int offset, int length) throws SAXException {
        String currentName = this.theStack.name();
        if (this.CDATAElements && (this.theStack.flags() & 2) != 0) {
            boolean realTag = length == currentName.length();
            if (realTag) {
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    if (Character.toLowerCase(buff[offset + i]) == Character.toLowerCase(currentName.charAt(i))) {
                        i++;
                    } else {
                        realTag = false;
                        break;
                    }
                }
            }
            if (!realTag) {
                this.theContentHandler.characters(etagchars, 0, 2);
                this.theContentHandler.characters(buff, offset, length);
                this.theContentHandler.characters(etagchars, 2, 1);
                this.theScanner.startCDATA();
                return true;
            }
        }
        return false;
    }

    public void etag_basic(char[] buff, int offset, int length) throws SAXException {
        String name;
        this.theNewElement = null;
        if (length != 0) {
            String name2 = makeName(buff, offset, length);
            ElementType type = this.theSchema.getElementType(name2);
            if (type == null) {
                return;
            } else {
                name = type.name();
            }
        } else {
            name = this.theStack.name();
        }
        boolean inNoforce = false;
        Element sp = this.theStack;
        while (sp != null && !sp.name().equals(name)) {
            if ((sp.flags() & 4) != 0) {
                inNoforce = true;
            }
            sp = sp.next();
        }
        if (sp == null || sp.next() == null || sp.next().next() == null) {
            return;
        }
        if (inNoforce) {
            sp.preclose();
        } else {
            while (this.theStack != sp) {
                restartablyPop();
            }
            pop();
        }
        while (this.theStack.isPreclosed()) {
            pop();
        }
        restart(null);
    }

    private void restart(Element e) throws SAXException {
        while (this.theSaved != null && this.theStack.canContain(this.theSaved)) {
            if (e != null && !this.theSaved.canContain(e)) {
                return;
            }
            Element next = this.theSaved.next();
            push(this.theSaved);
            this.theSaved = next;
        }
    }

    private void pop() throws SAXException {
        if (this.theStack == null) {
            return;
        }
        String name = this.theStack.name();
        String localName = this.theStack.localName();
        String namespace = this.theStack.namespace();
        String prefix = prefixOf(name);
        if (!this.namespaces) {
            localName = "";
            namespace = "";
        }
        this.theContentHandler.endElement(namespace, localName, name);
        if (foreign(prefix, namespace)) {
            this.theContentHandler.endPrefixMapping(prefix);
        }
        Attributes atts = this.theStack.atts();
        for (int i = atts.getLength() - 1; i >= 0; i--) {
            String attNamespace = atts.getURI(i);
            String attPrefix = prefixOf(atts.getQName(i));
            if (foreign(attPrefix, attNamespace)) {
                this.theContentHandler.endPrefixMapping(attPrefix);
            }
        }
        this.theStack = this.theStack.next();
    }

    private void restartablyPop() throws SAXException {
        Element popped = this.theStack;
        pop();
        if (!this.restartElements || (popped.flags() & 1) == 0) {
            return;
        }
        popped.anonymize();
        popped.setNext(this.theSaved);
        this.theSaved = popped;
    }

    private void push(Element e) throws SAXException {
        String name = e.name();
        String localName = e.localName();
        String namespace = e.namespace();
        String prefix = prefixOf(name);
        e.clean();
        if (!this.namespaces) {
            localName = "";
            namespace = "";
        }
        if (this.virginStack && localName.equalsIgnoreCase(this.theDoctypeName)) {
            try {
                this.theEntityResolver.resolveEntity(this.theDoctypePublicId, this.theDoctypeSystemId);
            } catch (IOException e2) {
            }
        }
        if (foreign(prefix, namespace)) {
            this.theContentHandler.startPrefixMapping(prefix, namespace);
        }
        Attributes atts = e.atts();
        int len = atts.getLength();
        for (int i = 0; i < len; i++) {
            String attNamespace = atts.getURI(i);
            String attPrefix = prefixOf(atts.getQName(i));
            if (foreign(attPrefix, attNamespace)) {
                this.theContentHandler.startPrefixMapping(attPrefix, attNamespace);
            }
        }
        this.theContentHandler.startElement(namespace, localName, name, e.atts());
        e.setNext(this.theStack);
        this.theStack = e;
        this.virginStack = false;
        if (!this.CDATAElements || (this.theStack.flags() & 2) == 0) {
            return;
        }
        this.theScanner.startCDATA();
    }

    private String prefixOf(String name) {
        int i = name.indexOf(58);
        if (i == -1) {
            return "";
        }
        String prefix = name.substring(0, i);
        return prefix;
    }

    private boolean foreign(String prefix, String namespace) {
        return (prefix.equals("") || namespace.equals("") || namespace.equals(this.theSchema.getURI())) ? false : true;
    }

    @Override
    public void decl(char[] buff, int offset, int length) throws SAXException {
        String s = new String(buff, offset, length);
        String name = null;
        String systemid = null;
        String publicid = null;
        String[] v = split(s);
        if (v.length > 0 && "DOCTYPE".equalsIgnoreCase(v[0])) {
            if (this.theDoctypeIsPresent) {
                return;
            }
            this.theDoctypeIsPresent = true;
            if (v.length > 1) {
                name = v[1];
                if (v.length > 3 && "SYSTEM".equals(v[2])) {
                    systemid = v[3];
                } else if (v.length > 3 && "PUBLIC".equals(v[2])) {
                    publicid = v[3];
                    systemid = v.length > 4 ? v[4] : "";
                }
            }
        }
        String publicid2 = trimquotes(publicid);
        String systemid2 = trimquotes(systemid);
        if (name == null) {
            return;
        }
        String publicid3 = cleanPublicid(publicid2);
        this.theLexicalHandler.startDTD(name, publicid3, systemid2);
        this.theLexicalHandler.endDTD();
        this.theDoctypeName = name;
        this.theDoctypePublicId = publicid3;
        if (!(this.theScanner instanceof Locator)) {
            return;
        }
        this.theDoctypeSystemId = ((Locator) this.theScanner).getSystemId();
        try {
            this.theDoctypeSystemId = new URL(new URL(this.theDoctypeSystemId), systemid2).toString();
        } catch (Exception e) {
        }
    }

    private static String trimquotes(String in) {
        int length;
        if (in == null || (length = in.length()) == 0) {
            return in;
        }
        char s = in.charAt(0);
        char e = in.charAt(length - 1);
        if (s != e) {
            return in;
        }
        if (s == '\'' || s == '\"') {
            return in.substring(1, in.length() - 1);
        }
        return in;
    }

    private static String[] split(String val) throws IllegalArgumentException {
        String val2 = val.trim();
        if (val2.length() == 0) {
            return new String[0];
        }
        ArrayList l = new ArrayList();
        int s = 0;
        boolean sq = false;
        boolean dq = false;
        char lastc = 0;
        int len = val2.length();
        int e = 0;
        while (e < len) {
            char c = val2.charAt(e);
            if (!dq && c == '\'' && lastc != '\\') {
                sq = !sq;
                if (s < 0) {
                    s = e;
                }
            } else if (!sq && c == '\"' && lastc != '\\') {
                dq = !dq;
                if (s < 0) {
                    s = e;
                }
            } else if (!sq && !dq) {
                if (Character.isWhitespace(c)) {
                    if (s >= 0) {
                        l.add(val2.substring(s, e));
                    }
                    s = -1;
                } else if (s < 0 && c != ' ') {
                    s = e;
                }
            }
            lastc = c;
            e++;
        }
        l.add(val2.substring(s, e));
        return (String[]) l.toArray(new String[0]);
    }

    private String cleanPublicid(String src) {
        if (src == null) {
            return null;
        }
        int len = src.length();
        StringBuffer dst = new StringBuffer(len);
        boolean suppressSpace = true;
        for (int i = 0; i < len; i++) {
            char ch = src.charAt(i);
            if (legal.indexOf(ch) != -1) {
                dst.append(ch);
                suppressSpace = false;
            } else if (!suppressSpace) {
                dst.append(' ');
                suppressSpace = true;
            }
        }
        return dst.toString().trim();
    }

    @Override
    public void gi(char[] buff, int offset, int length) throws SAXException {
        String name;
        if (this.theNewElement == null && (name = makeName(buff, offset, length)) != null) {
            ElementType type = this.theSchema.getElementType(name);
            if (type == null) {
                if (this.ignoreBogons) {
                    return;
                }
                int bogonModel = this.bogonsEmpty ? 0 : -1;
                int bogonMemberOf = this.rootBogons ? -1 : Integer.MAX_VALUE;
                this.theSchema.elementType(name, bogonModel, bogonMemberOf, 0);
                if (!this.rootBogons) {
                    this.theSchema.parent(name, this.theSchema.rootElementType().name());
                }
                type = this.theSchema.getElementType(name);
            }
            this.theNewElement = new Element(type, this.defaultAttributes);
        }
    }

    @Override
    public void cdsect(char[] buff, int offset, int length) throws SAXException {
        this.theLexicalHandler.startCDATA();
        pcdata(buff, offset, length);
        this.theLexicalHandler.endCDATA();
    }

    @Override
    public void pcdata(char[] buff, int offset, int length) throws SAXException {
        if (length == 0) {
            return;
        }
        boolean allWhite = true;
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(buff[offset + i])) {
                allWhite = false;
            }
        }
        if (allWhite && !this.theStack.canContain(this.thePCDATA)) {
            if (!this.ignorableWhitespace) {
                return;
            }
            this.theContentHandler.ignorableWhitespace(buff, offset, length);
        } else {
            rectify(this.thePCDATA);
            this.theContentHandler.characters(buff, offset, length);
        }
    }

    @Override
    public void pitarget(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement != null) {
            return;
        }
        this.thePITarget = makeName(buff, offset, length).replace(':', '_');
    }

    @Override
    public void pi(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement != null || this.thePITarget == null || "xml".equalsIgnoreCase(this.thePITarget)) {
            return;
        }
        if (length > 0 && buff[length - 1] == '?') {
            length--;
        }
        this.theContentHandler.processingInstruction(this.thePITarget, new String(buff, offset, length));
        this.thePITarget = null;
    }

    @Override
    public void stagc(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement == null) {
            return;
        }
        rectify(this.theNewElement);
        if (this.theStack.model() != 0) {
            return;
        }
        etag_basic(buff, offset, length);
    }

    @Override
    public void stage(char[] buff, int offset, int length) throws SAXException {
        if (this.theNewElement == null) {
            return;
        }
        rectify(this.theNewElement);
        etag_basic(buff, offset, length);
    }

    @Override
    public void cmnt(char[] buff, int offset, int length) throws SAXException {
        this.theLexicalHandler.comment(buff, offset, length);
    }

    private void rectify(Element e) throws SAXException {
        Element sp;
        ElementType parentType;
        while (true) {
            sp = this.theStack;
            while (sp != null && !sp.canContain(e)) {
                sp = sp.next();
            }
            if (sp != null || (parentType = e.parent()) == null) {
                break;
            }
            Element parent = new Element(parentType, this.defaultAttributes);
            parent.setNext(e);
            e = parent;
        }
        if (sp == null) {
            return;
        }
        while (this.theStack != sp && this.theStack != null && this.theStack.next() != null && this.theStack.next().next() != null) {
            restartablyPop();
        }
        while (e != null) {
            Element nexte = e.next();
            if (!e.name().equals("<pcdata>")) {
                push(e);
            }
            e = nexte;
            restart(nexte);
        }
        this.theNewElement = null;
    }

    @Override
    public int getEntity() {
        return this.theEntity;
    }

    private String makeName(char[] buff, int offset, int length) {
        StringBuffer dst = new StringBuffer(length + 2);
        boolean seenColon = false;
        boolean start = true;
        while (true) {
            int length2 = length;
            length = length2 - 1;
            if (length2 <= 0) {
                break;
            }
            char ch = buff[offset];
            if (Character.isLetter(ch) || ch == '_') {
                start = false;
                dst.append(ch);
            } else if (Character.isDigit(ch) || ch == '-' || ch == '.') {
                if (start) {
                    dst.append('_');
                }
                start = false;
                dst.append(ch);
            } else if (ch == ':' && !seenColon) {
                seenColon = true;
                if (start) {
                    dst.append('_');
                }
                start = true;
                if (this.translateColons) {
                    ch = '_';
                }
                dst.append(ch);
            }
            offset++;
        }
        int dstLength = dst.length();
        if (dstLength == 0 || dst.charAt(dstLength - 1) == ':') {
            dst.append('_');
        }
        return dst.toString().intern();
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void startDTD(String name, String publicid, String systemid) throws SAXException {
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }
}
