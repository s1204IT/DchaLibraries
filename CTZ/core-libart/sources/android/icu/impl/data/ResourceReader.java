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

    public ResourceReader(String str, String str2) throws UnsupportedEncodingException {
        this((Class<?>) ICUData.class, "data/" + str, str2);
    }

    public ResourceReader(String str) {
        this((Class<?>) ICUData.class, "data/" + str);
    }

    public ResourceReader(Class<?> cls, String str, String str2) throws UnsupportedEncodingException {
        this.reader = null;
        this.root = cls;
        this.resourceName = str;
        this.encoding = str2;
        this.lineNo = -1;
        _reset();
    }

    public ResourceReader(InputStream inputStream, String str, String str2) {
        InputStreamReader inputStreamReader;
        this.reader = null;
        this.root = null;
        this.resourceName = str;
        this.encoding = str2;
        this.lineNo = -1;
        try {
            if (str2 == null) {
                inputStreamReader = new InputStreamReader(inputStream);
            } else {
                inputStreamReader = new InputStreamReader(inputStream, str2);
            }
            this.reader = new BufferedReader(inputStreamReader);
            this.lineNo = 0;
        } catch (UnsupportedEncodingException e) {
        }
    }

    public ResourceReader(InputStream inputStream, String str) {
        this(inputStream, str, (String) null);
    }

    public ResourceReader(Class<?> cls, String str) {
        this.reader = null;
        this.root = cls;
        this.resourceName = str;
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

    public String readLineSkippingComments(boolean z) throws IOException {
        while (true) {
            String line = readLine();
            if (line == null) {
                return line;
            }
            int iSkipWhiteSpace = PatternProps.skipWhiteSpace(line, 0);
            if (iSkipWhiteSpace != line.length() && line.charAt(iSkipWhiteSpace) != '#') {
                return z ? line.substring(iSkipWhiteSpace) : line;
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
        InputStream stream = ICUData.getStream(this.root, this.resourceName);
        if (stream == null) {
            throw new IllegalArgumentException("Can't open " + this.resourceName);
        }
        this.reader = new BufferedReader(this.encoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, this.encoding));
        this.lineNo = 0;
    }

    @Override
    public void close() throws IOException {
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }
    }
}
