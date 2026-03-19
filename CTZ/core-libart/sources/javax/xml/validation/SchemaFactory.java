package javax.xml.validation;

import java.io.File;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public abstract class SchemaFactory {
    public abstract ErrorHandler getErrorHandler();

    public abstract LSResourceResolver getResourceResolver();

    public abstract boolean isSchemaLanguageSupported(String str);

    public abstract Schema newSchema() throws SAXException;

    public abstract Schema newSchema(Source[] sourceArr) throws SAXException;

    public abstract void setErrorHandler(ErrorHandler errorHandler);

    public abstract void setResourceResolver(LSResourceResolver lSResourceResolver);

    protected SchemaFactory() {
    }

    public static SchemaFactory newInstance(String str) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader == null) {
            contextClassLoader = SchemaFactory.class.getClassLoader();
        }
        SchemaFactory schemaFactoryNewFactory = new SchemaFactoryFinder(contextClassLoader).newFactory(str);
        if (schemaFactoryNewFactory == null) {
            throw new IllegalArgumentException(str);
        }
        return schemaFactoryNewFactory;
    }

    public static SchemaFactory newInstance(String str, String str2, ClassLoader classLoader) {
        Class<?> cls;
        if (str == null) {
            throw new NullPointerException("schemaLanguage == null");
        }
        if (str2 == null) {
            throw new NullPointerException("factoryClassName == null");
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        try {
            if (classLoader != null) {
                cls = classLoader.loadClass(str2);
            } else {
                cls = Class.forName(str2);
            }
            SchemaFactory schemaFactory = (SchemaFactory) cls.newInstance();
            if (schemaFactory == null || !schemaFactory.isSchemaLanguageSupported(str)) {
                throw new IllegalArgumentException(str);
            }
            return schemaFactory;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException(e2);
        } catch (InstantiationException e3) {
            throw new IllegalArgumentException(e3);
        }
    }

    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        throw new SAXNotRecognizedException(str);
    }

    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        throw new SAXNotRecognizedException(str);
    }

    public void setProperty(String str, Object obj) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        throw new SAXNotRecognizedException(str);
    }

    public Object getProperty(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        throw new SAXNotRecognizedException(str);
    }

    public Schema newSchema(Source source) throws SAXException {
        return newSchema(new Source[]{source});
    }

    public Schema newSchema(File file) throws SAXException {
        return newSchema(new StreamSource(file));
    }

    public Schema newSchema(URL url) throws SAXException {
        return newSchema(new StreamSource(url.toExternalForm()));
    }
}
