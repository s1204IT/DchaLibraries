package org.xml.sax.helpers;

import org.xml.sax.Parser;

@Deprecated
public class ParserFactory {
    private ParserFactory() {
    }

    public static Parser makeParser() throws IllegalAccessException, InstantiationException, ClassNotFoundException, ClassCastException, NullPointerException {
        String property = System.getProperty("org.xml.sax.parser");
        if (property == null) {
            throw new NullPointerException("No value for sax.parser property");
        }
        return makeParser(property);
    }

    public static Parser makeParser(String str) throws IllegalAccessException, InstantiationException, ClassNotFoundException, ClassCastException {
        return (Parser) NewInstance.newInstance(NewInstance.getClassLoader(), str);
    }
}
