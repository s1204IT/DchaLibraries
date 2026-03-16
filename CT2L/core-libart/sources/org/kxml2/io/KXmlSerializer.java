package org.kxml2.io;

import dalvik.bytecode.Opcodes;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;
import javax.xml.XMLConstants;
import org.xmlpull.v1.XmlSerializer;

public class KXmlSerializer implements XmlSerializer {
    private static final int WRITE_BUFFER_SIZE = 500;
    private int auto;
    private int depth;
    private String encoding;
    private boolean pending;
    private boolean unicode;
    private BufferedWriter writer;
    private String[] elementStack = new String[12];
    private int[] nspCounts = new int[4];
    private String[] nspStack = new String[8];
    private boolean[] indent = new boolean[4];

    private final void check(boolean close) throws IOException {
        if (this.pending) {
            this.depth++;
            this.pending = false;
            if (this.indent.length <= this.depth) {
                boolean[] hlp = new boolean[this.depth + 4];
                System.arraycopy(this.indent, 0, hlp, 0, this.depth);
                this.indent = hlp;
            }
            this.indent[this.depth] = this.indent[this.depth - 1];
            for (int i = this.nspCounts[this.depth - 1]; i < this.nspCounts[this.depth]; i++) {
                this.writer.write(32);
                this.writer.write(XMLConstants.XMLNS_ATTRIBUTE);
                if (!this.nspStack[i * 2].isEmpty()) {
                    this.writer.write(58);
                    this.writer.write(this.nspStack[i * 2]);
                } else if (getNamespace().isEmpty() && !this.nspStack[(i * 2) + 1].isEmpty()) {
                    throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
                }
                this.writer.write("=\"");
                writeEscaped(this.nspStack[(i * 2) + 1], 34);
                this.writer.write(34);
            }
            if (this.nspCounts.length <= this.depth + 1) {
                int[] hlp2 = new int[this.depth + 8];
                System.arraycopy(this.nspCounts, 0, hlp2, 0, this.depth + 1);
                this.nspCounts = hlp2;
            }
            this.nspCounts[this.depth + 1] = this.nspCounts[this.depth];
            this.writer.write(close ? " />" : ">");
        }
    }

    private final void writeEscaped(String s, int quot) throws IOException {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                    if (quot == -1) {
                        this.writer.write(c);
                    } else {
                        this.writer.write("&#" + ((int) c) + ';');
                    }
                    break;
                case '&':
                    this.writer.write("&amp;");
                    break;
                case Opcodes.OP_IF_GTZ:
                    this.writer.write("&lt;");
                    break;
                case '>':
                    this.writer.write("&gt;");
                    break;
                default:
                    if (c == quot) {
                        this.writer.write(c == '\"' ? "&quot;" : "&apos;");
                    } else {
                        boolean allowedInXml = (c >= ' ' && c <= 55295) || (c >= 57344 && c <= 65533);
                        if (allowedInXml) {
                            if (this.unicode || c < 127) {
                                this.writer.write(c);
                            } else {
                                this.writer.write("&#" + ((int) c) + ";");
                            }
                        } else if (Character.isHighSurrogate(c) && i < s.length() - 1) {
                            writeSurrogate(c, s.charAt(i + 1));
                            i++;
                        } else {
                            reportInvalidCharacter(c);
                        }
                    }
                    break;
            }
            i++;
        }
    }

    private static void reportInvalidCharacter(char ch) {
        throw new IllegalArgumentException("Illegal character (U+" + Integer.toHexString(ch) + ")");
    }

    @Override
    public void docdecl(String dd) throws IOException {
        this.writer.write("<!DOCTYPE");
        this.writer.write(dd);
        this.writer.write(">");
    }

    @Override
    public void endDocument() throws IOException {
        while (this.depth > 0) {
            endTag(this.elementStack[(this.depth * 3) - 3], this.elementStack[(this.depth * 3) - 1]);
        }
        flush();
    }

    @Override
    public void entityRef(String name) throws IOException {
        check(false);
        this.writer.write(38);
        this.writer.write(name);
        this.writer.write(59);
    }

    @Override
    public boolean getFeature(String name) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output".equals(name)) {
            return this.indent[this.depth];
        }
        return false;
    }

    @Override
    public String getPrefix(String namespace, boolean create) {
        try {
            return getPrefix(namespace, false, create);
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private final String getPrefix(String namespace, boolean includeDefault, boolean create) throws IOException {
        String prefix;
        for (int i = (this.nspCounts[this.depth + 1] * 2) - 2; i >= 0; i -= 2) {
            if (this.nspStack[i + 1].equals(namespace) && (includeDefault || !this.nspStack[i].isEmpty())) {
                String cand = this.nspStack[i];
                int j = i + 2;
                while (true) {
                    if (j >= this.nspCounts[this.depth + 1] * 2) {
                        break;
                    }
                    if (!this.nspStack[j].equals(cand)) {
                        j++;
                    } else {
                        cand = null;
                        break;
                    }
                }
                if (cand != null) {
                    return cand;
                }
            }
        }
        if (!create) {
            return null;
        }
        if (namespace.isEmpty()) {
            prefix = "";
        } else {
            do {
                StringBuilder sbAppend = new StringBuilder().append("n");
                int i2 = this.auto;
                this.auto = i2 + 1;
                prefix = sbAppend.append(i2).toString();
                int i3 = (this.nspCounts[this.depth + 1] * 2) - 2;
                while (true) {
                    if (i3 < 0) {
                        break;
                    }
                    if (prefix.equals(this.nspStack[i3])) {
                        prefix = null;
                        break;
                    }
                    i3 -= 2;
                }
            } while (prefix == null);
        }
        boolean p = this.pending;
        this.pending = false;
        setPrefix(prefix, namespace);
        this.pending = p;
        return prefix;
    }

    @Override
    public Object getProperty(String name) {
        throw new RuntimeException("Unsupported property");
    }

    @Override
    public void ignorableWhitespace(String s) throws IOException {
        text(s);
    }

    @Override
    public void setFeature(String name, boolean value) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output".equals(name)) {
            this.indent[this.depth] = value;
            return;
        }
        throw new RuntimeException("Unsupported Feature");
    }

    @Override
    public void setProperty(String name, Object value) {
        throw new RuntimeException("Unsupported Property:" + value);
    }

    @Override
    public void setPrefix(String prefix, String namespace) throws IOException {
        check(false);
        if (prefix == null) {
            prefix = "";
        }
        if (namespace == null) {
            namespace = "";
        }
        String defined = getPrefix(namespace, true, false);
        if (!prefix.equals(defined)) {
            int[] iArr = this.nspCounts;
            int i = this.depth + 1;
            int i2 = iArr[i];
            iArr[i] = i2 + 1;
            int pos = i2 << 1;
            if (this.nspStack.length < pos + 1) {
                String[] hlp = new String[this.nspStack.length + 16];
                System.arraycopy(this.nspStack, 0, hlp, 0, pos);
                this.nspStack = hlp;
            }
            this.nspStack[pos] = prefix;
            this.nspStack[pos + 1] = namespace;
        }
    }

    @Override
    public void setOutput(Writer writer) {
        if (writer instanceof BufferedWriter) {
            this.writer = (BufferedWriter) writer;
        } else {
            this.writer = new BufferedWriter(writer, 500);
        }
        this.nspCounts[0] = 2;
        this.nspCounts[1] = 2;
        this.nspStack[0] = "";
        this.nspStack[1] = "";
        this.nspStack[2] = XMLConstants.XML_NS_PREFIX;
        this.nspStack[3] = "http://www.w3.org/XML/1998/namespace";
        this.pending = false;
        this.auto = 0;
        this.depth = 0;
        this.unicode = false;
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException {
        if (os == null) {
            throw new IllegalArgumentException("os == null");
        }
        setOutput(encoding == null ? new OutputStreamWriter(os) : new OutputStreamWriter(os, encoding));
        this.encoding = encoding;
        if (encoding != null && encoding.toLowerCase(Locale.US).startsWith("utf")) {
            this.unicode = true;
        }
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException {
        this.writer.write("<?xml version='1.0' ");
        if (encoding != null) {
            this.encoding = encoding;
            if (encoding.toLowerCase(Locale.US).startsWith("utf")) {
                this.unicode = true;
            }
        }
        if (this.encoding != null) {
            this.writer.write("encoding='");
            this.writer.write(this.encoding);
            this.writer.write("' ");
        }
        if (standalone != null) {
            this.writer.write("standalone='");
            this.writer.write(standalone.booleanValue() ? "yes" : "no");
            this.writer.write("' ");
        }
        this.writer.write("?>");
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException {
        check(false);
        if (this.indent[this.depth]) {
            this.writer.write("\r\n");
            for (int i = 0; i < this.depth; i++) {
                this.writer.write("  ");
            }
        }
        int esp = this.depth * 3;
        if (this.elementStack.length < esp + 3) {
            String[] hlp = new String[this.elementStack.length + 12];
            System.arraycopy(this.elementStack, 0, hlp, 0, esp);
            this.elementStack = hlp;
        }
        String prefix = namespace == null ? "" : getPrefix(namespace, true, true);
        if (namespace != null && namespace.isEmpty()) {
            for (int i2 = this.nspCounts[this.depth]; i2 < this.nspCounts[this.depth + 1]; i2++) {
                if (this.nspStack[i2 * 2].isEmpty() && !this.nspStack[(i2 * 2) + 1].isEmpty()) {
                    throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
                }
            }
        }
        int esp2 = esp + 1;
        this.elementStack[esp] = namespace;
        this.elementStack[esp2] = prefix;
        this.elementStack[esp2 + 1] = name;
        this.writer.write(60);
        if (!prefix.isEmpty()) {
            this.writer.write(prefix);
            this.writer.write(58);
        }
        this.writer.write(name);
        this.pending = true;
        return this;
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException {
        if (!this.pending) {
            throw new IllegalStateException("illegal position for attribute");
        }
        if (namespace == null) {
            namespace = "";
        }
        String prefix = namespace.isEmpty() ? "" : getPrefix(namespace, false, true);
        this.writer.write(32);
        if (!prefix.isEmpty()) {
            this.writer.write(prefix);
            this.writer.write(58);
        }
        this.writer.write(name);
        this.writer.write(61);
        char q = value.indexOf(34) != -1 ? '\'' : '\"';
        this.writer.write(q);
        writeEscaped(value, q);
        this.writer.write(q);
        return this;
    }

    @Override
    public void flush() throws IOException {
        check(false);
        this.writer.flush();
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException {
        if (!this.pending) {
            this.depth--;
        }
        if ((namespace == null && this.elementStack[this.depth * 3] != null) || ((namespace != null && !namespace.equals(this.elementStack[this.depth * 3])) || !this.elementStack[(this.depth * 3) + 2].equals(name))) {
            throw new IllegalArgumentException("</{" + namespace + "}" + name + "> does not match start");
        }
        if (this.pending) {
            check(true);
            this.depth--;
        } else {
            if (this.indent[this.depth + 1]) {
                this.writer.write("\r\n");
                for (int i = 0; i < this.depth; i++) {
                    this.writer.write("  ");
                }
            }
            this.writer.write("</");
            String prefix = this.elementStack[(this.depth * 3) + 1];
            if (!prefix.isEmpty()) {
                this.writer.write(prefix);
                this.writer.write(58);
            }
            this.writer.write(name);
            this.writer.write(62);
        }
        this.nspCounts[this.depth + 1] = this.nspCounts[this.depth];
        return this;
    }

    @Override
    public String getNamespace() {
        if (getDepth() == 0) {
            return null;
        }
        return this.elementStack[(getDepth() * 3) - 3];
    }

    @Override
    public String getName() {
        if (getDepth() == 0) {
            return null;
        }
        return this.elementStack[(getDepth() * 3) - 1];
    }

    @Override
    public int getDepth() {
        return this.pending ? this.depth + 1 : this.depth;
    }

    @Override
    public XmlSerializer text(String text) throws IOException {
        check(false);
        this.indent[this.depth] = false;
        writeEscaped(text, -1);
        return this;
    }

    @Override
    public XmlSerializer text(char[] text, int start, int len) throws IOException {
        text(new String(text, start, len));
        return this;
    }

    @Override
    public void cdsect(String data) throws IOException {
        check(false);
        String data2 = data.replace("]]>", "]]]]><![CDATA[>");
        this.writer.write("<![CDATA[");
        int i = 0;
        while (i < data2.length()) {
            char ch = data2.charAt(i);
            boolean allowedInCdata = (ch >= ' ' && ch <= 55295) || ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 57344 && ch <= 65533);
            if (allowedInCdata) {
                this.writer.write(ch);
            } else if (Character.isHighSurrogate(ch) && i < data2.length() - 1) {
                this.writer.write("]]>");
                i++;
                writeSurrogate(ch, data2.charAt(i));
                this.writer.write("<![CDATA[");
            } else {
                reportInvalidCharacter(ch);
            }
            i++;
        }
        this.writer.write("]]>");
    }

    private void writeSurrogate(char high, char low) throws IOException {
        if (!Character.isLowSurrogate(low)) {
            throw new IllegalArgumentException("Bad surrogate pair (U+" + Integer.toHexString(high) + " U+" + Integer.toHexString(low) + ")");
        }
        int codePoint = Character.toCodePoint(high, low);
        this.writer.write("&#" + codePoint + ";");
    }

    @Override
    public void comment(String comment) throws IOException {
        check(false);
        this.writer.write("<!--");
        this.writer.write(comment);
        this.writer.write("-->");
    }

    @Override
    public void processingInstruction(String pi) throws IOException {
        check(false);
        this.writer.write("<?");
        this.writer.write(pi);
        this.writer.write("?>");
    }
}
