package org.apache.xalan.xslt;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    public static void main(String[] args) {
        PrintWriter sendOutputTo = new PrintWriter((OutputStream) System.out, true);
        int i = 0;
        while (i < args.length) {
            if ("-out".equalsIgnoreCase(args[i])) {
                i++;
                if (i < args.length) {
                    try {
                        PrintWriter sendOutputTo2 = new PrintWriter(new FileWriter(args[i], true));
                        sendOutputTo = sendOutputTo2;
                    } catch (Exception e) {
                        System.err.println("# WARNING: -out " + args[i] + " threw " + e.toString());
                    }
                } else {
                    System.err.println("# WARNING: -out argument should have a filename, output sent to console");
                }
            }
            i++;
        }
        EnvironmentCheck app = new EnvironmentCheck();
        app.checkEnvironment(sendOutputTo);
    }

    public boolean checkEnvironment(PrintWriter pw) {
        if (pw != null) {
            this.outWriter = pw;
        }
        Hashtable hash = getEnvironmentHash();
        boolean environmentHasErrors = writeEnvironmentReport(hash);
        if (environmentHasErrors) {
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
        Hashtable hash = new Hashtable();
        checkJAXPVersion(hash);
        checkProcessorVersion(hash);
        checkParserVersion(hash);
        checkAntVersion(hash);
        checkDOMVersion(hash);
        checkSAXVersion(hash);
        checkSystemProperties(hash);
        return hash;
    }

    protected boolean writeEnvironmentReport(Hashtable h) {
        if (h == null) {
            logMsg("# ERROR: writeEnvironmentReport called with null Hashtable");
            return false;
        }
        boolean errors = false;
        logMsg("#---- BEGIN writeEnvironmentReport($Revision: 468646 $): Useful stuff found: ----");
        Enumeration keys = h.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            String keyStr = (String) key;
            try {
                if (keyStr.startsWith(FOUNDCLASSES)) {
                    Vector v = (Vector) h.get(keyStr);
                    errors |= logFoundJars(v, keyStr);
                } else {
                    if (keyStr.startsWith(ERROR)) {
                        errors = true;
                    }
                    logMsg(keyStr + "=" + h.get(keyStr));
                }
            } catch (Exception e) {
                logMsg("Reading-" + key + "= threw: " + e.toString());
            }
        }
        logMsg("#----- END writeEnvironmentReport: Useful properties found: -----");
        return errors;
    }

    protected boolean logFoundJars(Vector v, String desc) {
        if (v == null || v.size() < 1) {
            return false;
        }
        boolean errors = false;
        logMsg("#---- BEGIN Listing XML-related jars in: " + desc + " ----");
        for (int i = 0; i < v.size(); i++) {
            Hashtable subhash = (Hashtable) v.elementAt(i);
            Enumeration keys = subhash.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                String keyStr = (String) key;
                try {
                    if (keyStr.startsWith(ERROR)) {
                        errors = true;
                    }
                    logMsg(keyStr + "=" + subhash.get(keyStr));
                } catch (Exception e) {
                    errors = true;
                    logMsg("Reading-" + key + "= threw: " + e.toString());
                }
            }
        }
        logMsg("#----- END Listing XML-related jars in: " + desc + " -----");
        return errors;
    }

    public void appendEnvironmentReport(Node node, Document factory, Hashtable h) {
        if (node == null || factory == null) {
            return;
        }
        try {
            Element envCheckNode = factory.createElement("EnvironmentCheck");
            envCheckNode.setAttribute("version", "$Revision: 468646 $");
            node.appendChild(envCheckNode);
            if (h == null) {
                Element statusNode = factory.createElement("status");
                statusNode.setAttribute(Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, "ERROR");
                statusNode.appendChild(factory.createTextNode("appendEnvironmentReport called with null Hashtable!"));
                envCheckNode.appendChild(statusNode);
                return;
            }
            boolean errors = false;
            Element hashNode = factory.createElement("environment");
            envCheckNode.appendChild(hashNode);
            Enumeration keys = h.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                String keyStr = (String) key;
                try {
                    if (keyStr.startsWith(FOUNDCLASSES)) {
                        Vector v = (Vector) h.get(keyStr);
                        errors |= appendFoundJars(hashNode, factory, v, keyStr);
                    } else {
                        if (keyStr.startsWith(ERROR)) {
                            errors = true;
                        }
                        Element node2 = factory.createElement("item");
                        node2.setAttribute("key", keyStr);
                        node2.appendChild(factory.createTextNode((String) h.get(keyStr)));
                        hashNode.appendChild(node2);
                    }
                } catch (Exception e) {
                    errors = true;
                    Element node3 = factory.createElement("item");
                    node3.setAttribute("key", keyStr);
                    node3.appendChild(factory.createTextNode("ERROR. Reading " + key + " threw: " + e.toString()));
                    hashNode.appendChild(node3);
                }
            }
            Element statusNode2 = factory.createElement("status");
            statusNode2.setAttribute(Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING, errors ? "ERROR" : "OK");
            envCheckNode.appendChild(statusNode2);
        } catch (Exception e2) {
            System.err.println("appendEnvironmentReport threw: " + e2.toString());
            e2.printStackTrace();
        }
    }

    protected boolean appendFoundJars(Node container, Document factory, Vector v, String desc) {
        if (v == null || v.size() < 1) {
            return false;
        }
        boolean errors = false;
        for (int i = 0; i < v.size(); i++) {
            Hashtable subhash = (Hashtable) v.elementAt(i);
            Enumeration keys = subhash.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                try {
                    String keyStr = (String) key;
                    if (keyStr.startsWith(ERROR)) {
                        errors = true;
                    }
                    Element node = factory.createElement("foundJar");
                    node.setAttribute("name", keyStr.substring(0, keyStr.indexOf("-")));
                    node.setAttribute("desc", keyStr.substring(keyStr.indexOf("-") + 1));
                    node.appendChild(factory.createTextNode((String) subhash.get(keyStr)));
                    container.appendChild(node);
                } catch (Exception e) {
                    errors = true;
                    Element node2 = factory.createElement("foundJar");
                    node2.appendChild(factory.createTextNode("ERROR. Reading " + key + " threw: " + e.toString()));
                    container.appendChild(node2);
                }
            }
        }
        return errors;
    }

    protected void checkSystemProperties(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        try {
            String javaVersion = System.getProperty("java.version");
            h.put("java.version", javaVersion);
        } catch (SecurityException e) {
            h.put("java.version", "WARNING: SecurityException thrown accessing system version properties");
        }
        try {
            String cp = System.getProperty("java.class.path");
            h.put("java.class.path", cp);
            Vector classpathJars = checkPathForJars(cp, this.jarNames);
            if (classpathJars != null) {
                h.put("foundclasses.java.class.path", classpathJars);
            }
            String othercp = System.getProperty("sun.boot.class.path");
            if (othercp != null) {
                h.put("sun.boot.class.path", othercp);
                Vector classpathJars2 = checkPathForJars(othercp, this.jarNames);
                if (classpathJars2 != null) {
                    h.put("foundclasses.sun.boot.class.path", classpathJars2);
                }
            }
            String othercp2 = System.getProperty("java.ext.dirs");
            if (othercp2 == null) {
                return;
            }
            h.put("java.ext.dirs", othercp2);
            Vector classpathJars3 = checkPathForJars(othercp2, this.jarNames);
            if (classpathJars3 == null) {
                return;
            }
            h.put("foundclasses.java.ext.dirs", classpathJars3);
        } catch (SecurityException e2) {
            h.put("java.class.path", "WARNING: SecurityException thrown accessing system classpath properties");
        }
    }

    protected Vector checkPathForJars(String cp, String[] jars) {
        if (cp == null || jars == null || cp.length() == 0 || jars.length == 0) {
            return null;
        }
        Vector v = new Vector();
        StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String filename = st.nextToken();
            for (int i = 0; i < jars.length; i++) {
                if (filename.indexOf(jars[i]) > -1) {
                    File f = new File(filename);
                    if (f.exists()) {
                        try {
                            Hashtable h = new Hashtable(2);
                            h.put(jars[i] + "-path", f.getAbsolutePath());
                            if (!"xalan.jar".equalsIgnoreCase(jars[i])) {
                                h.put(jars[i] + "-apparent.version", getApparentVersion(jars[i], f.length()));
                            }
                            v.addElement(h);
                        } catch (Exception e) {
                        }
                    } else {
                        Hashtable h2 = new Hashtable(2);
                        h2.put(jars[i] + "-path", "WARNING. Classpath entry: " + filename + " does not exist");
                        h2.put(jars[i] + "-apparent.version", CLASS_NOTPRESENT);
                        v.addElement(h2);
                    }
                }
            }
        }
        return v;
    }

    protected String getApparentVersion(String jarName, long jarSize) {
        String foundSize = (String) jarVersions.get(new Long(jarSize));
        if (foundSize != null && foundSize.startsWith(jarName)) {
            return foundSize;
        }
        if ("xerces.jar".equalsIgnoreCase(jarName) || "xercesImpl.jar".equalsIgnoreCase(jarName)) {
            return jarName + " " + WARNING + CLASS_PRESENT;
        }
        return jarName + " " + CLASS_PRESENT;
    }

    protected void checkJAXPVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        Class<?>[] clsArr = new Class[0];
        Class clazz = null;
        try {
            clazz = ObjectFactory.findProviderClass("javax.xml.parsers.DocumentBuilder", ObjectFactory.findClassLoader(), true);
            clazz.getMethod("getDOMImplementation", clsArr);
            h.put("version.JAXP", "1.1 or higher");
        } catch (Exception e) {
            if (clazz != null) {
                h.put("ERROR.version.JAXP", "1.0.1");
                h.put(ERROR, ERROR_FOUND);
            } else {
                h.put("ERROR.version.JAXP", CLASS_NOTPRESENT);
                h.put(ERROR, ERROR_FOUND);
            }
        }
    }

    protected void checkProcessorVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        try {
            Class clazz = ObjectFactory.findProviderClass("org.apache.xalan.xslt.XSLProcessorVersion", ObjectFactory.findClassLoader(), true);
            StringBuffer buf = new StringBuffer();
            Field f = clazz.getField("PRODUCT");
            buf.append(f.get(null));
            buf.append(';');
            Field f2 = clazz.getField("LANGUAGE");
            buf.append(f2.get(null));
            buf.append(';');
            Field f3 = clazz.getField("S_VERSION");
            buf.append(f3.get(null));
            buf.append(';');
            h.put("version.xalan1", buf.toString());
        } catch (Exception e) {
            h.put("version.xalan1", CLASS_NOTPRESENT);
        }
        try {
            Class clazz2 = ObjectFactory.findProviderClass("org.apache.xalan.processor.XSLProcessorVersion", ObjectFactory.findClassLoader(), true);
            StringBuffer buf2 = new StringBuffer();
            Field f4 = clazz2.getField("S_VERSION");
            buf2.append(f4.get(null));
            h.put("version.xalan2x", buf2.toString());
        } catch (Exception e2) {
            h.put("version.xalan2x", CLASS_NOTPRESENT);
        }
        try {
            Class clazz3 = ObjectFactory.findProviderClass("org.apache.xalan.Version", ObjectFactory.findClassLoader(), true);
            Method method = clazz3.getMethod("getVersion", new Class[0]);
            Object returnValue = method.invoke(null, new Object[0]);
            h.put("version.xalan2_2", (String) returnValue);
        } catch (Exception e3) {
            h.put("version.xalan2_2", CLASS_NOTPRESENT);
        }
    }

    protected void checkParserVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        try {
            Class clazz = ObjectFactory.findProviderClass("org.apache.xerces.framework.Version", ObjectFactory.findClassLoader(), true);
            Field f = clazz.getField("fVersion");
            String parserVersion = (String) f.get(null);
            h.put("version.xerces1", parserVersion);
        } catch (Exception e) {
            h.put("version.xerces1", CLASS_NOTPRESENT);
        }
        try {
            Class clazz2 = ObjectFactory.findProviderClass("org.apache.xerces.impl.Version", ObjectFactory.findClassLoader(), true);
            Field f2 = clazz2.getField("fVersion");
            String parserVersion2 = (String) f2.get(null);
            h.put("version.xerces2", parserVersion2);
        } catch (Exception e2) {
            h.put("version.xerces2", CLASS_NOTPRESENT);
        }
        try {
            ObjectFactory.findProviderClass("org.apache.crimson.parser.Parser2", ObjectFactory.findClassLoader(), true);
            h.put("version.crimson", CLASS_PRESENT);
        } catch (Exception e3) {
            h.put("version.crimson", CLASS_NOTPRESENT);
        }
    }

    protected void checkAntVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        try {
            Class clazz = ObjectFactory.findProviderClass("org.apache.tools.ant.Main", ObjectFactory.findClassLoader(), true);
            Method method = clazz.getMethod("getAntVersion", new Class[0]);
            Object returnValue = method.invoke(null, new Object[0]);
            h.put("version.ant", (String) returnValue);
        } catch (Exception e) {
            h.put("version.ant", CLASS_NOTPRESENT);
        }
    }

    protected void checkDOMVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        Class<?>[] clsArr = {String.class, String.class};
        try {
            Class clazz = ObjectFactory.findProviderClass("org.w3c.dom.Document", ObjectFactory.findClassLoader(), true);
            clazz.getMethod("createElementNS", clsArr);
            h.put("version.DOM", "2.0");
            try {
                Class clazz2 = ObjectFactory.findProviderClass("org.w3c.dom.Node", ObjectFactory.findClassLoader(), true);
                clazz2.getMethod("supported", clsArr);
                h.put("ERROR.version.DOM.draftlevel", "2.0wd");
                h.put(ERROR, ERROR_FOUND);
            } catch (Exception e) {
                try {
                    Class clazz3 = ObjectFactory.findProviderClass("org.w3c.dom.Node", ObjectFactory.findClassLoader(), true);
                    clazz3.getMethod("isSupported", clsArr);
                    h.put("version.DOM.draftlevel", "2.0fd");
                } catch (Exception e2) {
                    h.put("ERROR.version.DOM.draftlevel", "2.0unknown");
                    h.put(ERROR, ERROR_FOUND);
                }
            }
        } catch (Exception e3) {
            h.put("ERROR.version.DOM", "ERROR attempting to load DOM level 2 class: " + e3.toString());
            h.put(ERROR, ERROR_FOUND);
        }
    }

    protected void checkSAXVersion(Hashtable h) {
        if (h == null) {
            h = new Hashtable();
        }
        Class<?>[] clsArr = {String.class};
        Class<?>[] clsArr2 = {Attributes.class};
        try {
            Class clazz = ObjectFactory.findProviderClass("org.xml.sax.helpers.AttributesImpl", ObjectFactory.findClassLoader(), true);
            clazz.getMethod("setAttributes", clsArr2);
            h.put("version.SAX", "2.0");
        } catch (Exception e) {
            h.put("ERROR.version.SAX", "ERROR attempting to load SAX version 2 class: " + e.toString());
            h.put(ERROR, ERROR_FOUND);
            try {
                Class clazz2 = ObjectFactory.findProviderClass("org.xml.sax.XMLReader", ObjectFactory.findClassLoader(), true);
                clazz2.getMethod("parse", clsArr);
                h.put("version.SAX-backlevel", "2.0beta2-or-earlier");
            } catch (Exception e2) {
                h.put("ERROR.version.SAX", "ERROR attempting to load SAX version 2 class: " + e.toString());
                h.put(ERROR, ERROR_FOUND);
                try {
                    Class clazz3 = ObjectFactory.findProviderClass("org.xml.sax.Parser", ObjectFactory.findClassLoader(), true);
                    clazz3.getMethod("parse", clsArr);
                    h.put("version.SAX-backlevel", SerializerConstants.XMLVERSION10);
                } catch (Exception e3) {
                    h.put("ERROR.version.SAX-backlevel", "ERROR attempting to load SAX version 1 class: " + e3.toString());
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

    protected void logMsg(String s) {
        this.outWriter.println(s);
    }
}
