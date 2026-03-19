package javax.xml.xpath;

public abstract class XPathFactory {
    public static final String DEFAULT_OBJECT_MODEL_URI = "http://java.sun.com/jaxp/xpath/dom";
    public static final String DEFAULT_PROPERTY_NAME = "javax.xml.xpath.XPathFactory";

    public abstract boolean getFeature(String str) throws XPathFactoryConfigurationException;

    public abstract boolean isObjectModelSupported(String str);

    public abstract XPath newXPath();

    public abstract void setFeature(String str, boolean z) throws XPathFactoryConfigurationException;

    public abstract void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver);

    public abstract void setXPathVariableResolver(XPathVariableResolver xPathVariableResolver);

    protected XPathFactory() {
    }

    public static final XPathFactory newInstance() {
        try {
            return newInstance("http://java.sun.com/jaxp/xpath/dom");
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException("XPathFactory#newInstance() failed to create an XPathFactory for the default object model: http://java.sun.com/jaxp/xpath/dom with the XPathFactoryConfigurationException: " + e.toString());
        }
    }

    public static final XPathFactory newInstance(String str) throws XPathFactoryConfigurationException {
        if (str == null) {
            throw new NullPointerException("uri == null");
        }
        if (str.length() == 0) {
            throw new IllegalArgumentException("XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader == null) {
            contextClassLoader = XPathFactory.class.getClassLoader();
        }
        XPathFactory xPathFactoryNewFactory = new XPathFactoryFinder(contextClassLoader).newFactory(str);
        if (xPathFactoryNewFactory == null) {
            throw new XPathFactoryConfigurationException("No XPathFactory implementation found for the object model: " + str);
        }
        return xPathFactoryNewFactory;
    }

    public static XPathFactory newInstance(String str, String str2, ClassLoader classLoader) throws XPathFactoryConfigurationException {
        if (str == null) {
            throw new NullPointerException("uri == null");
        }
        if (str.length() == 0) {
            throw new IllegalArgumentException("XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        if (str2 == null) {
            throw new XPathFactoryConfigurationException("factoryClassName cannot be null.");
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        XPathFactory xPathFactoryCreateInstance = new XPathFactoryFinder(classLoader).createInstance(str2);
        if (xPathFactoryCreateInstance == null || !xPathFactoryCreateInstance.isObjectModelSupported(str)) {
            throw new XPathFactoryConfigurationException("No XPathFactory implementation found for the object model: " + str);
        }
        return xPathFactoryCreateInstance;
    }
}
