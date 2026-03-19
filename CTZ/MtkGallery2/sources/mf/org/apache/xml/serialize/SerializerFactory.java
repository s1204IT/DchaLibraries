package mf.org.apache.xml.serialize;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Hashtable;
import java.util.StringTokenizer;

public abstract class SerializerFactory {
    private static Hashtable _factories = new Hashtable();

    protected abstract String getSupportedMethod();

    public abstract Serializer makeSerializer(OutputStream outputStream, OutputFormat outputFormat) throws UnsupportedEncodingException;

    public abstract Serializer makeSerializer(Writer writer, OutputFormat outputFormat);

    static {
        SerializerFactory factory = new SerializerFactoryImpl("xml");
        registerSerializerFactory(factory);
        SerializerFactory factory2 = new SerializerFactoryImpl("html");
        registerSerializerFactory(factory2);
        SerializerFactory factory3 = new SerializerFactoryImpl("xhtml");
        registerSerializerFactory(factory3);
        SerializerFactory factory4 = new SerializerFactoryImpl("text");
        registerSerializerFactory(factory4);
        String list = SecuritySupport.getSystemProperty("org.apache.xml.serialize.factories");
        if (list != null) {
            StringTokenizer token = new StringTokenizer(list, " ;,:");
            while (token.hasMoreTokens()) {
                String className = token.nextToken();
                try {
                    SerializerFactory factory5 = (SerializerFactory) ObjectFactory.newInstance(className, SerializerFactory.class.getClassLoader(), true);
                    if (_factories.containsKey(factory5.getSupportedMethod())) {
                        _factories.put(factory5.getSupportedMethod(), factory5);
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public static void registerSerializerFactory(SerializerFactory factory) {
        synchronized (_factories) {
            String method = factory.getSupportedMethod();
            _factories.put(method, factory);
        }
    }

    public static SerializerFactory getSerializerFactory(String method) {
        return (SerializerFactory) _factories.get(method);
    }
}
