package org.apache.xml.utils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

final class Context2 {
    private static final Enumeration EMPTY_ENUMERATION = new Vector().elements();
    Hashtable attributeNameTable;
    Hashtable elementNameTable;
    Hashtable prefixTable;
    Hashtable uriTable;
    String defaultNS = null;
    private Vector declarations = null;
    private boolean tablesDirty = false;
    private Context2 parent = null;
    private Context2 child = null;

    Context2(Context2 context2) {
        if (context2 == null) {
            this.prefixTable = new Hashtable();
            this.uriTable = new Hashtable();
            this.elementNameTable = null;
            this.attributeNameTable = null;
            return;
        }
        setParent(context2);
    }

    Context2 getChild() {
        return this.child;
    }

    Context2 getParent() {
        return this.parent;
    }

    void setParent(Context2 context2) {
        this.parent = context2;
        context2.child = this;
        this.declarations = null;
        this.prefixTable = context2.prefixTable;
        this.uriTable = context2.uriTable;
        this.elementNameTable = context2.elementNameTable;
        this.attributeNameTable = context2.attributeNameTable;
        this.defaultNS = context2.defaultNS;
        this.tablesDirty = false;
    }

    void declarePrefix(String str, String str2) {
        if (!this.tablesDirty) {
            copyTables();
        }
        if (this.declarations == null) {
            this.declarations = new Vector();
        }
        String strIntern = str.intern();
        String strIntern2 = str2.intern();
        if ("".equals(strIntern)) {
            if ("".equals(strIntern2)) {
                this.defaultNS = null;
            } else {
                this.defaultNS = strIntern2;
            }
        } else {
            this.prefixTable.put(strIntern, strIntern2);
            this.uriTable.put(strIntern2, strIntern);
        }
        this.declarations.addElement(strIntern);
    }

    String[] processName(String str, boolean z) {
        Hashtable hashtable;
        String str2;
        if (z) {
            if (this.elementNameTable == null) {
                this.elementNameTable = new Hashtable();
            }
            hashtable = this.elementNameTable;
        } else {
            if (this.attributeNameTable == null) {
                this.attributeNameTable = new Hashtable();
            }
            hashtable = this.attributeNameTable;
        }
        String[] strArr = (String[]) hashtable.get(str);
        if (strArr != null) {
            return strArr;
        }
        String[] strArr2 = new String[3];
        int iIndexOf = str.indexOf(58);
        if (iIndexOf == -1) {
            if (z || this.defaultNS == null) {
                strArr2[0] = "";
            } else {
                strArr2[0] = this.defaultNS;
            }
            strArr2[1] = str.intern();
            strArr2[2] = strArr2[1];
        } else {
            String strSubstring = str.substring(0, iIndexOf);
            String strSubstring2 = str.substring(iIndexOf + 1);
            if ("".equals(strSubstring)) {
                str2 = this.defaultNS;
            } else {
                str2 = (String) this.prefixTable.get(strSubstring);
            }
            if (str2 == null) {
                return null;
            }
            strArr2[0] = str2;
            strArr2[1] = strSubstring2.intern();
            strArr2[2] = str.intern();
        }
        hashtable.put(strArr2[2], strArr2);
        this.tablesDirty = true;
        return strArr2;
    }

    String getURI(String str) {
        if ("".equals(str)) {
            return this.defaultNS;
        }
        if (this.prefixTable == null) {
            return null;
        }
        return (String) this.prefixTable.get(str);
    }

    String getPrefix(String str) {
        if (this.uriTable == null) {
            return null;
        }
        return (String) this.uriTable.get(str);
    }

    Enumeration getDeclaredPrefixes() {
        if (this.declarations == null) {
            return EMPTY_ENUMERATION;
        }
        return this.declarations.elements();
    }

    Enumeration getPrefixes() {
        if (this.prefixTable == null) {
            return EMPTY_ENUMERATION;
        }
        return this.prefixTable.keys();
    }

    private void copyTables() {
        this.prefixTable = (Hashtable) this.prefixTable.clone();
        this.uriTable = (Hashtable) this.uriTable.clone();
        if (this.elementNameTable != null) {
            this.elementNameTable = new Hashtable();
        }
        if (this.attributeNameTable != null) {
            this.attributeNameTable = new Hashtable();
        }
        this.tablesDirty = true;
    }
}
