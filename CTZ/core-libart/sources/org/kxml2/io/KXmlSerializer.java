package org.kxml2.io;

import android.icu.impl.PatternTokenizer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;
import javax.xml.XMLConstants;
import org.xmlpull.v1.XmlSerializer;

public class KXmlSerializer implements XmlSerializer {
    private static final int BUFFER_LEN = 8192;
    private int auto;
    private int depth;
    private String encoding;
    private int mPos;
    private boolean pending;
    private boolean unicode;
    private Writer writer;
    private final char[] mText = new char[8192];
    private String[] elementStack = new String[12];
    private int[] nspCounts = new int[4];
    private String[] nspStack = new String[8];
    private boolean[] indent = new boolean[4];

    private void append(char c) throws IOException {
        if (this.mPos >= 8192) {
            flushBuffer();
        }
        char[] cArr = this.mText;
        int i = this.mPos;
        this.mPos = i + 1;
        cArr[i] = c;
    }

    private void append(String str, int i, int i2) throws IOException {
        while (i2 > 0) {
            if (this.mPos == 8192) {
                flushBuffer();
            }
            int i3 = 8192 - this.mPos;
            if (i3 > i2) {
                i3 = i2;
            }
            int i4 = i + i3;
            str.getChars(i, i4, this.mText, this.mPos);
            i2 -= i3;
            this.mPos += i3;
            i = i4;
        }
    }

    private void append(String str) throws IOException {
        append(str, 0, str.length());
    }

    private final void flushBuffer() throws IOException {
        if (this.mPos > 0) {
            this.writer.write(this.mText, 0, this.mPos);
            this.writer.flush();
            this.mPos = 0;
        }
    }

    private final void check(boolean z) throws IOException {
        if (!this.pending) {
            return;
        }
        this.depth++;
        this.pending = false;
        if (this.indent.length <= this.depth) {
            boolean[] zArr = new boolean[this.depth + 4];
            System.arraycopy(this.indent, 0, zArr, 0, this.depth);
            this.indent = zArr;
        }
        this.indent[this.depth] = this.indent[this.depth - 1];
        for (int i = this.nspCounts[this.depth - 1]; i < this.nspCounts[this.depth]; i++) {
            append(" xmlns");
            int i2 = i * 2;
            if (!this.nspStack[i2].isEmpty()) {
                append(':');
                append(this.nspStack[i2]);
            } else if (getNamespace().isEmpty() && !this.nspStack[i2 + 1].isEmpty()) {
                throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
            }
            append("=\"");
            writeEscaped(this.nspStack[i2 + 1], 34);
            append('\"');
        }
        if (this.nspCounts.length <= this.depth + 1) {
            int[] iArr = new int[this.depth + 8];
            System.arraycopy(this.nspCounts, 0, iArr, 0, this.depth + 1);
            this.nspCounts = iArr;
        }
        this.nspCounts[this.depth + 1] = this.nspCounts[this.depth];
        if (z) {
            append(" />");
        } else {
            append('>');
        }
    }

    private final void writeEscaped(String str, int i) throws IOException {
        int i2 = 0;
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            if (cCharAt != '\r') {
                if (cCharAt != '&') {
                    if (cCharAt != '<') {
                        if (cCharAt == '>') {
                            append("&gt;");
                        } else {
                            switch (cCharAt) {
                                case '\t':
                                case '\n':
                                    break;
                                default:
                                    if (cCharAt == i) {
                                        append(cCharAt == '\"' ? "&quot;" : "&apos;");
                                    } else if ((cCharAt >= ' ' && cCharAt <= 55295) || (cCharAt >= 57344 && cCharAt <= 65533)) {
                                        if (this.unicode || cCharAt < 127) {
                                            append(cCharAt);
                                        } else {
                                            append("&#" + ((int) cCharAt) + ";");
                                        }
                                    } else if (Character.isHighSurrogate(cCharAt) && i2 < str.length() - 1) {
                                        i2++;
                                        writeSurrogate(cCharAt, str.charAt(i2));
                                    } else {
                                        reportInvalidCharacter(cCharAt);
                                    }
                                    break;
                            }
                        }
                    } else {
                        append("&lt;");
                    }
                } else {
                    append("&amp;");
                }
            } else if (i == -1) {
                append(cCharAt);
            } else {
                append("&#" + ((int) cCharAt) + ';');
            }
            i2++;
        }
    }

    private static void reportInvalidCharacter(char c) {
        throw new IllegalArgumentException("Illegal character (U+" + Integer.toHexString(c) + ")");
    }

    @Override
    public void docdecl(String str) throws IOException {
        append("<!DOCTYPE");
        append(str);
        append('>');
    }

    @Override
    public void endDocument() throws IOException {
        while (this.depth > 0) {
            endTag(this.elementStack[(this.depth * 3) - 3], this.elementStack[(this.depth * 3) - 1]);
        }
        flush();
    }

    @Override
    public void entityRef(String str) throws IOException {
        check(false);
        append('&');
        append(str);
        append(';');
    }

    @Override
    public boolean getFeature(String str) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output".equals(str)) {
            return this.indent[this.depth];
        }
        return false;
    }

    @Override
    public String getPrefix(String str, boolean z) {
        try {
            return getPrefix(str, false, z);
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private final String getPrefix(String str, boolean z, boolean z2) throws IOException {
        String string;
        int i = this.nspCounts[this.depth + 1] * 2;
        while (true) {
            i -= 2;
            String str2 = null;
            if (i >= 0) {
                if (this.nspStack[i + 1].equals(str) && (z || !this.nspStack[i].isEmpty())) {
                    String str3 = this.nspStack[i];
                    int i2 = i + 2;
                    while (true) {
                        if (i2 < this.nspCounts[this.depth + 1] * 2) {
                            if (this.nspStack[i2].equals(str3)) {
                                break;
                            }
                            i2++;
                        } else {
                            str2 = str3;
                            break;
                        }
                    }
                    if (str2 != null) {
                        return str2;
                    }
                }
            } else {
                if (!z2) {
                    return null;
                }
                if (str.isEmpty()) {
                    string = "";
                } else {
                    do {
                        StringBuilder sb = new StringBuilder();
                        sb.append("n");
                        int i3 = this.auto;
                        this.auto = i3 + 1;
                        sb.append(i3);
                        string = sb.toString();
                        int i4 = (this.nspCounts[this.depth + 1] * 2) - 2;
                        while (true) {
                            if (i4 < 0) {
                                break;
                            }
                            if (!string.equals(this.nspStack[i4])) {
                                i4 -= 2;
                            } else {
                                string = null;
                                break;
                            }
                        }
                    } while (string == null);
                }
                boolean z3 = this.pending;
                this.pending = false;
                setPrefix(string, str);
                this.pending = z3;
                return string;
            }
        }
    }

    @Override
    public Object getProperty(String str) {
        throw new RuntimeException("Unsupported property");
    }

    @Override
    public void ignorableWhitespace(String str) throws IOException {
        text(str);
    }

    @Override
    public void setFeature(String str, boolean z) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output".equals(str)) {
            this.indent[this.depth] = z;
            return;
        }
        throw new RuntimeException("Unsupported Feature");
    }

    @Override
    public void setProperty(String str, Object obj) {
        throw new RuntimeException("Unsupported Property:" + obj);
    }

    @Override
    public void setPrefix(String str, String str2) throws IOException {
        check(false);
        if (str == null) {
            str = "";
        }
        if (str2 == null) {
            str2 = "";
        }
        if (str.equals(getPrefix(str2, true, false))) {
            return;
        }
        int[] iArr = this.nspCounts;
        int i = this.depth + 1;
        int i2 = iArr[i];
        iArr[i] = i2 + 1;
        int i3 = i2 << 1;
        int i4 = i3 + 1;
        if (this.nspStack.length < i4) {
            String[] strArr = new String[this.nspStack.length + 16];
            System.arraycopy(this.nspStack, 0, strArr, 0, i3);
            this.nspStack = strArr;
        }
        this.nspStack[i3] = str;
        this.nspStack[i4] = str2;
    }

    @Override
    public void setOutput(Writer writer) {
        this.writer = writer;
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
    public void setOutput(OutputStream outputStream, String str) throws IOException {
        OutputStreamWriter outputStreamWriter;
        if (outputStream == null) {
            throw new IllegalArgumentException("os == null");
        }
        if (str == null) {
            outputStreamWriter = new OutputStreamWriter(outputStream);
        } else {
            outputStreamWriter = new OutputStreamWriter(outputStream, str);
        }
        setOutput(outputStreamWriter);
        this.encoding = str;
        if (str != null && str.toLowerCase(Locale.US).startsWith("utf")) {
            this.unicode = true;
        }
    }

    @Override
    public void startDocument(String str, Boolean bool) throws IOException {
        append("<?xml version='1.0' ");
        if (str != null) {
            this.encoding = str;
            if (str.toLowerCase(Locale.US).startsWith("utf")) {
                this.unicode = true;
            }
        }
        if (this.encoding != null) {
            append("encoding='");
            append(this.encoding);
            append("' ");
        }
        if (bool != null) {
            append("standalone='");
            append(bool.booleanValue() ? "yes" : "no");
            append("' ");
        }
        append("?>");
    }

    @Override
    public XmlSerializer startTag(String str, String str2) throws IOException {
        String prefix;
        check(false);
        if (this.indent[this.depth]) {
            append("\r\n");
            for (int i = 0; i < this.depth; i++) {
                append("  ");
            }
        }
        int i2 = this.depth * 3;
        if (this.elementStack.length < i2 + 3) {
            String[] strArr = new String[this.elementStack.length + 12];
            System.arraycopy(this.elementStack, 0, strArr, 0, i2);
            this.elementStack = strArr;
        }
        if (str == null) {
            prefix = "";
        } else {
            prefix = getPrefix(str, true, true);
        }
        if (str != null && str.isEmpty()) {
            for (int i3 = this.nspCounts[this.depth]; i3 < this.nspCounts[this.depth + 1]; i3++) {
                int i4 = i3 * 2;
                if (this.nspStack[i4].isEmpty() && !this.nspStack[i4 + 1].isEmpty()) {
                    throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
                }
            }
        }
        int i5 = i2 + 1;
        this.elementStack[i2] = str;
        this.elementStack[i5] = prefix;
        this.elementStack[i5 + 1] = str2;
        append('<');
        if (!prefix.isEmpty()) {
            append(prefix);
            append(':');
        }
        append(str2);
        this.pending = true;
        return this;
    }

    @Override
    public XmlSerializer attribute(String str, String str2, String str3) throws IOException {
        String prefix;
        if (!this.pending) {
            throw new IllegalStateException("illegal position for attribute");
        }
        if (str == null) {
            str = "";
        }
        if (str.isEmpty()) {
            prefix = "";
        } else {
            prefix = getPrefix(str, false, true);
        }
        append(' ');
        if (!prefix.isEmpty()) {
            append(prefix);
            append(':');
        }
        append(str2);
        append('=');
        char c = str3.indexOf(34) != -1 ? PatternTokenizer.SINGLE_QUOTE : '\"';
        append(c);
        writeEscaped(str3, c);
        append(c);
        return this;
    }

    @Override
    public void flush() throws IOException {
        check(false);
        flushBuffer();
    }

    @Override
    public XmlSerializer endTag(String str, String str2) throws IOException {
        if (!this.pending) {
            this.depth--;
        }
        if ((str == null && this.elementStack[this.depth * 3] != null) || ((str != null && !str.equals(this.elementStack[this.depth * 3])) || !this.elementStack[(this.depth * 3) + 2].equals(str2))) {
            throw new IllegalArgumentException("</{" + str + "}" + str2 + "> does not match start");
        }
        if (this.pending) {
            check(true);
            this.depth--;
        } else {
            if (this.indent[this.depth + 1]) {
                append("\r\n");
                for (int i = 0; i < this.depth; i++) {
                    append("  ");
                }
            }
            append("</");
            String str3 = this.elementStack[(this.depth * 3) + 1];
            if (!str3.isEmpty()) {
                append(str3);
                append(':');
            }
            append(str2);
            append('>');
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
    public XmlSerializer text(String str) throws IOException {
        check(false);
        this.indent[this.depth] = false;
        writeEscaped(str, -1);
        return this;
    }

    @Override
    public XmlSerializer text(char[] cArr, int i, int i2) throws IOException {
        text(new String(cArr, i, i2));
        return this;
    }

    @Override
    public void cdsect(String str) throws IOException {
        check(false);
        String strReplace = str.replace("]]>", "]]]]><![CDATA[>");
        append("<![CDATA[");
        int i = 0;
        while (i < strReplace.length()) {
            char cCharAt = strReplace.charAt(i);
            if ((cCharAt >= ' ' && cCharAt <= 55295) || cCharAt == '\t' || cCharAt == '\n' || cCharAt == '\r' || (cCharAt >= 57344 && cCharAt <= 65533)) {
                append(cCharAt);
            } else if (Character.isHighSurrogate(cCharAt) && i < strReplace.length() - 1) {
                append("]]>");
                i++;
                writeSurrogate(cCharAt, strReplace.charAt(i));
                append("<![CDATA[");
            } else {
                reportInvalidCharacter(cCharAt);
            }
            i++;
        }
        append("]]>");
    }

    private void writeSurrogate(char c, char c2) throws IOException {
        if (!Character.isLowSurrogate(c2)) {
            throw new IllegalArgumentException("Bad surrogate pair (U+" + Integer.toHexString(c) + " U+" + Integer.toHexString(c2) + ")");
        }
        append("&#" + Character.toCodePoint(c, c2) + ";");
    }

    @Override
    public void comment(String str) throws IOException {
        check(false);
        append("<!--");
        append(str);
        append("-->");
    }

    @Override
    public void processingInstruction(String str) throws IOException {
        check(false);
        append("<?");
        append(str);
        append("?>");
    }
}
