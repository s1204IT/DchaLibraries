package com.android.okhttp;

import com.android.okhttp.internal.Util;
import com.android.okio.ByteString;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public enum Protocol {
    HTTP_2("HTTP-draft-09/2.0", true),
    SPDY_3("spdy/3.1", true),
    HTTP_11("http/1.1", false);

    public final ByteString name;
    public final boolean spdyVariant;
    public static final List<Protocol> HTTP2_SPDY3_AND_HTTP = Util.immutableList(Arrays.asList(HTTP_2, SPDY_3, HTTP_11));
    public static final List<Protocol> SPDY3_AND_HTTP11 = Util.immutableList(Arrays.asList(SPDY_3, HTTP_11));
    public static final List<Protocol> HTTP2_AND_HTTP_11 = Util.immutableList(Arrays.asList(HTTP_2, HTTP_11));

    Protocol(String name, boolean spdyVariant) {
        this.name = ByteString.encodeUtf8(name);
        this.spdyVariant = spdyVariant;
    }

    public static Protocol find(ByteString input) throws IOException {
        if (input == null) {
            return HTTP_11;
        }
        Protocol[] arr$ = values();
        for (Protocol protocol : arr$) {
            if (protocol.name.equals(input)) {
                return protocol;
            }
        }
        throw new IOException("Unexpected protocol: " + input.utf8());
    }
}
