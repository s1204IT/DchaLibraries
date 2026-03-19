package mf.org.apache.xerces.impl;

import java.io.IOException;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;

public class XML11DocumentScannerImpl extends XMLDocumentScannerImpl {
    private final XMLString fString = new XMLString();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();
    private final XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();
    private final XMLStringBuffer fStringBuffer3 = new XMLStringBuffer();

    @Override
    protected int scanContent() throws IOException, XNIException {
        XMLString content = this.fString;
        int c = this.fEntityScanner.scanContent(content);
        if (c == 13 || c == 133 || c == 8232) {
            this.fEntityScanner.scanChar();
            this.fStringBuffer.clear();
            this.fStringBuffer.append(this.fString);
            this.fStringBuffer.append((char) c);
            content = this.fStringBuffer;
            c = -1;
        }
        if (this.fDocumentHandler != null && content.length > 0) {
            this.fDocumentHandler.characters(content, null);
        }
        if (c == 93 && this.fString.length == 0) {
            this.fStringBuffer.clear();
            this.fStringBuffer.append((char) this.fEntityScanner.scanChar());
            this.fInScanContent = true;
            if (this.fEntityScanner.skipChar(93)) {
                this.fStringBuffer.append(']');
                while (this.fEntityScanner.skipChar(93)) {
                    this.fStringBuffer.append(']');
                }
                if (this.fEntityScanner.skipChar(62)) {
                    reportFatalError("CDEndInContent", null);
                }
            }
            if (this.fDocumentHandler != null && this.fStringBuffer.length != 0) {
                this.fDocumentHandler.characters(this.fStringBuffer, null);
            }
            this.fInScanContent = false;
            return -1;
        }
        return c;
    }

    @Override
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
                        } else if (c2 == 10 || c2 == 13 || c2 == 133 || c2 == 8232) {
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

    @Override
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
            if (c == 32 || c == 10 || c == 13 || c == 133 || c == 8232) {
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

    @Override
    protected void normalizeWhitespace(XMLString value) {
        int end = value.offset + value.length;
        for (int i = value.offset; i < end; i++) {
            if (XMLChar.isSpace(value.ch[i])) {
                value.ch[i] = ' ';
            }
        }
    }

    @Override
    protected void normalizeWhitespace(XMLString value, int fromIndex) {
        int end = value.offset + value.length;
        for (int i = value.offset + fromIndex; i < end; i++) {
            if (XMLChar.isSpace(value.ch[i])) {
                value.ch[i] = ' ';
            }
        }
    }

    @Override
    protected int isUnchangedByNormalization(XMLString value) {
        int end = value.offset + value.length;
        for (int i = value.offset; i < end; i++) {
            if (XMLChar.isSpace(value.ch[i])) {
                return i - value.offset;
            }
        }
        return -1;
    }

    @Override
    protected boolean isInvalid(int value) {
        return XML11Char.isXML11Invalid(value);
    }

    @Override
    protected boolean isInvalidLiteral(int value) {
        return !XML11Char.isXML11ValidLiteral(value);
    }

    @Override
    protected boolean isValidNameChar(int value) {
        return XML11Char.isXML11Name(value);
    }

    @Override
    protected boolean isValidNameStartChar(int value) {
        return XML11Char.isXML11NameStart(value);
    }

    @Override
    protected boolean isValidNCName(int value) {
        return XML11Char.isXML11NCName(value);
    }

    @Override
    protected boolean isValidNameStartHighSurrogate(int value) {
        return XML11Char.isXML11NameHighSurrogate(value);
    }

    @Override
    protected boolean versionSupported(String version) {
        return version.equals("1.1") || version.equals("1.0");
    }

    @Override
    protected String getVersionNotSupportedKey() {
        return "VersionNotSupported11";
    }
}
