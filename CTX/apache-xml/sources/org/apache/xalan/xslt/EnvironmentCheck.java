package org.apache.xalan.xslt;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.xalan.templates.Constants;
import org.apache.xml.serializer.SerializerConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

public class EnvironmentCheck {
    public static final String CLASS_NOTPRESENT = "not-present";
    public static final String CLASS_PRESENT = "present-unknown-version";
    public static final String ERROR = "ERROR.";
    public static final String ERROR_FOUND = "At least one error was found!";
    public static final String FOUNDCLASSES = "foundclasses.";
    public static final String VERSION = "version.";
    public static final String WARNING = "WARNING.";
    private static Hashtable jarVersions = new Hashtable();
    public String[] jarNames = {"xalan.jar", "xalansamples.jar", "xalanj1compat.jar", "xalanservlet.jar", "serializer.jar", "xerces.jar", "xercesImpl.jar", "testxsl.jar", "crimson.jar", "lotusxsl.jar", "jaxp.jar", "parser.jar", "dom.jar", "sax.jar", "xml.jar", "xml-apis.jar", "xsltc.jar"};
    protected PrintWriter outWriter = new PrintWriter((OutputStream) System.out, true);

    public static void main(String[] strArr) {
        PrintWriter printWriter = new PrintWriter((OutputStream) System.out, true);
        int i = 0;
        while (i < strArr.length) {
            if ("-out".equalsIgnoreCase(strArr[i])) {
                i++;
                if (i < strArr.length) {
                    try {
                        printWriter = new PrintWriter(new FileWriter(strArr[i], true));
                    } catch (Exception e) {
                        System.err.println("# WARNING: -out " + strArr[i] + " threw " + e.toString());
                    }
                } else {
                    System.err.println("# WARNING: -out argument should have a filename, output sent to console");
                }
            }
            i++;
        }
        new EnvironmentCheck().checkEnvironment(printWriter);
    }

    public boolean checkEnvironment(PrintWriter printWriter) {
        if (printWriter != null) {
            this.outWriter = printWriter;
        }
        if (writeEnvironmentReport(getEnvironmentHash())) {
            logMsg("# WARNING: Potential problems found in your environment!");
            logMsg("#    Check any 'ERROR' items above against the Xalan FAQs");
            logMsg("#    to correct potential problems with your classes/jars");
            logMsg("#    http://xml.apache.org/xalan-j/faq.html");
            if (this.outWriter != null) {
                this.outWriter.flush();
                return false;
            }
            return false;
        }
        logMsg("# YAHOO! Your environment seems to be OK.");
        if (this.outWriter != null) {
            this.outWriter.flush();
            return true;
        }
        return true;
    }

    public Hashtable getEnvironmentHash() {
        Hashtable hashtable = new Hashtable();
        checkJAXPVersion(hashtable);
        checkProcessorVersion(hashtable);
        checkParserVersion(hashtable);
        checkAntVersion(hashtable);
        checkDOMVersion(hashtable);
        checkSAXVersion(hashtable);
        checkSystemProperties(hashtable);
        return hashtable;
    }

    protected boolean writeEnvironmentReport(Hashtable hashtable) {
        boolean zLogFoundJars = false;
        if (hashtable == null) {
            logMsg("# ERROR: writeEnvironmentReport called with null Hashtable");
            return false;
        }
        logMsg("#---- BEGIN writeEnvironmentReport($Revision: 468646 $): Useful stuff found: ----");
        Enumeration enumerationKeys = hashtable.keys();
        while (enumerationKeys.hasMoreElements()) {
            Object objNextElement = enumerationKeys.nextElement();
            String str = (String) objNextElement;
            try {
                if (str.startsWith(FOUNDCLASSES)) {
                    zLogFoundJars |= logFoundJars((Vector) hashtable.get(str), str);
                } else {
                    if (str.startsWith(ERROR)) {
                        zLogFoundJars = true;
                    }
                    logMsg(str + "=" + hashtable.get(str));
                }
            } catch (Exception e) {
                logMsg("Reading-" + objNextElement + "= threw: " + e.toString());
            }
        }
        logMsg("#----- END writeEnvironmentReport: Useful properties found: -----");
        return zLogFoundJars;
    }

    protected boolean logFoundJars(Vector vector, String str) {
        if (vector == null || vector.size() < 1) {
            return false;
        }
        logMsg("#---- BEGIN Listing XML-related jars in: " + str + " ----");
        boolean z = false;
        for (int i = 0; i < vector.size(); i++) {
            Hashtable hashtable = (Hashtable) vector.elementAt(i);
            Enumeration enumerationKeys = hashtable.keys();
            while (enumerationKeys.hasMoreElements()) {
                Object objNextElement = enumerationKeys.nextElement();
                String str2 = (String) objNextElement;
                try {
                    if (str2.startsWith(ERROR)) {
                        z = true;
                    }
                    logMsg(str2 + "=" + hashtable.get(str2));
                } catch (Exception e) {
                    logMsg("Reading-" + objNextElement + "= threw: " + e.toString());
                    z = true;
                }
            }
        }
        logMsg("#----- END Listing XML-related jars in: " + str + " -----");
        return z;
    }

    public void appendEnvironmentReport(Node node, Document document, Hashtable hashtable) {
        if (node == null || document == null) {
            return;
        }
        try {
            Element elementCreateElement = document.createElement("EnvironmentCheck");
            elementCreateElement.setAttribute("version", "$Revision: 468646 $");
            node.appendChild(elementCreateElement);
            if (hashtable == null) {
                Element elementCreateElement2 = document.createElement("status");
                elementCreateElement2.setAttribute(Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, "ERROR");
                elementCreateElement2.appendChild(document.createTextNode("appendEnvironmentReport called with null Hashtable!"));
                elementCreateElement.appendChild(elementCreateElement2);
                return;
            }
            boolean zAppendFoundJars = false;
            Element elementCreateElement3 = document.createElement("environment");
            elementCreateElement.appendChild(elementCreateElement3);
            Enumeration enumerationKeys = hashtable.keys();
            while (enumerationKeys.hasMoreElements()) {
                Object objNextElement = enumerationKeys.nextElement();
                String str = (String) objNextElement;
                try {
                    if (str.startsWith(FOUNDCLASSES)) {
                        zAppendFoundJars |= appendFoundJars(elementCreateElement3, document, (Vector) hashtable.get(str), str);
                    } else {
                        if (str.startsWith(ERROR)) {
                            zAppendFoundJars = true;
                        }
                        Element elementCreateElement4 = document.createElement("item");
                        elementCreateElement4.setAttribute("key", str);
                        elementCreateElement4.appendChild(document.createTextNode((String) hashtable.get(str)));
                        elementCreateElement3.appendChild(elementCreateElement4);
                    }
                } catch (Exception e) {
                    Element elementCreateElement5 = document.createElement("item");
                    elementCreateElement5.setAttribute("key", str);
                    elementCreateElement5.appendChild(document.createTextNode("ERROR. Reading " + objNextElement + " threw: " + e.toString()));
                    elementCreateElement3.appendChild(elementCreateElement5);
                    zAppendFoundJars = true;
                }
            }
            Element elementCreateElement6 = document.createElement("status");
            elementCreateElement6.setAttribute(Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, zAppendFoundJars ? "ERROR" : "OK");
            elementCreateElement.appendChild(elementCreateElement6);
        } catch (Exception e2) {
            System.err.println("appendEnvironmentReport threw: " + e2.toString());
            e2.printStackTrace();
        }
    }

    protected boolean appendFoundJars(Node node, Document document, Vector vector, String str) {
        if (vector == null || vector.size() < 1) {
            return false;
        }
        boolean z = false;
        for (int i = 0; i < vector.size(); i++) {
            Hashtable hashtable = (Hashtable) vector.elementAt(i);
            Enumeration enumerationKeys = hashtable.keys();
            while (enumerationKeys.hasMoreElements()) {
                Object objNextElement = enumerationKeys.nextElement();
                try {
                    String str2 = (String) objNextElement;
                    if (str2.startsWith(ERROR)) {
                        z = true;
                    }
                    Element elementCreateElement = document.createElement("foundJar");
                    elementCreateElement.setAttribute("name", str2.substring(0, str2.indexOf("-")));
                    elementCreateElement.setAttribute("desc", str2.substring(str2.indexOf("-") + 1));
                    elementCreateElement.appendChild(document.createTextNode((String) hashtable.get(str2)));
                    node.appendChild(elementCreateElement);
                } catch (Exception e) {
                    Element elementCreateElement2 = document.createElement("foundJar");
                    elementCreateElement2.appendChild(document.createTextNode("ERROR. Reading " + objNextElement + " threw: " + e.toString()));
                    node.appendChild(elementCreateElement2);
                    z = true;
                }
            }
        }
        return z;
    }

    protected void checkSystemProperties(Hashtable hashtable) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        try {
            hashtable.put("java.version", System.getProperty("java.version"));
        } catch (SecurityException e) {
            hashtable.put("java.version", "WARNING: SecurityException thrown accessing system version properties");
        }
        try {
            String property = System.getProperty("java.class.path");
            hashtable.put("java.class.path", property);
            Vector vectorCheckPathForJars = checkPathForJars(property, this.jarNames);
            if (vectorCheckPathForJars != null) {
                hashtable.put("foundclasses.java.class.path", vectorCheckPathForJars);
            }
            String property2 = System.getProperty("sun.boot.class.path");
            if (property2 != null) {
                hashtable.put("sun.boot.class.path", property2);
                Vector vectorCheckPathForJars2 = checkPathForJars(property2, this.jarNames);
                if (vectorCheckPathForJars2 != null) {
                    hashtable.put("foundclasses.sun.boot.class.path", vectorCheckPathForJars2);
                }
            }
            String property3 = System.getProperty("java.ext.dirs");
            if (property3 != null) {
                hashtable.put("java.ext.dirs", property3);
                Vector vectorCheckPathForJars3 = checkPathForJars(property3, this.jarNames);
                if (vectorCheckPathForJars3 != null) {
                    hashtable.put("foundclasses.java.ext.dirs", vectorCheckPathForJars3);
                }
            }
        } catch (SecurityException e2) {
            hashtable.put("java.class.path", "WARNING: SecurityException thrown accessing system classpath properties");
        }
    }

    protected Vector checkPathForJars(String str, String[] strArr) {
        if (str == null || strArr == null || str.length() == 0 || strArr.length == 0) {
            return null;
        }
        Vector vector = new Vector();
        StringTokenizer stringTokenizer = new StringTokenizer(str, File.pathSeparator);
        while (stringTokenizer.hasMoreTokens()) {
            String strNextToken = stringTokenizer.nextToken();
            for (int i = 0; i < strArr.length; i++) {
                if (strNextToken.indexOf(strArr[i]) > -1) {
                    File file = new File(strNextToken);
                    if (file.exists()) {
                        try {
                            Hashtable hashtable = new Hashtable(2);
                            hashtable.put(strArr[i] + "-path", file.getAbsolutePath());
                            if (!"xalan.jar".equalsIgnoreCase(strArr[i])) {
                                hashtable.put(strArr[i] + "-apparent.version", getApparentVersion(strArr[i], file.length()));
                            }
                            vector.addElement(hashtable);
                        } catch (Exception e) {
                        }
                    } else {
                        Hashtable hashtable2 = new Hashtable(2);
                        hashtable2.put(strArr[i] + "-path", "WARNING. Classpath entry: " + strNextToken + " does not exist");
                        StringBuilder sb = new StringBuilder();
                        sb.append(strArr[i]);
                        sb.append("-apparent.version");
                        hashtable2.put(sb.toString(), CLASS_NOTPRESENT);
                        vector.addElement(hashtable2);
                    }
                }
            }
        }
        return vector;
    }

    protected String getApparentVersion(String str, long j) {
        String str2 = (String) jarVersions.get(new Long(j));
        if (str2 != null && str2.startsWith(str)) {
            return str2;
        }
        if ("xerces.jar".equalsIgnoreCase(str) || "xercesImpl.jar".equalsIgnoreCase(str)) {
            return str + " " + WARNING + CLASS_PRESENT;
        }
        return str + " " + CLASS_PRESENT;
    }

    protected void checkJAXPVersion(Hashtable hashtable) {
        Class clsFindProviderClass;
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        Class<?>[] clsArr = new Class[0];
        try {
            clsFindProviderClass = ObjectFactory.findProviderClass("javax.xml.parsers.DocumentBuilder", ObjectFactory.findClassLoader(), true);
        } catch (Exception e) {
            clsFindProviderClass = null;
        }
        try {
            clsFindProviderClass.getMethod("getDOMImplementation", clsArr);
            hashtable.put("version.JAXP", "1.1 or higher");
        } catch (Exception e2) {
            if (clsFindProviderClass != null) {
                hashtable.put("ERROR.version.JAXP", "1.0.1");
                hashtable.put(ERROR, ERROR_FOUND);
            } else {
                hashtable.put("ERROR.version.JAXP", CLASS_NOTPRESENT);
                hashtable.put(ERROR, ERROR_FOUND);
            }
        }
    }

    protected void checkProcessorVersion(Hashtable hashtable) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        try {
            Class clsFindProviderClass = ObjectFactory.findProviderClass("org.apache.xalan.xslt.XSLProcessorVersion", ObjectFactory.findClassLoader(), true);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(clsFindProviderClass.getField("PRODUCT").get(null));
            stringBuffer.append(';');
            stringBuffer.append(clsFindProviderClass.getField("LANGUAGE").get(null));
            stringBuffer.append(';');
            stringBuffer.append(clsFindProviderClass.getField("S_VERSION").get(null));
            stringBuffer.append(';');
            hashtable.put("version.xalan1", stringBuffer.toString());
        } catch (Exception e) {
            hashtable.put("version.xalan1", CLASS_NOTPRESENT);
        }
        try {
            Class clsFindProviderClass2 = ObjectFactory.findProviderClass("org.apache.xalan.processor.XSLProcessorVersion", ObjectFactory.findClassLoader(), true);
            StringBuffer stringBuffer2 = new StringBuffer();
            stringBuffer2.append(clsFindProviderClass2.getField("S_VERSION").get(null));
            hashtable.put("version.xalan2x", stringBuffer2.toString());
        } catch (Exception e2) {
            hashtable.put("version.xalan2x", CLASS_NOTPRESENT);
        }
        try {
            hashtable.put("version.xalan2_2", (String) ObjectFactory.findProviderClass("org.apache.xalan.Version", ObjectFactory.findClassLoader(), true).getMethod("getVersion", new Class[0]).invoke(null, new Object[0]));
        } catch (Exception e3) {
            hashtable.put("version.xalan2_2", CLASS_NOTPRESENT);
        }
    }

    protected void checkParserVersion(Hashtable hashtable) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        try {
            hashtable.put("version.xerces1", (String) ObjectFactory.findProviderClass("org.apache.xerces.framework.Version", ObjectFactory.findClassLoader(), true).getField("fVersion").get(null));
        } catch (Exception e) {
            hashtable.put("version.xerces1", CLASS_NOTPRESENT);
        }
        try {
            hashtable.put("version.xerces2", (String) ObjectFactory.findProviderClass("org.apache.xerces.impl.Version", ObjectFactory.findClassLoader(), true).getField("fVersion").get(null));
        } catch (Exception e2) {
            hashtable.put("version.xerces2", CLASS_NOTPRESENT);
        }
        try {
            ObjectFactory.findProviderClass("org.apache.crimson.parser.Parser2", ObjectFactory.findClassLoader(), true);
            hashtable.put("version.crimson", CLASS_PRESENT);
        } catch (Exception e3) {
            hashtable.put("version.crimson", CLASS_NOTPRESENT);
        }
    }

    protected void checkAntVersion(Hashtable hashtable) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        try {
            hashtable.put("version.ant", (String) ObjectFactory.findProviderClass("org.apache.tools.ant.Main", ObjectFactory.findClassLoader(), true).getMethod("getAntVersion", new Class[0]).invoke(null, new Object[0]));
        } catch (Exception e) {
            hashtable.put("version.ant", CLASS_NOTPRESENT);
        }
    }

    protected void checkDOMVersion(Hashtable hashtable) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        Class<?>[] clsArr = {String.class, String.class};
        try {
            ObjectFactory.findProviderClass("org.w3c.dom.Document", ObjectFactory.findClassLoader(), true).getMethod("createElementNS", clsArr);
            hashtable.put("version.DOM", "2.0");
            try {
                ObjectFactory.findProviderClass("org.w3c.dom.Node", ObjectFactory.findClassLoader(), true).getMethod("supported", clsArr);
                hashtable.put("ERROR.version.DOM.draftlevel", "2.0wd");
                hashtable.put(ERROR, ERROR_FOUND);
            } catch (Exception e) {
                try {
                    ObjectFactory.findProviderClass("org.w3c.dom.Node", ObjectFactory.findClassLoader(), true).getMethod("isSupported", clsArr);
                    hashtable.put("version.DOM.draftlevel", "2.0fd");
                } catch (Exception e2) {
                    hashtable.put("ERROR.version.DOM.draftlevel", "2.0unknown");
                    hashtable.put(ERROR, ERROR_FOUND);
                }
            }
        } catch (Exception e3) {
            hashtable.put("ERROR.version.DOM", "ERROR attempting to load DOM level 2 class: " + e3.toString());
            hashtable.put(ERROR, ERROR_FOUND);
        }
    }

    protected void checkSAXVersion(Hashtable hashtable) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        Class<?>[] clsArr = {String.class};
        try {
            ObjectFactory.findProviderClass("org.xml.sax.helpers.AttributesImpl", ObjectFactory.findClassLoader(), true).getMethod("setAttributes", Attributes.class);
            hashtable.put("version.SAX", "2.0");
        } catch (Exception e) {
            hashtable.put("ERROR.version.SAX", "ERROR attempting to load SAX version 2 class: " + e.toString());
            hashtable.put(ERROR, ERROR_FOUND);
            try {
                ObjectFactory.findProviderClass("org.xml.sax.XMLReader", ObjectFactory.findClassLoader(), true).getMethod("parse", clsArr);
                hashtable.put("version.SAX-backlevel", "2.0beta2-or-earlier");
            } catch (Exception e2) {
                hashtable.put("ERROR.version.SAX", "ERROR attempting to load SAX version 2 class: " + e.toString());
                hashtable.put(ERROR, ERROR_FOUND);
                try {
                    ObjectFactory.findProviderClass("org.xml.sax.Parser", ObjectFactory.findClassLoader(), true).getMethod("parse", clsArr);
                    hashtable.put("version.SAX-backlevel", SerializerConstants.XMLVERSION10);
                } catch (Exception e3) {
                    hashtable.put("ERROR.version.SAX-backlevel", "ERROR attempting to load SAX version 1 class: " + e3.toString());
                }
            }
        }
    }

    static {
        jarVersions.put(new Long(857192L), "xalan.jar from xalan-j_1_1");
        jarVersions.put(new Long(440237L), "xalan.jar from xalan-j_1_2");
        jarVersions.put(new Long(436094L), "xalan.jar from xalan-j_1_2_1");
        jarVersions.put(new Long(426249L), "xalan.jar from xalan-j_1_2_2");
        jarVersions.put(new Long(702536L), "xalan.jar from xalan-j_2_0_0");
        jarVersions.put(new Long(720930L), "xalan.jar from xalan-j_2_0_1");
        jarVersions.put(new Long(732330L), "xalan.jar from xalan-j_2_1_0");
        jarVersions.put(new Long(872241L), "xalan.jar from xalan-j_2_2_D10");
        jarVersions.put(new Long(882739L), "xalan.jar from xalan-j_2_2_D11");
        jarVersions.put(new Long(923866L), "xalan.jar from xalan-j_2_2_0");
        jarVersions.put(new Long(905872L), "xalan.jar from xalan-j_2_3_D1");
        jarVersions.put(new Long(906122L), "xalan.jar from xalan-j_2_3_0");
        jarVersions.put(new Long(906248L), "xalan.jar from xalan-j_2_3_1");
        jarVersions.put(new Long(983377L), "xalan.jar from xalan-j_2_4_D1");
        jarVersions.put(new Long(997276L), "xalan.jar from xalan-j_2_4_0");
        jarVersions.put(new Long(1031036L), "xalan.jar from xalan-j_2_4_1");
        jarVersions.put(new Long(596540L), "xsltc.jar from xalan-j_2_2_0");
        jarVersions.put(new Long(590247L), "xsltc.jar from xalan-j_2_3_D1");
        jarVersions.put(new Long(589914L), "xsltc.jar from xalan-j_2_3_0");
        jarVersions.put(new Long(589915L), "xsltc.jar from xalan-j_2_3_1");
        jarVersions.put(new Long(1306667L), "xsltc.jar from xalan-j_2_4_D1");
        jarVersions.put(new Long(1328227L), "xsltc.jar from xalan-j_2_4_0");
        jarVersions.put(new Long(1344009L), "xsltc.jar from xalan-j_2_4_1");
        jarVersions.put(new Long(1348361L), "xsltc.jar from xalan-j_2_5_D1");
        jarVersions.put(new Long(1268634L), "xsltc.jar-bundled from xalan-j_2_3_0");
        jarVersions.put(new Long(100196L), "xml-apis.jar from xalan-j_2_2_0 or xalan-j_2_3_D1");
        jarVersions.put(new Long(108484L), "xml-apis.jar from xalan-j_2_3_0, or xalan-j_2_3_1 from xml-commons-1.0.b2");
        jarVersions.put(new Long(109049L), "xml-apis.jar from xalan-j_2_4_0 from xml-commons RIVERCOURT1 branch");
        jarVersions.put(new Long(113749L), "xml-apis.jar from xalan-j_2_4_1 from factoryfinder-build of xml-commons RIVERCOURT1");
        jarVersions.put(new Long(124704L), "xml-apis.jar from tck-jaxp-1_2_0 branch of xml-commons");
        jarVersions.put(new Long(124724L), "xml-apis.jar from tck-jaxp-1_2_0 branch of xml-commons, tag: xml-commons-external_1_2_01");
        jarVersions.put(new Long(194205L), "xml-apis.jar from head branch of xml-commons, tag: xml-commons-external_1_3_02");
        jarVersions.put(new Long(424490L), "xalan.jar from Xerces Tools releases - ERROR:DO NOT USE!");
        jarVersions.put(new Long(1591855L), "xerces.jar from xalan-j_1_1 from xerces-1...");
        jarVersions.put(new Long(1498679L), "xerces.jar from xalan-j_1_2 from xerces-1_2_0.bin");
        jarVersions.put(new Long(1484896L), "xerces.jar from xalan-j_1_2_1 from xerces-1_2_1.bin");
        jarVersions.put(new Long(804460L), "xerces.jar from xalan-j_1_2_2 from xerces-1_2_2.bin");
        jarVersions.put(new Long(1499244L), "xerces.jar from xalan-j_2_0_0 from xerces-1_2_3.bin");
        jarVersions.put(new Long(1605266L), "xerces.jar from xalan-j_2_0_1 from xerces-1_3_0.bin");
        jarVersions.put(new Long(904030L), "xerces.jar from xalan-j_2_1_0 from xerces-1_4.bin");
        jarVersions.put(new Long(904030L), "xerces.jar from xerces-1_4_0.bin");
        jarVersions.put(new Long(1802885L), "xerces.jar from xerces-1_4_2.bin");
        jarVersions.put(new Long(1734594L), "xerces.jar from Xerces-J-bin.2.0.0.beta3");
        jarVersions.put(new Long(1808883L), "xerces.jar from xalan-j_2_2_D10,D11,D12 or xerces-1_4_3.bin");
        jarVersions.put(new Long(1812019L), "xerces.jar from xalan-j_2_2_0");
        jarVersions.put(new Long(1720292L), "xercesImpl.jar from xalan-j_2_3_D1");
        jarVersions.put(new Long(1730053L), "xercesImpl.jar from xalan-j_2_3_0 or xalan-j_2_3_1 from xerces-2_0_0");
        jarVersions.put(new Long(1728861L), "xercesImpl.jar from xalan-j_2_4_D1 from xerces-2_0_1");
        jarVersions.put(new Long(972027L), "xercesImpl.jar from xalan-j_2_4_0 from xerces-2_1");
        jarVersions.put(new Long(831587L), "xercesImpl.jar from xalan-j_2_4_1 from xerces-2_2");
        jarVersions.put(new Long(891817L), "xercesImpl.jar from xalan-j_2_5_D1 from xerces-2_3");
        jarVersions.put(new Long(895924L), "xercesImpl.jar from xerces-2_4");
        jarVersions.put(new Long(1010806L), "xercesImpl.jar from Xerces-J-bin.2.6.2");
        jarVersions.put(new Long(1203860L), "xercesImpl.jar from Xerces-J-bin.2.7.1");
        jarVersions.put(new Long(37485L), "xalanj1compat.jar from xalan-j_2_0_0");
        jarVersions.put(new Long(38100L), "xalanj1compat.jar from xalan-j_2_0_1");
        jarVersions.put(new Long(18779L), "xalanservlet.jar from xalan-j_2_0_0");
        jarVersions.put(new Long(21453L), "xalanservlet.jar from xalan-j_2_0_1");
        jarVersions.put(new Long(24826L), "xalanservlet.jar from xalan-j_2_3_1 or xalan-j_2_4_1");
        jarVersions.put(new Long(24831L), "xalanservlet.jar from xalan-j_2_4_1");
        jarVersions.put(new Long(5618L), "jaxp.jar from jaxp1.0.1");
        jarVersions.put(new Long(136133L), "parser.jar from jaxp1.0.1");
        jarVersions.put(new Long(28404L), "jaxp.jar from jaxp-1.1");
        jarVersions.put(new Long(187162L), "crimson.jar from jaxp-1.1");
        jarVersions.put(new Long(801714L), "xalan.jar from jaxp-1.1");
        jarVersions.put(new Long(196399L), "crimson.jar from crimson-1.1.1");
        jarVersions.put(new Long(33323L), "jaxp.jar from crimson-1.1.1 or jakarta-ant-1.4.1b1");
        jarVersions.put(new Long(152717L), "crimson.jar from crimson-1.1.2beta2");
        jarVersions.put(new Long(88143L), "xml-apis.jar from crimson-1.1.2beta2");
        jarVersions.put(new Long(206384L), "crimson.jar from crimson-1.1.3 or jakarta-ant-1.4.1b1");
        jarVersions.put(new Long(136198L), "parser.jar from jakarta-ant-1.3 or 1.2");
        jarVersions.put(new Long(5537L), "jaxp.jar from jakarta-ant-1.3 or 1.2");
    }

    protected void logMsg(String str) {
        this.outWriter.println(str);
    }
}
