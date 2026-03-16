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

    public static Serializer getSerializer(Properties format) {
        try {
            String method = format.getProperty(Constants.ATTRNAME_OUTPUT_METHOD);
            if (method == null) {
                String msg = Utils.messages.createMessage(MsgKey.ER_FACTORY_PROPERTY_MISSING, new Object[]{Constants.ATTRNAME_OUTPUT_METHOD});
                throw new IllegalArgumentException(msg);
            }
            String className = format.getProperty(OutputPropertiesFactory.S_KEY_CONTENT_HANDLER);
            if (className == null) {
                Properties methodDefaults = OutputPropertiesFactory.getDefaultMethodProperties(method);
                className = methodDefaults.getProperty(OutputPropertiesFactory.S_KEY_CONTENT_HANDLER);
                if (className == null) {
                    String msg2 = Utils.messages.createMessage(MsgKey.ER_FACTORY_PROPERTY_MISSING, new Object[]{OutputPropertiesFactory.S_KEY_CONTENT_HANDLER});
                    throw new IllegalArgumentException(msg2);
                }
            }
            ClassLoader loader = ObjectFactory.findClassLoader();
            Class cls = ObjectFactory.findProviderClass(className, loader, true);
            Object obj = cls.newInstance();
            if (obj instanceof SerializationHandler) {
                Serializer ser = (Serializer) cls.newInstance();
                ser.setOutputFormat(format);
                return ser;
            }
            if (obj instanceof ContentHandler) {
                SerializationHandler sh = (SerializationHandler) ObjectFactory.findProviderClass(SerializerConstants.DEFAULT_SAX_SERIALIZER, loader, true).newInstance();
                sh.setContentHandler((ContentHandler) obj);
                sh.setOutputFormat(format);
                return sh;
            }
            throw new Exception(Utils.messages.createMessage(MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER, new Object[]{className}));
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
