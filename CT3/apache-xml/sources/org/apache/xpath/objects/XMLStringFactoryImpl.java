package org.apache.xpath.objects;

import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringFactory;

public class XMLStringFactoryImpl extends XMLStringFactory {
    private static XMLStringFactory m_xstringfactory = new XMLStringFactoryImpl();

    public static XMLStringFactory getFactory() {
        return m_xstringfactory;
    }

    @Override
    public XMLString newstr(String string) {
        return new XString(string);
    }

    @Override
    public XMLString newstr(FastStringBuffer fsb, int start, int length) {
        return new XStringForFSB(fsb, start, length);
    }

    @Override
    public XMLString newstr(char[] string, int start, int length) {
        return new XStringForChars(string, start, length);
    }

    @Override
    public XMLString emptystr() {
        return XString.EMPTYSTRING;
    }
}
