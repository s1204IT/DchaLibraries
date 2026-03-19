package mf.org.apache.xerces.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import mf.org.apache.xerces.xni.NamespaceContext;

public class NamespaceSupport implements NamespaceContext {
    protected int[] fContext;
    protected int fCurrentContext;
    protected String[] fNamespace;
    protected int fNamespaceSize;
    protected String[] fPrefixes;

    public NamespaceSupport() {
        this.fNamespace = new String[32];
        this.fContext = new int[8];
        this.fPrefixes = new String[16];
    }

    public NamespaceSupport(NamespaceContext context) {
        this.fNamespace = new String[32];
        this.fContext = new int[8];
        this.fPrefixes = new String[16];
        pushContext();
        Enumeration prefixes = context.getAllPrefixes();
        while (prefixes.hasMoreElements()) {
            String prefix = (String) prefixes.nextElement();
            String uri = context.getURI(prefix);
            declarePrefix(prefix, uri);
        }
    }

    @Override
    public void reset() {
        this.fNamespaceSize = 0;
        this.fCurrentContext = 0;
        this.fContext[this.fCurrentContext] = this.fNamespaceSize;
        String[] strArr = this.fNamespace;
        int i = this.fNamespaceSize;
        this.fNamespaceSize = i + 1;
        strArr[i] = XMLSymbols.PREFIX_XML;
        String[] strArr2 = this.fNamespace;
        int i2 = this.fNamespaceSize;
        this.fNamespaceSize = i2 + 1;
        strArr2[i2] = NamespaceContext.XML_URI;
        String[] strArr3 = this.fNamespace;
        int i3 = this.fNamespaceSize;
        this.fNamespaceSize = i3 + 1;
        strArr3[i3] = XMLSymbols.PREFIX_XMLNS;
        String[] strArr4 = this.fNamespace;
        int i4 = this.fNamespaceSize;
        this.fNamespaceSize = i4 + 1;
        strArr4[i4] = NamespaceContext.XMLNS_URI;
        this.fCurrentContext++;
    }

    @Override
    public void pushContext() {
        if (this.fCurrentContext + 1 == this.fContext.length) {
            int[] contextarray = new int[this.fContext.length * 2];
            System.arraycopy(this.fContext, 0, contextarray, 0, this.fContext.length);
            this.fContext = contextarray;
        }
        int[] contextarray2 = this.fContext;
        int i = this.fCurrentContext + 1;
        this.fCurrentContext = i;
        contextarray2[i] = this.fNamespaceSize;
    }

    @Override
    public void popContext() {
        int[] iArr = this.fContext;
        int i = this.fCurrentContext;
        this.fCurrentContext = i - 1;
        this.fNamespaceSize = iArr[i];
    }

    @Override
    public boolean declarePrefix(String prefix, String uri) {
        if (prefix == XMLSymbols.PREFIX_XML || prefix == XMLSymbols.PREFIX_XMLNS) {
            return false;
        }
        for (int i = this.fNamespaceSize; i > this.fContext[this.fCurrentContext]; i -= 2) {
            if (this.fNamespace[i - 2] == prefix) {
                this.fNamespace[i - 1] = uri;
                return true;
            }
        }
        int i2 = this.fNamespaceSize;
        if (i2 == this.fNamespace.length) {
            String[] namespacearray = new String[this.fNamespaceSize * 2];
            System.arraycopy(this.fNamespace, 0, namespacearray, 0, this.fNamespaceSize);
            this.fNamespace = namespacearray;
        }
        String[] namespacearray2 = this.fNamespace;
        int i3 = this.fNamespaceSize;
        this.fNamespaceSize = i3 + 1;
        namespacearray2[i3] = prefix;
        String[] strArr = this.fNamespace;
        int i4 = this.fNamespaceSize;
        this.fNamespaceSize = i4 + 1;
        strArr[i4] = uri;
        return true;
    }

    @Override
    public String getURI(String prefix) {
        for (int i = this.fNamespaceSize; i > 0; i -= 2) {
            if (this.fNamespace[i - 2] == prefix) {
                return this.fNamespace[i - 1];
            }
        }
        return null;
    }

    @Override
    public String getPrefix(String uri) {
        for (int i = this.fNamespaceSize; i > 0; i -= 2) {
            if (this.fNamespace[i - 1] == uri && getURI(this.fNamespace[i - 2]) == uri) {
                return this.fNamespace[i - 2];
            }
        }
        return null;
    }

    @Override
    public int getDeclaredPrefixCount() {
        return (this.fNamespaceSize - this.fContext[this.fCurrentContext]) / 2;
    }

    @Override
    public String getDeclaredPrefixAt(int index) {
        return this.fNamespace[this.fContext[this.fCurrentContext] + (index * 2)];
    }

    @Override
    public Enumeration getAllPrefixes() {
        int count = 0;
        if (this.fPrefixes.length < this.fNamespace.length / 2) {
            String[] prefixes = new String[this.fNamespaceSize];
            this.fPrefixes = prefixes;
        }
        boolean unique = true;
        for (int i = 2; i < this.fNamespaceSize - 2; i += 2) {
            String prefix = this.fNamespace[i + 2];
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
        return new Prefixes(this.fPrefixes, count);
    }

    public boolean containsPrefix(String prefix) {
        for (int i = this.fNamespaceSize; i > 0; i -= 2) {
            if (this.fNamespace[i - 2] == prefix) {
                return true;
            }
        }
        return false;
    }

    protected final class Prefixes implements Enumeration {
        private int counter = 0;
        private String[] prefixes;
        private int size;

        public Prefixes(String[] prefixes, int size) {
            this.size = 0;
            this.prefixes = prefixes;
            this.size = size;
        }

        @Override
        public boolean hasMoreElements() {
            return this.counter < this.size;
        }

        @Override
        public Object nextElement() {
            if (this.counter < this.size) {
                String[] strArr = NamespaceSupport.this.fPrefixes;
                int i = this.counter;
                this.counter = i + 1;
                return strArr[i];
            }
            throw new NoSuchElementException("Illegal access to Namespace prefixes enumeration.");
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < this.size; i++) {
                buf.append(this.prefixes[i]);
                buf.append(' ');
            }
            return buf.toString();
        }
    }
}
