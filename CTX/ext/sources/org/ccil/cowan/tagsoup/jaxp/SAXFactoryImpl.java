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
        } catch (SAXException e) {
            throw new ParserConfigurationException(e.getMessage());
        }
    }

    @Override
    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        getPrototype().setFeature(str, z);
        if (this.features == null) {
            this.features = new LinkedHashMap();
        }
        this.features.put(str, z ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        return getPrototype().getFeature(str);
    }

    private SAXParserImpl getPrototype() {
        if (this.prototypeParser == null) {
            this.prototypeParser = new SAXParserImpl();
        }
        return this.prototypeParser;
    }
}
