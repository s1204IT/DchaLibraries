package org.ccil.cowan.tagsoup.jaxp;

import java.util.Map;
import javax.xml.parsers.SAXParser;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class SAXParserImpl extends SAXParser {
    final Parser parser = new Parser();

    protected SAXParserImpl() {
    }

    public static SAXParserImpl newInstance(Map features) throws SAXException {
        SAXParserImpl parser = new SAXParserImpl();
        if (features != null) {
            for (Map.Entry entry : features.entrySet()) {
                parser.setFeature((String) entry.getKey(), ((Boolean) entry.getValue()).booleanValue());
            }
        }
        return parser;
    }

    @Override
    public org.xml.sax.Parser getParser() throws SAXException {
        return new SAX1ParserAdapter(this.parser);
    }

    @Override
    public XMLReader getXMLReader() {
        return this.parser;
    }

    @Override
    public boolean isNamespaceAware() {
        try {
            return this.parser.getFeature(Parser.namespacesFeature);
        } catch (SAXException sex) {
            throw new RuntimeException(sex.getMessage());
        }
    }

    @Override
    public boolean isValidating() {
        try {
            return this.parser.getFeature(Parser.validationFeature);
        } catch (SAXException sex) {
            throw new RuntimeException(sex.getMessage());
        }
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        this.parser.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.parser.getProperty(name);
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        this.parser.setFeature(name, value);
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return this.parser.getFeature(name);
    }
}
