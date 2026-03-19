package mf.org.apache.xerces.impl.dv.xs;

import java.io.UnsupportedEncodingException;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.URI;

public class AnyURIDV extends TypeValidator {
    private static final URI BASE_URI;
    private static char[] gAfterEscaping1;
    private static char[] gAfterEscaping2;
    private static char[] gHexChs;
    private static boolean[] gNeedEscaping;

    static {
        URI uri = null;
        try {
            uri = new URI("abc://def.ghi.jkl");
        } catch (URI.MalformedURIException e) {
        }
        BASE_URI = uri;
        gNeedEscaping = new boolean[128];
        gAfterEscaping1 = new char[128];
        gAfterEscaping2 = new char[128];
        gHexChs = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (int i = 0; i <= 31; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> 4];
            gAfterEscaping2[i] = gHexChs[i & 15];
        }
        gNeedEscaping[127] = true;
        gAfterEscaping1[127] = '7';
        gAfterEscaping2[127] = 'F';
        char[] escChs = {' ', '<', '>', '\"', '{', '}', '|', '\\', '^', '~', '`'};
        for (char ch : escChs) {
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 15];
        }
    }

    @Override
    public short getAllowedFacets() {
        return (short) 2079;
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try {
            if (content.length() != 0) {
                String encoded = encode(content);
                new URI(BASE_URI, encoded);
            }
            return content;
        } catch (URI.MalformedURIException e) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, SchemaSymbols.ATTVAL_ANYURI});
        }
    }

    private static String encode(String anyURI) {
        int ch;
        int len = anyURI.length();
        StringBuffer buffer = new StringBuffer(len * 3);
        int i = 0;
        while (i < len && (ch = anyURI.charAt(i)) < 128) {
            if (gNeedEscaping[ch]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[ch]);
                buffer.append(gAfterEscaping2[ch]);
            } else {
                buffer.append((char) ch);
            }
            i++;
        }
        if (i < len) {
            try {
                byte[] bytes = anyURI.substring(i).getBytes("UTF-8");
                int len2 = bytes.length;
                for (byte b : bytes) {
                    if (b < 0) {
                        int ch2 = b + 256;
                        buffer.append('%');
                        buffer.append(gHexChs[ch2 >> 4]);
                        buffer.append(gHexChs[ch2 & 15]);
                    } else if (gNeedEscaping[b]) {
                        buffer.append('%');
                        buffer.append(gAfterEscaping1[b]);
                        buffer.append(gAfterEscaping2[b]);
                    } else {
                        buffer.append((char) b);
                    }
                }
                len = len2;
            } catch (UnsupportedEncodingException e) {
                return anyURI;
            }
        }
        if (buffer.length() != len) {
            return buffer.toString();
        }
        return anyURI;
    }
}
