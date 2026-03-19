package mf.org.apache.xml.serialize;

import java.io.IOException;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import org.xml.sax.SAXException;

public class XML11Serializer extends XMLSerializer {
    protected boolean fDOML1 = false;
    protected int fNamespaceCounter = 1;
    protected boolean fNamespaces = false;

    public XML11Serializer() {
        this._format.setVersion("1.1");
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        try {
            ElementState state = content();
            if (!state.inCData && !state.doCData) {
                if (state.preserveSpace) {
                    int saveIndent = this._printer.getNextIndent();
                    this._printer.setNextIndent(0);
                    printText(chars, start, length, true, state.unescaped);
                    this._printer.setNextIndent(saveIndent);
                    return;
                }
                printText(chars, start, length, false, state.unescaped);
                return;
            }
            if (!state.inCData) {
                this._printer.printText("<![CDATA[");
                state.inCData = true;
            }
            int saveIndent2 = this._printer.getNextIndent();
            this._printer.setNextIndent(0);
            int end = start + length;
            int index = start;
            while (index < end) {
                char ch = chars[index];
                if (ch == ']' && index + 2 < end && chars[index + 1] == ']' && chars[index + 2] == '>') {
                    this._printer.printText("]]]]><![CDATA[>");
                    index += 2;
                } else if (!XML11Char.isXML11Valid(ch)) {
                    index++;
                    if (index < end) {
                        surrogates(ch, chars[index], true);
                    } else {
                        fatalError("The character '" + ch + "' is an invalid XML character");
                    }
                } else if (this._encodingInfo.isPrintable(ch) && XML11Char.isXML11ValidLiteral(ch)) {
                    this._printer.printText(ch);
                } else {
                    this._printer.printText("]]>&#x");
                    this._printer.printText(Integer.toHexString(ch));
                    this._printer.printText(";<![CDATA[");
                }
                index++;
            }
            this._printer.setNextIndent(saveIndent2);
        } catch (IOException except) {
            throw new SAXException(except);
        }
    }

    @Override
    protected void printEscaped(String source) throws IOException {
        int length = source.length();
        int i = 0;
        while (i < length) {
            int ch = source.charAt(i);
            if (!XML11Char.isXML11Valid(ch)) {
                i++;
                if (i < length) {
                    surrogates(ch, source.charAt(i), false);
                } else {
                    fatalError("The character '" + ((char) ch) + "' is an invalid XML character");
                }
            } else if (ch == 10 || ch == 13 || ch == 9 || ch == 133 || ch == 8232) {
                printHex(ch);
            } else if (ch == 60) {
                this._printer.printText("&lt;");
            } else if (ch == 38) {
                this._printer.printText("&amp;");
            } else if (ch == 34) {
                this._printer.printText("&quot;");
            } else if (ch >= 32 && this._encodingInfo.isPrintable((char) ch)) {
                this._printer.printText((char) ch);
            } else {
                printHex(ch);
            }
            i++;
        }
    }

    @Override
    protected final void printCDATAText(String text) throws IOException {
        int length = text.length();
        int index = 0;
        while (index < length) {
            char ch = text.charAt(index);
            if (ch == ']' && index + 2 < length && text.charAt(index + 1) == ']' && text.charAt(index + 2) == '>') {
                if (this.fDOMErrorHandler != null) {
                    if ((this.features & 16) == 0 && (this.features & 2) == 0) {
                        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "EndingCDATA", null);
                        modifyDOMError(msg, (short) 3, null, this.fCurrentNode);
                        boolean continueProcess = this.fDOMErrorHandler.handleError(this.fDOMError);
                        if (!continueProcess) {
                            throw new IOException();
                        }
                    } else {
                        String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "SplittingCDATA", null);
                        modifyDOMError(msg2, (short) 1, null, this.fCurrentNode);
                        this.fDOMErrorHandler.handleError(this.fDOMError);
                    }
                }
                this._printer.printText("]]]]><![CDATA[>");
                index += 2;
            } else if (!XML11Char.isXML11Valid(ch)) {
                index++;
                if (index < length) {
                    surrogates(ch, text.charAt(index), true);
                } else {
                    fatalError("The character '" + ch + "' is an invalid XML character");
                }
            } else if (this._encodingInfo.isPrintable(ch) && XML11Char.isXML11ValidLiteral(ch)) {
                this._printer.printText(ch);
            } else {
                this._printer.printText("]]>&#x");
                this._printer.printText(Integer.toHexString(ch));
                this._printer.printText(";<![CDATA[");
            }
            index++;
        }
    }

    @Override
    protected final void printXMLChar(int ch) throws IOException {
        if (ch == 13 || ch == 133 || ch == 8232) {
            printHex(ch);
            return;
        }
        if (ch == 60) {
            this._printer.printText("&lt;");
            return;
        }
        if (ch == 38) {
            this._printer.printText("&amp;");
            return;
        }
        if (ch == 62) {
            this._printer.printText("&gt;");
        } else if (this._encodingInfo.isPrintable((char) ch) && XML11Char.isXML11ValidLiteral(ch)) {
            this._printer.printText((char) ch);
        } else {
            printHex(ch);
        }
    }

    @Override
    protected final void surrogates(int high, int low, boolean inContent) throws IOException {
        if (XMLChar.isHighSurrogate(high)) {
            if (!XMLChar.isLowSurrogate(low)) {
                fatalError("The character '" + ((char) low) + "' is an invalid XML character");
                return;
            }
            int supplemental = XMLChar.supplemental((char) high, (char) low);
            if (!XML11Char.isXML11Valid(supplemental)) {
                fatalError("The character '" + ((char) supplemental) + "' is an invalid XML character");
                return;
            }
            if (inContent && content().inCData) {
                this._printer.printText("]]>&#x");
                this._printer.printText(Integer.toHexString(supplemental));
                this._printer.printText(";<![CDATA[");
                return;
            }
            printHex(supplemental);
            return;
        }
        fatalError("The character '" + ((char) high) + "' is an invalid XML character");
    }

    @Override
    protected void printText(String text, boolean preserveSpace, boolean unescaped) throws IOException {
        int length = text.length();
        if (preserveSpace) {
            int index = 0;
            while (index < length) {
                char ch = text.charAt(index);
                if (!XML11Char.isXML11Valid(ch)) {
                    index++;
                    if (index < length) {
                        surrogates(ch, text.charAt(index), true);
                    } else {
                        fatalError("The character '" + ch + "' is an invalid XML character");
                    }
                } else if (unescaped && XML11Char.isXML11ValidLiteral(ch)) {
                    this._printer.printText(ch);
                } else {
                    printXMLChar(ch);
                }
                index++;
            }
            return;
        }
        int index2 = 0;
        while (index2 < length) {
            char ch2 = text.charAt(index2);
            if (!XML11Char.isXML11Valid(ch2)) {
                index2++;
                if (index2 < length) {
                    surrogates(ch2, text.charAt(index2), true);
                } else {
                    fatalError("The character '" + ch2 + "' is an invalid XML character");
                }
            } else if (unescaped && XML11Char.isXML11ValidLiteral(ch2)) {
                this._printer.printText(ch2);
            } else {
                printXMLChar(ch2);
            }
            index2++;
        }
    }

    @Override
    protected void printText(char[] chars, int start, int start2, boolean preserveSpace, boolean unescaped) throws IOException {
        if (preserveSpace) {
            while (true) {
                int length = start2 - 1;
                if (start2 <= 0) {
                    return;
                }
                int start3 = start + 1;
                char ch = chars[start];
                if (XML11Char.isXML11Valid(ch)) {
                    if (unescaped && XML11Char.isXML11ValidLiteral(ch)) {
                        this._printer.printText(ch);
                    } else {
                        printXMLChar(ch);
                    }
                    start = start3;
                    start2 = length;
                } else {
                    int length2 = length - 1;
                    if (length > 0) {
                        surrogates(ch, chars[start3], true);
                        start = start3 + 1;
                    } else {
                        fatalError("The character '" + ch + "' is an invalid XML character");
                        start = start3;
                    }
                    start2 = length2;
                }
            }
        } else {
            while (true) {
                int length3 = start2 - 1;
                if (start2 <= 0) {
                    return;
                }
                int start4 = start + 1;
                char ch2 = chars[start];
                if (XML11Char.isXML11Valid(ch2)) {
                    if (unescaped && XML11Char.isXML11ValidLiteral(ch2)) {
                        this._printer.printText(ch2);
                    } else {
                        printXMLChar(ch2);
                    }
                    start = start4;
                    start2 = length3;
                } else {
                    int length4 = length3 - 1;
                    if (length3 > 0) {
                        surrogates(ch2, chars[start4], true);
                        start = start4 + 1;
                    } else {
                        fatalError("The character '" + ch2 + "' is an invalid XML character");
                        start = start4;
                    }
                    start2 = length4;
                }
            }
        }
    }

    @Override
    public boolean reset() {
        super.reset();
        return true;
    }
}
