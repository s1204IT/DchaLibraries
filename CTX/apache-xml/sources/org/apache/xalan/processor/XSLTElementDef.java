package org.apache.xalan.processor;

import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.xml.utils.Constants;
import org.apache.xml.utils.QName;

public class XSLTElementDef {
    static final int T_ANY = 3;
    static final int T_ELEMENT = 1;
    static final int T_PCDATA = 2;
    private XSLTAttributeDef[] m_attributes;
    private Class m_classObject;
    private XSLTElementProcessor m_elementProcessor;
    private XSLTElementDef[] m_elements;
    private boolean m_has_required;
    boolean m_isOrdered;
    private int m_lastOrder;
    private boolean m_multiAllowed;
    private String m_name;
    private String m_nameAlias;
    private String m_namespace;
    private int m_order;
    private boolean m_required;
    Hashtable m_requiredFound;
    private int m_type;

    XSLTElementDef() {
        this.m_type = 1;
        this.m_has_required = false;
        this.m_required = false;
        this.m_isOrdered = false;
        this.m_order = -1;
        this.m_lastOrder = -1;
        this.m_multiAllowed = true;
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls) {
        this.m_type = 1;
        this.m_has_required = false;
        this.m_required = false;
        this.m_isOrdered = false;
        this.m_order = -1;
        this.m_lastOrder = -1;
        this.m_multiAllowed = true;
        build(str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls);
        if (str != null) {
            if (str.equals(Constants.S_XSLNAMESPACEURL) || str.equals("http://xml.apache.org/xalan") || str.equals(Constants.S_BUILTIN_OLD_EXTENSIONS_URL)) {
                xSLTSchema.addAvailableElement(new QName(str, str2));
                if (str3 != null) {
                    xSLTSchema.addAvailableElement(new QName(str, str3));
                }
            }
        }
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls, boolean z) {
        this.m_type = 1;
        this.m_has_required = false;
        this.m_required = false;
        this.m_isOrdered = false;
        this.m_order = -1;
        this.m_lastOrder = -1;
        this.m_multiAllowed = true;
        this.m_has_required = z;
        build(str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls);
        if (str != null) {
            if (str.equals(Constants.S_XSLNAMESPACEURL) || str.equals("http://xml.apache.org/xalan") || str.equals(Constants.S_BUILTIN_OLD_EXTENSIONS_URL)) {
                xSLTSchema.addAvailableElement(new QName(str, str2));
                if (str3 != null) {
                    xSLTSchema.addAvailableElement(new QName(str, str3));
                }
            }
        }
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls, boolean z, boolean z2) {
        this(xSLTSchema, str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls, z);
        this.m_required = z2;
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls, boolean z, boolean z2, int i, boolean z3) {
        this(xSLTSchema, str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls, z, z2);
        this.m_order = i;
        this.m_multiAllowed = z3;
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls, boolean z, boolean z2, boolean z3, int i, boolean z4) {
        this(xSLTSchema, str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls, z, z2);
        this.m_order = i;
        this.m_multiAllowed = z4;
        this.m_isOrdered = z3;
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls, boolean z, int i, boolean z2) {
        this(xSLTSchema, str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls, i, z2);
        this.m_isOrdered = z;
    }

    XSLTElementDef(XSLTSchema xSLTSchema, String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls, int i, boolean z) {
        this(xSLTSchema, str, str2, str3, xSLTElementDefArr, xSLTAttributeDefArr, xSLTElementProcessor, cls);
        this.m_order = i;
        this.m_multiAllowed = z;
    }

    XSLTElementDef(Class cls, XSLTElementProcessor xSLTElementProcessor, int i) {
        this.m_type = 1;
        this.m_has_required = false;
        this.m_required = false;
        this.m_isOrdered = false;
        this.m_order = -1;
        this.m_lastOrder = -1;
        this.m_multiAllowed = true;
        this.m_classObject = cls;
        this.m_type = i;
        setElementProcessor(xSLTElementProcessor);
    }

    void build(String str, String str2, String str3, XSLTElementDef[] xSLTElementDefArr, XSLTAttributeDef[] xSLTAttributeDefArr, XSLTElementProcessor xSLTElementProcessor, Class cls) {
        this.m_namespace = str;
        this.m_name = str2;
        this.m_nameAlias = str3;
        this.m_elements = xSLTElementDefArr;
        this.m_attributes = xSLTAttributeDefArr;
        setElementProcessor(xSLTElementProcessor);
        this.m_classObject = cls;
        if (hasRequired() && this.m_elements != null) {
            int length = this.m_elements.length;
            for (int i = 0; i < length; i++) {
                XSLTElementDef xSLTElementDef = this.m_elements[i];
                if (xSLTElementDef != null && xSLTElementDef.getRequired()) {
                    if (this.m_requiredFound == null) {
                        this.m_requiredFound = new Hashtable();
                    }
                    this.m_requiredFound.put(xSLTElementDef.getName(), "xsl:" + xSLTElementDef.getName());
                }
            }
        }
    }

    private static boolean equalsMayBeNull(Object obj, Object obj2) {
        return obj2 == obj || !(obj == null || obj2 == null || !obj2.equals(obj));
    }

    private static boolean equalsMayBeNullOrZeroLen(String str, String str2) {
        int length;
        int length2;
        if (str != null) {
            length = str.length();
        } else {
            length = 0;
        }
        if (str2 != null) {
            length2 = str2.length();
        } else {
            length2 = 0;
        }
        if (length != length2) {
            return false;
        }
        if (length == 0) {
            return true;
        }
        return str.equals(str2);
    }

    int getType() {
        return this.m_type;
    }

    void setType(int i) {
        this.m_type = i;
    }

    String getNamespace() {
        return this.m_namespace;
    }

    String getName() {
        return this.m_name;
    }

    String getNameAlias() {
        return this.m_nameAlias;
    }

    public XSLTElementDef[] getElements() {
        return this.m_elements;
    }

    void setElements(XSLTElementDef[] xSLTElementDefArr) {
        this.m_elements = xSLTElementDefArr;
    }

    private boolean QNameEquals(String str, String str2) {
        return equalsMayBeNullOrZeroLen(this.m_namespace, str) && (equalsMayBeNullOrZeroLen(this.m_name, str2) || equalsMayBeNullOrZeroLen(this.m_nameAlias, str2));
    }

    XSLTElementProcessor getProcessorFor(String str, String str2) {
        if (this.m_elements == null) {
            return null;
        }
        int length = this.m_elements.length;
        int i = 0;
        int order = -1;
        boolean multiAllowed = true;
        XSLTElementProcessor xSLTElementProcessor = null;
        while (true) {
            if (i >= length) {
                break;
            }
            XSLTElementDef xSLTElementDef = this.m_elements[i];
            if (xSLTElementDef.m_name.equals("*")) {
                if (!equalsMayBeNullOrZeroLen(str, Constants.S_XSLNAMESPACEURL)) {
                    xSLTElementProcessor = xSLTElementDef.m_elementProcessor;
                    order = xSLTElementDef.getOrder();
                    multiAllowed = xSLTElementDef.getMultiAllowed();
                }
            } else if (xSLTElementDef.QNameEquals(str, str2)) {
                if (xSLTElementDef.getRequired()) {
                    setRequiredFound(xSLTElementDef.getName(), true);
                }
                order = xSLTElementDef.getOrder();
                multiAllowed = xSLTElementDef.getMultiAllowed();
                xSLTElementProcessor = xSLTElementDef.m_elementProcessor;
            }
            i++;
        }
        if (xSLTElementProcessor != null && isOrdered()) {
            int lastOrder = getLastOrder();
            if (order > lastOrder) {
                setLastOrder(order);
            } else {
                if (order == lastOrder && !multiAllowed) {
                    return null;
                }
                if (order < lastOrder && order > 0) {
                    return null;
                }
            }
        }
        return xSLTElementProcessor;
    }

    XSLTElementProcessor getProcessorForUnknown(String str, String str2) {
        if (this.m_elements == null) {
            return null;
        }
        int length = this.m_elements.length;
        for (int i = 0; i < length; i++) {
            XSLTElementDef xSLTElementDef = this.m_elements[i];
            if (xSLTElementDef.m_name.equals("unknown") && str.length() > 0) {
                return xSLTElementDef.m_elementProcessor;
            }
        }
        return null;
    }

    XSLTAttributeDef[] getAttributes() {
        return this.m_attributes;
    }

    XSLTAttributeDef getAttributeDef(String str, String str2) {
        XSLTAttributeDef xSLTAttributeDef = null;
        for (XSLTAttributeDef xSLTAttributeDef2 : getAttributes()) {
            String namespace = xSLTAttributeDef2.getNamespace();
            String name = xSLTAttributeDef2.getName();
            if (name.equals("*") && (equalsMayBeNullOrZeroLen(str, namespace) || (namespace != null && namespace.equals("*") && str != null && str.length() > 0))) {
                return xSLTAttributeDef2;
            }
            if (name.equals("*") && namespace == null) {
                xSLTAttributeDef = xSLTAttributeDef2;
            } else if (equalsMayBeNullOrZeroLen(str, namespace) && str2.equals(name)) {
                return xSLTAttributeDef2;
            }
        }
        if (xSLTAttributeDef == null && str.length() > 0 && !equalsMayBeNullOrZeroLen(str, Constants.S_XSLNAMESPACEURL)) {
            return XSLTAttributeDef.m_foreignAttr;
        }
        return xSLTAttributeDef;
    }

    public XSLTElementProcessor getElementProcessor() {
        return this.m_elementProcessor;
    }

    public void setElementProcessor(XSLTElementProcessor xSLTElementProcessor) {
        if (xSLTElementProcessor != null) {
            this.m_elementProcessor = xSLTElementProcessor;
            this.m_elementProcessor.setElemDef(this);
        }
    }

    Class getClassObject() {
        return this.m_classObject;
    }

    boolean hasRequired() {
        return this.m_has_required;
    }

    boolean getRequired() {
        return this.m_required;
    }

    void setRequiredFound(String str, boolean z) {
        if (this.m_requiredFound.get(str) != null) {
            this.m_requiredFound.remove(str);
        }
    }

    boolean getRequiredFound() {
        if (this.m_requiredFound == null) {
            return true;
        }
        return this.m_requiredFound.isEmpty();
    }

    String getRequiredElem() {
        if (this.m_requiredFound == null) {
            return null;
        }
        Enumeration enumerationElements = this.m_requiredFound.elements();
        String str = "";
        boolean z = true;
        while (enumerationElements.hasMoreElements()) {
            if (z) {
                z = false;
            } else {
                str = str + ", ";
            }
            str = str + ((String) enumerationElements.nextElement());
        }
        return str;
    }

    boolean isOrdered() {
        return this.m_isOrdered;
    }

    int getOrder() {
        return this.m_order;
    }

    int getLastOrder() {
        return this.m_lastOrder;
    }

    void setLastOrder(int i) {
        this.m_lastOrder = i;
    }

    boolean getMultiAllowed() {
        return this.m_multiAllowed;
    }
}
