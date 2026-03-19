package org.apache.http.conn.ssl;

import javax.net.ssl.SSLException;

@Deprecated
public class StrictHostnameVerifier extends AbstractVerifier {
    @Override
    public final void verify(String str, String[] strArr, String[] strArr2) throws SSLException {
        verify(str, strArr, strArr2, true);
    }

    public final String toString() {
        return "STRICT";
    }
}
