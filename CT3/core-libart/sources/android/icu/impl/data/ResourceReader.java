package android.icu.impl.data;

import android.icu.impl.ICUData;
import android.icu.impl.PatternProps;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class ResourceReader implements Closeable {
    private String encoding;
    private int lineNo;
    private BufferedReader reader;
    private String resourceName;
    private Class<?> root;

    public ResourceReader(String resourceName, String encoding) throws UnsupportedEncodingException {
        this((Class<?>) ICUData.class, "data/" + resourceName, encoding);
    }

    public ResourceReader(String resourceName) {
        this((Class<?>) ICUData.class, "data/" + resourceName);
    }

    public ResourceReader(Class<?> rootClass, String resourceName, String encoding) throws UnsupportedEncodingException {
        this.reader = null;
        this.root = rootClass;
        this.resourceName = resourceName;
        this.encoding = encoding;
        this.lineNo = -1;
        _reset();
    }

    public ResourceReader(InputStream is, String resourceName, String encoding) {
        InputStreamReader isr;
        this.reader = null;
        this.root = null;
        this.resourceName = resourceName;
        this.encoding = encoding;
        this.lineNo = -1;
        try {
            if (encoding == null) {
                isr = new InputStreamReader(is);
            } else {
                isr = new InputStreamReader(is, encoding);
            }
            this.reader = new BufferedReader(isr);
            this.lineNo = 0;
        } catch (UnsupportedEncodingException e) {
        }
    }

    public ResourceReader(InputStream is, String resourceName) {
        this(is, resourceName, (String) null);
    }

    public ResourceReader(Class<?> rootClass, String resourceName) {
        this.reader = null;
        this.root = rootClass;
        this.resourceName = resourceName;
        this.encoding = null;
        this.lineNo = -1;
        try {
            _reset();
        } catch (UnsupportedEncodingException e) {
        }
    }

    public String readLine() throws IOException {
        if (this.lineNo == 0) {
            this.lineNo++;
            String line = this.reader.readLine();
            if (line != null) {
                if (line.charAt(0) == 65519 || line.charAt(0) == 65279) {
                    return line.substring(1);
                }
                return line;
            }
            return line;
        }
        this.lineNo++;
        return this.reader.readLine();
    }

    public String readLineSkippingComments(boolean trim) throws IOException {
        while (true) {
            String line = readLine();
            if (line == null) {
                return line;
            }
            int pos = PatternProps.skipWhiteSpace(line, 0);
            if (pos != line.length() && line.charAt(pos) != '#') {
                return trim ? line.substring(pos) : line;
            }
        }
    }

    public String readLineSkippingComments() throws IOException {
        return readLineSkippingComments(false);
    }

    public int getLineNumber() {
        return this.lineNo;
    }

    public String describePosition() {
        return this.resourceName + ':' + this.lineNo;
    }

    public void reset() {
        try {
            _reset();
        } catch (UnsupportedEncodingException e) {
        }
    }

    private void _reset() throws UnsupportedEncodingException {
        try {
            close();
        } catch (IOException e) {
        }
        if (this.lineNo == 0) {
            return;
        }
        InputStream is = ICUData.getStream(this.root, this.resourceName);
        if (is == null) {
            throw new IllegalArgumentException("Can't open " + this.resourceName);
        }
        InputStreamReader isr = this.encoding == null ? new InputStreamReader(is) : new InputStreamReader(is, this.encoding);
        this.reader = new BufferedReader(isr);
        this.lineNo = 0;
    }

    @Override
    public void close() throws IOException {
        if (this.reader == null) {
            return;
        }
        this.reader.close();
        this.reader = null;
    }
}
