package com.android.internal.http.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.util.EncodingUtils;

public abstract class Part {
    protected static final String CHARSET = "; charset=";
    private byte[] boundaryBytes;
    private static final Log LOG = LogFactory.getLog(Part.class);
    protected static final String BOUNDARY = "----------------314159265358979323846";
    protected static final byte[] BOUNDARY_BYTES = EncodingUtils.getAsciiBytes(BOUNDARY);
    private static final byte[] DEFAULT_BOUNDARY_BYTES = BOUNDARY_BYTES;
    protected static final String CRLF = "\r\n";
    protected static final byte[] CRLF_BYTES = EncodingUtils.getAsciiBytes(CRLF);
    protected static final String QUOTE = "\"";
    protected static final byte[] QUOTE_BYTES = EncodingUtils.getAsciiBytes(QUOTE);
    protected static final String EXTRA = "--";
    protected static final byte[] EXTRA_BYTES = EncodingUtils.getAsciiBytes(EXTRA);
    protected static final String CONTENT_DISPOSITION = "Content-Disposition: form-data; name=";
    protected static final byte[] CONTENT_DISPOSITION_BYTES = EncodingUtils.getAsciiBytes(CONTENT_DISPOSITION);
    protected static final String CONTENT_TYPE = "Content-Type: ";
    protected static final byte[] CONTENT_TYPE_BYTES = EncodingUtils.getAsciiBytes(CONTENT_TYPE);
    protected static final byte[] CHARSET_BYTES = EncodingUtils.getAsciiBytes("; charset=");
    protected static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding: ";
    protected static final byte[] CONTENT_TRANSFER_ENCODING_BYTES = EncodingUtils.getAsciiBytes(CONTENT_TRANSFER_ENCODING);

    public abstract String getCharSet();

    public abstract String getContentType();

    public abstract String getName();

    public abstract String getTransferEncoding();

    protected abstract long lengthOfData() throws IOException;

    protected abstract void sendData(OutputStream outputStream) throws IOException;

    public static String getBoundary() {
        return BOUNDARY;
    }

    protected byte[] getPartBoundary() {
        if (this.boundaryBytes == null) {
            return DEFAULT_BOUNDARY_BYTES;
        }
        return this.boundaryBytes;
    }

    void setPartBoundary(byte[] bArr) {
        this.boundaryBytes = bArr;
    }

    public boolean isRepeatable() {
        return true;
    }

    protected void sendStart(OutputStream outputStream) throws IOException {
        LOG.trace("enter sendStart(OutputStream out)");
        outputStream.write(EXTRA_BYTES);
        outputStream.write(getPartBoundary());
        outputStream.write(CRLF_BYTES);
    }

    protected void sendDispositionHeader(OutputStream outputStream) throws IOException {
        LOG.trace("enter sendDispositionHeader(OutputStream out)");
        outputStream.write(CONTENT_DISPOSITION_BYTES);
        outputStream.write(QUOTE_BYTES);
        outputStream.write(EncodingUtils.getAsciiBytes(getName()));
        outputStream.write(QUOTE_BYTES);
    }

    protected void sendContentTypeHeader(OutputStream outputStream) throws IOException {
        LOG.trace("enter sendContentTypeHeader(OutputStream out)");
        String contentType = getContentType();
        if (contentType != null) {
            outputStream.write(CRLF_BYTES);
            outputStream.write(CONTENT_TYPE_BYTES);
            outputStream.write(EncodingUtils.getAsciiBytes(contentType));
            String charSet = getCharSet();
            if (charSet != null) {
                outputStream.write(CHARSET_BYTES);
                outputStream.write(EncodingUtils.getAsciiBytes(charSet));
            }
        }
    }

    protected void sendTransferEncodingHeader(OutputStream outputStream) throws IOException {
        LOG.trace("enter sendTransferEncodingHeader(OutputStream out)");
        String transferEncoding = getTransferEncoding();
        if (transferEncoding != null) {
            outputStream.write(CRLF_BYTES);
            outputStream.write(CONTENT_TRANSFER_ENCODING_BYTES);
            outputStream.write(EncodingUtils.getAsciiBytes(transferEncoding));
        }
    }

    protected void sendEndOfHeader(OutputStream outputStream) throws IOException {
        LOG.trace("enter sendEndOfHeader(OutputStream out)");
        outputStream.write(CRLF_BYTES);
        outputStream.write(CRLF_BYTES);
    }

    protected void sendEnd(OutputStream outputStream) throws IOException {
        LOG.trace("enter sendEnd(OutputStream out)");
        outputStream.write(CRLF_BYTES);
    }

    public void send(OutputStream outputStream) throws IOException {
        LOG.trace("enter send(OutputStream out)");
        sendStart(outputStream);
        sendDispositionHeader(outputStream);
        sendContentTypeHeader(outputStream);
        sendTransferEncodingHeader(outputStream);
        sendEndOfHeader(outputStream);
        sendData(outputStream);
        sendEnd(outputStream);
    }

    public long length() throws IOException {
        LOG.trace("enter length()");
        if (lengthOfData() < 0) {
            return -1L;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        sendStart(byteArrayOutputStream);
        sendDispositionHeader(byteArrayOutputStream);
        sendContentTypeHeader(byteArrayOutputStream);
        sendTransferEncodingHeader(byteArrayOutputStream);
        sendEndOfHeader(byteArrayOutputStream);
        sendEnd(byteArrayOutputStream);
        return ((long) byteArrayOutputStream.size()) + lengthOfData();
    }

    public String toString() {
        return getName();
    }

    public static void sendParts(OutputStream outputStream, Part[] partArr) throws IOException {
        sendParts(outputStream, partArr, DEFAULT_BOUNDARY_BYTES);
    }

    public static void sendParts(OutputStream outputStream, Part[] partArr, byte[] bArr) throws IOException {
        if (partArr == null) {
            throw new IllegalArgumentException("Parts may not be null");
        }
        if (bArr == null || bArr.length == 0) {
            throw new IllegalArgumentException("partBoundary may not be empty");
        }
        for (int i = 0; i < partArr.length; i++) {
            partArr[i].setPartBoundary(bArr);
            partArr[i].send(outputStream);
        }
        outputStream.write(EXTRA_BYTES);
        outputStream.write(bArr);
        outputStream.write(EXTRA_BYTES);
        outputStream.write(CRLF_BYTES);
    }

    public static long getLengthOfParts(Part[] partArr) throws IOException {
        return getLengthOfParts(partArr, DEFAULT_BOUNDARY_BYTES);
    }

    public static long getLengthOfParts(Part[] partArr, byte[] bArr) throws IOException {
        LOG.trace("getLengthOfParts(Parts[])");
        if (partArr == null) {
            throw new IllegalArgumentException("Parts may not be null");
        }
        long j = 0;
        for (int i = 0; i < partArr.length; i++) {
            partArr[i].setPartBoundary(bArr);
            long length = partArr[i].length();
            if (length < 0) {
                return -1L;
            }
            j += length;
        }
        return j + ((long) EXTRA_BYTES.length) + ((long) bArr.length) + ((long) EXTRA_BYTES.length) + ((long) CRLF_BYTES.length);
    }
}
