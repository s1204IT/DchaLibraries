package android.icu.impl.duration.impl;

import android.icu.lang.UCharacter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class XMLRecordReader implements RecordReader {
    private boolean atTag;
    private List<String> nameStack = new ArrayList();
    private Reader r;
    private String tag;

    public XMLRecordReader(Reader r) {
        this.r = r;
        if (getTag().startsWith("?xml")) {
            advance();
        }
        if (!getTag().startsWith("!--")) {
            return;
        }
        advance();
    }

    @Override
    public boolean open(String title) {
        if (getTag().equals(title)) {
            this.nameStack.add(title);
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean close() {
        int ix = this.nameStack.size() - 1;
        String name = this.nameStack.get(ix);
        if (getTag().equals("/" + name)) {
            this.nameStack.remove(ix);
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean bool(String name) {
        String s = string(name);
        if (s != null) {
            return "true".equals(s);
        }
        return false;
    }

    @Override
    public boolean[] boolArray(String name) {
        String[] sa = stringArray(name);
        if (sa == null) {
            return null;
        }
        boolean[] result = new boolean[sa.length];
        for (int i = 0; i < sa.length; i++) {
            result[i] = "true".equals(sa[i]);
        }
        return result;
    }

    @Override
    public char character(String name) {
        String s = string(name);
        if (s != null) {
            return s.charAt(0);
        }
        return (char) 65535;
    }

    @Override
    public char[] characterArray(String name) {
        String[] sa = stringArray(name);
        if (sa == null) {
            return null;
        }
        char[] result = new char[sa.length];
        for (int i = 0; i < sa.length; i++) {
            result[i] = sa[i].charAt(0);
        }
        return result;
    }

    @Override
    public byte namedIndex(String name, String[] names) {
        String sa = string(name);
        if (sa != null) {
            for (int i = 0; i < names.length; i++) {
                if (sa.equals(names[i])) {
                    return (byte) i;
                }
            }
            return (byte) -1;
        }
        return (byte) -1;
    }

    @Override
    public byte[] namedIndexArray(String name, String[] names) {
        String[] sa = stringArray(name);
        if (sa == null) {
            return null;
        }
        byte[] result = new byte[sa.length];
        for (int i = 0; i < sa.length; i++) {
            String s = sa[i];
            int j = 0;
            while (true) {
                if (j < names.length) {
                    if (!names[j].equals(s)) {
                        j++;
                    } else {
                        result[i] = (byte) j;
                        break;
                    }
                } else {
                    result[i] = -1;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public String string(String name) {
        if (match(name)) {
            String result = readData();
            if (match("/" + name)) {
                return result;
            }
            return null;
        }
        return null;
    }

    @Override
    public String[] stringArray(String name) {
        if (match(name + "List")) {
            List<String> list = new ArrayList<>();
            while (true) {
                String s = string(name);
                if (s == null) {
                    break;
                }
                if ("Null".equals(s)) {
                    s = null;
                }
                list.add(s);
            }
            if (match("/" + name + "List")) {
                return (String[]) list.toArray(new String[list.size()]);
            }
        }
        return null;
    }

    @Override
    public String[][] stringTable(String name) {
        if (match(name + "Table")) {
            List<String[]> list = new ArrayList<>();
            while (true) {
                String[] sa = stringArray(name);
                if (sa == null) {
                    break;
                }
                list.add(sa);
            }
            if (match("/" + name + "Table")) {
                return (String[][]) list.toArray(new String[list.size()][]);
            }
        }
        return null;
    }

    private boolean match(String target) {
        if (getTag().equals(target)) {
            advance();
            return true;
        }
        return false;
    }

    private String getTag() {
        if (this.tag == null) {
            this.tag = readNextTag();
        }
        return this.tag;
    }

    private void advance() {
        this.tag = null;
    }

    private String readData() {
        int c;
        StringBuilder sb = new StringBuilder();
        boolean inWhitespace = false;
        while (true) {
            c = readChar();
            if (c == -1 || c == 60) {
                break;
            }
            if (c == 38) {
                int c2 = readChar();
                if (c2 == 35) {
                    StringBuilder numBuf = new StringBuilder();
                    int radix = 10;
                    int c3 = readChar();
                    if (c3 == 120) {
                        radix = 16;
                        c3 = readChar();
                    }
                    while (c3 != 59 && c3 != -1) {
                        numBuf.append((char) c3);
                        c3 = readChar();
                    }
                    try {
                        int num = Integer.parseInt(numBuf.toString(), radix);
                        c = (char) num;
                    } catch (NumberFormatException ex) {
                        System.err.println("numbuf: " + numBuf.toString() + " radix: " + radix);
                        throw ex;
                    }
                } else {
                    StringBuilder charBuf = new StringBuilder();
                    while (c2 != 59 && c2 != -1) {
                        charBuf.append((char) c2);
                        c2 = readChar();
                    }
                    String charName = charBuf.toString();
                    if (charName.equals("lt")) {
                        c = 60;
                    } else if (charName.equals("gt")) {
                        c = 62;
                    } else if (charName.equals("quot")) {
                        c = 34;
                    } else if (charName.equals("apos")) {
                        c = 39;
                    } else if (charName.equals("amp")) {
                        c = 38;
                    } else {
                        System.err.println("unrecognized character entity: '" + charName + "'");
                    }
                }
            }
            if (UCharacter.isWhitespace(c)) {
                if (!inWhitespace) {
                    c = 32;
                    inWhitespace = true;
                }
            } else {
                inWhitespace = false;
            }
            sb.append((char) c);
        }
        this.atTag = c == 60;
        return sb.toString();
    }

    private String readNextTag() {
        int c;
        while (true) {
            if (this.atTag) {
                break;
            }
            c = readChar();
            if (c == 60 || c == -1) {
                break;
            }
            if (!UCharacter.isWhitespace(c)) {
                System.err.println("Unexpected non-whitespace character " + Integer.toHexString(c));
                break;
            }
        }
        if (c == 60) {
            this.atTag = true;
        }
        if (this.atTag) {
            this.atTag = false;
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c2 = readChar();
                if (c2 == 62 || c2 == -1) {
                    break;
                }
                sb.append((char) c2);
            }
            return sb.toString();
        }
        return null;
    }

    int readChar() {
        try {
            return this.r.read();
        } catch (IOException e) {
            return -1;
        }
    }
}
