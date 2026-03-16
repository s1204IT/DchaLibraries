package org.ksoap2.kobjects.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import org.ksoap2.kobjects.base64.Base64;

public class Decoder {
    String boundary;
    char[] buf;
    String characterEncoding;
    boolean consumed;
    boolean eof;
    Hashtable header;
    InputStream is;

    private final String readLine() throws IOException {
        int cnt = 0;
        while (true) {
            int i = this.is.read();
            if (i == -1 && cnt == 0) {
                return null;
            }
            if (i == -1 || i == 10) {
                break;
            }
            if (i != 13) {
                if (cnt >= this.buf.length) {
                    char[] tmp = new char[(this.buf.length * 3) / 2];
                    System.arraycopy(this.buf, 0, tmp, 0, this.buf.length);
                    this.buf = tmp;
                }
                this.buf[cnt] = (char) i;
                cnt++;
            }
        }
        return new String(this.buf, 0, cnt);
    }

    public static Hashtable getHeaderElements(String header) {
        int pos;
        int cut;
        String key = "";
        int pos2 = 0;
        Hashtable result = new Hashtable();
        int len = header.length();
        while (true) {
            if (pos2 < len && header.charAt(pos2) <= ' ') {
                pos2++;
            } else {
                if (pos2 >= len) {
                    break;
                }
                if (header.charAt(pos2) == '\"') {
                    int pos3 = pos2 + 1;
                    int cut2 = header.indexOf(34, pos3);
                    if (cut2 == -1) {
                        throw new RuntimeException("End quote expected in " + header);
                    }
                    result.put(key, header.substring(pos3, cut2));
                    pos = cut2 + 2;
                    if (pos < len) {
                        if (header.charAt(pos - 1) != ';') {
                            throw new RuntimeException("; expected in " + header);
                        }
                        cut = header.indexOf(61, pos);
                        if (cut != -1) {
                            break;
                        }
                        key = header.substring(pos, cut).toLowerCase().trim();
                        pos2 = cut + 1;
                    } else {
                        break;
                    }
                } else {
                    int cut3 = header.indexOf(59, pos2);
                    if (cut3 == -1) {
                        result.put(key, header.substring(pos2));
                        break;
                    }
                    result.put(key, header.substring(pos2, cut3));
                    pos = cut3 + 1;
                    cut = header.indexOf(61, pos);
                    if (cut != -1) {
                    }
                }
            }
        }
        return result;
    }

    public Decoder(InputStream is, String _bound) throws IOException {
        this(is, _bound, null);
    }

    public Decoder(InputStream is, String _bound, String characterEncoding) throws IOException {
        String line;
        this.buf = new char[256];
        this.characterEncoding = characterEncoding;
        this.is = is;
        this.boundary = "--" + _bound;
        do {
            line = readLine();
            if (line == null) {
                throw new IOException("Unexpected EOF");
            }
        } while (!line.startsWith(this.boundary));
        if (line.endsWith("--")) {
            this.eof = true;
            is.close();
        }
        this.consumed = true;
    }

    public Enumeration getHeaderNames() {
        return this.header.keys();
    }

    public String getHeader(String key) {
        return (String) this.header.get(key.toLowerCase());
    }

    public String readContent() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        readContent(bos);
        String result = this.characterEncoding == null ? new String(bos.toByteArray()) : new String(bos.toByteArray(), this.characterEncoding);
        System.out.println("Field content: '" + result + "'");
        return result;
    }

    public void readContent(OutputStream os) throws IOException {
        String line;
        if (this.consumed) {
            throw new RuntimeException("Content already consumed!");
        }
        getHeader("Content-Type");
        if ("base64".equals(getHeader("Content-Transfer-Encoding"))) {
            new ByteArrayOutputStream();
            while (true) {
                line = readLine();
                if (line == null) {
                    throw new IOException("Unexpected EOF");
                }
                if (line.startsWith(this.boundary)) {
                    break;
                } else {
                    Base64.decode(line, os);
                }
            }
        } else {
            String deli = "\r\n" + this.boundary;
            int match = 0;
            while (true) {
                int i = this.is.read();
                if (i == -1) {
                    throw new RuntimeException("Unexpected EOF");
                }
                if (((char) i) == deli.charAt(match)) {
                    match++;
                    if (match == deli.length()) {
                        line = readLine();
                        break;
                    }
                } else {
                    if (match > 0) {
                        for (int j = 0; j < match; j++) {
                            os.write((byte) deli.charAt(j));
                        }
                        match = ((char) i) == deli.charAt(0) ? 1 : 0;
                    }
                    if (match == 0) {
                        os.write((byte) i);
                    }
                }
            }
        }
        if (line.endsWith("--")) {
            this.eof = true;
        }
        this.consumed = true;
    }
}
