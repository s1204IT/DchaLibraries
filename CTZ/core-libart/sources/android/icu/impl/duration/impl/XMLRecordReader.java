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

    public XMLRecordReader(Reader reader) {
        this.r = reader;
        if (getTag().startsWith("?xml")) {
            advance();
        }
        if (getTag().startsWith("!--")) {
            advance();
        }
    }

    @Override
    public boolean open(String str) {
        if (getTag().equals(str)) {
            this.nameStack.add(str);
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean close() {
        int size = this.nameStack.size() - 1;
        String str = this.nameStack.get(size);
        if (getTag().equals("/" + str)) {
            this.nameStack.remove(size);
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean bool(String str) {
        String strString = string(str);
        if (strString != null) {
            return "true".equals(strString);
        }
        return false;
    }

    @Override
    public boolean[] boolArray(String str) {
        String[] strArrStringArray = stringArray(str);
        if (strArrStringArray != null) {
            boolean[] zArr = new boolean[strArrStringArray.length];
            for (int i = 0; i < strArrStringArray.length; i++) {
                zArr[i] = "true".equals(strArrStringArray[i]);
            }
            return zArr;
        }
        return null;
    }

    @Override
    public char character(String str) {
        String strString = string(str);
        if (strString != null) {
            return strString.charAt(0);
        }
        return (char) 65535;
    }

    @Override
    public char[] characterArray(String str) {
        String[] strArrStringArray = stringArray(str);
        if (strArrStringArray != null) {
            char[] cArr = new char[strArrStringArray.length];
            for (int i = 0; i < strArrStringArray.length; i++) {
                cArr[i] = strArrStringArray[i].charAt(0);
            }
            return cArr;
        }
        return null;
    }

    @Override
    public byte namedIndex(String str, String[] strArr) {
        String strString = string(str);
        if (strString != null) {
            for (int i = 0; i < strArr.length; i++) {
                if (strString.equals(strArr[i])) {
                    return (byte) i;
                }
            }
            return (byte) -1;
        }
        return (byte) -1;
    }

    @Override
    public byte[] namedIndexArray(String str, String[] strArr) {
        String[] strArrStringArray = stringArray(str);
        if (strArrStringArray != null) {
            byte[] bArr = new byte[strArrStringArray.length];
            for (int i = 0; i < strArrStringArray.length; i++) {
                String str2 = strArrStringArray[i];
                int i2 = 0;
                while (true) {
                    if (i2 < strArr.length) {
                        if (!strArr[i2].equals(str2)) {
                            i2++;
                        } else {
                            bArr[i] = (byte) i2;
                            break;
                        }
                    } else {
                        bArr[i] = -1;
                        break;
                    }
                }
            }
            return bArr;
        }
        return null;
    }

    @Override
    public String string(String str) {
        if (match(str)) {
            String data = readData();
            if (match("/" + str)) {
                return data;
            }
            return null;
        }
        return null;
    }

    @Override
    public String[] stringArray(String str) {
        if (match(str + "List")) {
            ArrayList arrayList = new ArrayList();
            while (true) {
                String strString = string(str);
                if (strString == null) {
                    break;
                }
                if ("Null".equals(strString)) {
                    strString = null;
                }
                arrayList.add(strString);
            }
            if (match("/" + str + "List")) {
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            }
        }
        return null;
    }

    @Override
    public String[][] stringTable(String str) {
        if (match(str + "Table")) {
            ArrayList arrayList = new ArrayList();
            while (true) {
                String[] strArrStringArray = stringArray(str);
                if (strArrStringArray == null) {
                    break;
                }
                arrayList.add(strArrStringArray);
            }
            if (match("/" + str + "Table")) {
                return (String[][]) arrayList.toArray(new String[arrayList.size()][]);
            }
            return null;
        }
        return null;
    }

    private boolean match(String str) {
        if (getTag().equals(str)) {
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
        int i;
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        boolean z2 = false;
        while (true) {
            i = readChar();
            if (i == -1 || i == 60) {
                break;
            }
            if (i == 38) {
                int i2 = readChar();
                if (i2 == 35) {
                    StringBuilder sb2 = new StringBuilder();
                    int i3 = 10;
                    int i4 = readChar();
                    if (i4 == 120) {
                        i3 = 16;
                        i4 = readChar();
                    }
                    while (i4 != 59 && i4 != -1) {
                        sb2.append((char) i4);
                        i4 = readChar();
                    }
                    try {
                        i = (char) Integer.parseInt(sb2.toString(), i3);
                    } catch (NumberFormatException e) {
                        System.err.println("numbuf: " + sb2.toString() + " radix: " + i3);
                        throw e;
                    }
                } else {
                    StringBuilder sb3 = new StringBuilder();
                    while (i2 != 59 && i2 != -1) {
                        sb3.append((char) i2);
                        i2 = readChar();
                    }
                    String string = sb3.toString();
                    if (!string.equals("lt")) {
                        if (string.equals("gt")) {
                            i = 62;
                        } else if (string.equals("quot")) {
                            i = 34;
                        } else if (string.equals("apos")) {
                            i = 39;
                        } else if (!string.equals("amp")) {
                            System.err.println("unrecognized character entity: '" + string + "'");
                        } else {
                            i = 38;
                        }
                    } else {
                        i = 60;
                    }
                }
            }
            if (UCharacter.isWhitespace(i)) {
                if (!z2) {
                    i = 32;
                    z2 = true;
                }
            } else {
                z2 = false;
            }
            sb.append((char) i);
        }
        if (i == 60) {
            z = true;
        }
        this.atTag = z;
        return sb.toString();
    }

    private String readNextTag() {
        while (true) {
            if (this.atTag) {
                break;
            }
            int i = readChar();
            if (i == 60 || i == -1) {
                break;
            }
            if (!UCharacter.isWhitespace(i)) {
                System.err.println("Unexpected non-whitespace character " + Integer.toHexString(i));
                break;
            }
        }
        if (this.atTag) {
            this.atTag = false;
            StringBuilder sb = new StringBuilder();
            while (true) {
                int i2 = readChar();
                if (i2 == 62 || i2 == -1) {
                    break;
                }
                sb.append((char) i2);
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
