package org.apache.xpath.objects;

import java.util.Locale;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XString extends XObject implements XMLString {
    public static final XString EMPTYSTRING = new XString("");
    static final long serialVersionUID = 2020470518395094525L;

    protected XString(Object obj) {
        super(obj);
    }

    public XString(String str) {
        super(str);
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public String getTypeString() {
        return "#STRING";
    }

    @Override
    public boolean hasString() {
        return true;
    }

    @Override
    public double num() {
        return toDouble();
    }

    @Override
    public double toDouble() {
        XMLString xMLStringTrim = trim();
        for (int i = 0; i < xMLStringTrim.length(); i++) {
            char cCharAt = xMLStringTrim.charAt(i);
            if (cCharAt != '-' && cCharAt != '.' && (cCharAt < '0' || cCharAt > '9')) {
                return Double.NaN;
            }
        }
        try {
            return Double.parseDouble(xMLStringTrim.toString());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    @Override
    public boolean bool() {
        return str().length() > 0;
    }

    @Override
    public XMLString xstr() {
        return this;
    }

    @Override
    public String str() {
        return this.m_obj != null ? (String) this.m_obj : "";
    }

    @Override
    public int rtf(XPathContext xPathContext) {
        DTM dtmCreateDocumentFragment = xPathContext.createDocumentFragment();
        dtmCreateDocumentFragment.appendTextChild(str());
        return dtmCreateDocumentFragment.getDocument();
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler contentHandler) throws SAXException {
        String str = str();
        contentHandler.characters(str.toCharArray(), 0, str.length());
    }

    @Override
    public void dispatchAsComment(LexicalHandler lexicalHandler) throws SAXException {
        String str = str();
        lexicalHandler.comment(str.toCharArray(), 0, str.length());
    }

    @Override
    public int length() {
        return str().length();
    }

    @Override
    public char charAt(int i) {
        return str().charAt(i);
    }

    @Override
    public void getChars(int i, int i2, char[] cArr, int i3) {
        str().getChars(i, i2, cArr, i3);
    }

    @Override
    public boolean equals(XObject xObject) {
        int type = xObject.getType();
        try {
            if (4 == type) {
                return xObject.equals((XObject) this);
            }
            if (1 == type) {
                return xObject.bool() == bool();
            }
            if (2 == type) {
                return xObject.num() == num();
            }
            return xstr().equals(xObject.xstr());
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public boolean equals(String str) {
        return str().equals(str);
    }

    @Override
    public boolean equals(XMLString xMLString) {
        if (xMLString != null) {
            if (!xMLString.hasString()) {
                return xMLString.equals(str());
            }
            return str().equals(xMLString.toString());
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof XNodeSet) {
            return obj.equals(this);
        }
        if (obj instanceof XNumber) {
            return obj.equals(this);
        }
        return str().equals(obj.toString());
    }

    @Override
    public boolean equalsIgnoreCase(String str) {
        return str().equalsIgnoreCase(str);
    }

    @Override
    public int compareTo(XMLString xMLString) {
        int length = length();
        int length2 = xMLString.length();
        int iMin = Math.min(length, length2);
        int i = 0;
        int i2 = 0;
        while (true) {
            int i3 = iMin - 1;
            if (iMin != 0) {
                char cCharAt = charAt(i);
                char cCharAt2 = xMLString.charAt(i2);
                if (cCharAt != cCharAt2) {
                    return cCharAt - cCharAt2;
                }
                i++;
                i2++;
                iMin = i3;
            } else {
                return length - length2;
            }
        }
    }

    @Override
    public int compareToIgnoreCase(XMLString xMLString) {
        throw new WrappedRuntimeException(new NoSuchMethodException("Java 1.2 method, not yet implemented"));
    }

    @Override
    public boolean startsWith(String str, int i) {
        return str().startsWith(str, i);
    }

    @Override
    public boolean startsWith(String str) {
        return startsWith(str, 0);
    }

    @Override
    public boolean startsWith(XMLString xMLString, int i) {
        int length = length();
        int length2 = xMLString.length();
        if (i < 0 || i > length - length2) {
            return false;
        }
        int i2 = 0;
        while (true) {
            length2--;
            if (length2 >= 0) {
                if (charAt(i) != xMLString.charAt(i2)) {
                    return false;
                }
                i++;
                i2++;
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
    public boolean endsWith(String str) {
        return str().endsWith(str);
    }

    @Override
    public int hashCode() {
        return str().hashCode();
    }

    @Override
    public int indexOf(int i) {
        return str().indexOf(i);
    }

    @Override
    public int indexOf(int i, int i2) {
        return str().indexOf(i, i2);
    }

    @Override
    public int lastIndexOf(int i) {
        return str().lastIndexOf(i);
    }

    @Override
    public int lastIndexOf(int i, int i2) {
        return str().lastIndexOf(i, i2);
    }

    @Override
    public int indexOf(String str) {
        return str().indexOf(str);
    }

    @Override
    public int indexOf(XMLString xMLString) {
        return str().indexOf(xMLString.toString());
    }

    @Override
    public int indexOf(String str, int i) {
        return str().indexOf(str, i);
    }

    @Override
    public int lastIndexOf(String str) {
        return str().lastIndexOf(str);
    }

    @Override
    public int lastIndexOf(String str, int i) {
        return str().lastIndexOf(str, i);
    }

    @Override
    public XMLString substring(int i) {
        return new XString(str().substring(i));
    }

    @Override
    public XMLString substring(int i, int i2) {
        return new XString(str().substring(i, i2));
    }

    @Override
    public XMLString concat(String str) {
        return new XString(str().concat(str));
    }

    @Override
    public XMLString toLowerCase(Locale locale) {
        return new XString(str().toLowerCase(locale));
    }

    @Override
    public XMLString toLowerCase() {
        return new XString(str().toLowerCase());
    }

    @Override
    public XMLString toUpperCase(Locale locale) {
        return new XString(str().toUpperCase(locale));
    }

    @Override
    public XMLString toUpperCase() {
        return new XString(str().toUpperCase());
    }

    @Override
    public XMLString trim() {
        return new XString(str().trim());
    }

    private static boolean isSpace(char c) {
        return XMLCharacterRecognizer.isWhiteSpace(c);
    }

    @Override
    public XMLString fixWhiteSpace(boolean z, boolean z2, boolean z3) {
        int length = length();
        char[] cArr = new char[length];
        int i = 0;
        getChars(0, length, cArr, 0);
        int i2 = 0;
        while (i2 < length && !isSpace(cArr[i2])) {
            i2++;
        }
        boolean z4 = false;
        boolean z5 = false;
        int i3 = i2;
        while (i2 < length) {
            char c = cArr[i2];
            if (!isSpace(c)) {
                cArr[i3] = c;
                i3++;
                z4 = false;
            } else if (z4) {
                z4 = true;
                z5 = true;
            } else {
                if (' ' != c) {
                    z5 = true;
                }
                int i4 = i3 + 1;
                cArr[i3] = ' ';
                if (!z3 || i2 == 0) {
                    z4 = true;
                } else {
                    char c2 = cArr[i2 - 1];
                    if (c2 != '.' && c2 != '!' && c2 != '?') {
                        z4 = true;
                    }
                }
                i3 = i4;
            }
            i2++;
        }
        if (z2 && 1 <= i3 && ' ' == cArr[i3 - 1]) {
            i3--;
            z5 = true;
        }
        if (z && i3 > 0 && ' ' == cArr[0]) {
            i = 1;
            z5 = true;
        }
        return z5 ? XMLStringFactoryImpl.getFactory().newstr(new String(cArr, i, i3 - i)) : this;
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        xPathVisitor.visitStringLiteral(expressionOwner, this);
    }
}
