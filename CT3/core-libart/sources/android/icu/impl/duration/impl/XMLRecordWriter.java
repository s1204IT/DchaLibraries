package android.icu.impl.duration.impl;

import android.icu.lang.UCharacter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class XMLRecordWriter implements RecordWriter {
    private static final String INDENT = "    ";
    static final String NULL_NAME = "Null";
    private List<String> nameStack = new ArrayList();
    private Writer w;

    public XMLRecordWriter(Writer w) {
        this.w = w;
    }

    @Override
    public boolean open(String title) {
        newline();
        writeString("<" + title + ">");
        this.nameStack.add(title);
        return true;
    }

    @Override
    public boolean close() {
        int ix = this.nameStack.size() - 1;
        if (ix < 0) {
            return false;
        }
        String name = this.nameStack.remove(ix);
        newline();
        writeString("</" + name + ">");
        return true;
    }

    public void flush() {
        try {
            this.w.flush();
        } catch (IOException e) {
        }
    }

    @Override
    public void bool(String name, boolean value) {
        internalString(name, String.valueOf(value));
    }

    @Override
    public void boolArray(String name, boolean[] values) {
        if (values == null) {
            return;
        }
        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = String.valueOf(values[i]);
        }
        stringArray(name, stringValues);
    }

    private static String ctos(char value) {
        if (value == '<') {
            return "&lt;";
        }
        if (value == '&') {
            return "&amp;";
        }
        return String.valueOf(value);
    }

    @Override
    public void character(String name, char value) {
        if (value == 65535) {
            return;
        }
        internalString(name, ctos(value));
    }

    @Override
    public void characterArray(String name, char[] values) {
        if (values == null) {
            return;
        }
        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            char value = values[i];
            if (value == 65535) {
                stringValues[i] = NULL_NAME;
            } else {
                stringValues[i] = ctos(value);
            }
        }
        internalStringArray(name, stringValues);
    }

    @Override
    public void namedIndex(String name, String[] names, int value) {
        if (value < 0) {
            return;
        }
        internalString(name, names[value]);
    }

    @Override
    public void namedIndexArray(String name, String[] names, byte[] values) {
        if (values == null) {
            return;
        }
        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            int value = values[i];
            if (value < 0) {
                stringValues[i] = NULL_NAME;
            } else {
                stringValues[i] = names[value];
            }
        }
        internalStringArray(name, stringValues);
    }

    public static String normalize(String str) {
        boolean special;
        if (str == null) {
            return null;
        }
        StringBuilder sb = null;
        boolean inWhitespace = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (UCharacter.isWhitespace(c)) {
                if (sb == null && (inWhitespace || c != ' ')) {
                    sb = new StringBuilder(str.substring(0, i));
                }
                if (!inWhitespace) {
                    inWhitespace = true;
                    special = false;
                    c = ' ';
                }
            } else {
                inWhitespace = false;
                special = c == '<' || c == '&';
                if (special && sb == null) {
                    sb = new StringBuilder(str.substring(0, i));
                }
            }
            if (sb != null) {
                if (special) {
                    sb.append(c == '<' ? "&lt;" : "&amp;");
                } else {
                    sb.append(c);
                }
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return str;
    }

    private void internalString(String name, String normalizedValue) {
        if (normalizedValue == null) {
            return;
        }
        newline();
        writeString("<" + name + ">" + normalizedValue + "</" + name + ">");
    }

    private void internalStringArray(String name, String[] normalizedValues) {
        if (normalizedValues == null) {
            return;
        }
        push(name + "List");
        for (String value : normalizedValues) {
            if (value == null) {
                value = NULL_NAME;
            }
            string(name, value);
        }
        pop();
    }

    @Override
    public void string(String name, String value) {
        internalString(name, normalize(value));
    }

    @Override
    public void stringArray(String name, String[] values) {
        if (values == null) {
            return;
        }
        push(name + "List");
        for (String str : values) {
            String value = normalize(str);
            if (value == null) {
                value = NULL_NAME;
            }
            internalString(name, value);
        }
        pop();
    }

    @Override
    public void stringTable(String name, String[][] values) {
        if (values == null) {
            return;
        }
        push(name + "Table");
        for (String[] rowValues : values) {
            if (rowValues == null) {
                internalString(name + "List", NULL_NAME);
            } else {
                stringArray(name, rowValues);
            }
        }
        pop();
    }

    private void push(String name) {
        newline();
        writeString("<" + name + ">");
        this.nameStack.add(name);
    }

    private void pop() {
        int ix = this.nameStack.size() - 1;
        String name = this.nameStack.remove(ix);
        newline();
        writeString("</" + name + ">");
    }

    private void newline() {
        writeString("\n");
        for (int i = 0; i < this.nameStack.size(); i++) {
            writeString(INDENT);
        }
    }

    private void writeString(String str) {
        if (this.w == null) {
            return;
        }
        try {
            this.w.write(str);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            this.w = null;
        }
    }
}
