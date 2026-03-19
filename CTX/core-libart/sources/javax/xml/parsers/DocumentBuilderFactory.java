package javax.xml.parsers;

import javax.xml.validation.Schema;
import org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl;

public abstract class DocumentBuilderFactory {
    private boolean validating = false;
    private boolean namespaceAware = false;
    private boolean whitespace = false;
    private boolean expandEntityRef = true;
    private boolean ignoreComments = false;
    private boolean coalescing = false;

    public abstract Object getAttribute(String str) throws IllegalArgumentException;

    public abstract boolean getFeature(String str) throws ParserConfigurationException;

    public abstract DocumentBuilder newDocumentBuilder() throws ParserConfigurationException;

    public abstract void setAttribute(String str, Object obj) throws IllegalArgumentException;

    public abstract void setFeature(String str, boolean z) throws ParserConfigurationException;

    protected DocumentBuilderFactory() {
    }

    public static DocumentBuilderFactory newInstance() {
        return new DocumentBuilderFactoryImpl();
    }

    public static DocumentBuilderFactory newInstance(String str, ClassLoader classLoader) {
        Class<?> cls;
        if (str == null) {
            throw new FactoryConfigurationError("factoryClassName == null");
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        try {
            if (classLoader != null) {
                cls = classLoader.loadClass(str);
            } else {
                cls = Class.forName(str);
            }
            return (DocumentBuilderFactory) cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new FactoryConfigurationError(e);
        } catch (IllegalAccessException e2) {
            throw new FactoryConfigurationError(e2);
        } catch (InstantiationException e3) {
            throw new FactoryConfigurationError(e3);
        }
    }

    public void setNamespaceAware(boolean z) {
        this.namespaceAware = z;
    }

    public void setValidating(boolean z) {
        this.validating = z;
    }

    public void setIgnoringElementContentWhitespace(boolean z) {
        this.whitespace = z;
    }

    public void setExpandEntityReferences(boolean z) {
        this.expandEntityRef = z;
    }

    public void setIgnoringComments(boolean z) {
        this.ignoreComments = z;
    }

    public void setCoalescing(boolean z) {
        this.coalescing = z;
    }

    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    public boolean isValidating() {
        return this.validating;
    }

    public boolean isIgnoringElementContentWhitespace() {
        return this.whitespace;
    }

    public boolean isExpandEntityReferences() {
        return this.expandEntityRef;
    }

    public boolean isIgnoringComments() {
        return this.ignoreComments;
    }

    public boolean isCoalescing() {
        return this.coalescing;
    }

    public Schema getSchema() {
        throw new UnsupportedOperationException("This parser does not support specification \"" + getClass().getPackage().getSpecificationTitle() + "\" version \"" + getClass().getPackage().getSpecificationVersion() + "\"");
    }

    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException("This parser does not support specification \"" + getClass().getPackage().getSpecificationTitle() + "\" version \"" + getClass().getPackage().getSpecificationVersion() + "\"");
    }

    public void setXIncludeAware(boolean z) {
        throw new UnsupportedOperationException("This parser does not support specification \"" + getClass().getPackage().getSpecificationTitle() + "\" version \"" + getClass().getPackage().getSpecificationVersion() + "\"");
    }

    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException("This parser does not support specification \"" + getClass().getPackage().getSpecificationTitle() + "\" version \"" + getClass().getPackage().getSpecificationVersion() + "\"");
    }
}
