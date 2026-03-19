package java.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Types;
import sun.misc.DoubleConsts;

public class Properties extends Hashtable<Object, Object> {
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final long serialVersionUID = 4112578634029874840L;
    protected Properties defaults;

    public Properties() {
        this(null);
    }

    public Properties(Properties properties) {
        this.defaults = properties;
    }

    public synchronized Object setProperty(String str, String str2) {
        return put(str, str2);
    }

    public synchronized void load(Reader reader) throws IOException {
        load0(new LineReader(reader));
    }

    public synchronized void load(InputStream inputStream) throws IOException {
        load0(new LineReader(inputStream));
    }

    private void load0(LineReader lineReader) throws IOException {
        int i;
        boolean z;
        char[] cArr = new char[1024];
        while (true) {
            int line = lineReader.readLine();
            if (line >= 0) {
                int i2 = 0;
                boolean z2 = false;
                while (true) {
                    if (i2 < line) {
                        char c = lineReader.lineBuf[i2];
                        if ((c == '=' || c == ':') && !z2) {
                            i = i2 + 1;
                            z = true;
                            break;
                        } else if (Character.isWhitespace(c) && !z2) {
                            i = i2 + 1;
                            break;
                        } else {
                            z2 = c == '\\' ? !z2 : false;
                            i2++;
                        }
                    } else {
                        i = line;
                        break;
                    }
                }
                z = false;
                while (i < line) {
                    char c2 = lineReader.lineBuf[i];
                    if (!Character.isWhitespace(c2)) {
                        if (z || !(c2 == '=' || c2 == ':')) {
                            break;
                        } else {
                            z = true;
                        }
                    }
                    i++;
                }
                put(loadConvert(lineReader.lineBuf, 0, i2, cArr), loadConvert(lineReader.lineBuf, i, line - i, cArr));
            } else {
                return;
            }
        }
    }

    class LineReader {
        byte[] inByteBuf;
        char[] inCharBuf;
        int inLimit;
        int inOff;
        InputStream inStream;
        char[] lineBuf;
        Reader reader;

        public LineReader(InputStream inputStream) {
            this.lineBuf = new char[1024];
            this.inLimit = 0;
            this.inOff = 0;
            this.inStream = inputStream;
            this.inByteBuf = new byte[8192];
        }

        public LineReader(Reader reader) {
            this.lineBuf = new char[1024];
            this.inLimit = 0;
            this.inOff = 0;
            this.reader = reader;
            this.inCharBuf = new char[8192];
        }

        int readLine() throws IOException {
            char c;
            boolean z = true;
            boolean z2 = true;
            boolean z3 = false;
            int i = 0;
            boolean z4 = false;
            boolean z5 = false;
            boolean z6 = false;
            while (true) {
                if (this.inOff >= this.inLimit) {
                    this.inLimit = this.inStream == null ? this.reader.read(this.inCharBuf) : this.inStream.read(this.inByteBuf);
                    this.inOff = 0;
                    if (this.inLimit <= 0) {
                        if (i == 0 || z4) {
                            return -1;
                        }
                        return z5 ? i - 1 : i;
                    }
                }
                if (this.inStream != null) {
                    byte[] bArr = this.inByteBuf;
                    int i2 = this.inOff;
                    this.inOff = i2 + 1;
                    c = (char) (255 & bArr[i2]);
                } else {
                    char[] cArr = this.inCharBuf;
                    int i3 = this.inOff;
                    this.inOff = i3 + 1;
                    c = cArr[i3];
                }
                if (z3) {
                    if (c == '\n') {
                        z3 = false;
                    } else {
                        z3 = false;
                    }
                }
                if (z) {
                    if (!Character.isWhitespace(c) && (z6 || (c != '\r' && c != '\n'))) {
                        z = false;
                        z6 = false;
                    }
                }
                if (z2) {
                    if (c == '#' || c == '!') {
                        z4 = true;
                        z2 = false;
                    } else {
                        z2 = false;
                    }
                }
                if (c != '\n' && c != '\r') {
                    int i4 = i + 1;
                    this.lineBuf[i] = c;
                    if (i4 == this.lineBuf.length) {
                        int length = this.lineBuf.length * 2;
                        if (length < 0) {
                            length = Integer.MAX_VALUE;
                        }
                        char[] cArr2 = new char[length];
                        System.arraycopy((Object) this.lineBuf, 0, (Object) cArr2, 0, this.lineBuf.length);
                        this.lineBuf = cArr2;
                    }
                    z5 = c == '\\' ? !z5 : false;
                    i = i4;
                } else if (z4 || i == 0) {
                    z = true;
                    z2 = true;
                    i = 0;
                    z4 = false;
                } else {
                    if (this.inOff >= this.inLimit) {
                        this.inLimit = this.inStream == null ? this.reader.read(this.inCharBuf) : this.inStream.read(this.inByteBuf);
                        this.inOff = 0;
                        if (this.inLimit <= 0) {
                            return z5 ? i - 1 : i;
                        }
                    }
                    if (!z5) {
                        return i;
                    }
                    i--;
                    if (c == '\r') {
                        z3 = true;
                        z = true;
                    } else {
                        z = true;
                    }
                    z6 = z;
                    z5 = false;
                }
            }
        }
    }

    private String loadConvert(char[] cArr, int i, int i2, char[] cArr2) {
        int i3;
        if (cArr2.length < i2) {
            int i4 = i2 * 2;
            if (i4 < 0) {
                i4 = Integer.MAX_VALUE;
            }
            cArr2 = new char[i4];
        }
        int i5 = i2 + i;
        int i6 = 0;
        while (i < i5) {
            int i7 = i + 1;
            char c = cArr[i];
            if (c == '\\') {
                i = i7 + 1;
                char c2 = cArr[i7];
                if (c2 == 'u') {
                    int i8 = i;
                    int i9 = 0;
                    int i10 = 0;
                    while (i9 < 4) {
                        int i11 = i8 + 1;
                        char c3 = cArr[i8];
                        switch (c3) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case DoubleConsts.SIGNIFICAND_WIDTH:
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                i10 = ((i10 << 4) + c3) - 48;
                                break;
                            default:
                                switch (c3) {
                                    case 'A':
                                    case 'B':
                                    case 'C':
                                    case 'D':
                                    case 'E':
                                    case Types.DATALINK:
                                        i10 = (((i10 << 4) + 10) + c3) - 65;
                                        break;
                                    default:
                                        switch (c3) {
                                            case 'a':
                                            case 'b':
                                            case 'c':
                                            case 'd':
                                            case 'e':
                                            case 'f':
                                                i10 = (((i10 << 4) + 10) + c3) - 97;
                                                break;
                                            default:
                                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                                        }
                                        break;
                                }
                                break;
                        }
                        i9++;
                        i8 = i11;
                    }
                    cArr2[i6] = (char) i10;
                    i6++;
                    i = i8;
                } else {
                    if (c2 == 't') {
                        c2 = '\t';
                    } else if (c2 == 'r') {
                        c2 = '\r';
                    } else if (c2 != 'n') {
                        if (c2 == 'f') {
                            c2 = '\f';
                        }
                    } else {
                        c2 = '\n';
                    }
                    i3 = i6 + 1;
                    cArr2[i6] = c2;
                }
            } else {
                i3 = i6 + 1;
                cArr2[i6] = c;
                i = i7;
            }
            i6 = i3;
        }
        return new String(cArr2, 0, i6);
    }

    private String saveConvert(String str, boolean z, boolean z2) {
        int length = str.length();
        int i = length * 2;
        if (i < 0) {
            i = Integer.MAX_VALUE;
        }
        StringBuffer stringBuffer = new StringBuffer(i);
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt > '=' && cCharAt < 127) {
                if (cCharAt == '\\') {
                    stringBuffer.append('\\');
                    stringBuffer.append('\\');
                } else {
                    stringBuffer.append(cCharAt);
                }
            } else {
                switch (cCharAt) {
                    case '\t':
                        stringBuffer.append('\\');
                        stringBuffer.append('t');
                        break;
                    case '\n':
                        stringBuffer.append('\\');
                        stringBuffer.append('n');
                        break;
                    case '\f':
                        stringBuffer.append('\\');
                        stringBuffer.append('f');
                        break;
                    case '\r':
                        stringBuffer.append('\\');
                        stringBuffer.append('r');
                        break;
                    case ' ':
                        if (i2 == 0 || z) {
                            stringBuffer.append('\\');
                        }
                        stringBuffer.append(' ');
                        break;
                    case '!':
                    case '#':
                    case ':':
                    case '=':
                        stringBuffer.append('\\');
                        stringBuffer.append(cCharAt);
                        break;
                    default:
                        if ((cCharAt < ' ' || cCharAt > '~') & z2) {
                            stringBuffer.append('\\');
                            stringBuffer.append('u');
                            stringBuffer.append(toHex((cCharAt >> '\f') & 15));
                            stringBuffer.append(toHex((cCharAt >> '\b') & 15));
                            stringBuffer.append(toHex((cCharAt >> 4) & 15));
                            stringBuffer.append(toHex(cCharAt & 15));
                        } else {
                            stringBuffer.append(cCharAt);
                        }
                        break;
                }
            }
        }
        return stringBuffer.toString();
    }

    private static void writeComments(BufferedWriter bufferedWriter, String str) throws IOException {
        bufferedWriter.write("#");
        int length = str.length();
        char[] cArr = new char[6];
        int i = 0;
        cArr[0] = '\\';
        cArr[1] = 'u';
        int i2 = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt > 255 || cCharAt == '\n' || cCharAt == '\r') {
                if (i2 != i) {
                    bufferedWriter.write(str.substring(i2, i));
                }
                if (cCharAt > 255) {
                    cArr[2] = toHex((cCharAt >> '\f') & 15);
                    cArr[3] = toHex((cCharAt >> '\b') & 15);
                    cArr[4] = toHex((cCharAt >> 4) & 15);
                    cArr[5] = toHex(cCharAt & 15);
                    bufferedWriter.write(new String(cArr));
                } else {
                    bufferedWriter.newLine();
                    if (cCharAt == '\r' && i != length - 1) {
                        int i3 = i + 1;
                        if (str.charAt(i3) == '\n') {
                            i = i3;
                        }
                    }
                    if (i != length - 1) {
                        int i4 = i + 1;
                        if (str.charAt(i4) != '#' && str.charAt(i4) != '!') {
                            bufferedWriter.write("#");
                        }
                    }
                }
                i2 = i + 1;
            }
            i++;
        }
        if (i2 != i) {
            bufferedWriter.write(str.substring(i2, i));
        }
        bufferedWriter.newLine();
    }

    @Deprecated
    public void save(OutputStream outputStream, String str) {
        try {
            store(outputStream, str);
        } catch (IOException e) {
        }
    }

    public void store(Writer writer, String str) throws IOException {
        store0(writer instanceof BufferedWriter ? (BufferedWriter) writer : new BufferedWriter(writer), str, false);
    }

    public void store(OutputStream outputStream, String str) throws IOException {
        store0(new BufferedWriter(new OutputStreamWriter(outputStream, "8859_1")), str, true);
    }

    private void store0(BufferedWriter bufferedWriter, String str, boolean z) throws IOException {
        if (str != null) {
            writeComments(bufferedWriter, str);
        }
        bufferedWriter.write("#" + new Date().toString());
        bufferedWriter.newLine();
        synchronized (this) {
            Enumeration enumerationKeys = keys();
            while (enumerationKeys.hasMoreElements()) {
                String str2 = (String) enumerationKeys.nextElement();
                String str3 = (String) get(str2);
                bufferedWriter.write(saveConvert(str2, true, z) + "=" + saveConvert(str3, false, z));
                bufferedWriter.newLine();
            }
        }
        bufferedWriter.flush();
    }

    public synchronized void loadFromXML(InputStream inputStream) throws IOException {
        XMLUtils.load(this, (InputStream) Objects.requireNonNull(inputStream));
        inputStream.close();
    }

    public void storeToXML(OutputStream outputStream, String str) throws IOException {
        storeToXML(outputStream, str, "UTF-8");
    }

    public void storeToXML(OutputStream outputStream, String str, String str2) throws IOException {
        XMLUtils.save(this, (OutputStream) Objects.requireNonNull(outputStream), str, (String) Objects.requireNonNull(str2));
    }

    public String getProperty(String str) {
        Object obj = super.get(str);
        String str2 = obj instanceof String ? (String) obj : null;
        return (str2 != null || this.defaults == null) ? str2 : this.defaults.getProperty(str);
    }

    public String getProperty(String str, String str2) {
        String property = getProperty(str);
        return property == null ? str2 : property;
    }

    public Enumeration<?> propertyNames() {
        Hashtable<String, Object> hashtable = new Hashtable<>();
        enumerate(hashtable);
        return hashtable.keys();
    }

    public Set<String> stringPropertyNames() {
        Hashtable<String, String> hashtable = new Hashtable<>();
        enumerateStringProperties(hashtable);
        return hashtable.keySet();
    }

    public void list(PrintStream printStream) {
        printStream.println("-- listing properties --");
        Hashtable<String, Object> hashtable = new Hashtable<>();
        enumerate(hashtable);
        Enumeration<String> enumerationKeys = hashtable.keys();
        while (enumerationKeys.hasMoreElements()) {
            String strNextElement = enumerationKeys.nextElement();
            String str = (String) hashtable.get(strNextElement);
            if (str.length() > 40) {
                str = str.substring(0, 37) + "...";
            }
            printStream.println(strNextElement + "=" + str);
        }
    }

    public void list(PrintWriter printWriter) {
        printWriter.println("-- listing properties --");
        Hashtable<String, Object> hashtable = new Hashtable<>();
        enumerate(hashtable);
        Enumeration<String> enumerationKeys = hashtable.keys();
        while (enumerationKeys.hasMoreElements()) {
            String strNextElement = enumerationKeys.nextElement();
            String str = (String) hashtable.get(strNextElement);
            if (str.length() > 40) {
                str = str.substring(0, 37) + "...";
            }
            printWriter.println(strNextElement + "=" + str);
        }
    }

    private synchronized void enumerate(Hashtable<String, Object> hashtable) {
        if (this.defaults != null) {
            this.defaults.enumerate(hashtable);
        }
        Enumeration<Object> enumerationKeys = keys();
        while (enumerationKeys.hasMoreElements()) {
            String str = (String) enumerationKeys.nextElement();
            hashtable.put(str, get(str));
        }
    }

    private synchronized void enumerateStringProperties(Hashtable<String, String> hashtable) {
        if (this.defaults != null) {
            this.defaults.enumerateStringProperties(hashtable);
        }
        Enumeration<Object> enumerationKeys = keys();
        while (enumerationKeys.hasMoreElements()) {
            Object objNextElement = enumerationKeys.nextElement();
            Object obj = get(objNextElement);
            if ((objNextElement instanceof String) && (obj instanceof String)) {
                hashtable.put((String) objNextElement, (String) obj);
            }
        }
    }

    private static char toHex(int i) {
        return hexDigit[i & 15];
    }
}
