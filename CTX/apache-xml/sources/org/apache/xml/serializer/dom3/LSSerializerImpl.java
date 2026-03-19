package org.apache.xml.serializer.dom3;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.xml.serializer.DOM3Serializer;
import org.apache.xml.serializer.Encodings;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerConstants;
import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.SystemIDResolver;
import org.apache.xml.serializer.utils.Utils;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;

public final class LSSerializerImpl implements DOMConfiguration, LSSerializer {
    private static final int CANONICAL = 1;
    private static final int CDATA = 2;
    private static final int CHARNORMALIZE = 4;
    private static final int COMMENTS = 8;
    private static final String DEFAULT_END_OF_LINE;
    private static final int DISCARDDEFAULT = 32768;
    private static final int DTNORMALIZE = 16;
    private static final int ELEM_CONTENT_WHITESPACE = 32;
    private static final int ENTITIES = 64;
    private static final int IGNORE_CHAR_DENORMALIZE = 131072;
    private static final int INFOSET = 128;
    private static final int NAMESPACEDECLS = 512;
    private static final int NAMESPACES = 256;
    private static final int NORMALIZECHARS = 1024;
    private static final int PRETTY_PRINT = 65536;
    private static final int SCHEMAVALIDATE = 8192;
    private static final int SPLITCDATA = 2048;
    private static final int VALIDATE = 4096;
    private static final int WELLFORMED = 16384;
    private static final int XMLDECL = 262144;
    private Properties fDOMConfigProperties;
    private String fEncoding;
    private Serializer fXMLSerializer;
    protected int fFeatures = 0;
    private DOM3Serializer fDOMSerializer = null;
    private LSSerializerFilter fSerializerFilter = null;
    private Node fVisitedNode = null;
    private String fEndOfLine = DEFAULT_END_OF_LINE;
    private DOMErrorHandler fDOMErrorHandler = null;
    private String[] fRecognizedParameters = {DOMConstants.DOM_CANONICAL_FORM, DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM_CHECK_CHAR_NORMALIZATION, DOMConstants.DOM_COMMENTS, DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM_ENTITIES, DOMConstants.DOM_INFOSET, DOMConstants.DOM_NAMESPACES, DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM_SPLIT_CDATA, DOMConstants.DOM_VALIDATE, DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM_WELLFORMED, DOMConstants.DOM_DISCARD_DEFAULT_CONTENT, DOMConstants.DOM_FORMAT_PRETTY_PRINT, DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS, DOMConstants.DOM_XMLDECL, DOMConstants.DOM_ERROR_HANDLER};

    static {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    return System.getProperty("line.separator");
                } catch (SecurityException e) {
                    return null;
                }
            }
        });
        if (str == null || (!str.equals("\r\n") && !str.equals("\r"))) {
            str = "\n";
        }
        DEFAULT_END_OF_LINE = str;
    }

    public LSSerializerImpl() {
        this.fXMLSerializer = null;
        this.fDOMConfigProperties = null;
        this.fFeatures |= 2;
        this.fFeatures |= 8;
        this.fFeatures |= 32;
        this.fFeatures |= 64;
        this.fFeatures |= 256;
        this.fFeatures |= 512;
        this.fFeatures |= 2048;
        this.fFeatures |= 16384;
        this.fFeatures |= 32768;
        this.fFeatures |= 262144;
        this.fDOMConfigProperties = new Properties();
        initializeSerializerProps();
        this.fXMLSerializer = SerializerFactory.getSerializer(OutputPropertiesFactory.getDefaultMethodProperties("xml"));
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
    }

    public void initializeSerializerProps() {
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}canonical-form", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}check-character-normalization", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_DEFAULT_TRUE);
        if ((this.fFeatures & 128) != 0) {
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_DEFAULT_TRUE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_DEFAULT_FALSE);
            this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_DEFAULT_FALSE);
        }
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_DEFAULT_FALSE);
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("indent", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, Integer.toString(3));
        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", DOMConstants.DOM3_DEFAULT_TRUE);
        this.fDOMConfigProperties.setProperty("omit-xml-declaration", "no");
    }

    @Override
    public boolean canSetParameter(String str, Object obj) {
        if (!(obj instanceof Boolean)) {
            return (str.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER) && obj == null) || (obj instanceof DOMErrorHandler);
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS) || str.equalsIgnoreCase(DOMConstants.DOM_COMMENTS) || str.equalsIgnoreCase(DOMConstants.DOM_ENTITIES) || str.equalsIgnoreCase(DOMConstants.DOM_INFOSET) || str.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE) || str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES) || str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS) || str.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA) || str.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED) || str.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT) || str.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT) || str.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
            return true;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || str.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE)) {
            return !((Boolean) obj).booleanValue();
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
            return ((Boolean) obj).booleanValue();
        }
        return false;
    }

    @Override
    public Object getParameter(String str) throws DOMException {
        if (str.equalsIgnoreCase(DOMConstants.DOM_COMMENTS)) {
            return (this.fFeatures & 8) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS)) {
            return (this.fFeatures & 2) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_ENTITIES)) {
            return (this.fFeatures & 64) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES)) {
            return (this.fFeatures & 256) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS)) {
            return (this.fFeatures & 512) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA)) {
            return (this.fFeatures & 2048) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED)) {
            return (this.fFeatures & 16384) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT)) {
            return (this.fFeatures & 32768) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
            return (this.fFeatures & 65536) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
            return (this.fFeatures & 262144) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
            return (this.fFeatures & 32) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
            return (this.fFeatures & 65536) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
            return Boolean.TRUE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || str.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
            return Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
            if ((this.fFeatures & 64) == 0 && (this.fFeatures & 2) == 0 && (this.fFeatures & 32) != 0 && (this.fFeatures & 256) != 0 && (this.fFeatures & 512) != 0 && (this.fFeatures & 16384) != 0 && (this.fFeatures & 8) != 0) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER)) {
            return this.fDOMErrorHandler;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || str.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
            return null;
        }
        throw new DOMException((short) 8, Utils.messages.createMessage("FEATURE_NOT_FOUND", new Object[]{str}));
    }

    @Override
    public DOMStringList getParameterNames() {
        return new DOMStringListImpl(this.fRecognizedParameters);
    }

    @Override
    public void setParameter(String str, Object obj) throws DOMException {
        if (obj instanceof Boolean) {
            boolean zBooleanValue = ((Boolean) obj).booleanValue();
            if (str.equalsIgnoreCase(DOMConstants.DOM_COMMENTS)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 8 : this.fFeatures & (-9);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 2 : this.fFeatures & (-3);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_ENTITIES)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 64 : this.fFeatures & (-65);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 256 : this.fFeatures & (-257);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 512 : this.fFeatures & (-513);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 2048 : this.fFeatures & (-2049);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}split-cdata-sections", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 16384 : this.fFeatures & (-16385);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 32768 : this.fFeatures & (-32769);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}discard-default-content", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 65536 : this.fFeatures & (-65537);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}format-pretty-print", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}format-pretty-print", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 262144 : this.fFeatures & (-262145);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("omit-xml-declaration", "no");
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("omit-xml-declaration", "yes");
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
                this.fFeatures = zBooleanValue ? this.fFeatures | 32 : this.fFeatures & (-33);
                if (zBooleanValue) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_EXPLICIT_TRUE);
                    return;
                } else {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                if (!zBooleanValue) {
                    throw new DOMException((short) 9, Utils.messages.createMessage("FEATURE_NOT_SUPPORTED", new Object[]{str}));
                }
                this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}ignore-unknown-character-denormalizations", DOMConstants.DOM3_EXPLICIT_TRUE);
                return;
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || str.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION)) {
                if (zBooleanValue) {
                    throw new DOMException((short) 9, Utils.messages.createMessage("FEATURE_NOT_SUPPORTED", new Object[]{str}));
                }
                if (str.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}canonical-form", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
                if (str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
                if (str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE)) {
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                } else if (str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                    this.fDOMConfigProperties.setProperty("check-character-normalizationcheck-character-normalization", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                } else {
                    if (str.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION)) {
                        this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_EXPLICIT_FALSE);
                        return;
                    }
                    return;
                }
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
                if (zBooleanValue) {
                    this.fFeatures &= -65;
                    this.fFeatures &= -3;
                    this.fFeatures &= -8193;
                    this.fFeatures &= -17;
                    this.fFeatures |= 256;
                    this.fFeatures |= 512;
                    this.fFeatures |= 16384;
                    this.fFeatures |= 32;
                    this.fFeatures |= 8;
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespaces", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}namespace-declarations", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}comments", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}element-content-whitespace", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}well-formed", DOMConstants.DOM3_EXPLICIT_TRUE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}entities", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}cdata-sections", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}validate-if-schema", DOMConstants.DOM3_EXPLICIT_FALSE);
                    this.fDOMConfigProperties.setProperty("{http://www.w3.org/TR/DOM-Level-3-LS}datatype-normalization", DOMConstants.DOM3_EXPLICIT_FALSE);
                    return;
                }
                return;
            }
            if (str.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER) || str.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || str.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
                throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{str}));
            }
            throw new DOMException((short) 8, Utils.messages.createMessage("FEATURE_NOT_FOUND", new Object[]{str}));
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER)) {
            if (obj == null || (obj instanceof DOMErrorHandler)) {
                this.fDOMErrorHandler = (DOMErrorHandler) obj;
                return;
            }
            throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{str}));
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || str.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
            if (obj != null) {
                if (!(obj instanceof String)) {
                    throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{str}));
                }
                throw new DOMException((short) 9, Utils.messages.createMessage("FEATURE_NOT_SUPPORTED", new Object[]{str}));
            }
            return;
        }
        if (str.equalsIgnoreCase(DOMConstants.DOM_COMMENTS) || str.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS) || str.equalsIgnoreCase(DOMConstants.DOM_ENTITIES) || str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES) || str.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS) || str.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA) || str.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED) || str.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT) || str.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT) || str.equalsIgnoreCase(DOMConstants.DOM_XMLDECL) || str.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE) || str.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS) || str.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || str.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || str.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || str.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
            throw new DOMException((short) 17, Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[]{str}));
        }
        throw new DOMException((short) 8, Utils.messages.createMessage("FEATURE_NOT_FOUND", new Object[]{str}));
    }

    @Override
    public DOMConfiguration getDomConfig() {
        return this;
    }

    public LSSerializerFilter getFilter() {
        return this.fSerializerFilter;
    }

    @Override
    public String getNewLine() {
        return this.fEndOfLine;
    }

    public void setFilter(LSSerializerFilter lSSerializerFilter) {
        this.fSerializerFilter = lSSerializerFilter;
    }

    @Override
    public void setNewLine(String str) {
        if (str == null) {
            str = DEFAULT_END_OF_LINE;
        }
        this.fEndOfLine = str;
    }

    @Override
    public boolean write(Node node, LSOutput lSOutput) throws LSException {
        OutputStream outputStream;
        if (lSOutput == null) {
            String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
            if (this.fDOMErrorHandler != null) {
                this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_NO_OUTPUT_SPECIFIED));
            }
            throw new LSException((short) 82, strCreateMessage);
        }
        if (node == null) {
            return false;
        }
        Serializer serializer = this.fXMLSerializer;
        serializer.reset();
        if (node != this.fVisitedNode) {
            String xMLVersion = getXMLVersion(node);
            this.fEncoding = lSOutput.getEncoding();
            if (this.fEncoding == null) {
                this.fEncoding = getInputEncoding(node);
                this.fEncoding = this.fEncoding != null ? this.fEncoding : getXMLEncoding(node) == null ? "UTF-8" : getXMLEncoding(node);
            }
            if (!Encodings.isRecognizedEncoding(this.fEncoding)) {
                String strCreateMessage2 = Utils.messages.createMessage(MsgKey.ER_UNSUPPORTED_ENCODING, null);
                if (this.fDOMErrorHandler != null) {
                    this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage2, MsgKey.ER_UNSUPPORTED_ENCODING));
                }
                throw new LSException((short) 82, strCreateMessage2);
            }
            serializer.getOutputFormat().setProperty("version", xMLVersion);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}xml-version", xMLVersion);
            this.fDOMConfigProperties.setProperty("encoding", this.fEncoding);
            if ((node.getNodeType() != 9 || node.getNodeType() != 1 || node.getNodeType() != 6) && (this.fFeatures & 262144) != 0) {
                this.fDOMConfigProperties.setProperty("omit-xml-declaration", DOMConstants.DOM3_DEFAULT_FALSE);
            }
            this.fVisitedNode = node;
        }
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
        try {
            Writer characterStream = lSOutput.getCharacterStream();
            if (characterStream == null) {
                OutputStream byteStream = lSOutput.getByteStream();
                if (byteStream == null) {
                    String systemId = lSOutput.getSystemId();
                    if (systemId == null) {
                        String strCreateMessage3 = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
                        if (this.fDOMErrorHandler != null) {
                            this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage3, MsgKey.ER_NO_OUTPUT_SPECIFIED));
                        }
                        throw new LSException((short) 82, strCreateMessage3);
                    }
                    URL url = new URL(SystemIDResolver.getAbsoluteURI(systemId));
                    String protocol = url.getProtocol();
                    String host = url.getHost();
                    if (protocol.equalsIgnoreCase("file") && (host == null || host.length() == 0 || host.equals("localhost"))) {
                        outputStream = new FileOutputStream(getPathWithoutEscapes(url.getPath()));
                    } else {
                        URLConnection uRLConnectionOpenConnection = url.openConnection();
                        uRLConnectionOpenConnection.setDoInput(false);
                        uRLConnectionOpenConnection.setDoOutput(true);
                        uRLConnectionOpenConnection.setUseCaches(false);
                        uRLConnectionOpenConnection.setAllowUserInteraction(false);
                        if (uRLConnectionOpenConnection instanceof HttpURLConnection) {
                            ((HttpURLConnection) uRLConnectionOpenConnection).setRequestMethod("PUT");
                        }
                        outputStream = uRLConnectionOpenConnection.getOutputStream();
                    }
                    serializer.setOutputStream(outputStream);
                } else {
                    serializer.setOutputStream(byteStream);
                }
            } else {
                serializer.setWriter(characterStream);
            }
            if (this.fDOMSerializer == null) {
                this.fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (this.fDOMErrorHandler != null) {
                this.fDOMSerializer.setErrorHandler(this.fDOMErrorHandler);
            }
            if (this.fSerializerFilter != null) {
                this.fDOMSerializer.setNodeFilter(this.fSerializerFilter);
            }
            this.fDOMSerializer.setNewLine(this.fEndOfLine.toCharArray());
            this.fDOMSerializer.serializeDOM3(node);
            return true;
        } catch (UnsupportedEncodingException e) {
            String strCreateMessage4 = Utils.messages.createMessage(MsgKey.ER_UNSUPPORTED_ENCODING, null);
            if (this.fDOMErrorHandler != null) {
                this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage4, MsgKey.ER_UNSUPPORTED_ENCODING, e));
            }
            throw ((LSException) createLSException((short) 82, e).fillInStackTrace());
        } catch (LSException e2) {
            throw e2;
        } catch (RuntimeException e3) {
            throw ((LSException) createLSException((short) 82, e3).fillInStackTrace());
        } catch (Exception e4) {
            if (this.fDOMErrorHandler != null) {
                this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, e4.getMessage(), null, e4));
            }
            throw ((LSException) createLSException((short) 82, e4).fillInStackTrace());
        }
    }

    @Override
    public String writeToString(Node node) throws DOMException, LSException {
        if (node == null) {
            return null;
        }
        Serializer serializer = this.fXMLSerializer;
        serializer.reset();
        if (node != this.fVisitedNode) {
            String xMLVersion = getXMLVersion(node);
            serializer.getOutputFormat().setProperty("version", xMLVersion);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}xml-version", xMLVersion);
            this.fDOMConfigProperties.setProperty("encoding", "UTF-16");
            if ((node.getNodeType() != 9 || node.getNodeType() != 1 || node.getNodeType() != 6) && (this.fFeatures & 262144) != 0) {
                this.fDOMConfigProperties.setProperty("omit-xml-declaration", DOMConstants.DOM3_DEFAULT_FALSE);
            }
            this.fVisitedNode = node;
        }
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
        StringWriter stringWriter = new StringWriter();
        try {
            serializer.setWriter(stringWriter);
            if (this.fDOMSerializer == null) {
                this.fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (this.fDOMErrorHandler != null) {
                this.fDOMSerializer.setErrorHandler(this.fDOMErrorHandler);
            }
            if (this.fSerializerFilter != null) {
                this.fDOMSerializer.setNodeFilter(this.fSerializerFilter);
            }
            this.fDOMSerializer.setNewLine(this.fEndOfLine.toCharArray());
            this.fDOMSerializer.serializeDOM3(node);
            return stringWriter.toString();
        } catch (LSException e) {
            throw e;
        } catch (RuntimeException e2) {
            throw ((LSException) createLSException((short) 82, e2).fillInStackTrace());
        } catch (Exception e3) {
            if (this.fDOMErrorHandler != null) {
                this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, e3.getMessage(), null, e3));
            }
            throw ((LSException) createLSException((short) 82, e3).fillInStackTrace());
        }
    }

    @Override
    public boolean writeToURI(Node node, String str) throws LSException {
        OutputStream outputStream;
        if (node == null) {
            return false;
        }
        Serializer serializer = this.fXMLSerializer;
        serializer.reset();
        if (node != this.fVisitedNode) {
            String xMLVersion = getXMLVersion(node);
            this.fEncoding = getInputEncoding(node);
            if (this.fEncoding == null) {
                this.fEncoding = this.fEncoding != null ? this.fEncoding : getXMLEncoding(node) == null ? "UTF-8" : getXMLEncoding(node);
            }
            serializer.getOutputFormat().setProperty("version", xMLVersion);
            this.fDOMConfigProperties.setProperty("{http://xml.apache.org/xerces-2j}xml-version", xMLVersion);
            this.fDOMConfigProperties.setProperty("encoding", this.fEncoding);
            if ((node.getNodeType() != 9 || node.getNodeType() != 1 || node.getNodeType() != 6) && (this.fFeatures & 262144) != 0) {
                this.fDOMConfigProperties.setProperty("omit-xml-declaration", DOMConstants.DOM3_DEFAULT_FALSE);
            }
            this.fVisitedNode = node;
        }
        this.fXMLSerializer.setOutputFormat(this.fDOMConfigProperties);
        try {
            if (str == null) {
                String strCreateMessage = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
                if (this.fDOMErrorHandler != null) {
                    this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, strCreateMessage, MsgKey.ER_NO_OUTPUT_SPECIFIED));
                }
                throw new LSException((short) 82, strCreateMessage);
            }
            URL url = new URL(SystemIDResolver.getAbsoluteURI(str));
            String protocol = url.getProtocol();
            String host = url.getHost();
            if (protocol.equalsIgnoreCase("file") && (host == null || host.length() == 0 || host.equals("localhost"))) {
                outputStream = new FileOutputStream(getPathWithoutEscapes(url.getPath()));
            } else {
                URLConnection uRLConnectionOpenConnection = url.openConnection();
                uRLConnectionOpenConnection.setDoInput(false);
                uRLConnectionOpenConnection.setDoOutput(true);
                uRLConnectionOpenConnection.setUseCaches(false);
                uRLConnectionOpenConnection.setAllowUserInteraction(false);
                if (uRLConnectionOpenConnection instanceof HttpURLConnection) {
                    ((HttpURLConnection) uRLConnectionOpenConnection).setRequestMethod("PUT");
                }
                outputStream = uRLConnectionOpenConnection.getOutputStream();
            }
            serializer.setOutputStream(outputStream);
            if (this.fDOMSerializer == null) {
                this.fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (this.fDOMErrorHandler != null) {
                this.fDOMSerializer.setErrorHandler(this.fDOMErrorHandler);
            }
            if (this.fSerializerFilter != null) {
                this.fDOMSerializer.setNodeFilter(this.fSerializerFilter);
            }
            this.fDOMSerializer.setNewLine(this.fEndOfLine.toCharArray());
            this.fDOMSerializer.serializeDOM3(node);
            return true;
        } catch (LSException e) {
            throw e;
        } catch (RuntimeException e2) {
            throw ((LSException) createLSException((short) 82, e2).fillInStackTrace());
        } catch (Exception e3) {
            if (this.fDOMErrorHandler != null) {
                this.fDOMErrorHandler.handleError(new DOMErrorImpl((short) 3, e3.getMessage(), null, e3));
            }
            throw ((LSException) createLSException((short) 82, e3).fillInStackTrace());
        }
    }

    protected String getXMLVersion(Node node) {
        Document ownerDocument;
        if (node != null) {
            if (node.getNodeType() == 9) {
                ownerDocument = (Document) node;
            } else {
                ownerDocument = node.getOwnerDocument();
            }
            if (ownerDocument != null && ownerDocument.getImplementation().hasFeature("Core", "3.0")) {
                return ownerDocument.getXmlVersion();
            }
            return SerializerConstants.XMLVERSION10;
        }
        return SerializerConstants.XMLVERSION10;
    }

    protected String getXMLEncoding(Node node) {
        Document ownerDocument;
        if (node != null) {
            if (node.getNodeType() == 9) {
                ownerDocument = (Document) node;
            } else {
                ownerDocument = node.getOwnerDocument();
            }
            if (ownerDocument != null && ownerDocument.getImplementation().hasFeature("Core", "3.0")) {
                return ownerDocument.getXmlEncoding();
            }
            return "UTF-8";
        }
        return "UTF-8";
    }

    protected String getInputEncoding(Node node) {
        Document ownerDocument;
        if (node != null) {
            if (node.getNodeType() == 9) {
                ownerDocument = (Document) node;
            } else {
                ownerDocument = node.getOwnerDocument();
            }
            if (ownerDocument != null && ownerDocument.getImplementation().hasFeature("Core", "3.0")) {
                return ownerDocument.getInputEncoding();
            }
            return null;
        }
        return null;
    }

    public DOMErrorHandler getErrorHandler() {
        return this.fDOMErrorHandler;
    }

    private static String getPathWithoutEscapes(String str) {
        if (str != null && str.length() != 0 && str.indexOf(37) != -1) {
            StringTokenizer stringTokenizer = new StringTokenizer(str, "%");
            StringBuffer stringBuffer = new StringBuffer(str.length());
            int iCountTokens = stringTokenizer.countTokens();
            stringBuffer.append(stringTokenizer.nextToken());
            for (int i = 1; i < iCountTokens; i++) {
                String strNextToken = stringTokenizer.nextToken();
                if (strNextToken.length() >= 2 && isHexDigit(strNextToken.charAt(0)) && isHexDigit(strNextToken.charAt(1))) {
                    stringBuffer.append((char) Integer.valueOf(strNextToken.substring(0, 2), 16).intValue());
                    strNextToken = strNextToken.substring(2);
                }
                stringBuffer.append(strNextToken);
            }
            return stringBuffer.toString();
        }
        return str;
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static LSException createLSException(short s, Throwable th) {
        LSException lSException = new LSException(s, th != null ? th.getMessage() : null);
        if (th != null && ThrowableMethods.fgThrowableMethodsAvailable) {
            try {
                ThrowableMethods.fgThrowableInitCauseMethod.invoke(lSException, th);
            } catch (Exception e) {
            }
        }
        return lSException;
    }

    static class ThrowableMethods {
        private static Method fgThrowableInitCauseMethod;
        private static boolean fgThrowableMethodsAvailable;

        static {
            fgThrowableInitCauseMethod = null;
            fgThrowableMethodsAvailable = false;
            try {
                fgThrowableInitCauseMethod = Throwable.class.getMethod("initCause", Throwable.class);
                fgThrowableMethodsAvailable = true;
            } catch (Exception e) {
                fgThrowableInitCauseMethod = null;
                fgThrowableMethodsAvailable = false;
            }
        }

        private ThrowableMethods() {
        }
    }
}
