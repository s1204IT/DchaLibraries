package org.apache.xalan.templates;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.OutputPropertyUtils;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.QName;

public class OutputProperties extends ElemTemplateElement implements Cloneable {
    static final long serialVersionUID = -6975274363881785488L;
    private Properties m_properties;

    public OutputProperties() {
        this("xml");
    }

    public OutputProperties(Properties properties) {
        this.m_properties = null;
        this.m_properties = new Properties(properties);
    }

    public OutputProperties(String str) {
        this.m_properties = null;
        this.m_properties = new Properties(OutputPropertiesFactory.getDefaultMethodProperties(str));
    }

    public Object clone() {
        try {
            OutputProperties outputProperties = (OutputProperties) super.clone();
            outputProperties.m_properties = (Properties) outputProperties.m_properties.clone();
            return outputProperties;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void setProperty(QName qName, String str) {
        setProperty(qName.toNamespacedString(), str);
    }

    public void setProperty(String str, String str2) {
        if (str.equals(Constants.ATTRNAME_OUTPUT_METHOD)) {
            setMethodDefaults(str2);
        }
        if (str.startsWith(OutputPropertiesFactory.S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL)) {
            str = OutputPropertiesFactory.S_BUILTIN_EXTENSIONS_UNIVERSAL + str.substring(OutputPropertiesFactory.S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL_LEN);
        }
        this.m_properties.put(str, str2);
    }

    public String getProperty(QName qName) {
        return this.m_properties.getProperty(qName.toNamespacedString());
    }

    public String getProperty(String str) {
        if (str.startsWith(OutputPropertiesFactory.S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL)) {
            str = OutputPropertiesFactory.S_BUILTIN_EXTENSIONS_UNIVERSAL + str.substring(OutputPropertiesFactory.S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL_LEN);
        }
        return this.m_properties.getProperty(str);
    }

    public void setBooleanProperty(QName qName, boolean z) {
        this.m_properties.put(qName.toNamespacedString(), z ? "yes" : "no");
    }

    public void setBooleanProperty(String str, boolean z) {
        this.m_properties.put(str, z ? "yes" : "no");
    }

    public boolean getBooleanProperty(QName qName) {
        return getBooleanProperty(qName.toNamespacedString());
    }

    public boolean getBooleanProperty(String str) {
        return OutputPropertyUtils.getBooleanProperty(str, this.m_properties);
    }

    public void setIntProperty(QName qName, int i) {
        setIntProperty(qName.toNamespacedString(), i);
    }

    public void setIntProperty(String str, int i) {
        this.m_properties.put(str, Integer.toString(i));
    }

    public int getIntProperty(QName qName) {
        return getIntProperty(qName.toNamespacedString());
    }

    public int getIntProperty(String str) {
        return OutputPropertyUtils.getIntProperty(str, this.m_properties);
    }

    public void setQNameProperty(QName qName, QName qName2) {
        setQNameProperty(qName.toNamespacedString(), qName2);
    }

    public void setMethodDefaults(String str) {
        String property = this.m_properties.getProperty(Constants.ATTRNAME_OUTPUT_METHOD);
        if (property == null || !property.equals(str) || property.equals("xml")) {
            Properties properties = this.m_properties;
            this.m_properties = new Properties(OutputPropertiesFactory.getDefaultMethodProperties(str));
            copyFrom(properties, false);
        }
    }

    public void setQNameProperty(String str, QName qName) {
        setProperty(str, qName.toNamespacedString());
    }

    public QName getQNameProperty(QName qName) {
        return getQNameProperty(qName.toNamespacedString());
    }

    public QName getQNameProperty(String str) {
        return getQNameProperty(str, this.m_properties);
    }

    public static QName getQNameProperty(String str, Properties properties) {
        String property = properties.getProperty(str);
        if (property != null) {
            return QName.getQNameFromString(property);
        }
        return null;
    }

    public void setQNameProperties(QName qName, Vector vector) {
        setQNameProperties(qName.toNamespacedString(), vector);
    }

    public void setQNameProperties(String str, Vector vector) {
        int size = vector.size();
        FastStringBuffer fastStringBuffer = new FastStringBuffer(9, 9);
        for (int i = 0; i < size; i++) {
            fastStringBuffer.append(((QName) vector.elementAt(i)).toNamespacedString());
            if (i < size - 1) {
                fastStringBuffer.append(' ');
            }
        }
        this.m_properties.put(str, fastStringBuffer.toString());
    }

    public Vector getQNameProperties(QName qName) {
        return getQNameProperties(qName.toNamespacedString());
    }

    public Vector getQNameProperties(String str) {
        return getQNameProperties(str, this.m_properties);
    }

    public static Vector getQNameProperties(String str, Properties properties) {
        String property = properties.getProperty(str);
        if (property != null) {
            Vector vector = new Vector();
            int length = property.length();
            FastStringBuffer fastStringBuffer = new FastStringBuffer();
            boolean z = false;
            for (int i = 0; i < length; i++) {
                char cCharAt = property.charAt(i);
                if (Character.isWhitespace(cCharAt)) {
                    if (!z) {
                        if (fastStringBuffer.length() > 0) {
                            vector.addElement(QName.getQNameFromString(fastStringBuffer.toString()));
                            fastStringBuffer.reset();
                        }
                    }
                } else if ('{' != cCharAt) {
                    if ('}' == cCharAt) {
                        z = false;
                    }
                } else {
                    z = true;
                }
                fastStringBuffer.append(cCharAt);
            }
            if (fastStringBuffer.length() > 0) {
                vector.addElement(QName.getQNameFromString(fastStringBuffer.toString()));
                fastStringBuffer.reset();
            }
            return vector;
        }
        return null;
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) throws TransformerException {
        stylesheetRoot.recomposeOutput(this);
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
    }

    public Properties getProperties() {
        return this.m_properties;
    }

    public void copyFrom(Properties properties) {
        copyFrom(properties, true);
    }

    public void copyFrom(Properties properties, boolean z) {
        Enumeration enumerationKeys = properties.keys();
        while (enumerationKeys.hasMoreElements()) {
            String str = (String) enumerationKeys.nextElement();
            if (!isLegalPropertyKey(str)) {
                throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_OUTPUT_PROPERTY_NOT_RECOGNIZED, new Object[]{str}));
            }
            Object obj = this.m_properties.get(str);
            if (obj == null) {
                String str2 = (String) properties.get(str);
                if (z && str.equals(Constants.ATTRNAME_OUTPUT_METHOD)) {
                    setMethodDefaults(str2);
                }
                this.m_properties.put(str, str2);
            } else if (str.equals(Constants.ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS)) {
                this.m_properties.put(str, ((String) obj) + " " + ((String) properties.get(str)));
            }
        }
    }

    public void copyFrom(OutputProperties outputProperties) throws TransformerException {
        copyFrom(outputProperties.getProperties());
    }

    public static boolean isLegalPropertyKey(String str) {
        return str.equals(Constants.ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS) || str.equals(Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC) || str.equals(Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM) || str.equals("encoding") || str.equals("indent") || str.equals(Constants.ATTRNAME_OUTPUT_MEDIATYPE) || str.equals(Constants.ATTRNAME_OUTPUT_METHOD) || str.equals("omit-xml-declaration") || str.equals(Constants.ATTRNAME_OUTPUT_STANDALONE) || str.equals("version") || (str.length() > 0 && str.charAt(0) == '{' && str.lastIndexOf(123) == 0 && str.indexOf(125) > 0 && str.lastIndexOf(125) == str.indexOf(125));
    }

    public static Properties getDefaultMethodProperties(String str) {
        return OutputPropertiesFactory.getDefaultMethodProperties(str);
    }
}
