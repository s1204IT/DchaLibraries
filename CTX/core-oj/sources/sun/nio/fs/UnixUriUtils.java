package sun.nio.fs;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;

class UnixUriUtils {
    static final boolean $assertionsDisabled = false;
    private static final long H_DIGIT = 0;
    private static final long L_ALPHA = 0;
    private static final long L_LOWALPHA = 0;
    private static final long L_UPALPHA = 0;
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long H_UPALPHA = highMask('A', 'Z');
    private static final long H_LOWALPHA = highMask('a', 'z');
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;
    private static final long L_ALPHANUM = L_DIGIT | 0;
    private static final long H_ALPHANUM = H_ALPHA | 0;
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long H_MARK = highMask("-_.!~*'()");
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;
    private static final long L_PCHAR = L_UNRESERVED | lowMask(":@&=+$,");
    private static final long H_PCHAR = H_UNRESERVED | highMask(":@&=+$,");
    private static final long L_PATH = L_PCHAR | lowMask(";/");
    private static final long H_PATH = H_PCHAR | highMask(";/");
    private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private UnixUriUtils() {
    }

    static Path fromUri(UnixFileSystem unixFileSystem, URI uri) {
        byte bDecode;
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        if (uri.isOpaque()) {
            throw new IllegalArgumentException("URI is not hierarchical");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("file")) {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
        if (uri.getAuthority() != null) {
            throw new IllegalArgumentException("URI has an authority component");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("URI has a fragment component");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("URI has a query component");
        }
        if (!uri.toString().startsWith("file:///")) {
            return new File(uri).toPath();
        }
        String rawPath = uri.getRawPath();
        int length = rawPath.length();
        if (length == 0) {
            throw new IllegalArgumentException("URI path component is empty");
        }
        if (rawPath.endsWith("/") && length > 1) {
            length--;
        }
        byte[] bArrCopyOf = new byte[length];
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = i + 1;
            char cCharAt = rawPath.charAt(i);
            if (cCharAt == '%') {
                int i4 = i3 + 1;
                char cCharAt2 = rawPath.charAt(i3);
                int i5 = i4 + 1;
                bDecode = (byte) (decode(rawPath.charAt(i4)) | (decode(cCharAt2) << 4));
                if (bDecode == 0) {
                    throw new IllegalArgumentException("Nul character not allowed");
                }
                i3 = i5;
            } else {
                bDecode = (byte) cCharAt;
            }
            bArrCopyOf[i2] = bDecode;
            i = i3;
            i2++;
        }
        if (i2 != bArrCopyOf.length) {
            bArrCopyOf = Arrays.copyOf(bArrCopyOf, i2);
        }
        return new UnixPath(unixFileSystem, bArrCopyOf);
    }

    static URI toUri(UnixPath unixPath) {
        byte[] bArrAsByteArray = unixPath.toAbsolutePath().asByteArray();
        StringBuilder sb = new StringBuilder("file:///");
        for (int i = 1; i < bArrAsByteArray.length; i++) {
            char c = (char) (bArrAsByteArray[i] & Character.DIRECTIONALITY_UNDEFINED);
            if (match(c, L_PATH, H_PATH)) {
                sb.append(c);
            } else {
                sb.append('%');
                sb.append(hexDigits[(c >> 4) & 15]);
                sb.append(hexDigits[c & 15]);
            }
        }
        if (sb.charAt(sb.length() - 1) != '/') {
            try {
                if (UnixFileAttributes.get(unixPath, true).isDirectory()) {
                    sb.append('/');
                }
            } catch (UnixException e) {
            }
        }
        try {
            return new URI(sb.toString());
        } catch (URISyntaxException e2) {
            throw new AssertionError(e2);
        }
    }

    private static long lowMask(String str) {
        int length = str.length();
        long j = 0;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < '@') {
                j |= 1 << cCharAt;
            }
        }
        return j;
    }

    private static long highMask(String str) {
        int length = str.length();
        long j = 0;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt >= '@' && cCharAt < 128) {
                j |= 1 << (cCharAt - '@');
            }
        }
        return j;
    }

    private static long lowMask(char c, char c2) {
        long j = 0;
        for (int iMax = Math.max(Math.min((int) c, 63), 0); iMax <= Math.max(Math.min((int) c2, 63), 0); iMax++) {
            j |= 1 << iMax;
        }
        return j;
    }

    private static long highMask(char c, char c2) {
        long j = 0;
        for (int iMax = Math.max(Math.min((int) c, 127), 64) - 64; iMax <= Math.max(Math.min((int) c2, 127), 64) - 64; iMax++) {
            j |= 1 << iMax;
        }
        return j;
    }

    private static boolean match(char c, long j, long j2) {
        return c < '@' ? ((1 << c) & j) != 0 : c < 128 && ((1 << (c - 64)) & j2) != 0;
    }

    private static int decode(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 'a') + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A') + 10;
        }
        throw new AssertionError();
    }
}
