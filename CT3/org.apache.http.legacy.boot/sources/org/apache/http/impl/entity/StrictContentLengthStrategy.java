package org.apache.http.impl.entity;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.protocol.HTTP;

@Deprecated
public class StrictContentLengthStrategy implements ContentLengthStrategy {
    @Override
    public long determineLength(HttpMessage message) throws HttpException {
        if (message == null) {
            throw new IllegalArgumentException("HTTP message may not be null");
        }
        Header transferEncodingHeader = message.getFirstHeader(HTTP.TRANSFER_ENCODING);
        Header contentLengthHeader = message.getFirstHeader(HTTP.CONTENT_LEN);
        if (transferEncodingHeader != null) {
            String s = transferEncodingHeader.getValue();
            if (HTTP.CHUNK_CODING.equalsIgnoreCase(s)) {
                if (message.getProtocolVersion().lessEquals(HttpVersion.HTTP_1_0)) {
                    throw new ProtocolException("Chunked transfer encoding not allowed for " + message.getProtocolVersion());
                }
                return -2L;
            }
            if (HTTP.IDENTITY_CODING.equalsIgnoreCase(s)) {
                return -1L;
            }
            throw new ProtocolException("Unsupported transfer encoding: " + s);
        }
        if (contentLengthHeader == null) {
            return -1L;
        }
        String s2 = contentLengthHeader.getValue();
        try {
            long len = Long.parseLong(s2);
            return len;
        } catch (NumberFormatException e) {
            throw new ProtocolException("Invalid content length: " + s2);
        }
    }
}
