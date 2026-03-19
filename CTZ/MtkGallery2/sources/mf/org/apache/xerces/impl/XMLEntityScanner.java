package mf.org.apache.xerces.impl;

import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.io.UCSReader;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLString;

public class XMLEntityScanner implements XMLLocator {
    private static final boolean DEBUG_BUFFER = false;
    private static final boolean DEBUG_ENCODINGS = false;
    private static final EOFException END_OF_DOCUMENT_ENTITY = new EOFException() {
        private static final long serialVersionUID = 980337771224675268L;

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    };
    protected XMLErrorReporter fErrorReporter;
    private XMLEntityManager fEntityManager = null;
    protected XMLEntityManager.ScannedEntity fCurrentEntity = null;
    protected SymbolTable fSymbolTable = null;
    protected int fBufferSize = 2048;

    @Override
    public final String getBaseSystemId() {
        if (this.fCurrentEntity == null || this.fCurrentEntity.entityLocation == null) {
            return null;
        }
        return this.fCurrentEntity.entityLocation.getExpandedSystemId();
    }

    public final void setEncoding(String encoding) throws IOException {
        if (this.fCurrentEntity.stream != null) {
            if (this.fCurrentEntity.encoding == null || !this.fCurrentEntity.encoding.equals(encoding)) {
                if (this.fCurrentEntity.encoding != null && this.fCurrentEntity.encoding.startsWith("UTF-16")) {
                    String ENCODING = encoding.toUpperCase(Locale.ENGLISH);
                    if (ENCODING.equals("UTF-16")) {
                        return;
                    }
                    if (ENCODING.equals("ISO-10646-UCS-4")) {
                        if (this.fCurrentEntity.encoding.equals("UTF-16BE")) {
                            this.fCurrentEntity.reader = new UCSReader(this.fCurrentEntity.stream, (short) 8);
                            return;
                        } else {
                            this.fCurrentEntity.reader = new UCSReader(this.fCurrentEntity.stream, (short) 4);
                            return;
                        }
                    }
                    if (ENCODING.equals("ISO-10646-UCS-2")) {
                        if (this.fCurrentEntity.encoding.equals("UTF-16BE")) {
                            this.fCurrentEntity.reader = new UCSReader(this.fCurrentEntity.stream, (short) 2);
                            return;
                        } else {
                            this.fCurrentEntity.reader = new UCSReader(this.fCurrentEntity.stream, (short) 1);
                            return;
                        }
                    }
                }
                this.fCurrentEntity.setReader(this.fCurrentEntity.stream, encoding, null);
                this.fCurrentEntity.encoding = encoding;
            }
        }
    }

    public final void setXMLVersion(String xmlVersion) {
        this.fCurrentEntity.xmlVersion = xmlVersion;
    }

    public final boolean isExternal() {
        return this.fCurrentEntity.isExternal();
    }

    public int peekChar() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (this.fCurrentEntity.isExternal()) {
            if (c != '\r') {
                return c;
            }
            return 10;
        }
        return c;
    }

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
            if (c == '\r') {
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
                if (cArr2[i2] != '\n') {
                    this.fCurrentEntity.position--;
                }
                c = '\n';
            }
        }
        this.fCurrentEntity.columnNumber++;
        return c;
    }

    public String scanNmtoken() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        while (XMLChar.isName(this.fCurrentEntity.ch[this.fCurrentEntity.position])) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            if (i == this.fCurrentEntity.count) {
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
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2;
        if (length2 <= 0) {
            return null;
        }
        String symbol = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length2);
        return symbol;
    }

    public String scanName() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        if (XMLChar.isNameStart(this.fCurrentEntity.ch[offset])) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            if (i == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[offset];
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.columnNumber++;
                    String symbol = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 1);
                    return symbol;
                }
            }
            while (XMLChar.isName(this.fCurrentEntity.ch[this.fCurrentEntity.position])) {
                XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                int i2 = scannedEntity2.position + 1;
                scannedEntity2.position = i2;
                if (i2 == this.fCurrentEntity.count) {
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
            }
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2;
        if (length2 <= 0) {
            return null;
        }
        String symbol2 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length2);
        return symbol2;
    }

    public String scanNCName() throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        if (XMLChar.isNCNameStart(this.fCurrentEntity.ch[offset])) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i = scannedEntity.position + 1;
            scannedEntity.position = i;
            if (i == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[offset];
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.columnNumber++;
                    String symbol = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 1);
                    return symbol;
                }
            }
            while (XMLChar.isNCName(this.fCurrentEntity.ch[this.fCurrentEntity.position])) {
                XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                int i2 = scannedEntity2.position + 1;
                scannedEntity2.position = i2;
                if (i2 == this.fCurrentEntity.count) {
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
            }
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2;
        if (length2 <= 0) {
            return null;
        }
        String symbol2 = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length2);
        return symbol2;
    }

    public boolean scanQName(QName qname) throws IOException {
        String localpart;
        int i;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        int offset = this.fCurrentEntity.position;
        if (XMLChar.isNCNameStart(this.fCurrentEntity.ch[offset])) {
            XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
            int i2 = scannedEntity.position + 1;
            scannedEntity.position = i2;
            if (i2 == this.fCurrentEntity.count) {
                this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[offset];
                offset = 0;
                if (load(1, false)) {
                    this.fCurrentEntity.columnNumber++;
                    String name = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, 0, 1);
                    qname.setValues(null, name, name, null);
                    return true;
                }
            }
            int index = -1;
            while (XMLChar.isName(this.fCurrentEntity.ch[this.fCurrentEntity.position])) {
                char c = this.fCurrentEntity.ch[this.fCurrentEntity.position];
                if (c == ':') {
                    if (index != -1) {
                        break;
                    }
                    index = this.fCurrentEntity.position;
                    XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
                    i = scannedEntity2.position + 1;
                    scannedEntity2.position = i;
                    if (i == this.fCurrentEntity.count) {
                    }
                } else {
                    XMLEntityManager.ScannedEntity scannedEntity22 = this.fCurrentEntity;
                    i = scannedEntity22.position + 1;
                    scannedEntity22.position = i;
                    if (i == this.fCurrentEntity.count) {
                        int length = this.fCurrentEntity.position - offset;
                        if (length == this.fCurrentEntity.ch.length) {
                            char[] tmp = new char[this.fCurrentEntity.ch.length << 1];
                            System.arraycopy(this.fCurrentEntity.ch, offset, tmp, 0, length);
                            this.fCurrentEntity.ch = tmp;
                        } else {
                            System.arraycopy(this.fCurrentEntity.ch, offset, this.fCurrentEntity.ch, 0, length);
                        }
                        if (index != -1) {
                            index -= offset;
                        }
                        offset = 0;
                        if (load(length, false)) {
                            break;
                        }
                    }
                }
            }
            int length2 = this.fCurrentEntity.position - offset;
            this.fCurrentEntity.columnNumber += length2;
            if (length2 > 0) {
                String prefix = null;
                String rawname = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, length2);
                if (index != -1) {
                    int prefixLength = index - offset;
                    prefix = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, offset, prefixLength);
                    int len = (length2 - prefixLength) - 1;
                    int startLocal = index + 1;
                    if (!XMLChar.isNCNameStart(this.fCurrentEntity.ch[startLocal])) {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "IllegalQName", null, (short) 2);
                    }
                    localpart = this.fSymbolTable.addSymbol(this.fCurrentEntity.ch, startLocal, len);
                } else {
                    localpart = rawname;
                }
                qname.setValues(prefix, localpart, rawname, null);
                return true;
            }
        }
        return false;
    }

    public int scanContent(XMLString content) throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        } else if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
            this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[this.fCurrentEntity.count - 1];
            load(1, false);
            this.fCurrentEntity.position = 0;
            this.fCurrentEntity.startPosition = 0;
        }
        int offset = this.fCurrentEntity.position;
        char c = this.fCurrentEntity.ch[offset];
        int newlines = 0;
        boolean external = this.fCurrentEntity.isExternal();
        if (c == '\n' || (c == '\r' && external)) {
            while (true) {
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
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    if (this.fCurrentEntity.ch[this.fCurrentEntity.position] == '\n') {
                        this.fCurrentEntity.position++;
                        offset++;
                    } else {
                        newlines++;
                    }
                    if (this.fCurrentEntity.position < this.fCurrentEntity.count - 1) {
                    }
                } else {
                    if (c2 != '\n') {
                        this.fCurrentEntity.position--;
                        break;
                    }
                    newlines++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                        if (this.fCurrentEntity.position < this.fCurrentEntity.count - 1) {
                            break;
                        }
                    }
                }
            }
            for (int i2 = offset; i2 < this.fCurrentEntity.position; i2++) {
                this.fCurrentEntity.ch[i2] = '\n';
            }
            int length = this.fCurrentEntity.position - offset;
            if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                content.setValues(this.fCurrentEntity.ch, offset, length);
                return -1;
            }
        }
        while (true) {
            if (this.fCurrentEntity.position >= this.fCurrentEntity.count) {
                break;
            }
            char[] cArr2 = this.fCurrentEntity.ch;
            XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
            int i3 = scannedEntity2.position;
            scannedEntity2.position = i3 + 1;
            if (!XMLChar.isContent(cArr2[i3])) {
                this.fCurrentEntity.position--;
                break;
            }
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2 - newlines;
        content.setValues(this.fCurrentEntity.ch, offset, length2);
        if (this.fCurrentEntity.position != this.fCurrentEntity.count) {
            char c3 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (c3 == '\r' && external) {
                return 10;
            }
            return c3;
        }
        return -1;
    }

    public int scanLiteral(int quote, XMLString content) throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        } else if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
            this.fCurrentEntity.ch[0] = this.fCurrentEntity.ch[this.fCurrentEntity.count - 1];
            load(1, false);
            this.fCurrentEntity.position = 0;
            this.fCurrentEntity.startPosition = 0;
        }
        int offset = this.fCurrentEntity.position;
        char c = this.fCurrentEntity.ch[offset];
        int newlines = 0;
        boolean external = this.fCurrentEntity.isExternal();
        if (c == '\n' || (c == '\r' && external)) {
            while (true) {
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
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    if (this.fCurrentEntity.ch[this.fCurrentEntity.position] == '\n') {
                        this.fCurrentEntity.position++;
                        offset++;
                    } else {
                        newlines++;
                    }
                    if (this.fCurrentEntity.position < this.fCurrentEntity.count - 1) {
                    }
                } else {
                    if (c2 != '\n') {
                        this.fCurrentEntity.position--;
                        break;
                    }
                    newlines++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                        if (this.fCurrentEntity.position < this.fCurrentEntity.count - 1) {
                            break;
                        }
                    }
                }
            }
            for (int i2 = offset; i2 < this.fCurrentEntity.position; i2++) {
                this.fCurrentEntity.ch[i2] = '\n';
            }
            int length = this.fCurrentEntity.position - offset;
            if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                content.setValues(this.fCurrentEntity.ch, offset, length);
                return -1;
            }
        }
        while (this.fCurrentEntity.position < this.fCurrentEntity.count) {
            char[] cArr2 = this.fCurrentEntity.ch;
            XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
            int i3 = scannedEntity2.position;
            scannedEntity2.position = i3 + 1;
            char c3 = cArr2[i3];
            if ((c3 == quote && (!this.fCurrentEntity.literal || external)) || c3 == '%' || !XMLChar.isContent(c3)) {
                this.fCurrentEntity.position--;
                break;
            }
        }
        int length2 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length2 - newlines;
        content.setValues(this.fCurrentEntity.ch, offset, length2);
        if (this.fCurrentEntity.position != this.fCurrentEntity.count) {
            char c4 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            if (c4 == quote && this.fCurrentEntity.literal) {
                return -1;
            }
            return c4;
        }
        return -1;
    }

    public boolean scanData(String delimiter, XMLStringBuffer buffer) throws IOException {
        boolean z;
        boolean found;
        int length;
        boolean found2 = false;
        int delimLen = delimiter.length();
        char charAt0 = delimiter.charAt(0);
        boolean external = this.fCurrentEntity.isExternal();
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        boolean bNextEntity = false;
        while (this.fCurrentEntity.position > this.fCurrentEntity.count - delimLen && !bNextEntity) {
            System.arraycopy(this.fCurrentEntity.ch, this.fCurrentEntity.position, this.fCurrentEntity.ch, 0, this.fCurrentEntity.count - this.fCurrentEntity.position);
            bNextEntity = load(this.fCurrentEntity.count - this.fCurrentEntity.position, false);
            this.fCurrentEntity.position = 0;
            this.fCurrentEntity.startPosition = 0;
        }
        if (this.fCurrentEntity.position > this.fCurrentEntity.count - delimLen) {
            int length2 = this.fCurrentEntity.count - this.fCurrentEntity.position;
            buffer.append(this.fCurrentEntity.ch, this.fCurrentEntity.position, length2);
            this.fCurrentEntity.columnNumber += this.fCurrentEntity.count;
            this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
            this.fCurrentEntity.position = this.fCurrentEntity.count;
            this.fCurrentEntity.startPosition = this.fCurrentEntity.count;
            load(0, true);
            return false;
        }
        int offset = this.fCurrentEntity.position;
        char c = this.fCurrentEntity.ch[offset];
        int newlines = 0;
        char c2 = '\r';
        if (c == '\n' || (c == '\r' && external)) {
            while (true) {
                char[] cArr = this.fCurrentEntity.ch;
                XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                int i = scannedEntity.position;
                scannedEntity.position = i + 1;
                char c3 = cArr[i];
                if (c3 != '\r' || !external) {
                    if (c3 != '\n') {
                        this.fCurrentEntity.position--;
                        break;
                    }
                    newlines++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        this.fCurrentEntity.count = newlines;
                        z = false;
                        if (load(newlines, false)) {
                            offset = 0;
                            break;
                        }
                        offset = 0;
                    } else {
                        z = false;
                    }
                    if (this.fCurrentEntity.position < this.fCurrentEntity.count - 1) {
                        break;
                    }
                } else {
                    newlines++;
                    this.fCurrentEntity.lineNumber++;
                    this.fCurrentEntity.columnNumber = 1;
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        offset = 0;
                        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
                        this.fCurrentEntity.position = newlines;
                        this.fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    if (this.fCurrentEntity.ch[this.fCurrentEntity.position] == '\n') {
                        this.fCurrentEntity.position++;
                        offset++;
                    } else {
                        newlines++;
                    }
                    z = false;
                    if (this.fCurrentEntity.position < this.fCurrentEntity.count - 1) {
                    }
                }
            }
            int i2 = offset;
            while (i2 < this.fCurrentEntity.position) {
                this.fCurrentEntity.ch[i2] = '\n';
                i2++;
                c2 = c2;
                found2 = found2;
            }
            int length3 = this.fCurrentEntity.position - offset;
            if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                buffer.append(this.fCurrentEntity.ch, offset, length3);
                return true;
            }
        }
        loop1: while (true) {
            if (this.fCurrentEntity.position >= this.fCurrentEntity.count) {
                break;
            }
            char[] cArr2 = this.fCurrentEntity.ch;
            XMLEntityManager.ScannedEntity scannedEntity2 = this.fCurrentEntity;
            int i3 = scannedEntity2.position;
            scannedEntity2.position = i3 + 1;
            char c4 = cArr2[i3];
            if (c4 == charAt0) {
                int delimOffset = this.fCurrentEntity.position - 1;
                int i4 = 1;
                while (true) {
                    if (i4 >= delimLen) {
                        found = found2;
                        break;
                    }
                    if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                        this.fCurrentEntity.position -= i4;
                        break loop1;
                    }
                    char[] cArr3 = this.fCurrentEntity.ch;
                    XMLEntityManager.ScannedEntity scannedEntity3 = this.fCurrentEntity;
                    int i5 = scannedEntity3.position;
                    found = found2;
                    scannedEntity3.position = i5 + 1;
                    if (delimiter.charAt(i4) != cArr3[i5]) {
                        this.fCurrentEntity.position--;
                        break;
                    }
                    i4++;
                    found2 = found;
                }
            } else {
                boolean found3 = found2;
                if (c4 == '\n') {
                    break;
                }
                if (external) {
                    length = 13;
                    if (c4 == '\r') {
                        break;
                    }
                } else {
                    length = 13;
                }
                if (XMLChar.isInvalid(c4)) {
                    this.fCurrentEntity.position--;
                    int length4 = this.fCurrentEntity.position - offset;
                    this.fCurrentEntity.columnNumber += length4 - newlines;
                    buffer.append(this.fCurrentEntity.ch, offset, length4);
                    return true;
                }
                found2 = found3;
            }
        }
        boolean z2 = true;
        int length5 = this.fCurrentEntity.position - offset;
        this.fCurrentEntity.columnNumber += length5 - newlines;
        if (found2) {
            length5 -= delimLen;
        }
        buffer.append(this.fCurrentEntity.ch, offset, length5);
        if (found2) {
            return false;
        }
        return z2;
    }

    public boolean skipChar(int c) throws IOException {
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (c2 == c) {
            this.fCurrentEntity.position++;
            if (c == 10) {
                this.fCurrentEntity.lineNumber++;
                this.fCurrentEntity.columnNumber = 1;
            } else {
                this.fCurrentEntity.columnNumber++;
            }
            return true;
        }
        if (c != 10 || c2 != '\r' || !this.fCurrentEntity.isExternal()) {
            return false;
        }
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            this.fCurrentEntity.ch[0] = c2;
            load(1, false);
        }
        this.fCurrentEntity.position++;
        if (this.fCurrentEntity.ch[this.fCurrentEntity.position] == '\n') {
            this.fCurrentEntity.position++;
        }
        this.fCurrentEntity.lineNumber++;
        this.fCurrentEntity.columnNumber = 1;
        return true;
    }

    public boolean skipSpaces() throws IOException {
        char c;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (!XMLChar.isSpace(c2)) {
            return false;
        }
        boolean external = this.fCurrentEntity.isExternal();
        do {
            boolean entityChanged = false;
            if (c2 == '\n' || (external && c2 == '\r')) {
                this.fCurrentEntity.lineNumber++;
                this.fCurrentEntity.columnNumber = 1;
                if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                    this.fCurrentEntity.ch[0] = c2;
                    entityChanged = load(1, true);
                    if (!entityChanged) {
                        this.fCurrentEntity.position = 0;
                        this.fCurrentEntity.startPosition = 0;
                    }
                }
                if (c2 == '\r' && external) {
                    char[] cArr = this.fCurrentEntity.ch;
                    XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                    int i = scannedEntity.position + 1;
                    scannedEntity.position = i;
                    if (cArr[i] != '\n') {
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
            c = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            c2 = c;
        } while (XMLChar.isSpace(c));
        return true;
    }

    public final boolean skipDeclSpaces() throws IOException {
        char c;
        if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
            load(0, true);
        }
        char c2 = this.fCurrentEntity.ch[this.fCurrentEntity.position];
        if (!XMLChar.isSpace(c2)) {
            return false;
        }
        boolean external = this.fCurrentEntity.isExternal();
        do {
            boolean entityChanged = false;
            if (c2 == '\n' || (external && c2 == '\r')) {
                this.fCurrentEntity.lineNumber++;
                this.fCurrentEntity.columnNumber = 1;
                if (this.fCurrentEntity.position == this.fCurrentEntity.count - 1) {
                    this.fCurrentEntity.ch[0] = c2;
                    entityChanged = load(1, true);
                    if (!entityChanged) {
                        this.fCurrentEntity.position = 0;
                        this.fCurrentEntity.startPosition = 0;
                    }
                }
                if (c2 == '\r' && external) {
                    char[] cArr = this.fCurrentEntity.ch;
                    XMLEntityManager.ScannedEntity scannedEntity = this.fCurrentEntity;
                    int i = scannedEntity.position + 1;
                    scannedEntity.position = i;
                    if (cArr[i] != '\n') {
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
            c = this.fCurrentEntity.ch[this.fCurrentEntity.position];
            c2 = c;
        } while (XMLChar.isSpace(c));
        return true;
    }

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

    @Override
    public final String getPublicId() {
        if (this.fCurrentEntity == null || this.fCurrentEntity.entityLocation == null) {
            return null;
        }
        return this.fCurrentEntity.entityLocation.getPublicId();
    }

    @Override
    public final String getExpandedSystemId() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.entityLocation != null && this.fCurrentEntity.entityLocation.getExpandedSystemId() != null) {
                return this.fCurrentEntity.entityLocation.getExpandedSystemId();
            }
            return this.fCurrentEntity.getExpandedSystemId();
        }
        return null;
    }

    @Override
    public final String getLiteralSystemId() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.entityLocation != null && this.fCurrentEntity.entityLocation.getLiteralSystemId() != null) {
                return this.fCurrentEntity.entityLocation.getLiteralSystemId();
            }
            return this.fCurrentEntity.getLiteralSystemId();
        }
        return null;
    }

    @Override
    public final int getLineNumber() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.isExternal()) {
                return this.fCurrentEntity.lineNumber;
            }
            return this.fCurrentEntity.getLineNumber();
        }
        return -1;
    }

    @Override
    public final int getColumnNumber() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.isExternal()) {
                return this.fCurrentEntity.columnNumber;
            }
            return this.fCurrentEntity.getColumnNumber();
        }
        return -1;
    }

    @Override
    public final int getCharacterOffset() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.isExternal()) {
                return this.fCurrentEntity.baseCharOffset + (this.fCurrentEntity.position - this.fCurrentEntity.startPosition);
            }
            return this.fCurrentEntity.getCharacterOffset();
        }
        return -1;
    }

    @Override
    public final String getEncoding() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.isExternal()) {
                return this.fCurrentEntity.encoding;
            }
            return this.fCurrentEntity.getEncoding();
        }
        return null;
    }

    @Override
    public final String getXMLVersion() {
        if (this.fCurrentEntity != null) {
            if (this.fCurrentEntity.isExternal()) {
                return this.fCurrentEntity.xmlVersion;
            }
            return this.fCurrentEntity.getXMLVersion();
        }
        return null;
    }

    public final void setCurrentEntity(XMLEntityManager.ScannedEntity ent) {
        this.fCurrentEntity = ent;
    }

    public final void setBufferSize(int size) {
        this.fBufferSize = size;
    }

    public final void reset(SymbolTable symbolTable, XMLEntityManager entityManager, XMLErrorReporter reporter) {
        this.fCurrentEntity = null;
        this.fSymbolTable = symbolTable;
        this.fEntityManager = entityManager;
        this.fErrorReporter = reporter;
    }

    final boolean load(int offset, boolean changeEntity) throws IOException {
        this.fCurrentEntity.baseCharOffset += this.fCurrentEntity.position - this.fCurrentEntity.startPosition;
        int length = this.fCurrentEntity.ch.length - offset;
        if (!this.fCurrentEntity.mayReadChunks && length > 64) {
            length = 64;
        }
        int count = this.fCurrentEntity.reader.read(this.fCurrentEntity.ch, offset, length);
        boolean entityChanged = false;
        if (count != -1) {
            if (count != 0) {
                this.fCurrentEntity.count = count + offset;
                this.fCurrentEntity.position = offset;
                this.fCurrentEntity.startPosition = offset;
            }
        } else {
            this.fCurrentEntity.count = offset;
            this.fCurrentEntity.position = offset;
            this.fCurrentEntity.startPosition = offset;
            entityChanged = true;
            if (changeEntity) {
                this.fEntityManager.endEntity();
                if (this.fCurrentEntity == null) {
                    throw END_OF_DOCUMENT_ENTITY;
                }
                if (this.fCurrentEntity.position == this.fCurrentEntity.count) {
                    load(0, true);
                }
            }
        }
        return entityChanged;
    }
}
