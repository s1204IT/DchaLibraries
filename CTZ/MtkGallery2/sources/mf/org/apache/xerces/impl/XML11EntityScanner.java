package mf.org.apache.xerces.impl;

import java.io.IOException;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLString;

public class XML11EntityScanner extends XMLEntityScanner {
    @Override
    public int peekChar() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (this.fCurrentEntity.isExternal()) {
            if (c == '\r' || c == 133 || c == 8232) {
                return 10;
            }
            return c;
        }
        return c;
    }

    @Override
    public int scanChar() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char[] cArr = this.fCurrentEntity.ch;
        XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
        int i = scannedEntity.position;
        scannedEntity.position = i + 1;
        char c = cArr[i];
        boolean external = false;
        if (c != '\n') {
            if (c == '\r' || c == 133 || c == 8232) {
                boolean zIsExternal = this.fCurrentEntity.isExternal();
                external = zIsExternal;
                if (zIsExternal) {
                }
            }
        } else {
            this.fCurrentEntity.lineNumber++;
            this.fCurrentEntity.columnNumber = 1;
            if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = c;
                load(1, false);
            }
            if (c == '\r' && external) {
                char[] cArr2 = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                int i2 = scannedEntity2.position;
                scannedEntity2.position = i2 + 1;
                char c2 = cArr2[i2];
                if (c2 != '\n' && c2 != 133) {
                    this.fCurrentEntity.position--;
                }
            }
            c = '\n';
        }
        this.fCurrentEntity.columnNumber++;
        return c;
    }

    @Override
    public String scanNmtoken() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        while (true) {
            char ch = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (XML11Char.isXML11Name(ch)) {
                XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                int i = scannedEntity.position + 1;
                scannedEntity.position = i;
                if (i != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length = this.fCurrentEntity.position - offset;
                    if (length == this.fCurrentEntity.ch.length) {
                        char[] tmp = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp, 0, length);
                        this.fCurrentEntity.ch = tmp;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            } else {
                if (!XML11Char.isXML11NameHighSurrogate(ch)) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                int i2 = scannedEntity2.position + 1;
                scannedEntity2.position = i2;
                if (i2 == this.fCurrentEntity.count) {
                    int length2 = this.fCurrentEntity.position - offset;
                    if (length2 == this.fCurrentEntity.ch.length) {
                        char[] tmp2 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp2, 0, length2);
                        this.fCurrentEntity.ch = tmp2;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length2);
                    }
                    offset = 0;
                    if (load(length2, false)) {
                        this.fCurrentEntity.startPosition--;
                        this.fCurrentEntity.position--;
                        break;
                    }
                }
                char ch2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                if (!XMLChar.isLowSurrogate(ch2) || !XML11Char.isXML11Name(XMLChar.supplemental(ch, ch2))) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
                int i3 = scannedEntity3.position + 1;
                scannedEntity3.position = i3;
                if (i3 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length3 = this.fCurrentEntity.position - offset;
                    if (length3 == this.fCurrentEntity.ch.length) {
                        char[] tmp3 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp3, 0, length3);
                        this.fCurrentEntity.ch = tmp3;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length3);
                    }
                    offset = 0;
                    if (load(length3, false)) {
                        break;
                    }
                }
            }
        }
        int length4 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length4;
        if (length4 <= 0) {
            return null;
        }
        String symbol = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length4);
        return symbol;
    }

    @Override
    public String scanName() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        char ch = this.fCurrentEntity.ch[offset];
        if (XML11Char.isXML11NameStart(ch)) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            if (i == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.columnNumber++;
                    String symbol = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 1);
                    return symbol;
                }
            }
        } else {
            if (!XML11Char.isXML11NameHighSurrogate(ch)) {
                return null;
            }
            XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
            int i2 = scannedEntity2.position + 1;
            scannedEntity2.position = i2;
            if (i2 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.position--;
                    this.fCurrentEntity.startPosition--;
                    return null;
                }
            }
            char ch2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (!XMLChar.isLowSurrogate(ch2) || !XML11Char.isXML11NameStart(XMLChar.supplemental(ch, ch2))) {
                this.fCurrentEntity.position--;
                return null;
            }
            XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
            int i3 = scannedEntity3.position + 1;
            scannedEntity3.position = i3;
            if (i3 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                this.fCurrentEntity.ch[1] = ch2;
                offset = 0;
                if (load(2, false)) {
                    this.fCurrentEntity.columnNumber += 2;
                    String symbol2 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 2);
                    return symbol2;
                }
            }
        }
        while (true) {
            char ch3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (XML11Char.isXML11Name(ch3)) {
                XMLEntityManager.ScannedEntity scannedEntity4 = this.fCurrentEntity;
                int i4 = scannedEntity4.position + 1;
                scannedEntity4.position = i4;
                if (i4 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length = this.fCurrentEntity.position - offset;
                    if (length == this.fCurrentEntity.ch.length) {
                        char[] tmp = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp, 0, length);
                        this.fCurrentEntity.ch = tmp;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            } else {
                if (!XML11Char.isXML11NameHighSurrogate(ch3)) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity5 = this.fCurrentEntity;
                int i5 = scannedEntity5.position + 1;
                scannedEntity5.position = i5;
                if (i5 == this.fCurrentEntity.count) {
                    int length2 = this.fCurrentEntity.position - offset;
                    if (length2 == this.fCurrentEntity.ch.length) {
                        char[] tmp2 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp2, 0, length2);
                        this.fCurrentEntity.ch = tmp2;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length2);
                    }
                    offset = 0;
                    if (load(length2, false)) {
                        this.fCurrentEntity.position--;
                        this.fCurrentEntity.startPosition--;
                        break;
                    }
                }
                char ch22 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                if (!XMLChar.isLowSurrogate(ch22) || !XML11Char.isXML11Name(XMLChar.supplemental(ch3, ch22))) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity6 = this.fCurrentEntity;
                int i6 = scannedEntity6.position + 1;
                scannedEntity6.position = i6;
                if (i6 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length3 = this.fCurrentEntity.position - offset;
                    if (length3 == this.fCurrentEntity.ch.length) {
                        char[] tmp3 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp3, 0, length3);
                        this.fCurrentEntity.ch = tmp3;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length3);
                    }
                    offset = 0;
                    if (load(length3, false)) {
                        break;
                    }
                }
            }
        }
        this.fCurrentEntity.position--;
        int length4 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length4;
        if (length4 <= 0) {
            return null;
        }
        String symbol3 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length4);
        return symbol3;
    }

    @Override
    public String scanNCName() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        char ch = this.fCurrentEntity.ch[offset];
        if (XML11Char.isXML11NCNameStart(ch)) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            if (i == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.columnNumber++;
                    String symbol = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 1);
                    return symbol;
                }
            }
        } else {
            if (!XML11Char.isXML11NameHighSurrogate(ch)) {
                return null;
            }
            XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
            int i2 = scannedEntity2.position + 1;
            scannedEntity2.position = i2;
            if (i2 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.position--;
                    this.fCurrentEntity.startPosition--;
                    return null;
                }
            }
            char ch2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (!XMLChar.isLowSurrogate(ch2) || !XML11Char.isXML11NCNameStart(XMLChar.supplemental(ch, ch2))) {
                this.fCurrentEntity.position--;
                return null;
            }
            XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
            int i3 = scannedEntity3.position + 1;
            scannedEntity3.position = i3;
            if (i3 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                this.fCurrentEntity.ch[1] = ch2;
                offset = 0;
                if (load(2, false)) {
                    this.fCurrentEntity.columnNumber += 2;
                    String symbol2 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 2);
                    return symbol2;
                }
            }
        }
        while (true) {
            char ch3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (XML11Char.isXML11NCName(ch3)) {
                XMLEntityManager.ScannedEntity scannedEntity4 = this.fCurrentEntity;
                int i4 = scannedEntity4.position + 1;
                scannedEntity4.position = i4;
                if (i4 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length = this.fCurrentEntity.position - offset;
                    if (length == this.fCurrentEntity.ch.length) {
                        char[] tmp = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp, 0, length);
                        this.fCurrentEntity.ch = tmp;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            } else {
                if (!XML11Char.isXML11NameHighSurrogate(ch3)) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity5 = this.fCurrentEntity;
                int i5 = scannedEntity5.position + 1;
                scannedEntity5.position = i5;
                if (i5 == this.fCurrentEntity.count) {
                    int length2 = this.fCurrentEntity.position - offset;
                    if (length2 == this.fCurrentEntity.ch.length) {
                        char[] tmp2 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp2, 0, length2);
                        this.fCurrentEntity.ch = tmp2;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length2);
                    }
                    offset = 0;
                    if (load(length2, false)) {
                        this.fCurrentEntity.startPosition--;
                        this.fCurrentEntity.position--;
                        break;
                    }
                }
                char ch22 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                if (!XMLChar.isLowSurrogate(ch22) || !XML11Char.isXML11NCName(XMLChar.supplemental(ch3, ch22))) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity6 = this.fCurrentEntity;
                int i6 = scannedEntity6.position + 1;
                scannedEntity6.position = i6;
                if (i6 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length3 = this.fCurrentEntity.position - offset;
                    if (length3 == this.fCurrentEntity.ch.length) {
                        char[] tmp3 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset, tmp3, 0, length3);
                        this.fCurrentEntity.ch = tmp3;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length3);
                    }
                    offset = 0;
                    if (load(length3, false)) {
                        break;
                    }
                }
            }
        }
        this.fCurrentEntity.position--;
        int length4 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length4;
        if (length4 <= 0) {
            return null;
        }
        String symbol3 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length4);
        return symbol3;
    }

    @Override
    public boolean scanQName(QName qname) throws IOException {
        String localpart;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        char ch = this.fCurrentEntity.ch[offset];
        if (XML11Char.isXML11NCNameStart(ch)) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            if (i == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.columnNumber++;
                    String name = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 1);
                    qname.setValues(null, name, name, null);
                    return true;
                }
            }
        } else {
            if (!XML11Char.isXML11NameHighSurrogate(ch)) {
                return false;
            }
            XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
            int i2 = scannedEntity2.position + 1;
            scannedEntity2.position = i2;
            if (i2 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.startPosition--;
                    this.fCurrentEntity.position--;
                    return false;
                }
            }
            char ch2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (!XMLChar.isLowSurrogate(ch2) || !XML11Char.isXML11NCNameStart(XMLChar.supplemental(ch, ch2))) {
                this.fCurrentEntity.position--;
                return false;
            }
            XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
            int i3 = scannedEntity3.position + 1;
            scannedEntity3.position = i3;
            if (i3 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = ch;
                this.fCurrentEntity.ch[1] = ch2;
                offset = 0;
                if (load(2, false)) {
                    this.fCurrentEntity.columnNumber += 2;
                    String name2 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 2);
                    qname.setValues(null, name2, name2, null);
                    return true;
                }
            }
        }
        int index = -1;
        int offset2 = offset;
        int offset3 = 0;
        while (true) {
            char ch3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (XML11Char.isXML11Name(ch3)) {
                if (ch3 == ':') {
                    if (index != -1) {
                        break;
                    }
                    index = this.fCurrentEntity.position;
                }
                XMLEntityManager.ScannedEntity scannedEntity4 = this.fCurrentEntity;
                int i4 = scannedEntity4.position + 1;
                scannedEntity4.position = i4;
                if (i4 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length = this.fCurrentEntity.position - offset2;
                    if (length == this.fCurrentEntity.ch.length) {
                        char[] tmp = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset2, tmp, 0, length);
                        this.fCurrentEntity.ch = tmp;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset2, this.fCurrentEntity.ch, 0, length);
                    }
                    if (index != -1) {
                        index -= offset2;
                    }
                    offset2 = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            } else {
                if (!XML11Char.isXML11NameHighSurrogate(ch3)) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity5 = this.fCurrentEntity;
                int i5 = scannedEntity5.position + 1;
                scannedEntity5.position = i5;
                if (i5 == this.fCurrentEntity.count) {
                    int length2 = this.fCurrentEntity.position - offset2;
                    if (length2 == this.fCurrentEntity.ch.length) {
                        char[] tmp2 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset2, tmp2, 0, length2);
                        this.fCurrentEntity.ch = tmp2;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset2, this.fCurrentEntity.ch, 0, length2);
                    }
                    if (index != -1) {
                        index -= offset2;
                    }
                    offset2 = 0;
                    if (load(length2, false)) {
                        offset3 = 1;
                        this.fCurrentEntity.startPosition--;
                        this.fCurrentEntity.position--;
                        break;
                    }
                }
                char ch22 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                if (!XMLChar.isLowSurrogate(ch22) || !XML11Char.isXML11Name(XMLChar.supplemental(ch3, ch22))) {
                    break;
                }
                XMLEntityManager.ScannedEntity scannedEntity6 = this.fCurrentEntity;
                int i6 = scannedEntity6.position + 1;
                scannedEntity6.position = i6;
                if (i6 != this.fCurrentEntity.count) {
                    continue;
                } else {
                    int length3 = this.fCurrentEntity.position - offset2;
                    if (length3 == this.fCurrentEntity.ch.length) {
                        char[] tmp3 = new char[this.fCurrentEntity.ch.length << 1];
                        System.arraycopy(this.fCurrentEntity.ch, offset2, tmp3, 0, length3);
                        this.fCurrentEntity.ch = tmp3;
                    } else {
                        System.arraycopy(this.fCurrentEntity.ch, offset2, this.fCurrentEntity.ch, 0, length3);
                    }
                    if (index != -1) {
                        index -= offset2;
                    }
                    offset2 = 0;
                    if (load(length3, false)) {
                        break;
                    }
                }
            }
        }
        int length4 = this.fCurrentEntity.position - offset2;
        this.fCurrentEntity.columnNumber += length4;
        if (length4 <= 0) {
            return false;
        }
        String prefix = null;
        String rawname = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset2, length4);
        if (index != -1) {
            int prefixLength = index - offset2;
            prefix = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset2, prefixLength);
            int len = (length4 - prefixLength) - 1;
            int startLocal = index + 1;
            if (!XML11Char.isXML11NCNameStart(this.fCurrentEntity.ch[startLocal]) && (!XML11Char.isXML11NameHighSurrogate(this.fCurrentEntity.ch[startLocal]) || offset3 != 0)) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "IllegalQName", null, (short) 2);
            }
            localpart = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, index + 1, len);
        } else {
            localpart = rawname;
        }
        qname.setValues(prefix, localpart, rawname, null);
        return true;
    }

    @Override
    public int scanContent(XMLString content) throws IOException {
        int offset;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        } else if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
            this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[this.fCurrentEntity.count - 1];
            load(1, false);
            this.fCurrentEntity.position = 0;
            this.fCurrentEntity.startPosition = 0;
        }
        int offset2 = this.fCurrentEntity.position;
        char c = this.fCurrentEntity.ch[offset2];
        int newlines = 0;
        boolean external = this.fCurrentEntity.isExternal();
        if (c == '\n' || ((c == '\r' || c == 133 || c == 8232) && external)) {
            do {
                char[] cArr = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                int i = scannedEntity.position;
                scannedEntity.position = i + 1;
                char c2 = cArr[i];
                if (c2 == '\r' && external) {
                    newlines++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset2 = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    char c3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                    if (c3 == '\n' || c3 == 133) {
                        this.fCurrentEntity.position++;
                        offset2++;
                    } else {
                        newlines++;
                    }
                } else {
                    if (c2 != '\n' && ((c2 != 133 && c2 != 8232) || !external)) {
                        this.fCurrentEntity.position--;
                        break;
                    }
                    newlines++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset2 = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                }
            } while (this.fCurrentEntity.position < this.fCurrentEntity.count - 1);
            int offset3 = offset2;
            int newlines2 = newlines;
            for (int i2 = offset3; i2 < this.fCurrentEntity.position; i2++) {
                this.fCurrentEntity.ch[i2] = '\n';
            }
            int length = this.fCurrentEntity.position - offset3;
            if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                content.setValues(this.fCurrentEntity.ch, offset3, length);
                return -1;
            }
            offset = offset3;
            newlines = newlines2;
        } else {
            offset = offset2;
        }
        if (!external) {
            while (true) {
                if (this.fCurrentEntity.position >= this.fCurrentEntity.count) {
                    break;
                }
                char[] cArr2 = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                int i3 = scannedEntity2.position;
                scannedEntity2.position = i3 + 1;
                if (!XML11Char.isXML11InternalEntityContent(cArr2[i3])) {
                    this.fCurrentEntity.position--;
                    break;
                }
            }
        } else {
            while (this.fCurrentEntity.position < this.fCurrentEntity.count) {
                char[] cArr3 = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
                int i4 = scannedEntity3.position;
                scannedEntity3.position = i4 + 1;
                char c4 = cArr3[i4];
                if (!XML11Char.isXML11Content(c4) || c4 == 133 || c4 == 8232) {
                    this.fCurrentEntity.position--;
                    break;
                }
            }
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2 - newlines;
        content.setValues(this.fCurrentEntity.ch, offset, length2);
        if (this.fCurrentEntity.position != this.fCurrentEntity.count) {
            char c5 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if ((c5 == '\r' || c5 == 133 || c5 == 8232) && external) {
                return 10;
            }
            return c5;
        }
        return -1;
    }

    @Override
    public int scanLiteral(int quote, XMLString content) throws IOException {
        int newlines;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        } else if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
            this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[this.fCurrentEntity.count - 1];
            load(1, false);
            this.fCurrentEntity.startPosition = 0;
            this.fCurrentEntity.position = 0;
        }
        int offset = this.fCurrentEntity.position;
        char c = this.fCurrentEntity.ch[offset];
        int newlines2 = 0;
        boolean external = this.fCurrentEntity.isExternal();
        if (c == '\n' || ((c == '\r' || c == 133 || c == 8232) && external)) {
            do {
                char[] cArr = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                int i = scannedEntity.position;
                scannedEntity.position = i + 1;
                char c2 = cArr[i];
                if (c2 == '\r' && external) {
                    newlines2++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines2;
                        this.fCurrentEntity.startPosition = newlines2;
                        if (load(newlines2, false)) {
                            break;
                        }
                    }
                    char c3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                    if (c3 == '\n' || c3 == 133) {
                        this.fCurrentEntity.position++;
                        offset++;
                    } else {
                        newlines2++;
                    }
                } else {
                    if (c2 != '\n' && ((c2 != 133 && c2 != 8232) || !external)) {
                        this.fCurrentEntity.position--;
                        break;
                    }
                    newlines2++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines2;
                        this.fCurrentEntity.startPosition = newlines2;
                        if (load(newlines2, false)) {
                            break;
                        }
                    }
                }
            } while (this.fCurrentEntity.position < this.fCurrentEntity.count - 1);
            int offset2 = offset;
            newlines = newlines2;
            for (int i2 = offset2; i2 < this.fCurrentEntity.position; i2++) {
                this.fCurrentEntity.ch[i2] = '\n';
            }
            int length = this.fCurrentEntity.position - offset2;
            if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                content.setValues(this.fCurrentEntity.ch, offset2, length);
                return -1;
            }
            offset = offset2;
        } else {
            newlines = 0;
        }
        if (external) {
            while (this.fCurrentEntity.position < this.fCurrentEntity.count) {
                char[] cArr2 = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                int i3 = scannedEntity2.position;
                scannedEntity2.position = i3 + 1;
                char c4 = cArr2[i3];
                if (c4 == quote || c4 == '%' || !XML11Char.isXML11Content(c4) || c4 == 133 || c4 == 8232) {
                    this.fCurrentEntity.position--;
                    break;
                }
            }
        } else {
            while (this.fCurrentEntity.position < this.fCurrentEntity.count) {
                char[] cArr3 = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
                int i4 = scannedEntity3.position;
                scannedEntity3.position = i4 + 1;
                char c5 = cArr3[i4];
                if ((c5 == quote && !this.fCurrentEntity.literal) || c5 == '%' || !XML11Char.isXML11InternalEntityContent(c5)) {
                    this.fCurrentEntity.position--;
                    break;
                }
            }
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2 - newlines;
        content.setValues(this.fCurrentEntity.ch, offset, length2);
        if (this.fCurrentEntity.position != this.fCurrentEntity.count) {
            char c6 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (c6 == quote && this.fCurrentEntity.literal) {
                return -1;
            }
            return c6;
        }
        return -1;
    }

    @Override
    public boolean scanData(String delimiter, XMLStringBuffer buffer) throws IOException {
        int offset;
        int newlines;
        int i;
        int newlines2;
        int newlines3;
        boolean done;
        boolean done2;
        boolean done3 = false;
        int delimLen = delimiter.length();
        ?? r5 = 0;
        char charAt0 = delimiter.charAt(0);
        boolean external = this.fCurrentEntity.isExternal();
        while (true) {
            if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                load(r5, true);
            }
            boolean bNextEntity = false;
            while (this.fCurrentEntity.position >= this.fCurrentEntity.count - delimLen && !bNextEntity) {
                System.arraycopy(this.fCurrentEntity.ch, this.fCurrentEntity.position, this.fCurrentEntity.ch, r5, this.fCurrentEntity.count - this.fCurrentEntity.position);
                bNextEntity = load(this.fCurrentEntity.count - this.fCurrentEntity.position, r5);
                this.fCurrentEntity.position = r5;
                this.fCurrentEntity.startPosition = r5;
            }
            if (this.fCurrentEntity.position >= this.fCurrentEntity.count - delimLen) {
                int length = this.fCurrentEntity.count - this.fCurrentEntity.position;
                buffer.append(this.fCurrentEntity.ch, this.fCurrentEntity.position, length);
                this.fCurrentEntity.columnNumber += this.fCurrentEntity.count;
                this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                this.fCurrentEntity.position = this.fCurrentEntity.count;
                this.fCurrentEntity.startPosition = this.fCurrentEntity.count;
                load(r5, true);
                return r5;
            }
            int offset2 = this.fCurrentEntity.position;
            char c = this.fCurrentEntity.ch[offset2];
            int newlines4 = 0;
            char c2 = '\r';
            if (c == '\n' || ((c == '\r' || c == 133 || c == 8232) && external)) {
                while (true) {
                    char[] cArr = this.fCurrentEntity.ch;
                    XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                    int i2 = scannedEntity.position;
                    scannedEntity.position = i2 + 1;
                    char c3 = cArr[i2];
                    if (c3 == '\r' && external) {
                        newlines4++;
                        this.fCurrentEntity.lineNumber++;
                        this.fCurrentEntity.columnNumber = 1;
                        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                            offset2 = 0;
                            this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                            this.fCurrentEntity.position = newlines4;
                            this.fCurrentEntity.startPosition = newlines4;
                            if (load(newlines4, false)) {
                                break;
                            }
                        }
                        char c4 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                        if (c4 == '\n' || c4 == 133) {
                            this.fCurrentEntity.position++;
                            offset2++;
                        } else {
                            newlines4++;
                        }
                        if (this.fCurrentEntity.position >= this.fCurrentEntity.count - 1) {
                        }
                    } else {
                        if (c3 != '\n' && ((c3 != 133 && c3 != 8232) || !external)) {
                            break;
                        }
                        newlines4++;
                        this.fCurrentEntity.lineNumber++;
                        this.fCurrentEntity.columnNumber = 1;
                        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                            offset = 0;
                            this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                            this.fCurrentEntity.position = newlines4;
                            this.fCurrentEntity.startPosition = newlines4;
                            this.fCurrentEntity.count = newlines4;
                            if (load(newlines4, false)) {
                                break;
                            }
                            offset2 = 0;
                        }
                        if (this.fCurrentEntity.position >= this.fCurrentEntity.count - 1) {
                            done3 = done3;
                        } else {
                            offset = offset2;
                            break;
                        }
                    }
                    i = offset;
                    while (i < this.fCurrentEntity.position) {
                        this.fCurrentEntity.ch[i] = '\n';
                        i++;
                        c2 = c2;
                        done3 = done3;
                    }
                    int length2 = this.fCurrentEntity.position - offset;
                    if (this.fCurrentEntity.position != this.fCurrentEntity.count - 1) {
                        buffer.append(this.fCurrentEntity.ch, offset, length2);
                        return true;
                    }
                    newlines2 = offset;
                    newlines3 = newlines;
                }
                newlines = newlines4;
                i = offset;
                while (i < this.fCurrentEntity.position) {
                }
                int length22 = this.fCurrentEntity.position - offset;
                if (this.fCurrentEntity.position != this.fCurrentEntity.count - 1) {
                }
            } else {
                newlines3 = 0;
                newlines2 = offset2;
            }
            if (external) {
                while (true) {
                    if (this.fCurrentEntity.position >= this.fCurrentEntity.count) {
                        break;
                    }
                    char[] cArr2 = this.fCurrentEntity.ch;
                    XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                    int i3 = scannedEntity2.position;
                    scannedEntity2.position = i3 + 1;
                    char c5 = cArr2[i3];
                    if (c5 == charAt0) {
                        int delimOffset = this.fCurrentEntity.position - 1;
                        int i4 = 1;
                        while (true) {
                            if (i4 < delimLen) {
                                if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                                    this.fCurrentEntity.position -= i4;
                                    break;
                                }
                                char[] cArr3 = this.fCurrentEntity.ch;
                                XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
                                int i5 = scannedEntity3.position;
                                done2 = done3;
                                scannedEntity3.position = i5 + 1;
                                if (delimiter.charAt(i4) == cArr3[i5]) {
                                    i4++;
                                    done3 = done2;
                                } else {
                                    this.fCurrentEntity.position--;
                                    break;
                                }
                            } else {
                                done2 = done3;
                                break;
                            }
                        }
                    } else {
                        done = done3;
                        if (c5 == '\n' || c5 == '\r' || c5 == 133 || c5 == 8232) {
                            break;
                        }
                        if (!XML11Char.isXML11ValidLiteral(c5)) {
                            this.fCurrentEntity.position--;
                            int length3 = this.fCurrentEntity.position - newlines2;
                            this.fCurrentEntity.columnNumber += length3 - newlines3;
                            buffer.append(this.fCurrentEntity.ch, newlines2, length3);
                            return true;
                        }
                        done3 = done;
                    }
                }
            } else {
                done = done3;
                while (true) {
                    if (this.fCurrentEntity.position >= this.fCurrentEntity.count) {
                        break;
                    }
                    char[] cArr4 = this.fCurrentEntity.ch;
                    XMLEntityManager.ScannedEntity scannedEntity4 = this.fCurrentEntity;
                    int i6 = scannedEntity4.position;
                    scannedEntity4.position = i6 + 1;
                    char c6 = cArr4[i6];
                    if (c6 == charAt0) {
                        int delimOffset2 = this.fCurrentEntity.position - 1;
                        int i7 = 1;
                        while (true) {
                            if (i7 >= delimLen) {
                                break;
                            }
                            if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                                this.fCurrentEntity.position -= i7;
                                break;
                            }
                            char[] cArr5 = this.fCurrentEntity.ch;
                            XMLEntityManager.ScannedEntity scannedEntity5 = this.fCurrentEntity;
                            int i8 = scannedEntity5.position;
                            scannedEntity5.position = i8 + 1;
                            if (delimiter.charAt(i7) == cArr5[i8]) {
                                i7++;
                            } else {
                                this.fCurrentEntity.position--;
                                break;
                            }
                        }
                        if (this.fCurrentEntity.position == delimOffset2 + delimLen) {
                            done3 = true;
                            break;
                        }
                    } else {
                        if (c6 == '\n') {
                            this.fCurrentEntity.position--;
                            break;
                        }
                        boolean done4 = XML11Char.isXML11Valid(c6);
                        if (!done4) {
                            this.fCurrentEntity.position--;
                            int length4 = this.fCurrentEntity.position - newlines2;
                            this.fCurrentEntity.columnNumber += length4 - newlines3;
                            buffer.append(this.fCurrentEntity.ch, newlines2, length4);
                            return true;
                        }
                    }
                }
                done3 = done;
            }
            int length5 = this.fCurrentEntity.position - newlines2;
            this.fCurrentEntity.columnNumber += length5 - newlines3;
            if (done3) {
                length5 -= delimLen;
            }
            buffer.append(this.fCurrentEntity.ch, newlines2, length5);
            if (done3) {
                return !done3;
            }
            r5 = 0;
        }
    }

    @Override
    public boolean skipChar(int c) throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (c2 != c) {
            if (c == 10 && ((c2 == 8232 || c2 == 133) && this.fCurrentEntity.isExternal())) {
                this.fCurrentEntity.position++;
                this.fCurrentEntity.lineNumber++;
                this.fCurrentEntity.columnNumber = 1;
                return true;
            }
            if (c != 10 || c2 != '\r' || !this.fCurrentEntity.isExternal()) {
                return false;
            }
            if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = c2;
                load(1, false);
            }
            char[] cArr = this.fCurrentEntity.ch;
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            char c3 = cArr[i];
            if (c3 == '\n' || c3 == 133) {
                this.fCurrentEntity.position++;
            }
            this.fCurrentEntity.lineNumber++;
            this.fCurrentEntity.columnNumber = 1;
            return true;
        }
        this.fCurrentEntity.position++;
        if (c == 10) {
            this.fCurrentEntity.lineNumber++;
            this.fCurrentEntity.columnNumber = 1;
        } else {
            this.fCurrentEntity.columnNumber++;
        }
        return true;
    }

    @Override
    public boolean skipSpaces() throws IOException {
        char c;
        char c2;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (this.fCurrentEntity.isExternal()) {
            if (XML11Char.isXML11Space(c3)) {
                do {
                    boolean entityChanged = false;
                    if (c3 == '\n' || c3 == '\r' || c3 == 133 || c3 == 8232) {
                        this.fCurrentEntity.lineNumber++;
                        this.fCurrentEntity.columnNumber = 1;
                        if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                            this.fCurrentEntity.ch[0] = c3;
                            entityChanged = load(1, true);
                            if (!entityChanged) {
                                this.fCurrentEntity.startPosition = 0;
                                this.fCurrentEntity.position = 0;
                            }
                        }
                        if (c3 == '\r') {
                            char[] cArr = this.fCurrentEntity.ch;
                            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                            int i = scannedEntity.position + 1;
                            scannedEntity.position = i;
                            char c4 = cArr[i];
                            if (c4 != '\n' && c4 != 133) {
                                this.fCurrentEntity.position--;
                            }
                        }
                    } else {
                        this.fCurrentEntity.columnNumber++;
                    }
                    if (!entityChanged) {
                        this.fCurrentEntity.position++;
                    }
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        load(0, true);
                    }
                    c2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                    c3 = c2;
                } while (XML11Char.isXML11Space(c2));
                return true;
            }
        } else if (XMLChar.isSpace(c3)) {
            do {
                boolean entityChanged2 = false;
                if (c3 == '\n') {
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                        this.fCurrentEntity.ch[0] = c3;
                        entityChanged2 = load(1, true);
                        if (!entityChanged2) {
                            this.fCurrentEntity.startPosition = 0;
                            this.fCurrentEntity.position = 0;
                        }
                    }
                } else {
                    this.fCurrentEntity.columnNumber++;
                }
                if (!entityChanged2) {
                    this.fCurrentEntity.position++;
                }
                if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                    load(0, true);
                }
                c = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                c3 = c;
            } while (XMLChar.isSpace(c));
            return true;
        }
        return false;
    }

    @Override
    public boolean skipString(String s) throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char[] cArr = this.fCurrentEntity.ch;
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i2 = scannedEntity.position;
            scannedEntity.position = i2 + 1;
            char c = cArr[i2];
            if (c != s.charAt(i)) {
                this.fCurrentEntity.position -= i + 1;
                return false;
            }
            if (i < length - 1 && this.fCurrentEntity.position == this.fCurrentEntity.count) {
                System.arraycopy(this.fCurrentEntity.ch, (this.fCurrentEntity.count - i) - 1, this.fCurrentEntity.ch, 0, i + 1);
                if (load(i + 1, false)) {
                    this.fCurrentEntity.startPosition -= i + 1;
                    this.fCurrentEntity.position -= i + 1;
                    return false;
                }
            }
        }
        this.fCurrentEntity.columnNumber += length;
        return true;
    }
}
