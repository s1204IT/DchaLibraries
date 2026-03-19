package java.util.prefs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import sun.security.x509.PolicyMappingsExtension;

class XmlSupport {
    private static final String EXTERNAL_XML_VERSION = "1.0";
    private static final String MAP_XML_VERSION = "1.0";
    private static final String PREFS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- DTD for preferences --><!ELEMENT preferences (root) ><!ATTLIST preferences EXTERNAL_XML_VERSION CDATA \"0.0\"  ><!ELEMENT root (map, node*) ><!ATTLIST root          type (system|user) #REQUIRED ><!ELEMENT node (map, node*) ><!ATTLIST node          name CDATA #REQUIRED ><!ELEMENT map (entry*) ><!ATTLIST map  MAP_XML_VERSION CDATA \"0.0\"  ><!ELEMENT entry EMPTY ><!ATTLIST entry          key CDATA #REQUIRED          value CDATA #REQUIRED >";
    private static final String PREFS_DTD_URI = "http://java.sun.com/dtd/preferences.dtd";

    XmlSupport() {
    }

    static void export(OutputStream outputStream, Preferences preferences, boolean z) throws BackingStoreException, IOException {
        if (((AbstractPreferences) preferences).isRemoved()) {
            throw new IllegalStateException("Node has been removed");
        }
        Document documentCreatePrefsDoc = createPrefsDoc("preferences");
        Element documentElement = documentCreatePrefsDoc.getDocumentElement();
        documentElement.setAttribute("EXTERNAL_XML_VERSION", "1.0");
        Element element = (Element) documentElement.appendChild(documentCreatePrefsDoc.createElement("root"));
        element.setAttribute("type", preferences.isUserNode() ? "user" : "system");
        ArrayList arrayList = new ArrayList();
        Preferences preferences2 = preferences;
        for (Preferences preferencesParent = preferences.parent(); preferencesParent != null; preferencesParent = preferencesParent.parent()) {
            arrayList.add(preferences2);
            preferences2 = preferencesParent;
        }
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            element.appendChild(documentCreatePrefsDoc.createElement(PolicyMappingsExtension.MAP));
            element = (Element) element.appendChild(documentCreatePrefsDoc.createElement("node"));
            element.setAttribute("name", ((Preferences) arrayList.get(size)).name());
        }
        putPreferencesInXml(element, documentCreatePrefsDoc, preferences, z);
        writeDoc(documentCreatePrefsDoc, outputStream);
    }

    private static void putPreferencesInXml(Element element, Document document, Preferences preferences, boolean z) throws BackingStoreException {
        String[] strArrChildrenNames;
        Preferences[] preferencesArr;
        synchronized (((AbstractPreferences) preferences).lock) {
            if (((AbstractPreferences) preferences).isRemoved()) {
                element.getParentNode().removeChild(element);
                return;
            }
            String[] strArrKeys = preferences.keys();
            Element element2 = (Element) element.appendChild(document.createElement(PolicyMappingsExtension.MAP));
            int i = 0;
            while (true) {
                strArrChildrenNames = null;
                if (i >= strArrKeys.length) {
                    break;
                }
                Element element3 = (Element) element2.appendChild(document.createElement("entry"));
                element3.setAttribute("key", strArrKeys[i]);
                element3.setAttribute("value", preferences.get(strArrKeys[i], null));
                i++;
            }
            if (z) {
                strArrChildrenNames = preferences.childrenNames();
                preferencesArr = new Preferences[strArrChildrenNames.length];
                for (int i2 = 0; i2 < strArrChildrenNames.length; i2++) {
                    preferencesArr[i2] = preferences.node(strArrChildrenNames[i2]);
                }
            } else {
                preferencesArr = null;
            }
            if (z) {
                for (int i3 = 0; i3 < strArrChildrenNames.length; i3++) {
                    Element element4 = (Element) element.appendChild(document.createElement("node"));
                    element4.setAttribute("name", strArrChildrenNames[i3]);
                    putPreferencesInXml(element4, document, preferencesArr[i3], z);
                }
            }
        }
    }

    static void importPreferences(InputStream inputStream) throws IOException, InvalidPreferencesFormatException {
        try {
            Document documentLoadPrefsDoc = loadPrefsDoc(inputStream);
            String attribute = documentLoadPrefsDoc.getDocumentElement().getAttribute("EXTERNAL_XML_VERSION");
            if (attribute.compareTo("1.0") > 0) {
                throw new InvalidPreferencesFormatException("Exported preferences file format version " + attribute + " is not supported. This java installation can read versions 1.0 or older. You may need to install a newer version of JDK.");
            }
            NodeList elementsByTagName = documentLoadPrefsDoc.getDocumentElement().getElementsByTagName("root");
            if (elementsByTagName == null || elementsByTagName.getLength() != 1) {
                throw new InvalidPreferencesFormatException("invalid root node");
            }
            Element element = (Element) elementsByTagName.item(0);
            ImportSubtree(element.getAttribute("type").equals("user") ? Preferences.userRoot() : Preferences.systemRoot(), element);
        } catch (SAXException e) {
            throw new InvalidPreferencesFormatException(e);
        }
    }

    private static Document createPrefsDoc(String str) {
        try {
            DOMImplementation dOMImplementation = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            return dOMImplementation.createDocument(null, str, dOMImplementation.createDocumentType(str, null, PREFS_DTD_URI));
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    private static Document loadPrefsDoc(InputStream inputStream) throws SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
        documentBuilderFactoryNewInstance.setIgnoringElementContentWhitespace(true);
        documentBuilderFactoryNewInstance.setCoalescing(true);
        documentBuilderFactoryNewInstance.setIgnoringComments(true);
        try {
            DocumentBuilder documentBuilderNewDocumentBuilder = documentBuilderFactoryNewInstance.newDocumentBuilder();
            documentBuilderNewDocumentBuilder.setEntityResolver(new Resolver());
            documentBuilderNewDocumentBuilder.setErrorHandler(new EH());
            return documentBuilderNewDocumentBuilder.parse(new InputSource(inputStream));
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    private static final void writeDoc(Document document, OutputStream outputStream) throws IOException {
        try {
            TransformerFactory transformerFactoryNewInstance = TransformerFactory.newInstance();
            try {
                transformerFactoryNewInstance.setAttribute("indent-number", new Integer(2));
            } catch (IllegalArgumentException e) {
            }
            Transformer transformerNewTransformer = transformerFactoryNewInstance.newTransformer();
            transformerNewTransformer.setOutputProperty("doctype-system", document.getDoctype().getSystemId());
            transformerNewTransformer.setOutputProperty("indent", "yes");
            transformerNewTransformer.transform(new DOMSource(document), new StreamResult(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))));
        } catch (TransformerException e2) {
            throw new AssertionError(e2);
        }
    }

    private static List<Element> getChildElements(Element element) {
        NodeList childNodes = element.getChildNodes();
        ArrayList arrayList = new ArrayList(childNodes.getLength());
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                arrayList.add((Element) childNodes.item(i));
            }
        }
        return arrayList;
    }

    private static void ImportSubtree(Preferences preferences, Element element) {
        List<Element> childElements = getChildElements(element);
        synchronized (((AbstractPreferences) preferences).lock) {
            if (((AbstractPreferences) preferences).isRemoved()) {
                return;
            }
            ImportPrefs(preferences, childElements.get(0));
            Preferences[] preferencesArr = new Preferences[childElements.size() - 1];
            for (int i = 1; i < childElements.size(); i++) {
                preferencesArr[i - 1] = preferences.node(childElements.get(i).getAttribute("name"));
            }
            for (int i2 = 1; i2 < childElements.size(); i2++) {
                ImportSubtree(preferencesArr[i2 - 1], childElements.get(i2));
            }
        }
    }

    private static void ImportPrefs(Preferences preferences, Element element) {
        List<Element> childElements = getChildElements(element);
        int size = childElements.size();
        for (int i = 0; i < size; i++) {
            Element element2 = childElements.get(i);
            preferences.put(element2.getAttribute("key"), element2.getAttribute("value"));
        }
    }

    static void exportMap(OutputStream outputStream, Map<String, String> map) throws IOException {
        Document documentCreatePrefsDoc = createPrefsDoc(PolicyMappingsExtension.MAP);
        Element documentElement = documentCreatePrefsDoc.getDocumentElement();
        documentElement.setAttribute("MAP_XML_VERSION", "1.0");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Element element = (Element) documentElement.appendChild(documentCreatePrefsDoc.createElement("entry"));
            element.setAttribute("key", entry.getKey());
            element.setAttribute("value", entry.getValue());
        }
        writeDoc(documentCreatePrefsDoc, outputStream);
    }

    static void importMap(InputStream inputStream, Map<String, String> map) throws IOException, InvalidPreferencesFormatException {
        try {
            Element documentElement = loadPrefsDoc(inputStream).getDocumentElement();
            String attribute = documentElement.getAttribute("MAP_XML_VERSION");
            if (attribute.compareTo("1.0") > 0) {
                throw new InvalidPreferencesFormatException("Preferences map file format version " + attribute + " is not supported. This java installation can read versions 1.0 or older. You may need to install a newer version of JDK.");
            }
            NodeList childNodes = documentElement.getChildNodes();
            int length = childNodes.getLength();
            for (int i = 0; i < length; i++) {
                if (childNodes.item(i) instanceof Element) {
                    Element element = (Element) childNodes.item(i);
                    map.put(element.getAttribute("key"), element.getAttribute("value"));
                }
            }
        } catch (SAXException e) {
            throw new InvalidPreferencesFormatException(e);
        }
    }

    private static class Resolver implements EntityResolver {
        private Resolver() {
        }

        @Override
        public InputSource resolveEntity(String str, String str2) throws SAXException {
            if (str2.equals(XmlSupport.PREFS_DTD_URI)) {
                InputSource inputSource = new InputSource(new StringReader(XmlSupport.PREFS_DTD));
                inputSource.setSystemId(XmlSupport.PREFS_DTD_URI);
                return inputSource;
            }
            throw new SAXException("Invalid system identifier: " + str2);
        }
    }

    private static class EH implements ErrorHandler {
        private EH() {
        }

        @Override
        public void error(SAXParseException sAXParseException) throws SAXException {
            throw sAXParseException;
        }

        @Override
        public void fatalError(SAXParseException sAXParseException) throws SAXException {
            throw sAXParseException;
        }

        @Override
        public void warning(SAXParseException sAXParseException) throws SAXException {
            throw sAXParseException;
        }
    }
}
