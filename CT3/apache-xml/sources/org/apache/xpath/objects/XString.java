package org.apache.xpath.objects;

import java.util.Locale;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLCharacterRecognizer;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XString extends XObject implements XMLString {
    public static final XString EMPTYSTRING = new XString("");
    static final long serialVersionUID = 2020470518395094525L;

    protected XString(Object val) {
        super(val);
    }

    public XString(String val) {
        super(val);
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
        XMLString s = trim();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '-' && c != '.' && (c < '0' || c > '9')) {
                return Double.NaN;
            }
        }
        try {
            double result = Double.parseDouble(s.toString());
            return result;
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
    public int rtf(XPathContext support) {
        DTM frag = support.createDocumentFragment();
        frag.appendTextChild(str());
        return frag.getDocument();
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler ch) throws SAXException {
        String str = str();
        ch.characters(str.toCharArray(), 0, str.length());
    }

    @Override
    public void dispatchAsComment(LexicalHandler lh) throws SAXException {
        String str = str();
        lh.comment(str.toCharArray(), 0, str.length());
    }

    @Override
    public int length() {
        return str().length();
    }

    @Override
    public char charAt(int index) {
        return str().charAt(index);
    }

    @Override
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        str().getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    @Override
    public boolean equals(XObject obj2) {
        int t = obj2.getType();
        try {
            if (4 == t) {
                return obj2.equals((XObject) this);
            }
            if (1 == t) {
                return obj2.bool() == bool();
            }
            if (2 == t) {
                return obj2.num() == num();
            }
            return xstr().equals(obj2.xstr());
        } catch (TransformerException te) {
            throw new WrappedRuntimeException(te);
        }
    }

    @Override
    public boolean equals(String obj2) {
        return str().equals(obj2);
    }

    @Override
    public boolean equals(XMLString obj2) {
        if (obj2 != null) {
            if (!obj2.hasString()) {
                return obj2.equals(str());
            }
            return str().equals(obj2.toString());
        }
        return false;
    }

    @Override
    public boolean equals(Object obj2) {
        if (obj2 == null) {
            return false;
        }
        if (obj2 instanceof XNodeSet) {
            return obj2.equals(this);
        }
        if (obj2 instanceof XNumber) {
            return obj2.equals(this);
        }
        return str().equals(obj2.toString());
    }

    @Override
    public boolean equalsIgnoreCase(String anotherString) {
        return str().equalsIgnoreCase(anotherString);
    }

    @Override
    public int compareTo(XMLString xstr) {
        int len1 = length();
        int len2 = xstr.length();
        int n = Math.min(len1, len2);
        int i = 0;
        int j = 0;
        while (true) {
            int n2 = n;
            n = n2 - 1;
            if (n2 != 0) {
                char c1 = charAt(i);
                char c2 = xstr.charAt(j);
                if (c1 != c2) {
                    return c1 - c2;
                }
                i++;
                j++;
            } else {
                return len1 - len2;
            }
        }
    }

    @Override
    public int compareToIgnoreCase(XMLString str) {
        throw new WrappedRuntimeException(new NoSuchMethodException("Java 1.2 method, not yet implemented"));
    }

    @Override
    public boolean startsWith(String prefix, int toffset) {
        return str().startsWith(prefix, toffset);
    }

    @Override
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    @Override
    public boolean startsWith(XMLString prefix, int toffset) {
        int to = toffset;
        int tlim = length();
        int po = 0;
        int pc = prefix.length();
        if (toffset < 0 || toffset > tlim - pc) {
            return false;
        }
        while (true) {
            pc--;
            if (pc >= 0) {
                if (charAt(to) != prefix.charAt(po)) {
                    return false;
                }
                to++;
                po++;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean startsWith(XMLString prefix) {
        return startsWith(prefix, 0);
    }

    @Override
    public boolean endsWith(String suffix) {
        return str().endsWith(suffix);
    }

    @Override
    public int hashCode() {
        return str().hashCode();
    }

    @Override
    public int indexOf(int ch) {
        return str().indexOf(ch);
    }

    @Override
    public int indexOf(int ch, int fromIndex) {
        return str().indexOf(ch, fromIndex);
    }

    @Override
    public int lastIndexOf(int ch) {
        return str().lastIndexOf(ch);
    }

    @Override
    public int lastIndexOf(int ch, int fromIndex) {
        return str().lastIndexOf(ch, fromIndex);
    }

    @Override
    public int indexOf(String str) {
        return str().indexOf(str);
    }

    @Override
    public int indexOf(XMLString str) {
        return str().indexOf(str.toString());
    }

    @Override
    public int indexOf(String str, int fromIndex) {
        return str().indexOf(str, fromIndex);
    }

    @Override
    public int lastIndexOf(String str) {
        return str().lastIndexOf(str);
    }

    @Override
    public int lastIndexOf(String str, int fromIndex) {
        return str().lastIndexOf(str, fromIndex);
    }

    @Override
    public XMLString substring(int beginIndex) {
        return new XString(str().substring(beginIndex));
    }

    @Override
    public XMLString substring(int beginIndex, int endIndex) {
        return new XString(str().substring(beginIndex, endIndex));
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

    private static boolean isSpace(char ch) {
        return XMLCharacterRecognizer.isWhiteSpace(ch);
    }

    @Override
    public XMLString fixWhiteSpace(boolean trimHead, boolean trimTail, boolean doublePunctuationSpaces) {
        int d;
        int d2;
        int len = length();
        char[] buf = new char[len];
        getChars(0, len, buf, 0);
        boolean edit = false;
        int s = 0;
        while (s < len && !isSpace(buf[s])) {
            s++;
        }
        int d3 = s;
        boolean pres = false;
        int d4 = d3;
        while (s < len) {
            char c = buf[s];
            if (isSpace(c)) {
                if (!pres) {
                    if (' ' != c) {
                        edit = true;
                    }
                    d2 = d4 + 1;
                    buf[d4] = ' ';
                    if (doublePunctuationSpaces && s != 0) {
                        char prevChar = buf[s - 1];
                        if (prevChar != '.' && prevChar != '!' && prevChar != '?') {
                            pres = true;
                        }
                    } else {
                        pres = true;
                    }
                } else {
                    edit = true;
                    pres = true;
                    d2 = d4;
                }
            } else {
                d2 = d4 + 1;
                buf[d4] = c;
                pres = false;
            }
            s++;
            d4 = d2;
        }
        if (trimTail && 1 <= d4 && ' ' == buf[d4 - 1]) {
            edit = true;
            d = d4 - 1;
        } else {
            d = d4;
        }
        int start = 0;
        if (trimHead && d > 0 && ' ' == buf[0]) {
            edit = true;
            start = 1;
        }
        XMLStringFactory xsf = XMLStringFactoryImpl.getFactory();
        return edit ? xsf.newstr(new String(buf, start, d - start)) : this;
    }

    @Override
    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        visitor.visitStringLiteral(owner, this);
    }
}
