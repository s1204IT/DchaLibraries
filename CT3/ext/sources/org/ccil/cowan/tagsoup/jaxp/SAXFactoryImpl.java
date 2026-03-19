package org.ccil.cowan.tagsoup.jaxp;

import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class SAXFactoryImpl extends SAXParserFactory {
    private SAXParserImpl prototypeParser = null;
    private HashMap features = null;

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException {
        try {
            return SAXParserImpl.newInstance(this.features);
        } catch (SAXException se) {
            throw new ParserConfigurationException(se.getMessage());
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        getPrototype().setFeature(name, value);
        if (this.features == null) {
            this.features = new LinkedHashMap();
        }
        this.features.put(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        return getPrototype().getFeature(name);
    }

    private SAXParserImpl getPrototype() {
        if (this.prototypeParser == null) {
            this.prototypeParser = new SAXParserImpl();
        }
        return this.prototypeParser;
    }
}
