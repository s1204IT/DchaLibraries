package mf.org.apache.xml.serialize;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Locale;
import mf.org.apache.xerces.dom.DOMMessageFormatter;

public final class HTMLdtd {
    private static Hashtable _boolAttrs;
    private static Hashtable _byChar;
    private static Hashtable _byName;
    private static Hashtable _elemDefs = new Hashtable();

    public static boolean isEmptyTag(String tagName) {
        return isElement(tagName, 17);
    }

    public static boolean isPreserveSpace(String tagName) {
        return isElement(tagName, 4);
    }

    public static boolean isOnlyOpening(String tagName) {
        return isElement(tagName, 1);
    }

    public static boolean isURI(String tagName, String attrName) {
        return attrName.equalsIgnoreCase("href") || attrName.equalsIgnoreCase("src");
    }

    public static boolean isBoolean(String tagName, String attrName) {
        String[] attrNames = (String[]) _boolAttrs.get(tagName.toUpperCase(Locale.ENGLISH));
        if (attrNames == null) {
            return false;
        }
        for (String str : attrNames) {
            if (str.equalsIgnoreCase(attrName)) {
                return true;
            }
        }
        return false;
    }

    public static String fromChar(int value) {
        if (value > 65535) {
            return null;
        }
        initialize();
        String name = (String) _byChar.get(new Integer(value));
        return name;
    }

    private static void initialize() {
        InputStream is = null;
        if (_byName != null) {
            return;
        }
        try {
            try {
                _byName = new Hashtable();
                _byChar = new Hashtable();
                InputStream is2 = HTMLdtd.class.getResourceAsStream("HTMLEntities.res");
                if (is2 == null) {
                    throw new RuntimeException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "ResourceNotFound", new Object[]{"HTMLEntities.res"}));
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is2, "ASCII"));
                String line = reader.readLine();
                while (line != null) {
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        line = reader.readLine();
                    } else {
                        int index = line.indexOf(32);
                        if (index > 1) {
                            String name = line.substring(0, index);
                            int index2 = index + 1;
                            if (index2 < line.length()) {
                                String value = line.substring(index2);
                                int index3 = value.indexOf(32);
                                if (index3 > 0) {
                                    value = value.substring(0, index3);
                                }
                                int code = Integer.parseInt(value);
                                defineEntity(name, (char) code);
                            }
                        }
                        line = reader.readLine();
                    }
                }
                is2.close();
                if (is2 != null) {
                    try {
                        is2.close();
                    } catch (Exception e) {
                    }
                }
            } catch (Exception except) {
                throw new RuntimeException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "ResourceNotLoaded", new Object[]{"HTMLEntities.res", except.toString()}));
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    is.close();
                } catch (Exception e2) {
                }
            }
            throw th;
        }
    }

    private static void defineEntity(String name, char value) {
        if (_byName.get(name) == null) {
            _byName.put(name, new Integer(value));
            _byChar.put(new Integer(value), name);
        }
    }

    private static void defineElement(String name, int flags) {
        _elemDefs.put(name, new Integer(flags));
    }

    private static void defineBoolean(String tagName, String attrName) {
        defineBoolean(tagName, new String[]{attrName});
    }

    private static void defineBoolean(String tagName, String[] attrNames) {
        _boolAttrs.put(tagName, attrNames);
    }

    private static boolean isElement(String name, int flag) {
        Integer flags = (Integer) _elemDefs.get(name.toUpperCase(Locale.ENGLISH));
        return flags != null && (flags.intValue() & flag) == flag;
    }

    static {
        defineElement("ADDRESS", 64);
        defineElement("AREA", 17);
        defineElement("BASE", 49);
        defineElement("BASEFONT", 17);
        defineElement("BLOCKQUOTE", 64);
        defineElement("BODY", 8);
        defineElement("BR", 17);
        defineElement("COL", 17);
        defineElement("COLGROUP", 522);
        defineElement("DD", 137);
        defineElement("DIV", 64);
        defineElement("DL", 66);
        defineElement("DT", 137);
        defineElement("FIELDSET", 64);
        defineElement("FORM", 64);
        defineElement("FRAME", 25);
        defineElement("H1", 64);
        defineElement("H2", 64);
        defineElement("H3", 64);
        defineElement("H4", 64);
        defineElement("H5", 64);
        defineElement("H6", 64);
        defineElement("HEAD", 10);
        defineElement("HR", 81);
        defineElement("HTML", 10);
        defineElement("IMG", 17);
        defineElement("INPUT", 17);
        defineElement("ISINDEX", 49);
        defineElement("LI", 265);
        defineElement("LINK", 49);
        defineElement("MAP", 32);
        defineElement("META", 49);
        defineElement("OL", 66);
        defineElement("OPTGROUP", 2);
        defineElement("OPTION", 265);
        defineElement("P", 328);
        defineElement("PARAM", 17);
        defineElement("PRE", 68);
        defineElement("SCRIPT", 36);
        defineElement("NOSCRIPT", 36);
        defineElement("SELECT", 2);
        defineElement("STYLE", 36);
        defineElement("TABLE", 66);
        defineElement("TBODY", 522);
        defineElement("TD", 16392);
        defineElement("TEXTAREA", 4);
        defineElement("TFOOT", 522);
        defineElement("TH", 16392);
        defineElement("THEAD", 522);
        defineElement("TITLE", 32);
        defineElement("TR", 522);
        defineElement("UL", 66);
        _boolAttrs = new Hashtable();
        defineBoolean("AREA", "href");
        defineBoolean("BUTTON", "disabled");
        defineBoolean("DIR", "compact");
        defineBoolean("DL", "compact");
        defineBoolean("FRAME", "noresize");
        defineBoolean("HR", "noshade");
        defineBoolean("IMAGE", "ismap");
        defineBoolean("INPUT", new String[]{"defaultchecked", "checked", "readonly", "disabled"});
        defineBoolean("LINK", "link");
        defineBoolean("MENU", "compact");
        defineBoolean("OBJECT", "declare");
        defineBoolean("OL", "compact");
        defineBoolean("OPTGROUP", "disabled");
        defineBoolean("OPTION", new String[]{"default-selected", "selected", "disabled"});
        defineBoolean("SCRIPT", "defer");
        defineBoolean("SELECT", new String[]{"multiple", "disabled"});
        defineBoolean("STYLE", "disabled");
        defineBoolean("TD", "nowrap");
        defineBoolean("TH", "nowrap");
        defineBoolean("TEXTAREA", new String[]{"disabled", "readonly"});
        defineBoolean("UL", "compact");
        initialize();
    }
}
