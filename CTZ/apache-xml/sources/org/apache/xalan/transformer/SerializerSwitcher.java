package org.apache.xalan.transformer;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.OutputProperties;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;

public class SerializerSwitcher {
    public static void switchSerializerIfHTML(TransformerImpl transformerImpl, String str, String str2) throws TransformerException {
        if (transformerImpl == null) {
            return;
        }
        if ((str != null && str.length() != 0) || !str2.equalsIgnoreCase("html") || transformerImpl.getOutputPropertyNoDefault(Constants.ATTRNAME_OUTPUT_METHOD) != null) {
            return;
        }
        Properties properties = transformerImpl.getOutputFormat().getProperties();
        OutputProperties outputProperties = new OutputProperties("html");
        outputProperties.copyFrom(properties, true);
        outputProperties.getProperties();
    }

    private static String getOutputPropertyNoDefault(String str, Properties properties) throws IllegalArgumentException {
        return (String) properties.get(str);
    }

    public static Serializer switchSerializerIfHTML(String str, String str2, Properties properties, Serializer serializer) throws TransformerException {
        if ((str == null || str.length() == 0) && str2.equalsIgnoreCase("html")) {
            if (getOutputPropertyNoDefault(Constants.ATTRNAME_OUTPUT_METHOD, properties) != null) {
                return serializer;
            }
            OutputProperties outputProperties = new OutputProperties("html");
            outputProperties.copyFrom(properties, true);
            Properties properties2 = outputProperties.getProperties();
            if (serializer != null) {
                Serializer serializer2 = SerializerFactory.getSerializer(properties2);
                Writer writer = serializer.getWriter();
                if (writer != null) {
                    serializer2.setWriter(writer);
                    return serializer2;
                }
                OutputStream outputStream = serializer2.getOutputStream();
                if (outputStream == null) {
                    return serializer2;
                }
                serializer2.setOutputStream(outputStream);
                return serializer2;
            }
        }
        return serializer;
    }
}
