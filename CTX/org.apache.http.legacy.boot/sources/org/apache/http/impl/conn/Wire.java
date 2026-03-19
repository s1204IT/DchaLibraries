package org.apache.http.impl.conn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.Log;

@Deprecated
public class Wire {
    private final Log log;

    public Wire(Log log) {
        this.log = log;
    }

    private void wire(String str, InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int i = inputStream.read();
            if (i == -1) {
                break;
            }
            if (i == 13) {
                sb.append("[\\r]");
            } else if (i == 10) {
                sb.append("[\\n]\"");
                sb.insert(0, "\"");
                sb.insert(0, str);
                this.log.debug(sb.toString());
                sb.setLength(0);
            } else if (i < 32 || i > 127) {
                sb.append("[0x");
                sb.append(Integer.toHexString(i));
                sb.append("]");
            } else {
                sb.append((char) i);
            }
        }
        if (sb.length() > 0) {
            sb.append('\"');
            sb.insert(0, '\"');
            sb.insert(0, str);
            this.log.debug(sb.toString());
        }
    }

    public boolean enabled() {
        return this.log.isDebugEnabled();
    }

    public void output(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Output may not be null");
        }
        wire(">> ", inputStream);
    }

    public void input(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input may not be null");
        }
        wire("<< ", inputStream);
    }

    public void output(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new IllegalArgumentException("Output may not be null");
        }
        wire(">> ", new ByteArrayInputStream(bArr, i, i2));
    }

    public void input(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new IllegalArgumentException("Input may not be null");
        }
        wire("<< ", new ByteArrayInputStream(bArr, i, i2));
    }

    public void output(byte[] bArr) throws IOException {
        if (bArr == null) {
            throw new IllegalArgumentException("Output may not be null");
        }
        wire(">> ", new ByteArrayInputStream(bArr));
    }

    public void input(byte[] bArr) throws IOException {
        if (bArr == null) {
            throw new IllegalArgumentException("Input may not be null");
        }
        wire("<< ", new ByteArrayInputStream(bArr));
    }

    public void output(int i) throws IOException {
        output(new byte[]{(byte) i});
    }

    public void input(int i) throws IOException {
        input(new byte[]{(byte) i});
    }

    public void output(String str) throws IOException {
        if (str == null) {
            throw new IllegalArgumentException("Output may not be null");
        }
        output(str.getBytes());
    }

    public void input(String str) throws IOException {
        if (str == null) {
            throw new IllegalArgumentException("Input may not be null");
        }
        input(str.getBytes());
    }
}
