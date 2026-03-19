package mf.org.apache.xerces.xinclude;

import java.util.Enumeration;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;

public class MultipleScopeNamespaceSupport extends NamespaceSupport {
    protected int fCurrentScope;
    protected int[] fScope;

    public MultipleScopeNamespaceSupport() {
        this.fScope = new int[8];
        this.fCurrentScope = 0;
        this.fScope[0] = 0;
    }

    public MultipleScopeNamespaceSupport(NamespaceContext context) {
        super(context);
        this.fScope = new int[8];
        this.fCurrentScope = 0;
        this.fScope[0] = 0;
    }

    @Override
    public Enumeration getAllPrefixes() {
        int count = 0;
        if (this.fPrefixes.length < this.fNamespace.length / 2) {
            String[] prefixes = new String[this.fNamespaceSize];
            this.fPrefixes = prefixes;
        }
        boolean unique = true;
        for (int i = this.fContext[this.fScope[this.fCurrentScope]]; i <= this.fNamespaceSize - 2; i += 2) {
            String prefix = this.fNamespace[i];
            int k = 0;
            while (true) {
                if (k >= count) {
                    break;
                }
                if (this.fPrefixes[k] != prefix) {
                    k++;
                } else {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                this.fPrefixes[count] = prefix;
                count++;
            }
            unique = true;
        }
        return new NamespaceSupport.Prefixes(this.fPrefixes, count);
    }

    public int getScopeForContext(int context) {
        int scope = this.fCurrentScope;
        while (context < this.fScope[scope]) {
            scope--;
        }
        return scope;
    }

    @Override
    public String getPrefix(String uri) {
        return getPrefix(uri, this.fNamespaceSize, this.fContext[this.fScope[this.fCurrentScope]]);
    }

    @Override
    public String getURI(String prefix) {
        return getURI(prefix, this.fNamespaceSize, this.fContext[this.fScope[this.fCurrentScope]]);
    }

    public String getPrefix(String uri, int context) {
        return getPrefix(uri, this.fContext[context + 1], this.fContext[this.fScope[getScopeForContext(context)]]);
    }

    public String getURI(String prefix, int context) {
        return getURI(prefix, this.fContext[context + 1], this.fContext[this.fScope[getScopeForContext(context)]]);
    }

    public String getPrefix(String uri, int start, int end) {
        if (uri == NamespaceContext.XML_URI) {
            return XMLSymbols.PREFIX_XML;
        }
        if (uri == NamespaceContext.XMLNS_URI) {
            return XMLSymbols.PREFIX_XMLNS;
        }
        for (int i = start; i > end; i -= 2) {
            if (this.fNamespace[i - 1] == uri && getURI(this.fNamespace[i - 2]) == uri) {
                return this.fNamespace[i - 2];
            }
        }
        return null;
    }

    public String getURI(String prefix, int start, int end) {
        if (prefix == XMLSymbols.PREFIX_XML) {
            return NamespaceContext.XML_URI;
        }
        if (prefix == XMLSymbols.PREFIX_XMLNS) {
            return NamespaceContext.XMLNS_URI;
        }
        for (int i = start; i > end; i -= 2) {
            if (this.fNamespace[i - 2] == prefix) {
                return this.fNamespace[i - 1];
            }
        }
        return null;
    }

    @Override
    public void reset() {
        this.fCurrentContext = this.fScope[this.fCurrentScope];
        this.fNamespaceSize = this.fContext[this.fCurrentContext];
    }

    public void pushScope() {
        if (this.fCurrentScope + 1 == this.fScope.length) {
            int[] contextarray = new int[this.fScope.length * 2];
            System.arraycopy(this.fScope, 0, contextarray, 0, this.fScope.length);
            this.fScope = contextarray;
        }
        pushContext();
        int[] iArr = this.fScope;
        int i = this.fCurrentScope + 1;
        this.fCurrentScope = i;
        iArr[i] = this.fCurrentContext;
    }

    public void popScope() {
        int[] iArr = this.fScope;
        int i = this.fCurrentScope;
        this.fCurrentScope = i - 1;
        this.fCurrentContext = iArr[i];
        popContext();
    }
}
