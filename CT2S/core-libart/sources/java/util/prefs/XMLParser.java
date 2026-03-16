package java.util.prefs;

import dalvik.bytecode.Opcodes;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import libcore.io.IoUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class XMLParser {
    static final String DOCTYPE = "<!DOCTYPE preferences SYSTEM";
    private static final String FILE_PREFS = "<!DOCTYPE map SYSTEM 'http://java.sun.com/dtd/preferences.dtd'>";
    static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    static final String PREFS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>    <!ELEMENT preferences (root)>    <!ATTLIST preferences EXTERNAL_XML_VERSION CDATA \"0.0\" >    <!ELEMENT root (map, node*) >    <!ATTLIST root type (system|user) #REQUIRED >    <!ELEMENT node (map, node*) >    <!ATTLIST node name CDATA #REQUIRED >    <!ELEMENT map (entry*) >    <!ELEMENT entry EMPTY >    <!ATTLIST entry key   CDATA #REQUIRED value CDATA #REQUIRED >";
    static final String PREFS_DTD_NAME = "http://java.sun.com/dtd/preferences.dtd";
    private static final float XML_VERSION = 1.0f;
    private static final DocumentBuilder builder;
    private static int indent = -1;

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        try {
            builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    if (systemId.equals(XMLParser.PREFS_DTD_NAME)) {
                        InputSource result = new InputSource(new StringReader(XMLParser.PREFS_DTD));
                        result.setSystemId(XMLParser.PREFS_DTD_NAME);
                        return result;
                    }
                    throw new SAXException("Invalid DOCTYPE declaration " + systemId);
                }
            });
            builder.setErrorHandler(new ErrorHandler() {
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
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }
    }

    private XMLParser() {
    }

    static void exportPrefs(Preferences prefs, OutputStream stream, boolean withSubTree) throws IOException, BackingStoreException {
        indent = -1;
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
        out.write(HEADER);
        out.newLine();
        out.newLine();
        out.write(DOCTYPE);
        out.write(" '");
        out.write(PREFS_DTD_NAME);
        out.write("'>");
        out.newLine();
        out.newLine();
        flushStartTag("preferences", new String[]{"EXTERNAL_XML_VERSION"}, new String[]{String.valueOf(XML_VERSION)}, out);
        String[] strArr = {"type"};
        String[] strArr2 = new String[1];
        strArr2[0] = prefs.isUserNode() ? "user" : "system";
        flushStartTag("root", strArr, strArr2, out);
        flushEmptyElement("map", out);
        StringTokenizer ancestors = new StringTokenizer(prefs.absolutePath(), "/");
        exportNode(ancestors, prefs, withSubTree, out);
        flushEndTag("root", out);
        flushEndTag("preferences", out);
        out.flush();
    }

    private static void exportNode(StringTokenizer ancestors, Preferences prefs, boolean withSubTree, BufferedWriter out) throws IOException, BackingStoreException {
        if (ancestors.hasMoreTokens()) {
            String name = ancestors.nextToken();
            flushStartTag("node", new String[]{"name"}, new String[]{name}, out);
            if (ancestors.hasMoreTokens()) {
                flushEmptyElement("map", out);
                exportNode(ancestors, prefs, withSubTree, out);
            } else {
                exportEntries(prefs, out);
                if (withSubTree) {
                    exportSubTree(prefs, out);
                }
            }
            flushEndTag("node", out);
        }
    }

    private static void exportSubTree(Preferences prefs, BufferedWriter out) throws BackingStoreException, IOException {
        String[] names = prefs.childrenNames();
        if (names.length > 0) {
            for (int i = 0; i < names.length; i++) {
                Preferences child = prefs.node(names[i]);
                flushStartTag("node", new String[]{"name"}, new String[]{names[i]}, out);
                exportEntries(child, out);
                exportSubTree(child, out);
                flushEndTag("node", out);
            }
        }
    }

    private static void exportEntries(Preferences prefs, BufferedWriter out) throws BackingStoreException, IOException {
        String[] keys = prefs.keys();
        String[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = prefs.get(keys[i], null);
        }
        exportEntries(keys, values, out);
    }

    private static void exportEntries(String[] keys, String[] values, BufferedWriter out) throws IOException {
        if (keys.length == 0) {
            flushEmptyElement("map", out);
            return;
        }
        flushStartTag("map", out);
        for (int i = 0; i < keys.length; i++) {
            if (values[i] != null) {
                flushEmptyElement("entry", new String[]{"key", "value"}, new String[]{keys[i], values[i]}, out);
            }
        }
        flushEndTag("map", out);
    }

    private static void flushEndTag(String tagName, BufferedWriter out) throws IOException {
        int i = indent;
        indent = i - 1;
        flushIndent(i, out);
        out.write("</");
        out.write(tagName);
        out.write(">");
        out.newLine();
    }

    private static void flushEmptyElement(String tagName, BufferedWriter out) throws IOException {
        int i = indent + 1;
        indent = i;
        flushIndent(i, out);
        out.write("<");
        out.write(tagName);
        out.write(" />");
        out.newLine();
        indent--;
    }

    private static void flushEmptyElement(String tagName, String[] attrKeys, String[] attrValues, BufferedWriter out) throws IOException {
        int i = indent + 1;
        indent = i;
        flushIndent(i, out);
        out.write("<");
        out.write(tagName);
        flushPairs(attrKeys, attrValues, out);
        out.write(" />");
        out.newLine();
        indent--;
    }

    private static void flushPairs(String[] attrKeys, String[] attrValues, BufferedWriter out) throws IOException {
        for (int i = 0; i < attrKeys.length; i++) {
            out.write(" ");
            out.write(attrKeys[i]);
            out.write("=\"");
            out.write(htmlEncode(attrValues[i]));
            out.write("\"");
        }
    }

    private static void flushIndent(int ind, BufferedWriter out) throws IOException {
        for (int i = 0; i < ind; i++) {
            out.write("  ");
        }
    }

    private static void flushStartTag(String tagName, String[] attrKeys, String[] attrValues, BufferedWriter out) throws IOException {
        int i = indent + 1;
        indent = i;
        flushIndent(i, out);
        out.write("<");
        out.write(tagName);
        flushPairs(attrKeys, attrValues, out);
        out.write(">");
        out.newLine();
    }

    private static void flushStartTag(String tagName, BufferedWriter out) throws IOException {
        int i = indent + 1;
        indent = i;
        flushIndent(i, out);
        out.write("<");
        out.write(tagName);
        out.write(">");
        out.newLine();
    }

    private static String htmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    sb.append("&quot;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case Opcodes.OP_IF_GTZ:
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    static void importPrefs(InputStream in) throws IOException, InvalidPreferencesFormatException {
        Preferences prefsRoot;
        try {
            Document doc = builder.parse(new InputSource(in));
            Element preferences = doc.getDocumentElement();
            String version = preferences.getAttribute("EXTERNAL_XML_VERSION");
            if (version != null && Float.parseFloat(version) > XML_VERSION) {
                throw new InvalidPreferencesFormatException("Preferences version " + version + " is not supported");
            }
            Element root = (Element) preferences.getElementsByTagName("root").item(0);
            String type = root.getAttribute("type");
            if (type.equals("user")) {
                prefsRoot = Preferences.userRoot();
            } else {
                prefsRoot = Preferences.systemRoot();
            }
            loadNode(prefsRoot, root);
        } catch (FactoryConfigurationError e) {
            throw new InvalidPreferencesFormatException(e);
        } catch (SAXException e2) {
            throw new InvalidPreferencesFormatException(e2);
        }
    }

    private static void loadNode(Preferences prefs, Element node) {
        NodeList children = selectNodeList(node, "node");
        NodeList entries = selectNodeList(node, "map/entry");
        int childNumber = children.getLength();
        Preferences[] prefChildren = new Preferences[childNumber];
        int entryNumber = entries.getLength();
        synchronized (((AbstractPreferences) prefs).lock) {
            if (!((AbstractPreferences) prefs).isRemoved()) {
                for (int i = 0; i < entryNumber; i++) {
                    Element entry = (Element) entries.item(i);
                    String key = entry.getAttribute("key");
                    String value = entry.getAttribute("value");
                    prefs.put(key, value);
                }
                for (int i2 = 0; i2 < childNumber; i2++) {
                    Element child = (Element) children.item(i2);
                    String name = child.getAttribute("name");
                    prefChildren[i2] = prefs.node(name);
                }
                for (int i3 = 0; i3 < childNumber; i3++) {
                    loadNode(prefChildren[i3], (Element) children.item(i3));
                }
            }
        }
    }

    private static NodeList selectNodeList(Element documentElement, String string) {
        ArrayList<Node> input = new ArrayList<>();
        String[] path = string.split("/");
        NodeList childNodes = documentElement.getChildNodes();
        if (path[0].equals("entry") || path[0].equals("node")) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node next = childNodes.item(i);
                if ((next instanceof Element) && ((Element) next).getNodeName().equals(path[0])) {
                    input.add(next);
                }
            }
        } else if (path[0].equals("map") && path[1].equals("entry")) {
            for (int i2 = 0; i2 < childNodes.getLength(); i2++) {
                Node next2 = childNodes.item(i2);
                if ((next2 instanceof Element) && ((Element) next2).getNodeName().equals(path[0])) {
                    NodeList nextChildNodes = next2.getChildNodes();
                    for (int j = 0; j < nextChildNodes.getLength(); j++) {
                        Node subnext = nextChildNodes.item(j);
                        if ((subnext instanceof Element) && ((Element) subnext).getNodeName().equals(path[1])) {
                            input.add(subnext);
                        }
                    }
                }
            }
        }
        NodeList result = new NodeSet(input.iterator());
        return result;
    }

    static Properties readXmlPreferences(File xmlFile) throws Throwable {
        Properties result = new Properties();
        if (!xmlFile.exists()) {
            xmlFile.getParentFile().mkdirs();
        } else if (xmlFile.canRead()) {
            Reader reader = null;
            try {
                Reader reader2 = new InputStreamReader(new FileInputStream(xmlFile), "UTF-8");
                try {
                    Document document = builder.parse(new InputSource(reader2));
                    NodeList entries = selectNodeList(document.getDocumentElement(), "entry");
                    int length = entries.getLength();
                    for (int i = 0; i < length; i++) {
                        Element node = (Element) entries.item(i);
                        String key = node.getAttribute("key");
                        String value = node.getAttribute("value");
                        result.setProperty(key, value);
                    }
                    IoUtils.closeQuietly(reader2);
                } catch (IOException e) {
                    reader = reader2;
                    IoUtils.closeQuietly(reader);
                } catch (SAXException e2) {
                    reader = reader2;
                    IoUtils.closeQuietly(reader);
                } catch (Throwable th) {
                    th = th;
                    reader = reader2;
                    IoUtils.closeQuietly(reader);
                    throw th;
                }
            } catch (IOException e3) {
            } catch (SAXException e4) {
            } catch (Throwable th2) {
                th = th2;
            }
        } else {
            xmlFile.delete();
        }
        return result;
    }

    static void writeXmlPreferences(File xmlFile, Properties properties) throws Throwable {
        File parent = xmlFile.getParentFile();
        File temporaryForWriting = new File(parent, "prefs-" + UUID.randomUUID() + ".xml.tmp");
        BufferedWriter out = null;
        try {
            BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temporaryForWriting), "UTF-8"));
            try {
                out2.write(HEADER);
                out2.newLine();
                out2.write(FILE_PREFS);
                out2.newLine();
                String[] keys = (String[]) properties.keySet().toArray(new String[properties.size()]);
                int length = keys.length;
                String[] values = new String[length];
                for (int i = 0; i < length; i++) {
                    values[i] = properties.getProperty(keys[i]);
                }
                exportEntries(keys, values, out2);
                out2.close();
                if (!temporaryForWriting.renameTo(xmlFile)) {
                    throw new IOException("Failed to write preferences to " + xmlFile);
                }
                IoUtils.closeQuietly(out2);
                temporaryForWriting.delete();
            } catch (Throwable th) {
                th = th;
                out = out2;
                IoUtils.closeQuietly(out);
                temporaryForWriting.delete();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }
}
