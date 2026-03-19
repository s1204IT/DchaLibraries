package java.util.jar;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Manifest implements Cloneable {
    private Attributes attr = new Attributes();
    private Map<String, Attributes> entries = new HashMap();

    public Manifest() {
    }

    public Manifest(InputStream inputStream) throws IOException {
        read(inputStream);
    }

    public Manifest(Manifest manifest) {
        this.attr.putAll(manifest.getMainAttributes());
        this.entries.putAll(manifest.getEntries());
    }

    public Attributes getMainAttributes() {
        return this.attr;
    }

    public Map<String, Attributes> getEntries() {
        return this.entries;
    }

    public Attributes getAttributes(String str) {
        return getEntries().get(str);
    }

    public void clear() {
        this.attr.clear();
        this.entries.clear();
    }

    public void write(OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        this.attr.writeMain(dataOutputStream);
        for (Map.Entry<String, Attributes> entry : this.entries.entrySet()) {
            StringBuffer stringBuffer = new StringBuffer("Name: ");
            String key = entry.getKey();
            if (key != null) {
                byte[] bytes = key.getBytes("UTF8");
                key = new String(bytes, 0, 0, bytes.length);
            }
            stringBuffer.append(key);
            stringBuffer.append("\r\n");
            make72Safe(stringBuffer);
            dataOutputStream.writeBytes(stringBuffer.toString());
            entry.getValue().write(dataOutputStream);
        }
        dataOutputStream.flush();
    }

    static void make72Safe(StringBuffer stringBuffer) {
        int length = stringBuffer.length();
        if (length > 72) {
            int i = 70;
            while (i < length - 2) {
                stringBuffer.insert(i, "\r\n ");
                i += 72;
                length += 3;
            }
        }
    }

    public void read(InputStream inputStream) throws IOException {
        Attributes attributes;
        FastInputStream fastInputStream = new FastInputStream(inputStream);
        byte[] bArr = new byte[512];
        this.attr.read(fastInputStream, bArr);
        int iMax = 2;
        String name = null;
        byte[] bArr2 = null;
        boolean z = true;
        int i = 0;
        int size = 0;
        while (true) {
            int line = fastInputStream.readLine(bArr);
            if (line == -1) {
                return;
            }
            int i2 = line - 1;
            if (bArr[i2] != 10) {
                throw new IOException("manifest line too long");
            }
            if (i2 > 0 && bArr[i2 - 1] == 13) {
                i2--;
            }
            if (i2 != 0 || !z) {
                if (name == null) {
                    name = parseName(bArr, i2);
                    if (name == null) {
                        throw new IOException("invalid manifest format");
                    }
                    if (fastInputStream.peek() == 32) {
                        int i3 = i2 - 6;
                        bArr2 = new byte[i3];
                        System.arraycopy(bArr, 6, bArr2, 0, i3);
                        z = false;
                    } else {
                        attributes = getAttributes(name);
                        if (attributes == null) {
                            attributes = new Attributes(iMax);
                            this.entries.put(name, attributes);
                        }
                        attributes.read(fastInputStream, bArr);
                        i++;
                        size += attributes.size();
                        iMax = Math.max(2, size / i);
                        name = null;
                        z = true;
                    }
                } else {
                    byte[] bArr3 = new byte[(bArr2.length + i2) - 1];
                    System.arraycopy(bArr2, 0, bArr3, 0, bArr2.length);
                    System.arraycopy(bArr, 1, bArr3, bArr2.length, i2 - 1);
                    if (fastInputStream.peek() == 32) {
                        z = false;
                        bArr2 = bArr3;
                    } else {
                        name = new String(bArr3, 0, bArr3.length, "UTF8");
                        bArr2 = null;
                        attributes = getAttributes(name);
                        if (attributes == null) {
                        }
                        attributes.read(fastInputStream, bArr);
                        i++;
                        size += attributes.size();
                        iMax = Math.max(2, size / i);
                        name = null;
                        z = true;
                    }
                }
            }
        }
    }

    private String parseName(byte[] bArr, int i) {
        if (toLower(bArr[0]) == 110 && toLower(bArr[1]) == 97 && toLower(bArr[2]) == 109 && toLower(bArr[3]) == 101 && bArr[4] == 58 && bArr[5] == 32) {
            try {
                return new String(bArr, 6, i - 6, "UTF8");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private int toLower(int i) {
        return (i < 65 || i > 90) ? i : (i - 65) + 97;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Manifest)) {
            return false;
        }
        Manifest manifest = (Manifest) obj;
        return this.attr.equals(manifest.getMainAttributes()) && this.entries.equals(manifest.getEntries());
    }

    public int hashCode() {
        return this.attr.hashCode() + this.entries.hashCode();
    }

    public Object clone() {
        return new Manifest(this);
    }

    static class FastInputStream extends FilterInputStream {
        private byte[] buf;
        private int count;
        private int pos;

        FastInputStream(InputStream inputStream) {
            this(inputStream, 8192);
        }

        FastInputStream(InputStream inputStream, int i) {
            super(inputStream);
            this.count = 0;
            this.pos = 0;
            this.buf = new byte[i];
        }

        @Override
        public int read() throws IOException {
            if (this.pos >= this.count) {
                fill();
                if (this.pos >= this.count) {
                    return -1;
                }
            }
            byte[] bArr = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            return Byte.toUnsignedInt(bArr[i]);
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            int i3 = this.count - this.pos;
            if (i3 <= 0) {
                if (i2 >= this.buf.length) {
                    return this.in.read(bArr, i, i2);
                }
                fill();
                i3 = this.count - this.pos;
                if (i3 <= 0) {
                    return -1;
                }
            }
            if (i2 > i3) {
                i2 = i3;
            }
            System.arraycopy(this.buf, this.pos, bArr, i, i2);
            this.pos += i2;
            return i2;
        }

        public int readLine(byte[] bArr, int i, int i2) throws IOException {
            byte[] bArr2 = this.buf;
            int i3 = 0;
            while (i3 < i2) {
                int i4 = this.count - this.pos;
                if (i4 <= 0) {
                    fill();
                    i4 = this.count - this.pos;
                    if (i4 <= 0) {
                        return -1;
                    }
                }
                int i5 = i2 - i3;
                if (i5 <= i4) {
                    i4 = i5;
                }
                int i6 = this.pos;
                int i7 = i4 + i6;
                while (true) {
                    if (i6 >= i7) {
                        break;
                    }
                    int i8 = i6 + 1;
                    if (bArr2[i6] == 10) {
                        i6 = i8;
                        break;
                    }
                    i6 = i8;
                }
                int i9 = i6 - this.pos;
                System.arraycopy(bArr2, this.pos, bArr, i, i9);
                i += i9;
                i3 += i9;
                this.pos = i6;
                if (bArr2[i6 - 1] == 10) {
                    break;
                }
            }
            return i3;
        }

        public byte peek() throws IOException {
            if (this.pos == this.count) {
                fill();
            }
            if (this.pos == this.count) {
                return (byte) -1;
            }
            return this.buf[this.pos];
        }

        public int readLine(byte[] bArr) throws IOException {
            return readLine(bArr, 0, bArr.length);
        }

        @Override
        public long skip(long j) throws IOException {
            if (j <= 0) {
                return 0L;
            }
            long j2 = this.count - this.pos;
            if (j2 <= 0) {
                return this.in.skip(j);
            }
            if (j > j2) {
                j = j2;
            }
            this.pos = (int) (((long) this.pos) + j);
            return j;
        }

        @Override
        public int available() throws IOException {
            return (this.count - this.pos) + this.in.available();
        }

        @Override
        public void close() throws IOException {
            if (this.in != null) {
                this.in.close();
                this.in = null;
                this.buf = null;
            }
        }

        private void fill() throws IOException {
            this.pos = 0;
            this.count = 0;
            int i = this.in.read(this.buf, 0, this.buf.length);
            if (i > 0) {
                this.count = i;
            }
        }
    }
}
