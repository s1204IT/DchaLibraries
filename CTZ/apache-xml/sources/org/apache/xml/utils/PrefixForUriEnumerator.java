package org.apache.xml.utils;

import java.util.Enumeration;
import java.util.NoSuchElementException;

class PrefixForUriEnumerator implements Enumeration {
    private Enumeration allPrefixes;
    private String lookahead = null;
    private NamespaceSupport2 nsup;
    private String uri;

    PrefixForUriEnumerator(NamespaceSupport2 namespaceSupport2, String str, Enumeration enumeration) {
        this.nsup = namespaceSupport2;
        this.uri = str;
        this.allPrefixes = enumeration;
    }

    @Override
    public boolean hasMoreElements() {
        if (this.lookahead != null) {
            return true;
        }
        while (this.allPrefixes.hasMoreElements()) {
            String str = (String) this.allPrefixes.nextElement();
            if (this.uri.equals(this.nsup.getURI(str))) {
                this.lookahead = str;
                return true;
            }
        }
        return false;
    }

    @Override
    public Object nextElement() {
        if (hasMoreElements()) {
            String str = this.lookahead;
            this.lookahead = null;
            return str;
        }
        throw new NoSuchElementException();
    }
}
