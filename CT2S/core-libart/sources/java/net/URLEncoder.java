package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import libcore.net.UriCodec;

public class URLEncoder {
    static UriCodec ENCODER = new UriCodec() {
        @Override
        protected boolean isRetained(char c) {
            return " .-*_".indexOf(c) != -1;
        }
    };

    private URLEncoder() {
    }

    @Deprecated
    public static String encode(String s) {
        return ENCODER.encode(s, StandardCharsets.UTF_8);
    }

    public static String encode(String s, String charsetName) throws UnsupportedEncodingException {
        return ENCODER.encode(s, Charset.forName(charsetName));
    }
}
