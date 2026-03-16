package java.util;

import com.android.dex.DexFormat;
import dalvik.bytecode.Opcodes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Properties extends Hashtable<Object, Object> {
    private static final int CONTINUE = 3;
    private static final int IGNORE = 5;
    private static final int KEY_DONE = 4;
    private static final int NONE = 0;
    private static final String PROP_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>    <!ELEMENT properties (comment?, entry*) >    <!ATTLIST properties version CDATA #FIXED \"1.0\" >    <!ELEMENT comment (#PCDATA) >    <!ELEMENT entry (#PCDATA) >    <!ATTLIST entry key CDATA #REQUIRED >";
    private static final String PROP_DTD_NAME = "http://java.sun.com/dtd/properties.dtd";
    private static final int SLASH = 1;
    private static final int UNICODE = 2;
    private static final long serialVersionUID = 4112578634029874840L;
    private transient DocumentBuilder builder = null;
    protected Properties defaults;

    public Properties() {
    }

    public Properties(Properties properties) {
        this.defaults = properties;
    }

    private void dumpString(StringBuilder buffer, String string, boolean key) {
        int i = 0;
        if (!key && 0 < string.length() && string.charAt(0) == ' ') {
            buffer.append("\\ ");
            i = 0 + 1;
        }
        while (i < string.length()) {
            char ch = string.charAt(i);
            switch (ch) {
                case '\t':
                    buffer.append("\\t");
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case 11:
                default:
                    if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
                        buffer.append('\\');
                    }
                    if (ch >= ' ' && ch <= '~') {
                        buffer.append(ch);
                    } else {
                        String hex = Integer.toHexString(ch);
                        buffer.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            buffer.append("0");
                        }
                        buffer.append(hex);
                    }
                    break;
                case '\f':
                    buffer.append("\\f");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
            }
            i++;
        }
    }

    public String getProperty(String name) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && this.defaults != null) {
            return this.defaults.getProperty(name);
        }
        return property;
    }

    public String getProperty(String name, String defaultValue) {
        Object result = super.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && this.defaults != null) {
            property = this.defaults.getProperty(name);
        }
        if (property == null) {
            return defaultValue;
        }
        String defaultValue2 = property;
        return defaultValue2;
    }

    public void list(PrintStream out) {
        listToAppendable(out);
    }

    public void list(PrintWriter out) {
        listToAppendable(out);
    }

    private void listToAppendable(Appendable out) {
        try {
            if (out == null) {
                throw new NullPointerException("out == null");
            }
            StringBuilder sb = new StringBuilder(80);
            Enumeration<?> keys = propertyNames();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                sb.append(key);
                sb.append('=');
                String property = (String) super.get(key);
                Properties def = this.defaults;
                while (property == null) {
                    property = (String) def.get(key);
                    def = def.defaults;
                }
                if (property.length() > 40) {
                    sb.append(property.substring(0, 37));
                    sb.append("...");
                } else {
                    sb.append(property);
                }
                sb.append(System.lineSeparator());
                out.append(sb.toString());
                sb.setLength(0);
            }
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    public synchronized void load(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        load(new InputStreamReader(in, "ISO-8859-1"));
    }

    public synchronized void load(Reader in) throws IOException {
        char nextChar;
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        int mode = 0;
        int unicode = 0;
        int count = 0;
        char[] buf = new char[40];
        int keyLength = -1;
        boolean firstChar = true;
        BufferedReader br = new BufferedReader(in);
        int offset = 0;
        while (true) {
            int intVal = br.read();
            if (intVal != -1) {
                char nextChar2 = (char) intVal;
                if (offset == buf.length) {
                    char[] newBuf = new char[buf.length * 2];
                    System.arraycopy(buf, 0, newBuf, 0, offset);
                    buf = newBuf;
                }
                if (mode == 2) {
                    int digit = Character.digit(nextChar2, 16);
                    if (digit >= 0) {
                        unicode = (unicode << 4) + digit;
                        count++;
                        if (count >= 4) {
                        }
                    } else if (count <= 4) {
                        throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
                    }
                    mode = 0;
                    int offset2 = offset + 1;
                    buf[offset] = (char) unicode;
                    if (nextChar2 != '\n') {
                        offset = offset2;
                    } else {
                        offset = offset2;
                    }
                }
                if (mode == 1) {
                    mode = 0;
                    switch (nextChar2) {
                        case '\n':
                            mode = 5;
                            continue;
                        case '\r':
                            mode = 3;
                            continue;
                        case Opcodes.OP_SGET_OBJECT:
                            nextChar2 = '\b';
                            break;
                        case Opcodes.OP_SGET_SHORT:
                            nextChar2 = '\f';
                            break;
                        case Opcodes.OP_INVOKE_VIRTUAL:
                            nextChar2 = '\n';
                            break;
                        case Opcodes.OP_INVOKE_INTERFACE:
                            nextChar2 = '\r';
                            break;
                        case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                            nextChar2 = '\t';
                            break;
                        case Opcodes.OP_INVOKE_SUPER_RANGE:
                            mode = 2;
                            count = 0;
                            unicode = 0;
                            continue;
                    }
                    firstChar = false;
                    if (mode == 4) {
                        keyLength = offset;
                        mode = 0;
                    }
                    buf[offset] = nextChar2;
                    offset++;
                } else {
                    switch (nextChar2) {
                        case '\n':
                            if (mode == 3) {
                                mode = 5;
                            } else {
                                mode = 0;
                                firstChar = true;
                                if (offset <= 0 || (offset == 0 && keyLength == 0)) {
                                    if (keyLength == -1) {
                                        keyLength = offset;
                                    }
                                    String temp = new String(buf, 0, offset);
                                    put(temp.substring(0, keyLength), temp.substring(keyLength));
                                }
                                keyLength = -1;
                                offset = 0;
                            }
                            break;
                        case '\r':
                            mode = 0;
                            firstChar = true;
                            if (offset <= 0) {
                                if (keyLength == -1) {
                                }
                                String temp2 = new String(buf, 0, offset);
                                put(temp2.substring(0, keyLength), temp2.substring(keyLength));
                                keyLength = -1;
                                offset = 0;
                                break;
                            }
                            break;
                        case Opcodes.OP_ARRAY_LENGTH:
                        case '#':
                            if (!firstChar) {
                                if (Character.isWhitespace(nextChar2)) {
                                    if (mode == 3) {
                                        mode = 5;
                                    }
                                    if (offset == 0 || offset == keyLength || mode == 5) {
                                        break;
                                    } else if (keyLength == -1) {
                                        mode = 4;
                                        break;
                                    }
                                }
                                if (mode == 5 || mode == 3) {
                                    mode = 0;
                                }
                                firstChar = false;
                                if (mode == 4) {
                                }
                                buf[offset] = nextChar2;
                                offset++;
                            } else {
                                do {
                                    int intVal2 = br.read();
                                    if (intVal2 == -1 || (nextChar = (char) intVal2) == '\r') {
                                    }
                                } while (nextChar != '\n');
                            }
                            break;
                        case Opcodes.OP_IF_LTZ:
                        case Opcodes.OP_IF_LEZ:
                            if (keyLength == -1) {
                                mode = 0;
                                keyLength = offset;
                                break;
                            } else {
                                if (Character.isWhitespace(nextChar2)) {
                                }
                                if (mode == 5) {
                                    mode = 0;
                                    firstChar = false;
                                    if (mode == 4) {
                                    }
                                    buf[offset] = nextChar2;
                                    offset++;
                                    break;
                                }
                            }
                            break;
                        case '\\':
                            if (mode == 4) {
                                keyLength = offset;
                            }
                            mode = 1;
                            break;
                        default:
                            if (Character.isWhitespace(nextChar2)) {
                            }
                            if (mode == 5) {
                            }
                            break;
                    }
                }
            } else {
                if (mode == 2 && count <= 4) {
                    throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
                }
                if (keyLength == -1 && offset > 0) {
                    keyLength = offset;
                }
                if (keyLength >= 0) {
                    String temp3 = new String(buf, 0, offset);
                    String key = temp3.substring(0, keyLength);
                    String value = temp3.substring(keyLength);
                    if (mode == 1) {
                        value = value + DexFormat.MAGIC_SUFFIX;
                    }
                    put(key, value);
                }
            }
        }
    }

    public Enumeration<?> propertyNames() {
        Hashtable hashtable = new Hashtable();
        selectProperties(hashtable, false);
        return hashtable.keys();
    }

    public Set<String> stringPropertyNames() {
        Hashtable<String, Object> stringProperties = new Hashtable<>();
        selectProperties(stringProperties, true);
        return Collections.unmodifiableSet(stringProperties.keySet());
    }

    private <K> void selectProperties(Hashtable<K, Object> hashtable, boolean isStringOnly) {
        if (this.defaults != null) {
            this.defaults.selectProperties(hashtable, isStringOnly);
        }
        Enumeration<Object> keys = keys();
        while (keys.hasMoreElements()) {
            Object objNextElement = keys.nextElement();
            if (!isStringOnly || (objNextElement instanceof String)) {
                Object value = get(objNextElement);
                hashtable.put(objNextElement, value);
            }
        }
    }

    @Deprecated
    public void save(OutputStream out, String comment) {
        try {
            store(out, comment);
        } catch (IOException e) {
        }
    }

    public Object setProperty(String name, String value) {
        return put(name, value);
    }

    public synchronized void store(OutputStream out, String comment) throws IOException {
        store(new OutputStreamWriter(out, "ISO-8859-1"), comment);
    }

    public synchronized void store(Writer writer, String comment) throws IOException {
        if (comment != null) {
            writer.write("#");
            writer.write(comment);
            writer.write(System.lineSeparator());
            writer.write("#");
            writer.write(new Date().toString());
            writer.write(System.lineSeparator());
            StringBuilder sb = new StringBuilder(200);
            for (Map.Entry<Object, Object> entry : entrySet()) {
                String key = (String) entry.getKey();
                dumpString(sb, key, true);
                sb.append('=');
                dumpString(sb, (String) entry.getValue(), false);
                sb.append(System.lineSeparator());
                writer.write(sb.toString());
                sb.setLength(0);
            }
            writer.flush();
        } else {
            writer.write("#");
            writer.write(new Date().toString());
            writer.write(System.lineSeparator());
            StringBuilder sb2 = new StringBuilder(200);
            while (r1.hasNext()) {
            }
            writer.flush();
        }
    }

    public synchronized void loadFromXML(InputStream in) throws IOException {
        NodeList entries;
        if (in == null) {
            throw new NullPointerException("in == null");
        }
        if (this.builder == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                this.builder = factory.newDocumentBuilder();
                this.builder.setErrorHandler(new ErrorHandler() {
                    @Override
                    public void warning(SAXParseException e) throws SAXException {
                        throw e;
                    }

                    @Override
                    public void error(SAXParseException e) throws SAXException {
                        throw e;
                    }

                    @Override
                    public void fatalError(SAXParseException e) throws SAXException {
                        throw e;
                    }
                });
                this.builder.setEntityResolver(new EntityResolver() {
                    @Override
                    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                        if (systemId.equals(Properties.PROP_DTD_NAME)) {
                            InputSource result = new InputSource(new StringReader(Properties.PROP_DTD));
                            result.setSystemId(Properties.PROP_DTD_NAME);
                            return result;
                        }
                        throw new SAXException("Invalid DOCTYPE declaration: " + systemId);
                    }
                });
                try {
                    Document doc = this.builder.parse(in);
                    entries = doc.getElementsByTagName("entry");
                    if (entries != null) {
                        int entriesListLength = entries.getLength();
                        for (int i = 0; i < entriesListLength; i++) {
                            Element entry = (Element) entries.item(i);
                            String key = entry.getAttribute("key");
                            String value = entry.getTextContent();
                            put(key, value);
                        }
                    }
                } catch (IOException e) {
                    throw e;
                } catch (SAXException e2) {
                    throw new InvalidPropertiesFormatException(e2);
                }
            } catch (ParserConfigurationException e3) {
                throw new Error(e3);
            }
        } else {
            Document doc2 = this.builder.parse(in);
            entries = doc2.getElementsByTagName("entry");
            if (entries != null) {
            }
        }
    }

    public void storeToXML(OutputStream os, String comment) throws IOException {
        storeToXML(os, comment, "UTF-8");
    }

    public synchronized void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        String encodingCanonicalName;
        if (os == null) {
            throw new NullPointerException("os == null");
        }
        if (encoding == null) {
            throw new NullPointerException("encoding == null");
        }
        try {
            encodingCanonicalName = Charset.forName(encoding).name();
        } catch (IllegalCharsetNameException e) {
            System.out.println("Warning: encoding name " + encoding + " is illegal, using UTF-8 as default encoding");
            encodingCanonicalName = "UTF-8";
        } catch (UnsupportedCharsetException e2) {
            System.out.println("Warning: encoding " + encoding + " is not supported, using UTF-8 as default encoding");
            encodingCanonicalName = "UTF-8";
        }
        PrintStream printStream = new PrintStream(os, false, encodingCanonicalName);
        printStream.print("<?xml version=\"1.0\" encoding=\"");
        printStream.print(encodingCanonicalName);
        printStream.println("\"?>");
        printStream.print("<!DOCTYPE properties SYSTEM \"");
        printStream.print(PROP_DTD_NAME);
        printStream.println("\">");
        printStream.println("<properties>");
        if (comment != null) {
            printStream.print("<comment>");
            printStream.print(substitutePredefinedEntries(comment));
            printStream.println("</comment>");
        }
        for (Map.Entry<Object, Object> entry : entrySet()) {
            String keyValue = (String) entry.getKey();
            String entryValue = (String) entry.getValue();
            printStream.print("<entry key=\"");
            printStream.print(substitutePredefinedEntries(keyValue));
            printStream.print("\">");
            printStream.print(substitutePredefinedEntries(entryValue));
            printStream.println("</entry>");
        }
        printStream.println("</properties>");
        printStream.flush();
    }

    private String substitutePredefinedEntries(String s) {
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;");
    }
}
