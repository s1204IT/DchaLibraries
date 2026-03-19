package org.apache.xml.utils;

import java.util.EmptyStackException;
import java.util.Enumeration;
import org.xml.sax.helpers.NamespaceSupport;

public class NamespaceSupport2 extends NamespaceSupport {
    public static final String XMLNS = "http://www.w3.org/XML/1998/namespace";
    private Context2 currentContext;

    public NamespaceSupport2() {
        reset();
    }

    @Override
    public void reset() {
        this.currentContext = new Context2(null);
        this.currentContext.declarePrefix("xml", "http://www.w3.org/XML/1998/namespace");
    }

    @Override
    public void pushContext() {
        Context2 context2 = this.currentContext;
        this.currentContext = context2.getChild();
        if (this.currentContext == null) {
            this.currentContext = new Context2(context2);
        } else {
            this.currentContext.setParent(context2);
        }
    }

    @Override
    public void popContext() {
        Context2 parent = this.currentContext.getParent();
        if (parent == null) {
            throw new EmptyStackException();
        }
        this.currentContext = parent;
    }

    @Override
    public boolean declarePrefix(String str, String str2) {
        if (str.equals("xml") || str.equals("xmlns")) {
            return false;
        }
        this.currentContext.declarePrefix(str, str2);
        return true;
    }

    @Override
    public String[] processName(String str, String[] strArr, boolean z) {
        String[] strArrProcessName = this.currentContext.processName(str, z);
        if (strArrProcessName == null) {
            return null;
        }
        System.arraycopy(strArrProcessName, 0, strArr, 0, 3);
        return strArr;
    }

    @Override
    public String getURI(String str) {
        return this.currentContext.getURI(str);
    }

    @Override
    public Enumeration getPrefixes() {
        return this.currentContext.getPrefixes();
    }

    @Override
    public String getPrefix(String str) {
        return this.currentContext.getPrefix(str);
    }

    @Override
    public Enumeration getPrefixes(String str) {
        return new PrefixForUriEnumerator(this, str, getPrefixes());
    }

    @Override
    public Enumeration getDeclaredPrefixes() {
        return this.currentContext.getDeclaredPrefixes();
    }
}
