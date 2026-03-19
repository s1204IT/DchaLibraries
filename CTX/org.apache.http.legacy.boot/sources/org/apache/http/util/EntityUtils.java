package org.apache.http.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;

@Deprecated
public final class EntityUtils {
    private EntityUtils() {
    }

    public static byte[] toByteArray(HttpEntity httpEntity) throws IOException {
        if (httpEntity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream content = httpEntity.getContent();
        if (content == null) {
            return new byte[0];
        }
        if (httpEntity.getContentLength() > 2147483647L) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        int contentLength = (int) httpEntity.getContentLength();
        if (contentLength < 0) {
            contentLength = 4096;
        }
        ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer(contentLength);
        try {
            byte[] bArr = new byte[4096];
            while (true) {
                int i = content.read(bArr);
                if (i != -1) {
                    byteArrayBuffer.append(bArr, 0, i);
                } else {
                    content.close();
                    return byteArrayBuffer.toByteArray();
                }
            }
        } catch (Throwable th) {
            content.close();
            throw th;
        }
    }

    public static String getContentCharSet(HttpEntity httpEntity) throws ParseException {
        NameValuePair parameterByName;
        if (httpEntity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        if (httpEntity.getContentType() == null) {
            return null;
        }
        HeaderElement[] elements = httpEntity.getContentType().getElements();
        if (elements.length <= 0 || (parameterByName = elements[0].getParameterByName("charset")) == null) {
            return null;
        }
        return parameterByName.getValue();
    }

    public static String toString(HttpEntity httpEntity, String str) throws ParseException, IOException {
        if (httpEntity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream content = httpEntity.getContent();
        if (content == null) {
            return "";
        }
        if (httpEntity.getContentLength() > 2147483647L) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        int contentLength = (int) httpEntity.getContentLength();
        if (contentLength < 0) {
            contentLength = 4096;
        }
        String contentCharSet = getContentCharSet(httpEntity);
        if (contentCharSet == null) {
            contentCharSet = str;
        }
        if (contentCharSet == null) {
            contentCharSet = "ISO-8859-1";
        }
        InputStreamReader inputStreamReader = new InputStreamReader(content, contentCharSet);
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(contentLength);
        try {
            char[] cArr = new char[1024];
            while (true) {
                int i = inputStreamReader.read(cArr);
                if (i != -1) {
                    charArrayBuffer.append(cArr, 0, i);
                } else {
                    inputStreamReader.close();
                    return charArrayBuffer.toString();
                }
            }
        } catch (Throwable th) {
            inputStreamReader.close();
            throw th;
        }
    }

    public static String toString(HttpEntity httpEntity) throws ParseException, IOException {
        return toString(httpEntity, null);
    }
}
