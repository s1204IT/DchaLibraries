package javax.net.ssl;

import java.net.IDN;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class SNIHostName extends SNIServerName {
    private final String hostname;

    public SNIHostName(String str) {
        String ascii = IDN.toASCII((String) Objects.requireNonNull(str, "Server name value of host_name cannot be null"), 2);
        super(0, ascii.getBytes(StandardCharsets.US_ASCII));
        this.hostname = ascii;
        checkHostName();
    }

    public SNIHostName(byte[] bArr) {
        super(0, bArr);
        try {
            this.hostname = IDN.toASCII(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bArr)).toString());
            checkHostName();
        } catch (RuntimeException | CharacterCodingException e) {
            throw new IllegalArgumentException("The encoded server name value is invalid", e);
        }
    }

    public String getAsciiName() {
        return this.hostname;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SNIHostName) {
            return this.hostname.equalsIgnoreCase(((SNIHostName) obj).hostname);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 527 + this.hostname.toUpperCase(Locale.ENGLISH).hashCode();
    }

    @Override
    public String toString() {
        return "type=host_name (0), value=" + this.hostname;
    }

    public static SNIMatcher createSNIMatcher(String str) {
        if (str == null) {
            throw new NullPointerException("The regular expression cannot be null");
        }
        return new SNIHostNameMatcher(str);
    }

    private void checkHostName() {
        if (this.hostname.isEmpty()) {
            throw new IllegalArgumentException("Server name value of host_name cannot be empty");
        }
        if (this.hostname.endsWith(".")) {
            throw new IllegalArgumentException("Server name value of host_name cannot have the trailing dot");
        }
    }

    private static final class SNIHostNameMatcher extends SNIMatcher {
        private final Pattern pattern;

        SNIHostNameMatcher(String str) {
            super(0);
            this.pattern = Pattern.compile(str, 2);
        }

        @Override
        public boolean matches(SNIServerName sNIServerName) {
            SNIHostName sNIHostName;
            if (sNIServerName == null) {
                throw new NullPointerException("The SNIServerName argument cannot be null");
            }
            if (!(sNIServerName instanceof SNIHostName)) {
                if (sNIServerName.getType() != 0) {
                    throw new IllegalArgumentException("The server name type is not host_name");
                }
                try {
                    sNIHostName = new SNIHostName(sNIServerName.getEncoded());
                } catch (IllegalArgumentException | NullPointerException e) {
                    return false;
                }
            } else {
                sNIHostName = (SNIHostName) sNIServerName;
            }
            String asciiName = sNIHostName.getAsciiName();
            if (this.pattern.matcher(asciiName).matches()) {
                return true;
            }
            return this.pattern.matcher(IDN.toUnicode(asciiName)).matches();
        }
    }
}
