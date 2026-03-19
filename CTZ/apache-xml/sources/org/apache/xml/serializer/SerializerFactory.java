package org.apache.xml.serializer;

import java.util.Hashtable;
import java.util.Properties;
import org.apache.xalan.templates.Constants;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xml.serializer.utils.WrappedRuntimeException;
import org.xml.sax.ContentHandler;

public final class SerializerFactory {
    private static Hashtable m_formats = new Hashtable();

    private SerializerFactory() {
    }

    public static Serializer getSerializer(Properties properties) {
        try {
            String property = properties.getProperty(Constants.ATTRNAME_OUTPUT_METHOD);
            if (property == null) {
                throw new IllegalArgumentException(Utils.messages.createMessage(MsgKey.ER_FACTORY_PROPERTY_MISSING, new Object[]{Constants.ATTRNAME_OUTPUT_METHOD}));
            }
            String property2 = properties.getProperty(OutputPropertiesFactory.S_KEY_CONTENT_HANDLER);
            if (property2 == null && (property2 = OutputPropertiesFactory.getDefaultMethodProperties(property).getProperty(OutputPropertiesFactory.S_KEY_CONTENT_HANDLER)) == null) {
                throw new IllegalArgumentException(Utils.messages.createMessage(MsgKey.ER_FACTORY_PROPERTY_MISSING, new Object[]{OutputPropertiesFactory.S_KEY_CONTENT_HANDLER}));
            }
            ClassLoader classLoaderFindClassLoader = ObjectFactory.findClassLoader();
            Class clsFindProviderClass = ObjectFactory.findProviderClass(property2, classLoaderFindClassLoader, true);
            Object objNewInstance = clsFindProviderClass.newInstance();
            if (objNewInstance instanceof SerializationHandler) {
                Serializer serializer = (Serializer) clsFindProviderClass.newInstance();
                serializer.setOutputFormat(properties);
                return serializer;
            }
            if (objNewInstance instanceof ContentHandler) {
                SerializationHandler serializationHandler = (SerializationHandler) ObjectFactory.findProviderClass(SerializerConstants.DEFAULT_SAX_SERIALIZER, classLoaderFindClassLoader, true).newInstance();
                serializationHandler.setContentHandler((ContentHandler) objNewInstance);
                serializationHandler.setOutputFormat(properties);
                return serializationHandler;
            }
            throw new Exception(Utils.messages.createMessage(MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER, new Object[]{property2}));
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
