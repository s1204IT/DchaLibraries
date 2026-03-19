package org.apache.xpath.objects;

import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.res.XPATHErrorResources;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XStringForFSB extends XString {
    static final long serialVersionUID = -1533039186550674548L;
    protected int m_hash;
    int m_length;
    int m_start;
    protected String m_strCache;

    public XStringForFSB(FastStringBuffer fastStringBuffer, int i, int i2) {
        super(fastStringBuffer);
        this.m_strCache = null;
        this.m_hash = 0;
        this.m_start = i;
        this.m_length = i2;
        if (fastStringBuffer == null) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FASTSTRINGBUFFER_CANNOT_BE_NULL, null));
        }
    }

    private XStringForFSB(String str) {
        super(str);
        this.m_strCache = null;
        this.m_hash = 0;
        throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FSB_CANNOT_TAKE_STRING, null));
    }

    public FastStringBuffer fsb() {
        return (FastStringBuffer) this.m_obj;
    }

    @Override
    public void appendToFsb(FastStringBuffer fastStringBuffer) {
        fastStringBuffer.append(str());
    }

    @Override
    public boolean hasString() {
        return this.m_strCache != null;
    }

    @Override
    public Object object() {
        return str();
    }

    @Override
    public String str() {
        if (this.m_strCache == null) {
            this.m_strCache = fsb().getString(this.m_start, this.m_length);
        }
        return this.m_strCache;
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler contentHandler) throws SAXException {
        fsb().sendSAXcharacters(contentHandler, this.m_start, this.m_length);
    }

    @Override
    public void dispatchAsComment(LexicalHandler lexicalHandler) throws SAXException {
        fsb().sendSAXComment(lexicalHandler, this.m_start, this.m_length);
    }

    @Override
    public int length() {
        return this.m_length;
    }

    @Override
    public char charAt(int i) {
        return fsb().charAt(this.m_start + i);
    }

    @Override
    public void getChars(int i, int i2, char[] cArr, int i3) {
        int length = i2 - i;
        if (length > this.m_length) {
            length = this.m_length;
        }
        if (length > cArr.length - i3) {
            length = cArr.length - i3;
        }
        int i4 = this.m_start + i + length;
        FastStringBuffer fastStringBufferFsb = fsb();
        int i5 = i + this.m_start;
        while (i5 < i4) {
            cArr[i3] = fastStringBufferFsb.charAt(i5);
            i5++;
            i3++;
        }
    }

    @Override
    public boolean equals(XMLString xMLString) {
        if (this == xMLString) {
            return true;
        }
        int i = this.m_length;
        if (i != xMLString.length()) {
            return false;
        }
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = this.m_start;
        int i3 = 0;
        while (true) {
            int i4 = i - 1;
            if (i == 0) {
                return true;
            }
            if (fastStringBufferFsb.charAt(i2) != xMLString.charAt(i3)) {
                return false;
            }
            i2++;
            i3++;
            i = i4;
        }
    }

    @Override
    public boolean equals(XObject xObject) {
        if (this == xObject) {
            return true;
        }
        if (xObject.getType() == 2) {
            return xObject.equals((XObject) this);
        }
        String str = xObject.str();
        int i = this.m_length;
        if (i != str.length()) {
            return false;
        }
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = this.m_start;
        int i3 = 0;
        while (true) {
            int i4 = i - 1;
            if (i == 0) {
                return true;
            }
            if (fastStringBufferFsb.charAt(i2) != str.charAt(i3)) {
                return false;
            }
            i2++;
            i3++;
            i = i4;
        }
    }

    @Override
    public boolean equals(String str) {
        int i = this.m_length;
        if (i != str.length()) {
            return false;
        }
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = this.m_start;
        int i3 = 0;
        while (true) {
            int i4 = i - 1;
            if (i != 0) {
                if (fastStringBufferFsb.charAt(i2) != str.charAt(i3)) {
                    return false;
                }
                i2++;
                i3++;
                i = i4;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof XNumber) {
            return obj.equals(this);
        }
        if (obj instanceof XNodeSet) {
            return obj.equals(this);
        }
        if (obj instanceof XStringForFSB) {
            return equals((XMLString) obj);
        }
        return equals(obj.toString());
    }

    @Override
    public boolean equalsIgnoreCase(String str) {
        if (this.m_length == str.length()) {
            return str().equalsIgnoreCase(str);
        }
        return false;
    }

    @Override
    public int compareTo(XMLString xMLString) {
        int i = this.m_length;
        int length = xMLString.length();
        int iMin = Math.min(i, length);
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = this.m_start;
        int i3 = 0;
        while (true) {
            int i4 = iMin - 1;
            if (iMin != 0) {
                char cCharAt = fastStringBufferFsb.charAt(i2);
                char cCharAt2 = xMLString.charAt(i3);
                if (cCharAt != cCharAt2) {
                    return cCharAt - cCharAt2;
                }
                i2++;
                i3++;
                iMin = i4;
            } else {
                return i - length;
            }
        }
    }

    @Override
    public int compareToIgnoreCase(XMLString xMLString) {
        int i = this.m_length;
        int length = xMLString.length();
        int iMin = Math.min(i, length);
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = this.m_start;
        int i3 = 0;
        while (true) {
            int i4 = iMin - 1;
            if (iMin != 0) {
                char lowerCase = Character.toLowerCase(fastStringBufferFsb.charAt(i2));
                char lowerCase2 = Character.toLowerCase(xMLString.charAt(i3));
                if (lowerCase != lowerCase2) {
                    return lowerCase - lowerCase2;
                }
                i2++;
                i3++;
                iMin = i4;
            } else {
                return i - length;
            }
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean startsWith(XMLString xMLString, int i) {
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = this.m_start + i;
        int i3 = this.m_start;
        int i4 = this.m_length;
        int length = xMLString.length();
        if (i < 0 || i > this.m_length - length) {
            return false;
        }
        int i5 = 0;
        while (true) {
            length--;
            if (length >= 0) {
                if (fastStringBufferFsb.charAt(i2) != xMLString.charAt(i5)) {
                    return false;
                }
                i2++;
                i5++;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean startsWith(XMLString xMLString) {
        return startsWith(xMLString, 0);
    }

    @Override
    public int indexOf(int i) {
        return indexOf(i, 0);
    }

    @Override
    public int indexOf(int i, int i2) {
        int i3 = this.m_start + this.m_length;
        FastStringBuffer fastStringBufferFsb = fsb();
        if (i2 < 0) {
            i2 = 0;
        } else if (i2 >= this.m_length) {
            return -1;
        }
        for (int i4 = this.m_start + i2; i4 < i3; i4++) {
            if (fastStringBufferFsb.charAt(i4) == i) {
                return i4 - this.m_start;
            }
        }
        return -1;
    }

    @Override
    public XMLString substring(int i) {
        int i2 = this.m_length - i;
        if (i2 <= 0) {
            return XString.EMPTYSTRING;
        }
        return new XStringForFSB(fsb(), this.m_start + i, i2);
    }

    @Override
    public XMLString substring(int i, int i2) {
        int i3 = i2 - i;
        if (i3 > this.m_length) {
            i3 = this.m_length;
        }
        if (i3 <= 0) {
            return XString.EMPTYSTRING;
        }
        return new XStringForFSB(fsb(), this.m_start + i, i3);
    }

    @Override
    public XMLString concat(String str) {
        return new XString(str().concat(str));
    }

    @Override
    public XMLString trim() {
        return fixWhiteSpace(true, true, false);
    }

    private static boolean isSpace(char c) {
        return XMLCharacterRecognizer.isWhiteSpace(c);
    }

    @Override
    public XMLString fixWhiteSpace(boolean z, boolean z2, boolean z3) {
        int i = this.m_length + this.m_start;
        char[] cArr = new char[this.m_length];
        FastStringBuffer fastStringBufferFsb = fsb();
        int i2 = 0;
        boolean z4 = false;
        int i3 = 0;
        boolean z5 = false;
        for (int i4 = this.m_start; i4 < i; i4++) {
            char cCharAt = fastStringBufferFsb.charAt(i4);
            if (!isSpace(cCharAt)) {
                cArr[i3] = cCharAt;
                i3++;
                z4 = false;
            } else if (z4) {
                z4 = true;
                z5 = true;
            } else {
                if (' ' != cCharAt) {
                    z5 = true;
                }
                int i5 = i3 + 1;
                cArr[i3] = ' ';
                if (!z3 || i5 == 0) {
                    z4 = true;
                } else {
                    char c = cArr[i5 - 1];
                    if (c != '.' && c != '!' && c != '?') {
                        z4 = true;
                    }
                }
                i3 = i5;
            }
        }
        if (z2 && 1 <= i3 && ' ' == cArr[i3 - 1]) {
            i3--;
            z5 = true;
        }
        if (z && i3 > 0 && ' ' == cArr[0]) {
            i2 = 1;
            z5 = true;
        }
        return z5 ? XMLStringFactoryImpl.getFactory().newstr(cArr, i2, i3 - i2) : this;
    }

    @Override
    public double toDouble() {
        char cCharAt;
        if (this.m_length == 0) {
            return Double.NaN;
        }
        String string = fsb().getString(this.m_start, this.m_length);
        int i = 0;
        while (i < this.m_length && XMLCharacterRecognizer.isWhiteSpace(string.charAt(i))) {
            i++;
        }
        if (i == this.m_length) {
            return Double.NaN;
        }
        if (string.charAt(i) == '-') {
            i++;
        }
        while (i < this.m_length && ((cCharAt = string.charAt(i)) == '.' || (cCharAt >= '0' && cCharAt <= '9'))) {
            i++;
        }
        while (i < this.m_length && XMLCharacterRecognizer.isWhiteSpace(string.charAt(i))) {
            i++;
        }
        if (i != this.m_length) {
            return Double.NaN;
        }
        try {
            return new Double(string).doubleValue();
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
