package com.android.internal.http.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;

public class MultipartEntity extends AbstractHttpEntity {
    public static final String MULTIPART_BOUNDARY = "http.method.multipart.boundary";
    private static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";
    private boolean contentConsumed = false;
    private byte[] multipartBoundary;
    private HttpParams params;
    protected Part[] parts;
    private static final Log log = LogFactory.getLog(MultipartEntity.class);
    private static byte[] MULTIPART_CHARS = EncodingUtils.getAsciiBytes("-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");

    private static byte[] generateMultipartBoundary() {
        Random random = new Random();
        byte[] bArr = new byte[random.nextInt(11) + 30];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = MULTIPART_CHARS[random.nextInt(MULTIPART_CHARS.length)];
        }
        return bArr;
    }

    public MultipartEntity(Part[] partArr, HttpParams httpParams) {
        if (partArr == null) {
            throw new IllegalArgumentException("parts cannot be null");
        }
        if (httpParams == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.parts = partArr;
        this.params = httpParams;
    }

    public MultipartEntity(Part[] partArr) {
        setContentType(MULTIPART_FORM_CONTENT_TYPE);
        if (partArr == null) {
            throw new IllegalArgumentException("parts cannot be null");
        }
        this.parts = partArr;
        this.params = null;
    }

    protected byte[] getMultipartBoundary() {
        if (this.multipartBoundary == null) {
            String str = null;
            if (this.params != null) {
                str = (String) this.params.getParameter(MULTIPART_BOUNDARY);
            }
            if (str != null) {
                this.multipartBoundary = EncodingUtils.getAsciiBytes(str);
            } else {
                this.multipartBoundary = generateMultipartBoundary();
            }
        }
        return this.multipartBoundary;
    }

    @Override
    public boolean isRepeatable() {
        for (int i = 0; i < this.parts.length; i++) {
            if (!this.parts[i].isRepeatable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        Part.sendParts(outputStream, this.parts, getMultipartBoundary());
    }

    @Override
    public Header getContentType() {
        StringBuffer stringBuffer = new StringBuffer(MULTIPART_FORM_CONTENT_TYPE);
        stringBuffer.append("; boundary=");
        stringBuffer.append(EncodingUtils.getAsciiString(getMultipartBoundary()));
        return new BasicHeader(HTTP.CONTENT_TYPE, stringBuffer.toString());
    }

    @Override
    public long getContentLength() {
        try {
            return Part.getLengthOfParts(this.parts, getMultipartBoundary());
        } catch (Exception e) {
            log.error("An exception occurred while getting the length of the parts", e);
            return 0L;
        }
    }

    @Override
    public InputStream getContent() throws IllegalStateException, IOException {
        if (!isRepeatable() && this.contentConsumed) {
            throw new IllegalStateException("Content has been consumed");
        }
        this.contentConsumed = true;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Part.sendParts(byteArrayOutputStream, this.parts, this.multipartBoundary);
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    @Override
    public boolean isStreaming() {
        return false;
    }
}
