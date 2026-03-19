package org.json;

import android.icu.impl.PatternTokenizer;
import android.icu.text.PluralRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSONStringer {
    private final String indent;
    final StringBuilder out;
    private final List<Scope> stack;

    enum Scope {
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        DANGLING_KEY,
        NONEMPTY_OBJECT,
        NULL
    }

    public JSONStringer() {
        this.out = new StringBuilder();
        this.stack = new ArrayList();
        this.indent = null;
    }

    JSONStringer(int i) {
        this.out = new StringBuilder();
        this.stack = new ArrayList();
        char[] cArr = new char[i];
        Arrays.fill(cArr, ' ');
        this.indent = new String(cArr);
    }

    public JSONStringer array() throws JSONException {
        return open(Scope.EMPTY_ARRAY, "[");
    }

    public JSONStringer endArray() throws JSONException {
        return close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
    }

    public JSONStringer object() throws JSONException {
        return open(Scope.EMPTY_OBJECT, "{");
    }

    public JSONStringer endObject() throws JSONException {
        return close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}");
    }

    JSONStringer open(Scope scope, String str) throws JSONException {
        if (this.stack.isEmpty() && this.out.length() > 0) {
            throw new JSONException("Nesting problem: multiple top-level roots");
        }
        beforeValue();
        this.stack.add(scope);
        this.out.append(str);
        return this;
    }

    JSONStringer close(Scope scope, Scope scope2, String str) throws JSONException {
        Scope scopePeek = peek();
        if (scopePeek != scope2 && scopePeek != scope) {
            throw new JSONException("Nesting problem");
        }
        this.stack.remove(this.stack.size() - 1);
        if (scopePeek == scope2) {
            newline();
        }
        this.out.append(str);
        return this;
    }

    private Scope peek() throws JSONException {
        if (this.stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        return this.stack.get(this.stack.size() - 1);
    }

    private void replaceTop(Scope scope) {
        this.stack.set(this.stack.size() - 1, scope);
    }

    public JSONStringer value(Object obj) throws JSONException {
        if (this.stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        if (obj instanceof JSONArray) {
            ((JSONArray) obj).writeTo(this);
            return this;
        }
        if (obj instanceof JSONObject) {
            ((JSONObject) obj).writeTo(this);
            return this;
        }
        beforeValue();
        if (obj == null || (obj instanceof Boolean) || obj == JSONObject.NULL) {
            this.out.append(obj);
        } else if (obj instanceof Number) {
            this.out.append(JSONObject.numberToString((Number) obj));
        } else {
            string(obj.toString());
        }
        return this;
    }

    public JSONStringer value(boolean z) throws JSONException {
        if (this.stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue();
        this.out.append(z);
        return this;
    }

    public JSONStringer value(double d) throws JSONException {
        if (this.stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue();
        this.out.append(JSONObject.numberToString(Double.valueOf(d)));
        return this;
    }

    public JSONStringer value(long j) throws JSONException {
        if (this.stack.isEmpty()) {
            throw new JSONException("Nesting problem");
        }
        beforeValue();
        this.out.append(j);
        return this;
    }

    private void string(String str) {
        this.out.append("\"");
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"' || cCharAt == '/' || cCharAt == '\\') {
                StringBuilder sb = this.out;
                sb.append(PatternTokenizer.BACK_SLASH);
                sb.append(cCharAt);
            } else {
                switch (cCharAt) {
                    case '\b':
                        this.out.append("\\b");
                        break;
                    case '\t':
                        this.out.append("\\t");
                        break;
                    case '\n':
                        this.out.append("\\n");
                        break;
                    default:
                        switch (cCharAt) {
                            case '\f':
                                this.out.append("\\f");
                                break;
                            case '\r':
                                this.out.append("\\r");
                                break;
                            default:
                                if (cCharAt <= 31) {
                                    this.out.append(String.format("\\u%04x", Integer.valueOf(cCharAt)));
                                } else {
                                    this.out.append(cCharAt);
                                }
                                break;
                        }
                        break;
                }
            }
        }
        this.out.append("\"");
    }

    private void newline() {
        if (this.indent == null) {
            return;
        }
        this.out.append("\n");
        for (int i = 0; i < this.stack.size(); i++) {
            this.out.append(this.indent);
        }
    }

    public JSONStringer key(String str) throws JSONException {
        if (str == null) {
            throw new JSONException("Names must be non-null");
        }
        beforeKey();
        string(str);
        return this;
    }

    private void beforeKey() throws JSONException {
        Scope scopePeek = peek();
        if (scopePeek == Scope.NONEMPTY_OBJECT) {
            this.out.append(',');
        } else if (scopePeek != Scope.EMPTY_OBJECT) {
            throw new JSONException("Nesting problem");
        }
        newline();
        replaceTop(Scope.DANGLING_KEY);
    }

    private void beforeValue() throws JSONException {
        if (this.stack.isEmpty()) {
            return;
        }
        Scope scopePeek = peek();
        if (scopePeek == Scope.EMPTY_ARRAY) {
            replaceTop(Scope.NONEMPTY_ARRAY);
            newline();
        } else if (scopePeek == Scope.NONEMPTY_ARRAY) {
            this.out.append(',');
            newline();
        } else if (scopePeek == Scope.DANGLING_KEY) {
            this.out.append(this.indent == null ? ":" : PluralRules.KEYWORD_RULE_SEPARATOR);
            replaceTop(Scope.NONEMPTY_OBJECT);
        } else if (scopePeek != Scope.NULL) {
            throw new JSONException("Nesting problem");
        }
    }

    public String toString() {
        if (this.out.length() == 0) {
            return null;
        }
        return this.out.toString();
    }
}
