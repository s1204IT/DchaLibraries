package org.apache.xpath.objects;

import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xpath.res.XPATHErrorResources;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XStringForChars extends XString {
    static final long serialVersionUID = -2235248887220850467L;
    int m_length;
    int m_start;
    protected String m_strCache;

    public XStringForChars(char[] val, int start, int length) {
        super(val);
        this.m_strCache = null;
        this.m_start = start;
        this.m_length = length;
        if (val != null) {
        } else {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FASTSTRINGBUFFER_CANNOT_BE_NULL, null));
        }
    }

    private XStringForChars(String val) {
        super(val);
        this.m_strCache = null;
        throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING, null));
    }

    public FastStringBuffer fsb() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS, null));
    }

    @Override
    public void appendToFsb(FastStringBuffer fsb) {
        fsb.append((char[]) this.m_obj, this.m_start, this.m_length);
    }

    @Override
    public boolean hasString() {
        return this.m_strCache != null;
    }

    @Override
    public String str() {
        if (this.m_strCache == null) {
            this.m_strCache = new String((char[]) this.m_obj, this.m_start, this.m_length);
        }
        return this.m_strCache;
    }

    @Override
    public Object object() {
        return str();
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler ch) throws SAXException {
        ch.characters((char[]) this.m_obj, this.m_start, this.m_length);
    }

    @Override
    public void dispatchAsComment(LexicalHandler lh) throws SAXException {
        lh.comment((char[]) this.m_obj, this.m_start, this.m_length);
    }

    @Override
    public int length() {
        return this.m_length;
    }

    @Override
    public char charAt(int index) {
        return ((char[]) this.m_obj)[this.m_start + index];
    }

    @Override
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        System.arraycopy((char[]) this.m_obj, this.m_start + srcBegin, dst, dstBegin, srcEnd);
    }
}
