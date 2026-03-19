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
    public void dispatchCharactersEvents(ContentHandler contentHandler) throws SAXException {
    }

    @Override
    public void dispatchAsComment(LexicalHandler lexicalHandler) throws SAXException {
    }

    @Override
    public XMLString fixWhiteSpace(boolean z, boolean z2, boolean z3) {
        return new XMLStringDefault(this.m_str.trim());
    }

    @Override
    public int length() {
        return this.m_str.length();
    }

    @Override
    public char charAt(int i) {
        return this.m_str.charAt(i);
    }

    @Override
    public void getChars(int i, int i2, char[] cArr, int i3) {
        while (i < i2) {
            cArr[i3] = this.m_str.charAt(i);
            i++;
            i3++;
        }
    }

    @Override
    public boolean equals(String str) {
        return this.m_str.equals(str);
    }

    @Override
    public boolean equals(XMLString xMLString) {
        return this.m_str.equals(xMLString.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return this.m_str.equals(obj);
    }

    @Override
    public boolean equalsIgnoreCase(String str) {
        return this.m_str.equalsIgnoreCase(str);
    }

    @Override
    public int compareTo(XMLString xMLString) {
        return this.m_str.compareTo(xMLString.toString());
    }

    @Override
    public int compareToIgnoreCase(XMLString xMLString) {
        return this.m_str.compareToIgnoreCase(xMLString.toString());
    }

    @Override
    public boolean startsWith(String str, int i) {
        return this.m_str.startsWith(str, i);
    }

    @Override
    public boolean startsWith(XMLString xMLString, int i) {
        return this.m_str.startsWith(xMLString.toString(), i);
    }

    @Override
    public boolean startsWith(String str) {
        return this.m_str.startsWith(str);
    }

    @Override
    public boolean startsWith(XMLString xMLString) {
        return this.m_str.startsWith(xMLString.toString());
    }

    @Override
    public boolean endsWith(String str) {
        return this.m_str.endsWith(str);
    }

    @Override
    public int hashCode() {
        return this.m_str.hashCode();
    }

    @Override
    public int indexOf(int i) {
        return this.m_str.indexOf(i);
    }

    @Override
    public int indexOf(int i, int i2) {
        return this.m_str.indexOf(i, i2);
    }

    @Override
    public int lastIndexOf(int i) {
        return this.m_str.lastIndexOf(i);
    }

    @Override
    public int lastIndexOf(int i, int i2) {
        return this.m_str.lastIndexOf(i, i2);
    }

    @Override
    public int indexOf(String str) {
        return this.m_str.indexOf(str);
    }

    @Override
    public int indexOf(XMLString xMLString) {
        return this.m_str.indexOf(xMLString.toString());
    }

    @Override
    public int indexOf(String str, int i) {
        return this.m_str.indexOf(str, i);
    }

    @Override
    public int lastIndexOf(String str) {
        return this.m_str.lastIndexOf(str);
    }

    @Override
    public int lastIndexOf(String str, int i) {
        return this.m_str.lastIndexOf(str, i);
    }

    @Override
    public XMLString substring(int i) {
        return new XMLStringDefault(this.m_str.substring(i));
    }

    @Override
    public XMLString substring(int i, int i2) {
        return new XMLStringDefault(this.m_str.substring(i, i2));
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
