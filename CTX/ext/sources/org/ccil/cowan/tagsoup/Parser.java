package org.ccil.cowan.tagsoup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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

    private static Boolean truthValue(boolean z) {
        return z ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        Boolean bool = (Boolean) this.theFeatures.get(str);
        if (bool == null) {
            throw new SAXNotRecognizedException("Unknown feature " + str);
        }
        return bool.booleanValue();
    }

    @Override
    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (((Boolean) this.theFeatures.get(str)) == null) {
            throw new SAXNotRecognizedException("Unknown feature " + str);
        }
        if (z) {
            this.theFeatures.put(str, Boolean.TRUE);
        } else {
            this.theFeatures.put(str, Boolean.FALSE);
        }
        if (!str.equals(namespacesFeature)) {
            if (!str.equals(ignoreBogonsFeature)) {
                if (!str.equals(bogonsEmptyFeature)) {
                    if (!str.equals(rootBogonsFeature)) {
                        if (!str.equals(defaultAttributesFeature)) {
                            if (!str.equals(translateColonsFeature)) {
                                if (!str.equals(restartElementsFeature)) {
                                    if (!str.equals(ignorableWhitespaceFeature)) {
                                        if (str.equals(CDATAElementsFeature)) {
                                            this.CDATAElements = z;
                                            return;
                                        }
                                        return;
                                    }
                                    this.ignorableWhitespace = z;
                                    return;
                                }
                                this.restartElements = z;
                                return;
                            }
                            this.translateColons = z;
                            return;
                        }
                        this.defaultAttributes = z;
                        return;
                    }
                    this.rootBogons = z;
                    return;
                }
                this.bogonsEmpty = z;
                return;
            }
            this.ignoreBogons = z;
            return;
        }
        this.namespaces = z;
    }

    @Override
    public Object getProperty(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str.equals(lexicalHandlerProperty)) {
            if (this.theLexicalHandler == this) {
                return null;
            }
            return this.theLexicalHandler;
        }
        if (str.equals(scannerProperty)) {
            return this.theScanner;
        }
        if (str.equals(schemaProperty)) {
            return this.theSchema;
        }
        if (str.equals(autoDetectorProperty)) {
            return this.theAutoDetector;
        }
        throw new SAXNotRecognizedException("Unknown property " + str);
    }

    @Override
    public void setProperty(String str, Object obj) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str.equals(lexicalHandlerProperty)) {
            if (obj == null) {
                this.theLexicalHandler = this;
                return;
            } else {
                if (obj instanceof LexicalHandler) {
                    this.theLexicalHandler = (LexicalHandler) obj;
                    return;
                }
                throw new SAXNotSupportedException("Your lexical handler is not a LexicalHandler");
            }
        }
        if (str.equals(scannerProperty)) {
            if (obj instanceof Scanner) {
                this.theScanner = (Scanner) obj;
                return;
            }
            throw new SAXNotSupportedException("Your scanner is not a Scanner");
        }
        if (str.equals(schemaProperty)) {
            if (obj instanceof Schema) {
                this.theSchema = (Schema) obj;
                return;
            }
            throw new SAXNotSupportedException("Your schema is not a Schema");
        }
        if (str.equals(autoDetectorProperty)) {
            if (obj instanceof AutoDetector) {
                this.theAutoDetector = (AutoDetector) obj;
                return;
            }
            throw new SAXNotSupportedException("Your auto-detector is not an AutoDetector");
        }
        throw new SAXNotRecognizedException("Unknown property " + str);
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        if (entityResolver == null) {
            entityResolver = this;
        }
        this.theEntityResolver = entityResolver;
    }

    @Override
    public EntityResolver getEntityResolver() {
        if (this.theEntityResolver == this) {
            return null;
        }
        return this.theEntityResolver;
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
        if (dTDHandler == null) {
            dTDHandler = this;
        }
        this.theDTDHandler = dTDHandler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        if (this.theDTDHandler == this) {
            return null;
        }
        return this.theDTDHandler;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        if (contentHandler == null) {
            contentHandler = this;
        }
        this.theContentHandler = contentHandler;
    }

    @Override
    public ContentHandler getContentHandler() {
        if (this.theContentHandler == this) {
            return null;
        }
        return this.theContentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        if (errorHandler == null) {
            errorHandler = this;
        }
        this.theErrorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        if (this.theErrorHandler == this) {
            return null;
        }
        return this.theErrorHandler;
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        setup();
        Reader reader = getReader(inputSource);
        this.theContentHandler.startDocument();
        this.theScanner.resetDocumentLocator(inputSource.getPublicId(), inputSource.getSystemId());
        if (this.theScanner instanceof Locator) {
            this.theContentHandler.setDocumentLocator((Locator) this.theScanner);
        }
        if (!this.theSchema.getURI().equals("")) {
            this.theContentHandler.startPrefixMapping(this.theSchema.getPrefix(), this.theSchema.getURI());
        }
        this.theScanner.scan(reader, this);
    }

    @Override
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
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
                public Reader autoDetectingReader(InputStream inputStream) {
                    return new InputStreamReader(inputStream);
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

    private Reader getReader(InputSource inputSource) throws SAXException, IOException {
        Reader characterStream = inputSource.getCharacterStream();
        InputStream byteStream = inputSource.getByteStream();
        String encoding = inputSource.getEncoding();
        String publicId = inputSource.getPublicId();
        String systemId = inputSource.getSystemId();
        if (characterStream == null) {
            if (byteStream == null) {
                byteStream = getInputStream(publicId, systemId);
            }
            if (encoding == null) {
                return this.theAutoDetector.autoDetectingReader(byteStream);
            }
            try {
                return new InputStreamReader(byteStream, encoding);
            } catch (UnsupportedEncodingException e) {
                return new InputStreamReader(byteStream);
            }
        }
        return characterStream;
    }

    private InputStream getInputStream(String str, String str2) throws SAXException, IOException {
        return new URL(new URL("file", "", System.getProperty("user.dir") + "/."), str2).openConnection().getInputStream();
    }

    @Override
    public void adup(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement == null || this.theAttributeName == null) {
            return;
        }
        this.theNewElement.setAttribute(this.theAttributeName, null, this.theAttributeName);
        this.theAttributeName = null;
    }

    @Override
    public void aname(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement == null) {
            return;
        }
        this.theAttributeName = makeName(cArr, i, i2).toLowerCase(Locale.ROOT);
    }

    @Override
    public void aval(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement == null || this.theAttributeName == null) {
            return;
        }
        this.theNewElement.setAttribute(this.theAttributeName, null, expandEntities(new String(cArr, i, i2)));
        this.theAttributeName = null;
    }

    private String expandEntities(String str) {
        int length = str.length();
        char[] cArr = new char[length];
        int i = 0;
        int i2 = -1;
        for (int i3 = 0; i3 < length; i3++) {
            char cCharAt = str.charAt(i3);
            int i4 = i + 1;
            cArr[i] = cCharAt;
            if (cCharAt == '&' && i2 == -1) {
                i = i4;
                i2 = i;
            } else if (i2 == -1 || Character.isLetter(cCharAt) || Character.isDigit(cCharAt) || cCharAt == '#') {
                i = i4;
            } else if (cCharAt == ';') {
                int iLookupEntity = lookupEntity(cArr, i2, (i4 - i2) - 1);
                if (iLookupEntity > 65535) {
                    int i5 = iLookupEntity - HTMLModels.M_OPTION;
                    cArr[i2 - 1] = (char) ((i5 >> 10) + 55296);
                    cArr[i2] = (char) ((i5 & 1023) + 56320);
                    i2++;
                } else if (iLookupEntity != 0) {
                    cArr[i2 - 1] = (char) iLookupEntity;
                } else {
                    i2 = i4;
                }
                i = i2;
                i2 = -1;
            } else {
                i2 = -1;
                i = i4;
            }
        }
        return new String(cArr, 0, i);
    }

    @Override
    public void entity(char[] cArr, int i, int i2) throws SAXException {
        this.theEntity = lookupEntity(cArr, i, i2);
    }

    private int lookupEntity(char[] cArr, int i, int i2) {
        if (i2 < 1) {
            return 0;
        }
        if (cArr[i] == '#') {
            if (i2 > 1) {
                int i3 = i + 1;
                if (cArr[i3] == 'x' || cArr[i3] == 'X') {
                    try {
                        return Integer.parseInt(new String(cArr, i + 2, i2 - 2), 16);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            try {
                return Integer.parseInt(new String(cArr, i + 1, i2 - 1), 10);
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
        return this.theSchema.getEntity(new String(cArr, i, i2));
    }

    @Override
    public void eof(char[] cArr, int i, int i2) throws SAXException {
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
    public void etag(char[] cArr, int i, int i2) throws SAXException {
        if (etag_cdata(cArr, i, i2)) {
            return;
        }
        etag_basic(cArr, i, i2);
    }

    public boolean etag_cdata(char[] cArr, int i, int i2) throws SAXException {
        String strName = this.theStack.name();
        if (this.CDATAElements && (this.theStack.flags() & 2) != 0) {
            boolean z = i2 == strName.length();
            if (z) {
                int i3 = 0;
                while (true) {
                    if (i3 >= i2) {
                        break;
                    }
                    if (Character.toLowerCase(cArr[i + i3]) == Character.toLowerCase(strName.charAt(i3))) {
                        i3++;
                    } else {
                        z = false;
                        break;
                    }
                }
            }
            if (!z) {
                this.theContentHandler.characters(etagchars, 0, 2);
                this.theContentHandler.characters(cArr, i, i2);
                this.theContentHandler.characters(etagchars, 2, 1);
                this.theScanner.startCDATA();
                return true;
            }
        }
        return false;
    }

    public void etag_basic(char[] cArr, int i, int i2) throws SAXException {
        String strName;
        this.theNewElement = null;
        if (i2 != 0) {
            ElementType elementType = this.theSchema.getElementType(makeName(cArr, i, i2));
            if (elementType == null) {
                return;
            } else {
                strName = elementType.name();
            }
        } else {
            strName = this.theStack.name();
        }
        boolean z = false;
        Element next = this.theStack;
        while (next != null && !next.name().equals(strName)) {
            if ((next.flags() & 4) != 0) {
                z = true;
            }
            next = next.next();
        }
        if (next == null || next.next() == null || next.next().next() == null) {
            return;
        }
        if (z) {
            next.preclose();
        } else {
            while (this.theStack != next) {
                restartablyPop();
            }
            pop();
        }
        while (this.theStack.isPreclosed()) {
            pop();
        }
        restart(null);
    }

    private void restart(Element element) throws SAXException {
        while (this.theSaved != null && this.theStack.canContain(this.theSaved)) {
            if (element == null || this.theSaved.canContain(element)) {
                Element next = this.theSaved.next();
                push(this.theSaved);
                this.theSaved = next;
            } else {
                return;
            }
        }
    }

    private void pop() throws SAXException {
        String str;
        String str2;
        if (this.theStack == null) {
            return;
        }
        String strName = this.theStack.name();
        String strLocalName = this.theStack.localName();
        String strNamespace = this.theStack.namespace();
        String strPrefixOf = prefixOf(strName);
        if (this.namespaces) {
            str = strLocalName;
            str2 = strNamespace;
        } else {
            str2 = "";
            str = "";
        }
        this.theContentHandler.endElement(str2, str, strName);
        if (foreign(strPrefixOf, str2)) {
            this.theContentHandler.endPrefixMapping(strPrefixOf);
        }
        AttributesImpl attributesImplAtts = this.theStack.atts();
        for (int length = attributesImplAtts.getLength() - 1; length >= 0; length--) {
            String uri = attributesImplAtts.getURI(length);
            String strPrefixOf2 = prefixOf(attributesImplAtts.getQName(length));
            if (foreign(strPrefixOf2, uri)) {
                this.theContentHandler.endPrefixMapping(strPrefixOf2);
            }
        }
        this.theStack = this.theStack.next();
    }

    private void restartablyPop() throws SAXException {
        Element element = this.theStack;
        pop();
        if (this.restartElements && (element.flags() & 1) != 0) {
            element.anonymize();
            element.setNext(this.theSaved);
            this.theSaved = element;
        }
    }

    private void push(Element element) throws SAXException {
        String strName = element.name();
        String strLocalName = element.localName();
        String strNamespace = element.namespace();
        String strPrefixOf = prefixOf(strName);
        element.clean();
        if (!this.namespaces) {
            strLocalName = "";
            strNamespace = "";
        }
        if (this.virginStack && strLocalName.equalsIgnoreCase(this.theDoctypeName)) {
            try {
                this.theEntityResolver.resolveEntity(this.theDoctypePublicId, this.theDoctypeSystemId);
            } catch (IOException e) {
            }
        }
        if (foreign(strPrefixOf, strNamespace)) {
            this.theContentHandler.startPrefixMapping(strPrefixOf, strNamespace);
        }
        AttributesImpl attributesImplAtts = element.atts();
        int length = attributesImplAtts.getLength();
        for (int i = 0; i < length; i++) {
            String uri = attributesImplAtts.getURI(i);
            String strPrefixOf2 = prefixOf(attributesImplAtts.getQName(i));
            if (foreign(strPrefixOf2, uri)) {
                this.theContentHandler.startPrefixMapping(strPrefixOf2, uri);
            }
        }
        this.theContentHandler.startElement(strNamespace, strLocalName, strName, element.atts());
        element.setNext(this.theStack);
        this.theStack = element;
        this.virginStack = false;
        if (this.CDATAElements && (this.theStack.flags() & 2) != 0) {
            this.theScanner.startCDATA();
        }
    }

    private String prefixOf(String str) {
        int iIndexOf = str.indexOf(58);
        return iIndexOf != -1 ? str.substring(0, iIndexOf) : "";
    }

    private boolean foreign(String str, String str2) {
        return (str.equals("") || str2.equals("") || str2.equals(this.theSchema.getURI())) ? false : true;
    }

    @Override
    public void decl(char[] cArr, int i, int i2) throws SAXException {
        String str;
        String str2;
        String[] strArrSplit = split(new String(cArr, i, i2));
        String str3 = null;
        if (strArrSplit.length <= 0 || !"DOCTYPE".equalsIgnoreCase(strArrSplit[0])) {
            str = null;
            str2 = null;
        } else {
            if (this.theDoctypeIsPresent) {
                return;
            }
            this.theDoctypeIsPresent = true;
            if (strArrSplit.length > 1) {
                str2 = strArrSplit[1];
                if (strArrSplit.length > 3 && "SYSTEM".equals(strArrSplit[2])) {
                    str = strArrSplit[3];
                } else if (strArrSplit.length > 3 && "PUBLIC".equals(strArrSplit[2])) {
                    str3 = strArrSplit[3];
                    if (strArrSplit.length > 4) {
                        str = strArrSplit[4];
                    } else {
                        str = "";
                    }
                } else {
                    str = null;
                }
            }
        }
        String strTrimquotes = trimquotes(str3);
        String strTrimquotes2 = trimquotes(str);
        if (str2 != null) {
            String strCleanPublicid = cleanPublicid(strTrimquotes);
            this.theLexicalHandler.startDTD(str2, strCleanPublicid, strTrimquotes2);
            this.theLexicalHandler.endDTD();
            this.theDoctypeName = str2;
            this.theDoctypePublicId = strCleanPublicid;
            if (this.theScanner instanceof Locator) {
                this.theDoctypeSystemId = ((Locator) this.theScanner).getSystemId();
                try {
                    this.theDoctypeSystemId = new URL(new URL(this.theDoctypeSystemId), strTrimquotes2).toString();
                } catch (Exception e) {
                }
            }
        }
    }

    private static String trimquotes(String str) {
        int length;
        char cCharAt;
        if (str == null || (length = str.length()) == 0 || (cCharAt = str.charAt(0)) != str.charAt(length - 1)) {
            return str;
        }
        if (cCharAt == '\'' || cCharAt == '\"') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static String[] split(String str) throws IllegalArgumentException {
        String strTrim = str.trim();
        if (strTrim.length() == 0) {
            return new String[0];
        }
        ArrayList arrayList = new ArrayList();
        int length = strTrim.length();
        int i = 0;
        boolean z = false;
        int i2 = 0;
        char c = 0;
        boolean z2 = false;
        while (i < length) {
            char cCharAt = strTrim.charAt(i);
            if (!z && cCharAt == '\'' && c != '\\') {
                boolean z3 = !z2;
                if (i2 < 0) {
                    i2 = i;
                }
                z2 = z3;
            } else if (!z2 && cCharAt == '\"' && c != '\\') {
                z = !z;
                if (i2 < 0) {
                }
            } else if (!z2 && !z) {
                if (Character.isWhitespace(cCharAt)) {
                    if (i2 >= 0) {
                        arrayList.add(strTrim.substring(i2, i));
                    }
                    i2 = -1;
                } else if (i2 < 0 && cCharAt != ' ') {
                    i2 = i;
                }
            }
            i++;
            c = cCharAt;
        }
        arrayList.add(strTrim.substring(i2, i));
        return (String[]) arrayList.toArray(new String[0]);
    }

    private String cleanPublicid(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        StringBuffer stringBuffer = new StringBuffer(length);
        boolean z = true;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (legal.indexOf(cCharAt) != -1) {
                stringBuffer.append(cCharAt);
                z = false;
            } else if (!z) {
                stringBuffer.append(' ');
                z = true;
            }
        }
        return stringBuffer.toString().trim();
    }

    @Override
    public void gi(char[] cArr, int i, int i2) throws SAXException {
        String strMakeName;
        if (this.theNewElement == null && (strMakeName = makeName(cArr, i, i2)) != null) {
            ElementType elementType = this.theSchema.getElementType(strMakeName);
            if (elementType == null) {
                if (this.ignoreBogons) {
                    return;
                }
                this.theSchema.elementType(strMakeName, this.bogonsEmpty ? 0 : -1, this.rootBogons ? -1 : Integer.MAX_VALUE, 0);
                if (!this.rootBogons) {
                    this.theSchema.parent(strMakeName, this.theSchema.rootElementType().name());
                }
                elementType = this.theSchema.getElementType(strMakeName);
            }
            this.theNewElement = new Element(elementType, this.defaultAttributes);
        }
    }

    @Override
    public void cdsect(char[] cArr, int i, int i2) throws SAXException {
        this.theLexicalHandler.startCDATA();
        pcdata(cArr, i, i2);
        this.theLexicalHandler.endCDATA();
    }

    @Override
    public void pcdata(char[] cArr, int i, int i2) throws SAXException {
        if (i2 == 0) {
            return;
        }
        boolean z = true;
        for (int i3 = 0; i3 < i2; i3++) {
            if (!Character.isWhitespace(cArr[i + i3])) {
                z = false;
            }
        }
        if (z && !this.theStack.canContain(this.thePCDATA)) {
            if (this.ignorableWhitespace) {
                this.theContentHandler.ignorableWhitespace(cArr, i, i2);
            }
        } else {
            rectify(this.thePCDATA);
            this.theContentHandler.characters(cArr, i, i2);
        }
    }

    @Override
    public void pitarget(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement != null) {
            return;
        }
        this.thePITarget = makeName(cArr, i, i2).replace(':', '_');
    }

    @Override
    public void pi(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement != null || this.thePITarget == null || "xml".equalsIgnoreCase(this.thePITarget)) {
            return;
        }
        if (i2 > 0 && cArr[i2 - 1] == '?') {
            i2--;
        }
        this.theContentHandler.processingInstruction(this.thePITarget, new String(cArr, i, i2));
        this.thePITarget = null;
    }

    @Override
    public void stagc(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement == null) {
            return;
        }
        rectify(this.theNewElement);
        if (this.theStack.model() == 0) {
            etag_basic(cArr, i, i2);
        }
    }

    @Override
    public void stage(char[] cArr, int i, int i2) throws SAXException {
        if (this.theNewElement == null) {
            return;
        }
        rectify(this.theNewElement);
        etag_basic(cArr, i, i2);
    }

    @Override
    public void cmnt(char[] cArr, int i, int i2) throws SAXException {
        this.theLexicalHandler.comment(cArr, i, i2);
    }

    private void rectify(Element element) throws SAXException {
        Element next;
        ElementType elementTypeParent;
        while (true) {
            next = this.theStack;
            while (next != null && !next.canContain(element)) {
                next = next.next();
            }
            if (next != null || (elementTypeParent = element.parent()) == null) {
                break;
            }
            Element element2 = new Element(elementTypeParent, this.defaultAttributes);
            element2.setNext(element);
            element = element2;
        }
        if (next == null) {
            return;
        }
        while (this.theStack != next && this.theStack != null && this.theStack.next() != null && this.theStack.next().next() != null) {
            restartablyPop();
        }
        while (element != null) {
            Element next2 = element.next();
            if (!element.name().equals("<pcdata>")) {
                push(element);
            }
            restart(next2);
            element = next2;
        }
        this.theNewElement = null;
    }

    @Override
    public int getEntity() {
        return this.theEntity;
    }

    private String makeName(char[] cArr, int i, int i2) {
        StringBuffer stringBuffer = new StringBuffer(i2 + 2);
        boolean z = false;
        boolean z2 = true;
        while (true) {
            int i3 = i2 - 1;
            if (i2 <= 0) {
                break;
            }
            char c = cArr[i];
            if (Character.isLetter(c) || c == '_') {
                stringBuffer.append(c);
            } else if (Character.isDigit(c) || c == '-' || c == '.') {
                if (z2) {
                    stringBuffer.append('_');
                }
                stringBuffer.append(c);
            } else {
                if (c == ':' && !z) {
                    if (z2) {
                        stringBuffer.append('_');
                    }
                    if (this.translateColons) {
                        c = '_';
                    }
                    stringBuffer.append(c);
                    z2 = true;
                    z = true;
                }
                i++;
                i2 = i3;
            }
            z2 = false;
            i++;
            i2 = i3;
        }
        int length = stringBuffer.length();
        if (length == 0 || stringBuffer.charAt(length - 1) == ':') {
            stringBuffer.append('_');
        }
        return stringBuffer.toString().intern();
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void endEntity(String str) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void startDTD(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void startEntity(String str) throws SAXException {
    }
}
