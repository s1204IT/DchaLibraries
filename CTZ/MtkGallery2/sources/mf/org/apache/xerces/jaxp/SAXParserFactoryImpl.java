package mf.org.apache.xerces.jaxp;

import java.util.Hashtable;
import mf.javax.xml.parsers.ParserConfigurationException;
import mf.javax.xml.parsers.SAXParser;
import mf.javax.xml.parsers.SAXParserFactory;
import mf.javax.xml.validation.Schema;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class SAXParserFactoryImpl extends SAXParserFactory {
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
    private static final String XINCLUDE_FEATURE = "http://apache.org/xml/features/xinclude";
    private boolean fSecureProcess = false;
    private Hashtable features;
    private Schema grammar;
    private boolean isXIncludeAware;

    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException {
        try {
            SAXParser saxParserImpl = new SAXParserImpl(this, this.features, this.fSecureProcess);
            return saxParserImpl;
        } catch (SAXException se) {
            throw new ParserConfigurationException(se.getMessage());
        }
    }

    private SAXParserImpl newSAXParserImpl() throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        try {
            SAXParserImpl saxParserImpl = new SAXParserImpl(this, this.features);
            return saxParserImpl;
        } catch (SAXNotRecognizedException e) {
            throw e;
        } catch (SAXNotSupportedException e2) {
            throw e2;
        } catch (SAXException se) {
            throw new ParserConfigurationException(se.getMessage());
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            this.fSecureProcess = value;
            return;
        }
        if (name.equals(NAMESPACES_FEATURE)) {
            setNamespaceAware(value);
            return;
        }
        if (name.equals(VALIDATION_FEATURE)) {
            setValidating(value);
            return;
        }
        if (name.equals(XINCLUDE_FEATURE)) {
            setXIncludeAware(value);
            return;
        }
        if (this.features == null) {
            this.features = new Hashtable();
        }
        this.features.put(name, value ? Boolean.TRUE : Boolean.FALSE);
        try {
            newSAXParserImpl();
        } catch (SAXNotRecognizedException e) {
            this.features.remove(name);
            throw e;
        } catch (SAXNotSupportedException e2) {
            this.features.remove(name);
            throw e2;
        }
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        if (name == null) {
            throw new NullPointerException();
        }
        if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            return this.fSecureProcess;
        }
        if (name.equals(NAMESPACES_FEATURE)) {
            return isNamespaceAware();
        }
        if (name.equals(VALIDATION_FEATURE)) {
            return isValidating();
        }
        if (name.equals(XINCLUDE_FEATURE)) {
            return isXIncludeAware();
        }
        return newSAXParserImpl().getXMLReader().getFeature(name);
    }

    @Override
    public Schema getSchema() {
        return this.grammar;
    }

    @Override
    public void setSchema(Schema grammar) {
        this.grammar = grammar;
    }

    @Override
    public boolean isXIncludeAware() {
        return this.isXIncludeAware;
    }

    @Override
    public void setXIncludeAware(boolean state) {
        this.isXIncludeAware = state;
    }
}
