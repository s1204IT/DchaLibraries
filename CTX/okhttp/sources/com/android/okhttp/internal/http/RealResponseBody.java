package com.android.okhttp.internal.http;

import com.android.okhttp.Headers;
import com.android.okhttp.MediaType;
import com.android.okhttp.ResponseBody;
import com.android.okhttp.okio.BufferedSource;

public final class RealResponseBody extends ResponseBody {
    private final Headers headers;
    private final BufferedSource source;

    public RealResponseBody(Headers headers, BufferedSource bufferedSource) {
        this.headers = headers;
        this.source = bufferedSource;
    }

    @Override
    public MediaType contentType() {
        String str = this.headers.get("Content-Type");
        if (str != null) {
            return MediaType.parse(str);
        }
        return null;
    }

    @Override
    public long contentLength() {
        return OkHeaders.contentLength(this.headers);
    }

    @Override
    public BufferedSource source() {
        return this.source;
    }
}
