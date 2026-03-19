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
    public XMLString newstr(String str) {
        return new XString(str);
    }

    @Override
    public XMLString newstr(FastStringBuffer fastStringBuffer, int i, int i2) {
        return new XStringForFSB(fastStringBuffer, i, i2);
    }

    @Override
    public XMLString newstr(char[] cArr, int i, int i2) {
        return new XStringForChars(cArr, i, i2);
    }

    @Override
    public XMLString emptystr() {
        return XString.EMPTYSTRING;
    }
}
