package javax.xml.transform;

public abstract class TransformerFactory {
    public abstract Source getAssociatedStylesheet(Source source, String str, String str2, String str3) throws TransformerConfigurationException;

    public abstract Object getAttribute(String str);

    public abstract ErrorListener getErrorListener();

    public abstract boolean getFeature(String str);

    public abstract URIResolver getURIResolver();

    public abstract Templates newTemplates(Source source) throws TransformerConfigurationException;

    public abstract Transformer newTransformer() throws TransformerConfigurationException;

    public abstract Transformer newTransformer(Source source) throws TransformerConfigurationException;

    public abstract void setAttribute(String str, Object obj);

    public abstract void setErrorListener(ErrorListener errorListener);

    public abstract void setFeature(String str, boolean z) throws TransformerConfigurationException;

    public abstract void setURIResolver(URIResolver uRIResolver);

    protected TransformerFactory() {
    }

    public static TransformerFactory newInstance() throws TransformerFactoryConfigurationError {
        try {
            return (TransformerFactory) Class.forName("org.apache.xalan.processor.TransformerFactoryImpl").newInstance();
        } catch (Exception e) {
            throw new NoClassDefFoundError("org.apache.xalan.processor.TransformerFactoryImpl");
        }
    }

    public static TransformerFactory newInstance(String str, ClassLoader classLoader) throws TransformerFactoryConfigurationError {
        Class<?> cls;
        if (str == null) {
            throw new TransformerFactoryConfigurationError("factoryClassName == null");
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
            return (TransformerFactory) cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new TransformerFactoryConfigurationError(e);
        } catch (IllegalAccessException e2) {
            throw new TransformerFactoryConfigurationError(e2);
        } catch (InstantiationException e3) {
            throw new TransformerFactoryConfigurationError(e3);
        }
    }
}
