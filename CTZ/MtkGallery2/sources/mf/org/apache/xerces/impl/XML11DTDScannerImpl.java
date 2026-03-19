package mf.org.apache.xerces.impl;

import java.io.IOException;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;

public class XML11DTDScannerImpl extends XMLDTDScannerImpl {
    private final XMLStringBuffer fStringBuffer;

    public XML11DTDScannerImpl() {
        this.fStringBuffer = new XMLStringBuffer();
    }

    public XML11DTDScannerImpl(SymbolTable symbolTable, XMLErrorReporter errorReporter, XMLEntityManager entityManager) {
        super(symbolTable, errorReporter, entityManager);
        this.fStringBuffer = new XMLStringBuffer();
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
        return !XML11Char.isXML11Valid(value);
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
