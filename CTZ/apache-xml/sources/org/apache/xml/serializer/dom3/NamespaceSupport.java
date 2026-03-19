package org.apache.xml.serializer.dom3;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.apache.xml.serializer.SerializerConstants;

public class NamespaceSupport {
    protected int fCurrentContext;
    protected int fNamespaceSize;
    static final String PREFIX_XML = "xml".intern();
    static final String PREFIX_XMLNS = "xmlns".intern();
    public static final String XML_URI = "http://www.w3.org/XML/1998/namespace".intern();
    public static final String XMLNS_URI = SerializerConstants.XMLNS_URI.intern();
    protected String[] fNamespace = new String[32];
    protected int[] fContext = new int[8];
    protected String[] fPrefixes = new String[16];

    public void reset() {
        this.fNamespaceSize = 0;
        this.fCurrentContext = 0;
        this.fContext[this.fCurrentContext] = this.fNamespaceSize;
        String[] strArr = this.fNamespace;
        int i = this.fNamespaceSize;
        this.fNamespaceSize = i + 1;
        strArr[i] = PREFIX_XML;
        String[] strArr2 = this.fNamespace;
        int i2 = this.fNamespaceSize;
        this.fNamespaceSize = i2 + 1;
        strArr2[i2] = XML_URI;
        String[] strArr3 = this.fNamespace;
        int i3 = this.fNamespaceSize;
        this.fNamespaceSize = i3 + 1;
        strArr3[i3] = PREFIX_XMLNS;
        String[] strArr4 = this.fNamespace;
        int i4 = this.fNamespaceSize;
        this.fNamespaceSize = i4 + 1;
        strArr4[i4] = XMLNS_URI;
        this.fCurrentContext++;
    }

    public void pushContext() {
        if (this.fCurrentContext + 1 == this.fContext.length) {
            int[] iArr = new int[this.fContext.length * 2];
            System.arraycopy(this.fContext, 0, iArr, 0, this.fContext.length);
            this.fContext = iArr;
        }
        int[] iArr2 = this.fContext;
        int i = this.fCurrentContext + 1;
        this.fCurrentContext = i;
        iArr2[i] = this.fNamespaceSize;
    }

    public void popContext() {
        int[] iArr = this.fContext;
        int i = this.fCurrentContext;
        this.fCurrentContext = i - 1;
        this.fNamespaceSize = iArr[i];
    }

    public boolean declarePrefix(String str, String str2) {
        if (str == PREFIX_XML || str == PREFIX_XMLNS) {
            return false;
        }
        for (int i = this.fNamespaceSize; i > this.fContext[this.fCurrentContext]; i -= 2) {
            if (this.fNamespace[i - 2].equals(str)) {
                this.fNamespace[i - 1] = str2;
                return true;
            }
        }
        if (this.fNamespaceSize == this.fNamespace.length) {
            String[] strArr = new String[this.fNamespaceSize * 2];
            System.arraycopy(this.fNamespace, 0, strArr, 0, this.fNamespaceSize);
            this.fNamespace = strArr;
        }
        String[] strArr2 = this.fNamespace;
        int i2 = this.fNamespaceSize;
        this.fNamespaceSize = i2 + 1;
        strArr2[i2] = str;
        String[] strArr3 = this.fNamespace;
        int i3 = this.fNamespaceSize;
        this.fNamespaceSize = i3 + 1;
        strArr3[i3] = str2;
        return true;
    }

    public String getURI(String str) {
        for (int i = this.fNamespaceSize; i > 0; i -= 2) {
            if (this.fNamespace[i - 2].equals(str)) {
                return this.fNamespace[i - 1];
            }
        }
        return null;
    }

    public String getPrefix(String str) {
        for (int i = this.fNamespaceSize; i > 0; i -= 2) {
            if (this.fNamespace[i - 1].equals(str)) {
                int i2 = i - 2;
                if (getURI(this.fNamespace[i2]).equals(str)) {
                    return this.fNamespace[i2];
                }
            }
        }
        return null;
    }

    public int getDeclaredPrefixCount() {
        return (this.fNamespaceSize - this.fContext[this.fCurrentContext]) / 2;
    }

    public String getDeclaredPrefixAt(int i) {
        return this.fNamespace[this.fContext[this.fCurrentContext] + (i * 2)];
    }

    public Enumeration getAllPrefixes() {
        boolean z;
        if (this.fPrefixes.length < this.fNamespace.length / 2) {
            this.fPrefixes = new String[this.fNamespaceSize];
        }
        int i = 0;
        int i2 = 2;
        while (i2 < this.fNamespaceSize - 2) {
            i2 += 2;
            String str = this.fNamespace[i2];
            int i3 = 0;
            while (true) {
                if (i3 < i) {
                    if (this.fPrefixes[i3] != str) {
                        i3++;
                    } else {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                this.fPrefixes[i] = str;
                i++;
            }
        }
        return new Prefixes(this.fPrefixes, i);
    }

    protected final class Prefixes implements Enumeration {
        private int counter = 0;
        private String[] prefixes;
        private int size;

        public Prefixes(String[] strArr, int i) {
            this.size = 0;
            this.prefixes = strArr;
            this.size = i;
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
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < this.size; i++) {
                stringBuffer.append(this.prefixes[i]);
                stringBuffer.append(" ");
            }
            return stringBuffer.toString();
        }
    }
}
