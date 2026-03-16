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

    Context2(Context2 parent) {
        if (parent == null) {
            this.prefixTable = new Hashtable();
            this.uriTable = new Hashtable();
            this.elementNameTable = null;
            this.attributeNameTable = null;
            return;
        }
        setParent(parent);
    }

    Context2 getChild() {
        return this.child;
    }

    Context2 getParent() {
        return this.parent;
    }

    void setParent(Context2 parent) {
        this.parent = parent;
        parent.child = this;
        this.declarations = null;
        this.prefixTable = parent.prefixTable;
        this.uriTable = parent.uriTable;
        this.elementNameTable = parent.elementNameTable;
        this.attributeNameTable = parent.attributeNameTable;
        this.defaultNS = parent.defaultNS;
        this.tablesDirty = false;
    }

    void declarePrefix(String prefix, String uri) {
        if (!this.tablesDirty) {
            copyTables();
        }
        if (this.declarations == null) {
            this.declarations = new Vector();
        }
        String prefix2 = prefix.intern();
        String uri2 = uri.intern();
        if ("".equals(prefix2)) {
            if ("".equals(uri2)) {
                this.defaultNS = null;
            } else {
                this.defaultNS = uri2;
            }
        } else {
            this.prefixTable.put(prefix2, uri2);
            this.uriTable.put(uri2, prefix2);
        }
        this.declarations.addElement(prefix2);
    }

    String[] processName(String qName, boolean isAttribute) {
        Hashtable table;
        String uri;
        if (isAttribute) {
            if (this.elementNameTable == null) {
                this.elementNameTable = new Hashtable();
            }
            table = this.elementNameTable;
        } else {
            if (this.attributeNameTable == null) {
                this.attributeNameTable = new Hashtable();
            }
            table = this.attributeNameTable;
        }
        String[] name = (String[]) table.get(qName);
        if (name != null) {
            return name;
        }
        String[] name2 = new String[3];
        int index = qName.indexOf(58);
        if (index == -1) {
            if (isAttribute || this.defaultNS == null) {
                name2[0] = "";
            } else {
                name2[0] = this.defaultNS;
            }
            name2[1] = qName.intern();
            name2[2] = name2[1];
        } else {
            String prefix = qName.substring(0, index);
            String local = qName.substring(index + 1);
            if ("".equals(prefix)) {
                uri = this.defaultNS;
            } else {
                uri = (String) this.prefixTable.get(prefix);
            }
            if (uri == null) {
                return null;
            }
            name2[0] = uri;
            name2[1] = local.intern();
            name2[2] = qName.intern();
        }
        table.put(name2[2], name2);
        this.tablesDirty = true;
        return name2;
    }

    String getURI(String prefix) {
        if ("".equals(prefix)) {
            return this.defaultNS;
        }
        if (this.prefixTable == null) {
            return null;
        }
        return (String) this.prefixTable.get(prefix);
    }

    String getPrefix(String uri) {
        if (this.uriTable == null) {
            return null;
        }
        return (String) this.uriTable.get(uri);
    }

    Enumeration getDeclaredPrefixes() {
        return this.declarations == null ? EMPTY_ENUMERATION : this.declarations.elements();
    }

    Enumeration getPrefixes() {
        return this.prefixTable == null ? EMPTY_ENUMERATION : this.prefixTable.keys();
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
