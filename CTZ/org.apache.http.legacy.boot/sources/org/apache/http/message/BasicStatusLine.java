package org.apache.http.message;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BasicStatusLine implements StatusLine, Cloneable {
    private final ProtocolVersion protoVersion;
    private final String reasonPhrase;
    private final int statusCode;

    public BasicStatusLine(ProtocolVersion protocolVersion, int i, String str) {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("Protocol version may not be null.");
        }
        if (i < 0) {
            throw new IllegalArgumentException("Status code may not be negative.");
        }
        this.protoVersion = protocolVersion;
        this.statusCode = i;
        this.reasonPhrase = str;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return this.protoVersion;
    }

    @Override
    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    public String toString() {
        return BasicLineFormatter.DEFAULT.formatStatusLine((CharArrayBuffer) null, this).toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
