package org.xml.sax.helpers;

import org.xml.sax.Parser;

@Deprecated
public class ParserFactory {
    private ParserFactory() {
    }

    public static Parser makeParser() throws IllegalAccessException, InstantiationException, ClassNotFoundException, ClassCastException, NullPointerException {
        String className = System.getProperty("org.xml.sax.parser");
        if (className == null) {
            throw new NullPointerException("No value for sax.parser property");
        }
        return makeParser(className);
    }

    public static Parser makeParser(String className) throws IllegalAccessException, InstantiationException, ClassNotFoundException, ClassCastException {
        return (Parser) NewInstance.newInstance(NewInstance.getClassLoader(), className);
    }
}
