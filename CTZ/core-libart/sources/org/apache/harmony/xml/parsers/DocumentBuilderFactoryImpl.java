package org.apache.harmony.xml.parsers;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DocumentBuilderFactoryImpl extends DocumentBuilderFactory {
    private static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    private static final String VALIDATION = "http://xml.org/sax/features/validation";

    @Override
    public Object getAttribute(String str) throws IllegalArgumentException {
        throw new IllegalArgumentException(str);
    }

    @Override
    public boolean getFeature(String str) throws ParserConfigurationException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (NAMESPACES.equals(str)) {
            return isNamespaceAware();
        }
        if (VALIDATION.equals(str)) {
            return isValidating();
        }
        throw new ParserConfigurationException(str);
    }

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        if (isValidating()) {
            throw new ParserConfigurationException("No validating DocumentBuilder implementation available");
        }
        DocumentBuilderImpl documentBuilderImpl = new DocumentBuilderImpl();
        documentBuilderImpl.setCoalescing(isCoalescing());
        documentBuilderImpl.setIgnoreComments(isIgnoringComments());
        documentBuilderImpl.setIgnoreElementContentWhitespace(isIgnoringElementContentWhitespace());
        documentBuilderImpl.setNamespaceAware(isNamespaceAware());
        return documentBuilderImpl;
    }

    @Override
    public void setAttribute(String str, Object obj) throws IllegalArgumentException {
        throw new IllegalArgumentException(str);
    }

    @Override
    public void setFeature(String str, boolean z) throws ParserConfigurationException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (NAMESPACES.equals(str)) {
            setNamespaceAware(z);
        } else {
            if (VALIDATION.equals(str)) {
                setValidating(z);
                return;
            }
            throw new ParserConfigurationException(str);
        }
    }
}
