package mf.org.apache.xerces.impl;

import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import java.io.IOException;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLResourceIdentifierImpl;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;

public abstract class XMLScanner implements XMLComponent {
    protected static final boolean DEBUG_ATTR_NORMALIZATION = false;
    protected static final String ENTITY_MANAGER = "http://apache.org/xml/properties/internal/entity-manager";
    protected static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    protected static final String NOTIFY_CHAR_REFS = "http://apache.org/xml/features/scanner/notify-char-refs";
    protected static final String PARSER_SETTINGS = "http://apache.org/xml/features/internal/parser-settings";
    protected static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    protected static final String VALIDATION = "http://xml.org/sax/features/validation";
    protected int fEntityDepth;
    protected XMLEntityManager fEntityManager;
    protected XMLEntityScanner fEntityScanner;
    protected XMLErrorReporter fErrorReporter;
    protected boolean fNamespaces;
    protected boolean fReportEntity;
    protected boolean fScanningAttribute;
    protected SymbolTable fSymbolTable;
    protected static final String fVersionSymbol = PluginDescriptorBuilder.VALUE_VERSION.intern();
    protected static final String fEncodingSymbol = "encoding".intern();
    protected static final String fStandaloneSymbol = "standalone".intern();
    protected static final String fAmpSymbol = "amp".intern();
    protected static final String fLtSymbol = "lt".intern();
    protected static final String fGtSymbol = "gt".intern();
    protected static final String fQuotSymbol = "quot".intern();
    protected static final String fAposSymbol = "apos".intern();
    protected boolean fValidation = false;
    protected boolean fNotifyCharRefs = false;
    protected boolean fParserSettings = true;
    protected String fCharRefLiteral = null;
    private final XMLString fString = new XMLString();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();
    private final XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();
    private final XMLStringBuffer fStringBuffer3 = new XMLStringBuffer();
    protected final XMLResourceIdentifierImpl fResourceIdentifier = new XMLResourceIdentifierImpl();

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        try {
            this.fParserSettings = componentManager.getFeature(PARSER_SETTINGS);
        } catch (XMLConfigurationException e) {
            this.fParserSettings = true;
        }
        if (!this.fParserSettings) {
            init();
            return;
        }
        this.fSymbolTable = (SymbolTable) componentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        this.fErrorReporter = (XMLErrorReporter) componentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        this.fEntityManager = (XMLEntityManager) componentManager.getProperty(ENTITY_MANAGER);
        try {
            this.fValidation = componentManager.getFeature(VALIDATION);
        } catch (XMLConfigurationException e2) {
            this.fValidation = false;
        }
        try {
            this.fNamespaces = componentManager.getFeature(NAMESPACES);
        } catch (XMLConfigurationException e3) {
            this.fNamespaces = true;
        }
        try {
            this.fNotifyCharRefs = componentManager.getFeature(NOTIFY_CHAR_REFS);
        } catch (XMLConfigurationException e4) {
            this.fNotifyCharRefs = false;
        }
        init();
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();
            if (suffixLength == Constants.SYMBOL_TABLE_PROPERTY.length() && propertyId.endsWith(Constants.SYMBOL_TABLE_PROPERTY)) {
                this.fSymbolTable = (SymbolTable) value;
                return;
            }
            if (suffixLength == Constants.ERROR_REPORTER_PROPERTY.length() && propertyId.endsWith(Constants.ERROR_REPORTER_PROPERTY)) {
                this.fErrorReporter = (XMLErrorReporter) value;
            } else if (suffixLength == Constants.ENTITY_MANAGER_PROPERTY.length() && propertyId.endsWith(Constants.ENTITY_MANAGER_PROPERTY)) {
                this.fEntityManager = (XMLEntityManager) value;
            }
        }
    }

    @Override
    public void setFeature(String featureId, boolean value) throws XMLConfigurationException {
        if (VALIDATION.equals(featureId)) {
            this.fValidation = value;
        } else if (NOTIFY_CHAR_REFS.equals(featureId)) {
            this.fNotifyCharRefs = value;
        }
    }

    public boolean getFeature(String featureId) throws XMLConfigurationException {
        if (VALIDATION.equals(featureId)) {
            return this.fValidation;
        }
        if (NOTIFY_CHAR_REFS.equals(featureId)) {
            return this.fNotifyCharRefs;
        }
        throw new XMLConfigurationException((short) 0, featureId);
    }

    protected void reset() {
        init();
        this.fValidation = true;
        this.fNotifyCharRefs = false;
    }

    protected void scanXMLDeclOrTextDecl(boolean scanningTextDecl, String[] pseudoAttributeValues) throws IOException, XNIException {
        String str;
        String encoding;
        String standalone;
        int state;
        String version = null;
        String encoding2 = null;
        String standalone2 = null;
        int STATE_VERSION = 0;
        int state2 = 0;
        boolean dataFoundForTarget = false;
        boolean sawSpace = this.fEntityScanner.skipDeclSpaces();
        XMLEntityManager.ScannedEntity currEnt = this.fEntityManager.getCurrentEntity();
        boolean currLiteral = currEnt.literal;
        currEnt.literal = false;
        while (true) {
            int STATE_VERSION2 = STATE_VERSION;
            if (this.fEntityScanner.peekChar() == 63) {
                if (currLiteral) {
                    currEnt.literal = true;
                }
                if (scanningTextDecl && state2 != 3) {
                    reportFatalError("MorePseudoAttributes", null);
                }
                if (scanningTextDecl) {
                    if (!dataFoundForTarget && encoding2 == null) {
                        reportFatalError("EncodingDeclRequired", null);
                    }
                } else if (!dataFoundForTarget && version == null) {
                    reportFatalError("VersionInfoRequired", null);
                }
                if (!this.fEntityScanner.skipChar(63)) {
                    reportFatalError("XMLDeclUnterminated", null);
                }
                if (!this.fEntityScanner.skipChar(62)) {
                    reportFatalError("XMLDeclUnterminated", null);
                }
                pseudoAttributeValues[0] = version;
                pseudoAttributeValues[1] = encoding2;
                pseudoAttributeValues[2] = standalone2;
                return;
            }
            dataFoundForTarget = true;
            String name = scanPseudoAttribute(scanningTextDecl, this.fString);
            switch (state2) {
                case 0:
                    if (name != fVersionSymbol) {
                        str = null;
                        if (name == fEncodingSymbol) {
                            if (!scanningTextDecl) {
                                reportFatalError("VersionInfoRequired", null);
                            }
                            if (!sawSpace) {
                                reportFatalError(scanningTextDecl ? "SpaceRequiredBeforeEncodingInTextDecl" : "SpaceRequiredBeforeEncodingInXMLDecl", null);
                            }
                            encoding = this.fString.toString();
                            int state3 = scanningTextDecl ? 3 : 2;
                            state2 = state3;
                            encoding2 = encoding;
                        } else if (!scanningTextDecl) {
                            reportFatalError("VersionInfoRequired", null);
                        } else {
                            reportFatalError("EncodingDeclRequired", null);
                        }
                    } else {
                        if (!sawSpace) {
                            reportFatalError(scanningTextDecl ? "SpaceRequiredBeforeVersionInTextDecl" : "SpaceRequiredBeforeVersionInXMLDecl", null);
                        }
                        String version2 = this.fString.toString();
                        if (versionSupported(version2)) {
                            str = null;
                        } else {
                            str = null;
                            reportFatalError(getVersionNotSupportedKey(), new Object[]{version2});
                        }
                        state2 = 1;
                        version = version2;
                    }
                    break;
                case 1:
                    if (name == fEncodingSymbol) {
                        if (!sawSpace) {
                            reportFatalError(scanningTextDecl ? "SpaceRequiredBeforeEncodingInTextDecl" : "SpaceRequiredBeforeEncodingInXMLDecl", null);
                        }
                        encoding = this.fString.toString();
                        int state4 = scanningTextDecl ? 3 : 2;
                        state2 = state4;
                        str = null;
                        encoding2 = encoding;
                    } else if (!scanningTextDecl && name == fStandaloneSymbol) {
                        if (!sawSpace) {
                            reportFatalError("SpaceRequiredBeforeStandalone", null);
                        }
                        standalone = this.fString.toString();
                        state = 3;
                        if (!standalone.equals("yes") && !standalone.equals("no")) {
                            reportFatalError("SDDeclInvalid", new Object[]{standalone});
                        }
                        state2 = state;
                        str = null;
                        standalone2 = standalone;
                    } else {
                        reportFatalError("EncodingDeclRequired", null);
                        str = null;
                    }
                    break;
                case 2:
                    if (name != fStandaloneSymbol) {
                        reportFatalError("EncodingDeclRequired", null);
                        str = null;
                    } else {
                        if (!sawSpace) {
                            reportFatalError("SpaceRequiredBeforeStandalone", null);
                        }
                        standalone = this.fString.toString();
                        state = 3;
                        if (!standalone.equals("yes") && !standalone.equals("no")) {
                            reportFatalError("SDDeclInvalid", new Object[]{standalone});
                        }
                        state2 = state;
                        str = null;
                        standalone2 = standalone;
                    }
                    break;
                default:
                    str = null;
                    reportFatalError("NoMorePseudoAttributes", null);
                    break;
            }
            sawSpace = this.fEntityScanner.skipDeclSpaces();
            STATE_VERSION = STATE_VERSION2;
        }
    }

    public String scanPseudoAttribute(boolean scanningTextDecl, XMLString value) throws IOException, XNIException {
        String name = this.fEntityScanner.scanName();
        XMLEntityManager.print(this.fEntityManager.getCurrentEntity());
        if (name == null) {
            reportFatalError("PseudoAttrNameExpected", null);
        }
        this.fEntityScanner.skipDeclSpaces();
        if (!this.fEntityScanner.skipChar(61)) {
            reportFatalError(scanningTextDecl ? "EqRequiredInTextDecl" : "EqRequiredInXMLDecl", new Object[]{name});
        }
        this.fEntityScanner.skipDeclSpaces();
        int quote = this.fEntityScanner.peekChar();
        if (quote != 39 && quote != 34) {
            reportFatalError(scanningTextDecl ? "QuoteRequiredInTextDecl" : "QuoteRequiredInXMLDecl", new Object[]{name});
        }
        this.fEntityScanner.scanChar();
        int c = this.fEntityScanner.scanLiteral(quote, value);
        if (c != quote) {
            this.fStringBuffer2.clear();
            do {
                this.fStringBuffer2.append(value);
                if (c != -1) {
                    if (c == 38 || c == 37 || c == 60 || c == 93) {
                        this.fStringBuffer2.append((char) this.fEntityScanner.scanChar());
                    } else if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(this.fStringBuffer2);
                    } else if (isInvalidLiteral(c)) {
                        String key = scanningTextDecl ? "InvalidCharInTextDecl" : "InvalidCharInXMLDecl";
                        reportFatalError(key, new Object[]{Integer.toString(c, 16)});
                        this.fEntityScanner.scanChar();
                    }
                }
                c = this.fEntityScanner.scanLiteral(quote, value);
            } while (c != quote);
            this.fStringBuffer2.append(value);
            value.setValues(this.fStringBuffer2);
        }
        if (!this.fEntityScanner.skipChar(quote)) {
            reportFatalError(scanningTextDecl ? "CloseQuoteMissingInTextDecl" : "CloseQuoteMissingInXMLDecl", new Object[]{name});
        }
        return name;
    }

    protected void scanPI() throws IOException, XNIException {
        String target;
        this.fReportEntity = false;
        if (this.fNamespaces) {
            target = this.fEntityScanner.scanNCName();
        } else {
            target = this.fEntityScanner.scanName();
        }
        if (target == null) {
            reportFatalError("PITargetRequired", null);
        }
        scanPIData(target, this.fString);
        this.fReportEntity = true;
    }

    protected void scanPIData(String target, XMLString data) throws IOException, XNIException {
        if (target.length() == 3) {
            char c0 = Character.toLowerCase(target.charAt(0));
            char c1 = Character.toLowerCase(target.charAt(1));
            char c2 = Character.toLowerCase(target.charAt(2));
            if (c0 == 'x' && c1 == 'm' && c2 == 'l') {
                reportFatalError("ReservedPITarget", null);
            }
        }
        if (!this.fEntityScanner.skipSpaces()) {
            if (this.fEntityScanner.skipString("?>")) {
                data.clear();
                return;
            }
            if (this.fNamespaces && this.fEntityScanner.peekChar() == 58) {
                this.fEntityScanner.scanChar();
                XMLStringBuffer colonName = new XMLStringBuffer(target);
                colonName.append(':');
                String str = this.fEntityScanner.scanName();
                if (str != null) {
                    colonName.append(str);
                }
                reportFatalError("ColonNotLegalWithNS", new Object[]{colonName.toString()});
                this.fEntityScanner.skipSpaces();
            } else {
                reportFatalError("SpaceRequiredInPI", null);
            }
        }
        this.fStringBuffer.clear();
        if (this.fEntityScanner.scanData("?>", this.fStringBuffer)) {
            do {
                int c = this.fEntityScanner.peekChar();
                if (c != -1) {
                    if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(this.fStringBuffer);
                    } else if (isInvalidLiteral(c)) {
                        reportFatalError("InvalidCharInPI", new Object[]{Integer.toHexString(c)});
                        this.fEntityScanner.scanChar();
                    }
                }
            } while (this.fEntityScanner.scanData("?>", this.fStringBuffer));
        }
        data.setValues(this.fStringBuffer);
    }

    protected void scanComment(XMLStringBuffer text) throws IOException, XNIException {
        text.clear();
        while (this.fEntityScanner.scanData("--", text)) {
            int c = this.fEntityScanner.peekChar();
            if (c != -1) {
                if (XMLChar.isHighSurrogate(c)) {
                    scanSurrogates(text);
                } else if (isInvalidLiteral(c)) {
                    reportFatalError("InvalidCharInComment", new Object[]{Integer.toHexString(c)});
                    this.fEntityScanner.scanChar();
                }
            }
        }
        if (!this.fEntityScanner.skipChar(62)) {
            reportFatalError("DashDashInComment", null);
        }
    }

    protected boolean scanAttributeValue(XMLString value, XMLString nonNormalizedValue, String atName, boolean checkEntities, String eleName) throws IOException, XNIException {
        int i;
        int quote = this.fEntityScanner.peekChar();
        char c = '\'';
        if (quote != 39 && quote != 34) {
            reportFatalError("OpenQuoteExpected", new Object[]{eleName, atName});
        }
        this.fEntityScanner.scanChar();
        int entityDepth = this.fEntityDepth;
        int c2 = this.fEntityScanner.scanLiteral(quote, value);
        int fromIndex = 0;
        int i2 = -1;
        if (c2 == quote) {
            int iIsUnchangedByNormalization = isUnchangedByNormalization(value);
            fromIndex = iIsUnchangedByNormalization;
            if (iIsUnchangedByNormalization == -1) {
                nonNormalizedValue.setValues(value);
                int cquote = this.fEntityScanner.scanChar();
                if (cquote != quote) {
                    reportFatalError("CloseQuoteExpected", new Object[]{eleName, atName});
                }
                return true;
            }
        }
        this.fStringBuffer2.clear();
        this.fStringBuffer2.append(value);
        normalizeWhitespace(value, fromIndex);
        if (c2 != quote) {
            this.fScanningAttribute = true;
            this.fStringBuffer.clear();
            while (true) {
                this.fStringBuffer.append(value);
                if (c2 == 38) {
                    this.fEntityScanner.skipChar(38);
                    if (entityDepth == this.fEntityDepth) {
                        this.fStringBuffer2.append('&');
                    }
                    if (this.fEntityScanner.skipChar(35)) {
                        if (entityDepth == this.fEntityDepth) {
                            this.fStringBuffer2.append('#');
                        }
                        int ch = scanCharReferenceValue(this.fStringBuffer, this.fStringBuffer2);
                        if (ch != i2) {
                        }
                        i = i2;
                        c2 = this.fEntityScanner.scanLiteral(quote, value);
                        if (entityDepth == this.fEntityDepth) {
                            this.fStringBuffer2.append(value);
                        }
                        normalizeWhitespace(value);
                        if (c2 != quote && entityDepth == this.fEntityDepth) {
                            break;
                        }
                        i2 = i;
                        c = '\'';
                    } else {
                        String entityName = this.fEntityScanner.scanName();
                        if (entityName == null) {
                            reportFatalError("NameRequiredInReference", null);
                        } else if (entityDepth == this.fEntityDepth) {
                            this.fStringBuffer2.append(entityName);
                        }
                        if (!this.fEntityScanner.skipChar(59)) {
                            reportFatalError("SemicolonRequiredInReference", new Object[]{entityName});
                        } else if (entityDepth == this.fEntityDepth) {
                            this.fStringBuffer2.append(';');
                        }
                        if (entityName == fAmpSymbol) {
                            this.fStringBuffer.append('&');
                        } else if (entityName == fAposSymbol) {
                            this.fStringBuffer.append(c);
                        } else if (entityName == fLtSymbol) {
                            this.fStringBuffer.append('<');
                        } else if (entityName == fGtSymbol) {
                            this.fStringBuffer.append('>');
                        } else if (entityName == fQuotSymbol) {
                            this.fStringBuffer.append('\"');
                        } else if (this.fEntityManager.isExternalEntity(entityName)) {
                            reportFatalError("ReferenceToExternalEntity", new Object[]{entityName});
                        } else {
                            if (!this.fEntityManager.isDeclaredEntity(entityName)) {
                                if (checkEntities) {
                                    if (this.fValidation) {
                                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "EntityNotDeclared", new Object[]{entityName}, (short) 1);
                                    }
                                } else {
                                    reportFatalError("EntityNotDeclared", new Object[]{entityName});
                                }
                            }
                            this.fEntityManager.startEntity(entityName, true);
                        }
                        i = -1;
                        c2 = this.fEntityScanner.scanLiteral(quote, value);
                        if (entityDepth == this.fEntityDepth) {
                        }
                        normalizeWhitespace(value);
                        if (c2 != quote) {
                        }
                        i2 = i;
                        c = '\'';
                    }
                } else {
                    if (c2 == 60) {
                        reportFatalError("LessthanInAttValue", new Object[]{eleName, atName});
                        this.fEntityScanner.scanChar();
                        if (entityDepth == this.fEntityDepth) {
                            this.fStringBuffer2.append((char) c2);
                        }
                    } else {
                        if (c2 == 37 || c2 == 93) {
                            i = -1;
                            this.fEntityScanner.scanChar();
                            this.fStringBuffer.append((char) c2);
                            if (entityDepth == this.fEntityDepth) {
                                this.fStringBuffer2.append((char) c2);
                            }
                        } else if (c2 == 10 || c2 == 13) {
                            i = -1;
                            this.fEntityScanner.scanChar();
                            this.fStringBuffer.append(' ');
                            if (entityDepth == this.fEntityDepth) {
                                this.fStringBuffer2.append('\n');
                            }
                        } else if (c2 != -1 && XMLChar.isHighSurrogate(c2)) {
                            this.fStringBuffer3.clear();
                            if (scanSurrogates(this.fStringBuffer3)) {
                                this.fStringBuffer.append(this.fStringBuffer3);
                                if (entityDepth == this.fEntityDepth) {
                                    this.fStringBuffer2.append(this.fStringBuffer3);
                                }
                            }
                        } else {
                            i = -1;
                            if (c2 != -1 && isInvalidLiteral(c2)) {
                                reportFatalError("InvalidCharInAttValue", new Object[]{eleName, atName, Integer.toString(c2, 16)});
                                this.fEntityScanner.scanChar();
                                if (entityDepth == this.fEntityDepth) {
                                    this.fStringBuffer2.append((char) c2);
                                }
                            }
                        }
                        c2 = this.fEntityScanner.scanLiteral(quote, value);
                        if (entityDepth == this.fEntityDepth) {
                        }
                        normalizeWhitespace(value);
                        if (c2 != quote) {
                        }
                        i2 = i;
                        c = '\'';
                    }
                    i = -1;
                    c2 = this.fEntityScanner.scanLiteral(quote, value);
                    if (entityDepth == this.fEntityDepth) {
                    }
                    normalizeWhitespace(value);
                    if (c2 != quote) {
                    }
                    i2 = i;
                    c = '\'';
                }
            }
            this.fStringBuffer.append(value);
            value.setValues(this.fStringBuffer);
            this.fScanningAttribute = false;
        }
        nonNormalizedValue.setValues(this.fStringBuffer2);
        int cquote2 = this.fEntityScanner.scanChar();
        if (cquote2 != quote) {
            reportFatalError("CloseQuoteExpected", new Object[]{eleName, atName});
        }
        return nonNormalizedValue.equals(value.ch, value.offset, value.length);
    }

    protected void scanExternalID(String[] identifiers, boolean optionalSystemId) throws IOException, XNIException {
        String systemId = null;
        String publicId = null;
        if (this.fEntityScanner.skipString("PUBLIC")) {
            if (!this.fEntityScanner.skipSpaces()) {
                reportFatalError("SpaceRequiredAfterPUBLIC", null);
            }
            scanPubidLiteral(this.fString);
            publicId = this.fString.toString();
            if (!this.fEntityScanner.skipSpaces() && !optionalSystemId) {
                reportFatalError("SpaceRequiredBetweenPublicAndSystem", null);
            }
        }
        if (publicId != null || this.fEntityScanner.skipString("SYSTEM")) {
            if (publicId == null && !this.fEntityScanner.skipSpaces()) {
                reportFatalError("SpaceRequiredAfterSYSTEM", null);
            }
            int quote = this.fEntityScanner.peekChar();
            if (quote != 39 && quote != 34) {
                if (publicId != null && optionalSystemId) {
                    identifiers[0] = null;
                    identifiers[1] = publicId;
                    return;
                }
                reportFatalError("QuoteRequiredInSystemID", null);
            }
            this.fEntityScanner.scanChar();
            XMLString ident = this.fString;
            if (this.fEntityScanner.scanLiteral(quote, ident) != quote) {
                this.fStringBuffer.clear();
                do {
                    this.fStringBuffer.append(ident);
                    int c = this.fEntityScanner.peekChar();
                    if (XMLChar.isMarkup(c) || c == 93) {
                        this.fStringBuffer.append((char) this.fEntityScanner.scanChar());
                    } else if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(this.fStringBuffer);
                    } else if (isInvalidLiteral(c)) {
                        reportFatalError("InvalidCharInSystemID", new Object[]{Integer.toHexString(c)});
                        this.fEntityScanner.scanChar();
                    }
                } while (this.fEntityScanner.scanLiteral(quote, ident) != quote);
                this.fStringBuffer.append(ident);
                ident = this.fStringBuffer;
            }
            systemId = ident.toString();
            if (!this.fEntityScanner.skipChar(quote)) {
                reportFatalError("SystemIDUnterminated", null);
            }
        }
        identifiers[0] = systemId;
        identifiers[1] = publicId;
    }

    protected boolean scanPubidLiteral(XMLString literal) throws IOException, XNIException {
        int quote = this.fEntityScanner.scanChar();
        if (quote != 39 && quote != 34) {
            reportFatalError("QuoteRequiredInPublicID", null);
            return false;
        }
        this.fStringBuffer.clear();
        boolean skipSpace = true;
        boolean dataok = true;
        while (true) {
            int c = this.fEntityScanner.scanChar();
            if (c == 32 || c == 10 || c == 13) {
                if (!skipSpace) {
                    this.fStringBuffer.append(' ');
                    skipSpace = true;
                }
            } else {
                if (c == quote) {
                    if (skipSpace) {
                        this.fStringBuffer.length--;
                    }
                    literal.setValues(this.fStringBuffer);
                    return dataok;
                }
                if (XMLChar.isPubid(c)) {
                    this.fStringBuffer.append((char) c);
                    skipSpace = false;
                } else {
                    if (c == -1) {
                        reportFatalError("PublicIDUnterminated", null);
                        return false;
                    }
                    dataok = false;
                    reportFatalError("InvalidCharInPublicID", new Object[]{Integer.toHexString(c)});
                }
            }
        }
    }

    protected void normalizeWhitespace(XMLString value) {
        int end = value.offset + value.length;
        for (int i = value.offset; i < end; i++) {
            if (value.ch[i] < ' ') {
                value.ch[i] = ' ';
            }
        }
    }

    protected void normalizeWhitespace(XMLString value, int fromIndex) {
        int end = value.offset + value.length;
        for (int i = value.offset + fromIndex; i < end; i++) {
            if (value.ch[i] < ' ') {
                value.ch[i] = ' ';
            }
        }
    }

    protected int isUnchangedByNormalization(XMLString value) {
        int end = value.offset + value.length;
        for (int i = value.offset; i < end; i++) {
            if (value.ch[i] < ' ') {
                return i - value.offset;
            }
        }
        return -1;
    }

    public void startEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        this.fEntityDepth++;
        this.fEntityScanner = this.fEntityManager.getEntityScanner();
    }

    public void endEntity(String name, Augmentations augs) throws XNIException {
        this.fEntityDepth--;
    }

    protected int scanCharReferenceValue(XMLStringBuffer buf, XMLStringBuffer buf2) throws IOException, XNIException {
        boolean digit;
        boolean digit2;
        boolean hex = false;
        if (this.fEntityScanner.skipChar(120)) {
            if (buf2 != null) {
                buf2.append('x');
            }
            hex = true;
            this.fStringBuffer3.clear();
            int c = this.fEntityScanner.peekChar();
            if ((c >= 48 && c <= 57) || (c >= 97 && c <= 102) || (c >= 65 && c <= 70)) {
                if (buf2 != null) {
                    buf2.append((char) c);
                }
                this.fEntityScanner.scanChar();
                this.fStringBuffer3.append((char) c);
                do {
                    int c2 = this.fEntityScanner.peekChar();
                    digit2 = (c2 >= 48 && c2 <= 57) || (c2 >= 97 && c2 <= 102) || (c2 >= 65 && c2 <= 70);
                    if (digit2) {
                        if (buf2 != null) {
                            buf2.append((char) c2);
                        }
                        this.fEntityScanner.scanChar();
                        this.fStringBuffer3.append((char) c2);
                    }
                } while (digit2);
            } else {
                reportFatalError("HexdigitRequiredInCharRef", null);
            }
        } else {
            this.fStringBuffer3.clear();
            int c3 = this.fEntityScanner.peekChar();
            if (c3 >= 48 && c3 <= 57) {
                if (buf2 != null) {
                    buf2.append((char) c3);
                }
                this.fEntityScanner.scanChar();
                this.fStringBuffer3.append((char) c3);
                do {
                    int c4 = this.fEntityScanner.peekChar();
                    digit = c4 >= 48 && c4 <= 57;
                    if (digit) {
                        if (buf2 != null) {
                            buf2.append((char) c4);
                        }
                        this.fEntityScanner.scanChar();
                        this.fStringBuffer3.append((char) c4);
                    }
                } while (digit);
            } else {
                reportFatalError("DigitRequiredInCharRef", null);
            }
        }
        boolean digit3 = hex;
        if (!this.fEntityScanner.skipChar(59)) {
            reportFatalError("SemicolonRequiredInCharRef", null);
        }
        if (buf2 != null) {
            buf2.append(';');
        }
        int value = -1;
        try {
            value = Integer.parseInt(this.fStringBuffer3.toString(), digit3 ? 16 : 10);
            if (isInvalid(value)) {
                StringBuffer errorBuf = new StringBuffer(this.fStringBuffer3.length + 1);
                if (digit3) {
                    errorBuf.append('x');
                }
                errorBuf.append(this.fStringBuffer3.ch, this.fStringBuffer3.offset, this.fStringBuffer3.length);
                reportFatalError("InvalidCharRef", new Object[]{errorBuf.toString()});
            }
        } catch (NumberFormatException e) {
            StringBuffer errorBuf2 = new StringBuffer(this.fStringBuffer3.length + 1);
            if (digit3) {
                errorBuf2.append('x');
            }
            errorBuf2.append(this.fStringBuffer3.ch, this.fStringBuffer3.offset, this.fStringBuffer3.length);
            reportFatalError("InvalidCharRef", new Object[]{errorBuf2.toString()});
        }
        if (!XMLChar.isSupplemental(value)) {
            buf.append((char) value);
        } else {
            buf.append(XMLChar.highSurrogate(value));
            buf.append(XMLChar.lowSurrogate(value));
        }
        if (this.fNotifyCharRefs && value != -1) {
            StringBuilder sb = new StringBuilder("#");
            sb.append(digit3 ? "x" : "");
            sb.append(this.fStringBuffer3.toString());
            String literal = sb.toString();
            if (!this.fScanningAttribute) {
                this.fCharRefLiteral = literal;
            }
        }
        return value;
    }

    protected boolean isInvalid(int value) {
        return XMLChar.isInvalid(value);
    }

    protected boolean isInvalidLiteral(int value) {
        return XMLChar.isInvalid(value);
    }

    protected boolean isValidNameChar(int value) {
        return XMLChar.isName(value);
    }

    protected boolean isValidNameStartChar(int value) {
        return XMLChar.isNameStart(value);
    }

    protected boolean isValidNCName(int value) {
        return XMLChar.isNCName(value);
    }

    protected boolean isValidNameStartHighSurrogate(int value) {
        return false;
    }

    protected boolean versionSupported(String version) {
        return version.equals("1.0");
    }

    protected String getVersionNotSupportedKey() {
        return "VersionNotSupported";
    }

    protected boolean scanSurrogates(XMLStringBuffer buf) throws IOException, XNIException {
        int high = this.fEntityScanner.scanChar();
        int low = this.fEntityScanner.peekChar();
        if (!XMLChar.isLowSurrogate(low)) {
            reportFatalError("InvalidCharInContent", new Object[]{Integer.toString(high, 16)});
            return false;
        }
        this.fEntityScanner.scanChar();
        int c = XMLChar.supplemental((char) high, (char) low);
        if (isInvalid(c)) {
            reportFatalError("InvalidCharInContent", new Object[]{Integer.toString(c, 16)});
            return false;
        }
        buf.append((char) high);
        buf.append((char) low);
        return true;
    }

    protected void reportFatalError(String msgId, Object[] args) throws XNIException {
        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", msgId, args, (short) 2);
    }

    private void init() {
        this.fEntityScanner = null;
        this.fEntityDepth = 0;
        this.fReportEntity = true;
        this.fResourceIdentifier.clear();
    }
}
