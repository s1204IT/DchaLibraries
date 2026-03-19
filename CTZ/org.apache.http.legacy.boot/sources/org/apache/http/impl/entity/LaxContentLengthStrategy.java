package org.apache.http.impl.entity;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

@Deprecated
public class LaxContentLengthStrategy implements ContentLengthStrategy {
    @Override
    public long determineLength(HttpMessage httpMessage) throws HttpException {
        long j;
        if (httpMessage == null) {
            throw new IllegalArgumentException("HTTP message may not be null");
        }
        boolean zIsParameterTrue = httpMessage.getParams().isParameterTrue(CoreProtocolPNames.STRICT_TRANSFER_ENCODING);
        Header firstHeader = httpMessage.getFirstHeader(HTTP.TRANSFER_ENCODING);
        Header firstHeader2 = httpMessage.getFirstHeader(HTTP.CONTENT_LEN);
        if (firstHeader != null) {
            try {
                HeaderElement[] elements = firstHeader.getElements();
                if (zIsParameterTrue) {
                    for (HeaderElement headerElement : elements) {
                        String name = headerElement.getName();
                        if (name != null && name.length() > 0 && !name.equalsIgnoreCase(HTTP.CHUNK_CODING) && !name.equalsIgnoreCase(HTTP.IDENTITY_CODING)) {
                            throw new ProtocolException("Unsupported transfer encoding: " + name);
                        }
                    }
                }
                int length = elements.length;
                if (HTTP.IDENTITY_CODING.equalsIgnoreCase(firstHeader.getValue())) {
                    return -1L;
                }
                if (length > 0 && HTTP.CHUNK_CODING.equalsIgnoreCase(elements[length - 1].getName())) {
                    return -2L;
                }
                if (zIsParameterTrue) {
                    throw new ProtocolException("Chunk-encoding must be the last one applied");
                }
                return -1L;
            } catch (ParseException e) {
                throw new ProtocolException("Invalid Transfer-Encoding header value: " + firstHeader, e);
            }
        }
        if (firstHeader2 == null) {
            return -1L;
        }
        Header[] headers = httpMessage.getHeaders(HTTP.CONTENT_LEN);
        if (!zIsParameterTrue || headers.length <= 1) {
            int length2 = headers.length - 1;
            while (true) {
                if (length2 >= 0) {
                    Header header = headers[length2];
                    try {
                        j = Long.parseLong(header.getValue());
                        break;
                    } catch (NumberFormatException e2) {
                        if (zIsParameterTrue) {
                            throw new ProtocolException("Invalid content length: " + header.getValue());
                        }
                        length2--;
                    }
                } else {
                    j = -1;
                    break;
                }
            }
            if (j >= 0) {
                return j;
            }
            return -1L;
        }
        throw new ProtocolException("Multiple content length headers");
    }
}
