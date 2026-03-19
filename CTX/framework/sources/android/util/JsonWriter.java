package android.util;

import android.provider.SettingsStringUtil;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class JsonWriter implements Closeable {
    private String indent;
    private boolean lenient;
    private final Writer out;
    private String separator;
    private final List<JsonScope> stack = new ArrayList();

    public JsonWriter(Writer writer) {
        this.stack.add(JsonScope.EMPTY_DOCUMENT);
        this.separator = SettingsStringUtil.DELIMITER;
        if (writer == null) {
            throw new NullPointerException("out == null");
        }
        this.out = writer;
    }

    public void setIndent(String str) {
        if (str.isEmpty()) {
            this.indent = null;
            this.separator = SettingsStringUtil.DELIMITER;
        } else {
            this.indent = str;
            this.separator = ": ";
        }
    }

    public void setLenient(boolean z) {
        this.lenient = z;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public JsonWriter beginArray() throws IOException {
        return open(JsonScope.EMPTY_ARRAY, "[");
    }

    public JsonWriter endArray() throws IOException {
        return close(JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY, "]");
    }

    public JsonWriter beginObject() throws IOException {
        return open(JsonScope.EMPTY_OBJECT, "{");
    }

    public JsonWriter endObject() throws IOException {
        return close(JsonScope.EMPTY_OBJECT, JsonScope.NONEMPTY_OBJECT, "}");
    }

    private JsonWriter open(JsonScope jsonScope, String str) throws IOException {
        beforeValue(true);
        this.stack.add(jsonScope);
        this.out.write(str);
        return this;
    }

    private JsonWriter close(JsonScope jsonScope, JsonScope jsonScope2, String str) throws IOException {
        JsonScope jsonScopePeek = peek();
        if (jsonScopePeek != jsonScope2 && jsonScopePeek != jsonScope) {
            throw new IllegalStateException("Nesting problem: " + this.stack);
        }
        this.stack.remove(this.stack.size() - 1);
        if (jsonScopePeek == jsonScope2) {
            newline();
        }
        this.out.write(str);
        return this;
    }

    private JsonScope peek() {
        return this.stack.get(this.stack.size() - 1);
    }

    private void replaceTop(JsonScope jsonScope) {
        this.stack.set(this.stack.size() - 1, jsonScope);
    }

    public JsonWriter name(String str) throws IOException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        beforeName();
        string(str);
        return this;
    }

    public JsonWriter value(String str) throws IOException {
        if (str == null) {
            return nullValue();
        }
        beforeValue(false);
        string(str);
        return this;
    }

    public JsonWriter nullValue() throws IOException {
        beforeValue(false);
        this.out.write("null");
        return this;
    }

    public JsonWriter value(boolean z) throws IOException {
        beforeValue(false);
        this.out.write(z ? "true" : "false");
        return this;
    }

    public JsonWriter value(double d) throws IOException {
        if (!this.lenient && (Double.isNaN(d) || Double.isInfinite(d))) {
            throw new IllegalArgumentException("Numeric values must be finite, but was " + d);
        }
        beforeValue(false);
        this.out.append((CharSequence) Double.toString(d));
        return this;
    }

    public JsonWriter value(long j) throws IOException {
        beforeValue(false);
        this.out.write(Long.toString(j));
        return this;
    }

    public JsonWriter value(Number number) throws IOException {
        if (number == null) {
            return nullValue();
        }
        String string = number.toString();
        if (!this.lenient && (string.equals("-Infinity") || string.equals("Infinity") || string.equals("NaN"))) {
            throw new IllegalArgumentException("Numeric values must be finite, but was " + number);
        }
        beforeValue(false);
        this.out.append((CharSequence) string);
        return this;
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void close() throws IOException {
        this.out.close();
        if (peek() != JsonScope.NONEMPTY_DOCUMENT) {
            throw new IOException("Incomplete document");
        }
    }

    private void string(String str) throws IOException {
        this.out.write("\"");
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            switch (cCharAt) {
                case '\b':
                    this.out.write("\\b");
                    break;
                case '\t':
                    this.out.write("\\t");
                    break;
                case '\n':
                    this.out.write("\\n");
                    break;
                case '\f':
                    this.out.write("\\f");
                    break;
                case '\r':
                    this.out.write("\\r");
                    break;
                case '\"':
                case '\\':
                    this.out.write(92);
                    this.out.write(cCharAt);
                    break;
                case 8232:
                case 8233:
                    this.out.write(String.format("\\u%04x", Integer.valueOf(cCharAt)));
                    break;
                default:
                    if (cCharAt <= 31) {
                        this.out.write(String.format("\\u%04x", Integer.valueOf(cCharAt)));
                    } else {
                        this.out.write(cCharAt);
                    }
                    break;
            }
        }
        this.out.write("\"");
    }

    private void newline() throws IOException {
        if (this.indent == null) {
            return;
        }
        this.out.write("\n");
        for (int i = 1; i < this.stack.size(); i++) {
            this.out.write(this.indent);
        }
    }

    private void beforeName() throws IOException {
        JsonScope jsonScopePeek = peek();
        if (jsonScopePeek == JsonScope.NONEMPTY_OBJECT) {
            this.out.write(44);
        } else if (jsonScopePeek != JsonScope.EMPTY_OBJECT) {
            throw new IllegalStateException("Nesting problem: " + this.stack);
        }
        newline();
        replaceTop(JsonScope.DANGLING_NAME);
    }

    private void beforeValue(boolean z) throws IOException {
        switch (peek()) {
            case EMPTY_DOCUMENT:
                if (!this.lenient && !z) {
                    throw new IllegalStateException("JSON must start with an array or an object.");
                }
                replaceTop(JsonScope.NONEMPTY_DOCUMENT);
                return;
            case EMPTY_ARRAY:
                replaceTop(JsonScope.NONEMPTY_ARRAY);
                newline();
                return;
            case NONEMPTY_ARRAY:
                this.out.append(',');
                newline();
                return;
            case DANGLING_NAME:
                this.out.append((CharSequence) this.separator);
                replaceTop(JsonScope.NONEMPTY_OBJECT);
                return;
            case NONEMPTY_DOCUMENT:
                throw new IllegalStateException("JSON must have only one top-level value.");
            default:
                throw new IllegalStateException("Nesting problem: " + this.stack);
        }
    }
}
