package mf.org.apache.xerces.jaxp;

import java.util.Hashtable;
import mf.javax.xml.parsers.DocumentBuilder;
import mf.javax.xml.parsers.DocumentBuilderFactory;
import mf.javax.xml.parsers.ParserConfigurationException;
import mf.javax.xml.validation.Schema;
import mf.org.apache.xerces.parsers.DOMParser;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class DocumentBuilderFactoryImpl extends DocumentBuilderFactory {
    private static final String CREATE_CDATA_NODES_FEATURE = "http://apache.org/xml/features/create-cdata-nodes";
    private static final String CREATE_ENTITY_REF_NODES_FEATURE = "http://apache.org/xml/features/dom/create-entity-ref-nodes";
    private static final String INCLUDE_COMMENTS_FEATURE = "http://apache.org/xml/features/include-comments";
    private static final String INCLUDE_IGNORABLE_WHITESPACE = "http://apache.org/xml/features/dom/include-ignorable-whitespace";
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";
    private static final String XINCLUDE_FEATURE = "http://apache.org/xml/features/xinclude";
    private Hashtable attributes;
    private boolean fSecureProcess = false;
    private Hashtable features;
    private Schema grammar;
    private boolean isXIncludeAware;

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        if (this.grammar != null && this.attributes != null) {
            if (this.attributes.containsKey(JAXPConstants.JAXP_SCHEMA_LANGUAGE)) {
                throw new ParserConfigurationException(SAXMessageFormatter.formatMessage(null, "schema-already-specified", new Object[]{JAXPConstants.JAXP_SCHEMA_LANGUAGE}));
            }
            if (this.attributes.containsKey(JAXPConstants.JAXP_SCHEMA_SOURCE)) {
                throw new ParserConfigurationException(SAXMessageFormatter.formatMessage(null, "schema-already-specified", new Object[]{JAXPConstants.JAXP_SCHEMA_SOURCE}));
            }
        }
        try {
            return new DocumentBuilderImpl(this, this.attributes, this.features, this.fSecureProcess);
        } catch (SAXException se) {
            throw new ParserConfigurationException(se.getMessage());
        }
    }

    @Override
    public void setAttribute(String name, Object value) throws IllegalArgumentException {
        if (value == null) {
            if (this.attributes != null) {
                this.attributes.remove(name);
                return;
            }
            return;
        }
        if (this.attributes == null) {
            this.attributes = new Hashtable();
        }
        this.attributes.put(name, value);
        try {
            new DocumentBuilderImpl(this, this.attributes, this.features);
        } catch (Exception e) {
            this.attributes.remove(name);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public Object getAttribute(String name) throws IllegalArgumentException {
        Object val;
        if (this.attributes != null && (val = this.attributes.get(name)) != null) {
            return val;
        }
        DOMParser domParser = null;
        try {
            domParser = new DocumentBuilderImpl(this, this.attributes, this.features).getDOMParser();
            return domParser.getProperty(name);
        } catch (SAXException se1) {
            try {
                boolean result = domParser.getFeature(name);
                return result ? Boolean.TRUE : Boolean.FALSE;
            } catch (SAXException e) {
                throw new IllegalArgumentException(se1.getMessage());
            }
        }
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

    @Override
    public boolean getFeature(String name) throws ParserConfigurationException {
        Object val;
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
        if (name.equals(INCLUDE_IGNORABLE_WHITESPACE)) {
            return !isIgnoringElementContentWhitespace();
        }
        if (name.equals(CREATE_ENTITY_REF_NODES_FEATURE)) {
            return !isExpandEntityReferences();
        }
        if (name.equals(INCLUDE_COMMENTS_FEATURE)) {
            return !isIgnoringComments();
        }
        if (name.equals(CREATE_CDATA_NODES_FEATURE)) {
            return !isCoalescing();
        }
        if (this.features != null && (val = this.features.get(name)) != null) {
            return ((Boolean) val).booleanValue();
        }
        try {
            DOMParser domParser = new DocumentBuilderImpl(this, this.attributes, this.features).getDOMParser();
            return domParser.getFeature(name);
        } catch (SAXException e) {
            throw new ParserConfigurationException(e.getMessage());
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws ParserConfigurationException {
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
        if (name.equals(INCLUDE_IGNORABLE_WHITESPACE)) {
            setIgnoringElementContentWhitespace(!value);
            return;
        }
        if (name.equals(CREATE_ENTITY_REF_NODES_FEATURE)) {
            setExpandEntityReferences(!value);
            return;
        }
        if (name.equals(INCLUDE_COMMENTS_FEATURE)) {
            setIgnoringComments(!value);
            return;
        }
        if (name.equals(CREATE_CDATA_NODES_FEATURE)) {
            setCoalescing(!value);
            return;
        }
        if (this.features == null) {
            this.features = new Hashtable();
        }
        this.features.put(name, value ? Boolean.TRUE : Boolean.FALSE);
        try {
            new DocumentBuilderImpl(this, this.attributes, this.features);
        } catch (SAXNotRecognizedException e) {
            this.features.remove(name);
            throw new ParserConfigurationException(e.getMessage());
        } catch (SAXNotSupportedException e2) {
            this.features.remove(name);
            throw new ParserConfigurationException(e2.getMessage());
        }
    }
}
