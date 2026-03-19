package org.xml.sax.helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public final class XMLReaderFactory {
    private static final String property = "org.xml.sax.driver";

    private XMLReaderFactory() {
    }

    public static XMLReader createXMLReader() throws SAXException {
        String property2;
        InputStream resourceAsStream;
        ClassLoader classLoader = NewInstance.getClassLoader();
        try {
            property2 = System.getProperty(property);
        } catch (RuntimeException e) {
            property2 = null;
        }
        if (property2 == null) {
            try {
                if (classLoader == null) {
                    resourceAsStream = ClassLoader.getSystemResourceAsStream("META-INF/services/org.xml.sax.driver");
                } else {
                    resourceAsStream = classLoader.getResourceAsStream("META-INF/services/org.xml.sax.driver");
                }
                if (resourceAsStream != null) {
                    try {
                        String line = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8)).readLine();
                        try {
                            property2 = line;
                        } catch (Exception e2) {
                            property2 = line;
                        }
                    } finally {
                        resourceAsStream.close();
                    }
                }
            } catch (Exception e3) {
            }
        }
        if (property2 != null) {
            return loadClass(classLoader, property2);
        }
        try {
            return new ParserAdapter(ParserFactory.makeParser());
        } catch (Exception e4) {
            throw new SAXException("Can't create default XMLReader; is system property org.xml.sax.driver set?");
        }
    }

    public static XMLReader createXMLReader(String str) throws SAXException {
        return loadClass(NewInstance.getClassLoader(), str);
    }

    private static XMLReader loadClass(ClassLoader classLoader, String str) throws SAXException {
        try {
            return (XMLReader) NewInstance.newInstance(classLoader, str);
        } catch (ClassCastException e) {
            throw new SAXException("SAX2 driver class " + str + " does not implement XMLReader", e);
        } catch (ClassNotFoundException e2) {
            throw new SAXException("SAX2 driver class " + str + " not found", e2);
        } catch (IllegalAccessException e3) {
            throw new SAXException("SAX2 driver class " + str + " found but cannot be loaded", e3);
        } catch (InstantiationException e4) {
            throw new SAXException("SAX2 driver class " + str + " loaded but cannot be instantiated (no empty public constructor?)", e4);
        }
    }
}
