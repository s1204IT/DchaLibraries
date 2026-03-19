package org.apache.xml.serializer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.Utils;
import org.apache.xml.serializer.utils.WrappedRuntimeException;

public final class OutputPropertiesFactory {
    private static final String PROP_FILE_HTML = "output_html.properties";
    private static final String PROP_FILE_TEXT = "output_text.properties";
    private static final String PROP_FILE_UNKNOWN = "output_unknown.properties";
    private static final String PROP_FILE_XML = "output_xml.properties";
    public static final String S_BUILTIN_EXTENSIONS_UNIVERSAL = "{http://xml.apache.org/xalan}";
    private static final String S_BUILTIN_EXTENSIONS_URL = "http://xml.apache.org/xalan";
    private static final String S_BUILTIN_OLD_EXTENSIONS_URL = "http://xml.apache.org/xslt";
    public static final String S_KEY_CONTENT_HANDLER = "{http://xml.apache.org/xalan}content-handler";
    public static final String S_KEY_ENTITIES = "{http://xml.apache.org/xalan}entities";
    public static final String S_KEY_INDENT_AMOUNT = "{http://xml.apache.org/xalan}indent-amount";
    public static final String S_KEY_LINE_SEPARATOR = "{http://xml.apache.org/xalan}line-separator";
    public static final String S_OMIT_META_TAG = "{http://xml.apache.org/xalan}omit-meta-tag";
    public static final String S_USE_URL_ESCAPING = "{http://xml.apache.org/xalan}use-url-escaping";
    public static final String S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL = "{http://xml.apache.org/xslt}";
    public static final int S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL_LEN = S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL.length();
    private static final String S_XSLT_PREFIX = "xslt.output.";
    private static final int S_XSLT_PREFIX_LEN = S_XSLT_PREFIX.length();
    private static final String S_XALAN_PREFIX = "org.apache.xslt.";
    private static final int S_XALAN_PREFIX_LEN = S_XALAN_PREFIX.length();
    private static Integer m_synch_object = new Integer(1);
    private static final String PROP_DIR = SerializerBase.PKG_PATH + '/';
    private static Properties m_xml_properties = null;
    private static Properties m_html_properties = null;
    private static Properties m_text_properties = null;
    private static Properties m_unknown_properties = null;
    private static final Class ACCESS_CONTROLLER_CLASS = findAccessControllerClass();

    private static Class findAccessControllerClass() {
        try {
            return Class.forName("java.security.AccessController");
        } catch (Exception e) {
            return null;
        }
    }

    public static final Properties getDefaultMethodProperties(String str) throws Throwable {
        Object obj;
        IOException e;
        String str2;
        Properties properties;
        Properties properties2;
        try {
            try {
                try {
                    synchronized (m_synch_object) {
                        try {
                            if (m_xml_properties == null) {
                                str2 = PROP_FILE_XML;
                                m_xml_properties = loadPropertiesFile(PROP_FILE_XML, null);
                            } else {
                                str2 = null;
                            }
                            if (str.equals("xml")) {
                                properties = m_xml_properties;
                            } else {
                                if (str.equals("html")) {
                                    if (m_html_properties == null) {
                                        m_html_properties = loadPropertiesFile(PROP_FILE_HTML, m_xml_properties);
                                    }
                                    properties2 = m_html_properties;
                                } else if (str.equals("text")) {
                                    if (m_text_properties == null) {
                                        try {
                                            m_text_properties = loadPropertiesFile(PROP_FILE_TEXT, m_xml_properties);
                                            if (m_text_properties.getProperty("encoding") == null) {
                                                m_text_properties.put("encoding", Encodings.getMimeEncoding(null));
                                            }
                                        } catch (IOException e2) {
                                            e = e2;
                                            obj = PROP_FILE_TEXT;
                                            throw new WrappedRuntimeException(Utils.messages.createMessage(MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY, new Object[]{obj, str}), e);
                                        }
                                    }
                                    properties2 = m_text_properties;
                                } else if (str.equals("")) {
                                    if (m_unknown_properties == null) {
                                        m_unknown_properties = loadPropertiesFile(PROP_FILE_UNKNOWN, m_xml_properties);
                                    }
                                    properties2 = m_unknown_properties;
                                } else {
                                    properties = m_xml_properties;
                                }
                                return new Properties(properties2);
                            }
                            properties2 = properties;
                            return new Properties(properties2);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e3) {
                e = e3;
            }
        } catch (IOException e4) {
            obj = null;
            e = e4;
        }
    }

    private static Properties loadPropertiesFile(final String str, Properties properties) throws Throwable {
        BufferedInputStream bufferedInputStream;
        InputStream resourceAsStream;
        String property;
        String property2;
        Properties properties2 = new Properties(properties);
        InputStream inputStream = null;
        try {
            try {
                resourceAsStream = ACCESS_CONTROLLER_CLASS != null ? (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
                    @Override
                    public Object run() {
                        return OutputPropertiesFactory.class.getResourceAsStream(str);
                    }
                }) : OutputPropertiesFactory.class.getResourceAsStream(str);
                try {
                    bufferedInputStream = new BufferedInputStream(resourceAsStream);
                } catch (IOException e) {
                    e = e;
                } catch (SecurityException e2) {
                    e = e2;
                } catch (Throwable th) {
                    th = th;
                    bufferedInputStream = null;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                properties2.load(bufferedInputStream);
                bufferedInputStream.close();
                if (resourceAsStream != null) {
                    resourceAsStream.close();
                }
                Enumeration enumerationKeys = ((Properties) properties2.clone()).keys();
                while (enumerationKeys.hasMoreElements()) {
                    String str2 = (String) enumerationKeys.nextElement();
                    try {
                        property = System.getProperty(str2);
                    } catch (SecurityException e3) {
                        property = null;
                    }
                    if (property == null) {
                        property = (String) properties2.get(str2);
                    }
                    String strFixupPropertyString = fixupPropertyString(str2, true);
                    try {
                        property2 = System.getProperty(strFixupPropertyString);
                    } catch (SecurityException e4) {
                        property2 = null;
                    }
                    String strFixupPropertyString2 = property2 == null ? fixupPropertyString(property, false) : fixupPropertyString(property2, false);
                    if (str2 != strFixupPropertyString || property != strFixupPropertyString2) {
                        properties2.remove(str2);
                        properties2.put(strFixupPropertyString, strFixupPropertyString2);
                    }
                }
                return properties2;
            } catch (IOException e5) {
                e = e5;
                if (properties == null) {
                    throw e;
                }
                throw new WrappedRuntimeException(Utils.messages.createMessage("ER_COULD_NOT_LOAD_RESOURCE", new Object[]{str}), e);
            } catch (SecurityException e6) {
                e = e6;
                if (properties == null) {
                    throw e;
                }
                throw new WrappedRuntimeException(Utils.messages.createMessage("ER_COULD_NOT_LOAD_RESOURCE", new Object[]{str}), e);
            } catch (Throwable th3) {
                th = th3;
                inputStream = resourceAsStream;
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                throw th;
            }
        } catch (IOException e7) {
            e = e7;
        } catch (SecurityException e8) {
            e = e8;
        } catch (Throwable th4) {
            th = th4;
            bufferedInputStream = null;
        }
    }

    private static String fixupPropertyString(String str, boolean z) {
        if (z && str.startsWith(S_XSLT_PREFIX)) {
            str = str.substring(S_XSLT_PREFIX_LEN);
        }
        if (str.startsWith(S_XALAN_PREFIX)) {
            str = S_BUILTIN_EXTENSIONS_UNIVERSAL + str.substring(S_XALAN_PREFIX_LEN);
        }
        int iIndexOf = str.indexOf("\\u003a");
        if (iIndexOf > 0) {
            return str.substring(0, iIndexOf) + ":" + str.substring(iIndexOf + 6);
        }
        return str;
    }
}
