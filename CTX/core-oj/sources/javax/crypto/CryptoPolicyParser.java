package javax.crypto;

import java.io.IOException;
import java.io.Reader;
import java.security.GeneralSecurityException;

final class CryptoPolicyParser {
    CryptoPolicyParser() {
    }

    void read(Reader reader) throws ParsingException, IOException {
    }

    CryptoPermission[] getPermissions() {
        return null;
    }

    static final class ParsingException extends GeneralSecurityException {
        ParsingException(String str) {
            super("");
        }

        ParsingException(int i, String str) {
            super("");
        }

        ParsingException(int i, String str, String str2) {
            super("");
        }
    }
}
