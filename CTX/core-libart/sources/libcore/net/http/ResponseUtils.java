package libcore.net.http;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {
    public static Charset responseCharset(String str) throws IllegalCharsetNameException, UnsupportedCharsetException {
        String str2;
        Charset charset = StandardCharsets.UTF_8;
        if (str != null && (str2 = parseContentTypeParameters(str).get("charset")) != null) {
            return Charset.forName(str2);
        }
        return charset;
    }

    private static Map<String, String> parseContentTypeParameters(String str) {
        Map<String, String> map = Collections.EMPTY_MAP;
        String[] strArrSplit = str.split(";");
        if (strArrSplit.length > 1) {
            map = new HashMap<>();
            for (int i = 1; i < strArrSplit.length; i++) {
                String str2 = strArrSplit[i];
                if (!str2.isEmpty()) {
                    String[] strArrSplit2 = str2.split("=");
                    if (strArrSplit2.length == 2) {
                        String lowerCase = strArrSplit2[0].trim().toLowerCase();
                        String strTrim = strArrSplit2[1].trim();
                        if (!lowerCase.isEmpty() && !strTrim.isEmpty()) {
                            map.put(lowerCase, strTrim);
                        }
                    }
                }
            }
        }
        return map;
    }
}
