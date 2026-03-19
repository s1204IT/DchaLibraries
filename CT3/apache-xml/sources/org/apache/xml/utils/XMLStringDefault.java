package org.apache.xml.utils;

import java.util.Locale;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class XMLStringDefault implements XMLString {
    private String m_str;

    public XMLStringDefault(String str) {
        this.m_str = str;
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler ch) throws SAXException {
    }

    @Override
    public void dispatchAsComment(LexicalHandler lh) throws SAXException {
    }

    @Override
    public XMLString fixWhiteSpace(boolean trimHead, boolean trimTail, boolean doublePunctuationSpaces) {
        return new XMLStringDefault(this.m_str.trim());
    }

    @Override
    public int length() {
        return this.m_str.length();
    }

    @Override
    public char charAt(int index) {
        return this.m_str.charAt(index);
    }

    @Override
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        int i = srcBegin;
        int destIndex = dstBegin;
        while (i < srcEnd) {
            dst[destIndex] = this.m_str.charAt(i);
            i++;
            destIndex++;
        }
    }

    @Override
    public boolean equals(String obj2) {
        return this.m_str.equals(obj2);
    }

    @Override
    public boolean equals(XMLString anObject) {
        return this.m_str.equals(anObject.toString());
    }

    @Override
    public boolean equals(Object anObject) {
        return this.m_str.equals(anObject);
    }

    @Override
    public boolean equalsIgnoreCase(String anotherString) {
        return this.m_str.equalsIgnoreCase(anotherString);
    }

    @Override
    public int compareTo(XMLString anotherString) {
        return this.m_str.compareTo(anotherString.toString());
    }

    @Override
    public int compareToIgnoreCase(XMLString str) {
        return this.m_str.compareToIgnoreCase(str.toString());
    }

    @Override
    public boolean startsWith(String prefix, int toffset) {
        return this.m_str.startsWith(prefix, toffset);
    }

    @Override
    public boolean startsWith(XMLString prefix, int toffset) {
        return this.m_str.startsWith(prefix.toString(), toffset);
    }

    @Override
    public boolean startsWith(String prefix) {
        return this.m_str.startsWith(prefix);
    }

    @Override
    public boolean startsWith(XMLString prefix) {
        return this.m_str.startsWith(prefix.toString());
    }

    @Override
    public boolean endsWith(String suffix) {
        return this.m_str.endsWith(suffix);
    }

    @Override
    public int hashCode() {
        return this.m_str.hashCode();
    }

    @Override
    public int indexOf(int ch) {
        return this.m_str.indexOf(ch);
    }

    @Override
    public int indexOf(int ch, int fromIndex) {
        return this.m_str.indexOf(ch, fromIndex);
    }

    @Override
    public int lastIndexOf(int ch) {
        return this.m_str.lastIndexOf(ch);
    }

    @Override
    public int lastIndexOf(int ch, int fromIndex) {
        return this.m_str.lastIndexOf(ch, fromIndex);
    }

    @Override
    public int indexOf(String str) {
        return this.m_str.indexOf(str);
    }

    @Override
    public int indexOf(XMLString str) {
        return this.m_str.indexOf(str.toString());
    }

    @Override
    public int indexOf(String str, int fromIndex) {
        return this.m_str.indexOf(str, fromIndex);
    }

    @Override
    public int lastIndexOf(String str) {
        return this.m_str.lastIndexOf(str);
    }

    @Override
    public int lastIndexOf(String str, int fromIndex) {
        return this.m_str.lastIndexOf(str, fromIndex);
    }

    @Override
    public XMLString substring(int beginIndex) {
        return new XMLStringDefault(this.m_str.substring(beginIndex));
    }

    @Override
    public XMLString substring(int beginIndex, int endIndex) {
        return new XMLStringDefault(this.m_str.substring(beginIndex, endIndex));
    }

    @Override
    public XMLString concat(String str) {
        return new XMLStringDefault(this.m_str.concat(str));
    }

    @Override
    public XMLString toLowerCase(Locale locale) {
        return new XMLStringDefault(this.m_str.toLowerCase(locale));
    }

    @Override
    public XMLString toLowerCase() {
        return new XMLStringDefault(this.m_str.toLowerCase());
    }

    @Override
    public XMLString toUpperCase(Locale locale) {
        return new XMLStringDefault(this.m_str.toUpperCase(locale));
    }

    @Override
    public XMLString toUpperCase() {
        return new XMLStringDefault(this.m_str.toUpperCase());
    }

    @Override
    public XMLString trim() {
        return new XMLStringDefault(this.m_str.trim());
    }

    @Override
    public String toString() {
        return this.m_str;
    }

    @Override
    public boolean hasString() {
        return true;
    }

    @Override
    public double toDouble() {
        try {
            return Double.valueOf(this.m_str).doubleValue();
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
