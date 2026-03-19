package com.android.okhttp;

import com.android.okhttp.internal.Util;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MultipartBuilder {
    private final ByteString boundary;
    private final List<RequestBody> partBodies;
    private final List<Headers> partHeaders;
    private MediaType type;
    public static final MediaType MIXED = MediaType.parse("multipart/mixed");
    public static final MediaType ALTERNATIVE = MediaType.parse("multipart/alternative");
    public static final MediaType DIGEST = MediaType.parse("multipart/digest");
    public static final MediaType PARALLEL = MediaType.parse("multipart/parallel");
    public static final MediaType FORM = MediaType.parse("multipart/form-data");
    private static final byte[] COLONSPACE = {58, 32};
    private static final byte[] CRLF = {13, 10};
    private static final byte[] DASHDASH = {45, 45};

    public MultipartBuilder() {
        this(UUID.randomUUID().toString());
    }

    public MultipartBuilder(String str) {
        this.type = MIXED;
        this.partHeaders = new ArrayList();
        this.partBodies = new ArrayList();
        this.boundary = ByteString.encodeUtf8(str);
    }

    public MultipartBuilder type(MediaType mediaType) {
        if (mediaType == null) {
            throw new NullPointerException("type == null");
        }
        if (!mediaType.type().equals("multipart")) {
            throw new IllegalArgumentException("multipart != " + mediaType);
        }
        this.type = mediaType;
        return this;
    }

    public MultipartBuilder addPart(RequestBody requestBody) {
        return addPart(null, requestBody);
    }

    public MultipartBuilder addPart(Headers headers, RequestBody requestBody) {
        if (requestBody == null) {
            throw new NullPointerException("body == null");
        }
        if (headers != null && headers.get("Content-Type") != null) {
            throw new IllegalArgumentException("Unexpected header: Content-Type");
        }
        if (headers != null && headers.get("Content-Length") != null) {
            throw new IllegalArgumentException("Unexpected header: Content-Length");
        }
        this.partHeaders.add(headers);
        this.partBodies.add(requestBody);
        return this;
    }

    private static StringBuilder appendQuotedString(StringBuilder sb, String str) {
        sb.append('\"');
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\n') {
                sb.append("%0A");
            } else if (cCharAt != '\r') {
                if (cCharAt == '\"') {
                    sb.append("%22");
                } else {
                    sb.append(cCharAt);
                }
            } else {
                sb.append("%0D");
            }
        }
        sb.append('\"');
        return sb;
    }

    public MultipartBuilder addFormDataPart(String str, String str2) {
        return addFormDataPart(str, null, RequestBody.create((MediaType) null, str2));
    }

    public MultipartBuilder addFormDataPart(String str, String str2, RequestBody requestBody) {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        StringBuilder sb = new StringBuilder("form-data; name=");
        appendQuotedString(sb, str);
        if (str2 != null) {
            sb.append("; filename=");
            appendQuotedString(sb, str2);
        }
        return addPart(Headers.of("Content-Disposition", sb.toString()), requestBody);
    }

    public RequestBody build() {
        if (this.partHeaders.isEmpty()) {
            throw new IllegalStateException("Multipart body must have at least one part.");
        }
        return new MultipartRequestBody(this.type, this.boundary, this.partHeaders, this.partBodies);
    }

    private static final class MultipartRequestBody extends RequestBody {
        private final ByteString boundary;
        private long contentLength = -1;
        private final MediaType contentType;
        private final List<RequestBody> partBodies;
        private final List<Headers> partHeaders;

        public MultipartRequestBody(MediaType mediaType, ByteString byteString, List<Headers> list, List<RequestBody> list2) {
            if (mediaType == null) {
                throw new NullPointerException("type == null");
            }
            this.boundary = byteString;
            this.contentType = MediaType.parse(mediaType + "; boundary=" + byteString.utf8());
            this.partHeaders = Util.immutableList(list);
            this.partBodies = Util.immutableList(list2);
        }

        @Override
        public MediaType contentType() {
            return this.contentType;
        }

        @Override
        public long contentLength() throws IOException {
            long j = this.contentLength;
            if (j != -1) {
                return j;
            }
            long jWriteOrCountBytes = writeOrCountBytes(null, true);
            this.contentLength = jWriteOrCountBytes;
            return jWriteOrCountBytes;
        }

        private long writeOrCountBytes(BufferedSink bufferedSink, boolean z) throws IOException {
            Buffer buffer;
            if (z) {
                bufferedSink = new Buffer();
                buffer = bufferedSink;
            } else {
                buffer = 0;
            }
            int size = this.partHeaders.size();
            long j = 0;
            for (int i = 0; i < size; i++) {
                Headers headers = this.partHeaders.get(i);
                RequestBody requestBody = this.partBodies.get(i);
                bufferedSink.write(MultipartBuilder.DASHDASH);
                bufferedSink.write(this.boundary);
                bufferedSink.write(MultipartBuilder.CRLF);
                if (headers != null) {
                    int size2 = headers.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        bufferedSink.writeUtf8(headers.name(i2)).write(MultipartBuilder.COLONSPACE).writeUtf8(headers.value(i2)).write(MultipartBuilder.CRLF);
                    }
                }
                MediaType mediaTypeContentType = requestBody.contentType();
                if (mediaTypeContentType != null) {
                    bufferedSink.writeUtf8("Content-Type: ").writeUtf8(mediaTypeContentType.toString()).write(MultipartBuilder.CRLF);
                }
                long jContentLength = requestBody.contentLength();
                if (jContentLength != -1) {
                    bufferedSink.writeUtf8("Content-Length: ").writeDecimalLong(jContentLength).write(MultipartBuilder.CRLF);
                } else if (z) {
                    buffer.clear();
                    return -1L;
                }
                bufferedSink.write(MultipartBuilder.CRLF);
                if (z) {
                    j += jContentLength;
                } else {
                    this.partBodies.get(i).writeTo(bufferedSink);
                }
                bufferedSink.write(MultipartBuilder.CRLF);
            }
            bufferedSink.write(MultipartBuilder.DASHDASH);
            bufferedSink.write(this.boundary);
            bufferedSink.write(MultipartBuilder.DASHDASH);
            bufferedSink.write(MultipartBuilder.CRLF);
            if (z) {
                long size3 = j + buffer.size();
                buffer.clear();
                return size3;
            }
            return j;
        }

        @Override
        public void writeTo(BufferedSink bufferedSink) throws IOException {
            writeOrCountBytes(bufferedSink, false);
        }
    }
}
