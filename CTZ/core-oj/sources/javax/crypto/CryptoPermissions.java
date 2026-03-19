package javax.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import javax.crypto.CryptoPolicyParser;

final class CryptoPermissions extends PermissionCollection implements Serializable {
    CryptoPermissions() {
    }

    void load(InputStream inputStream) throws CryptoPolicyParser.ParsingException, IOException {
    }

    boolean isEmpty() {
        return true;
    }

    @Override
    public void add(Permission permission) {
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }

    @Override
    public Enumeration elements() {
        return null;
    }

    CryptoPermissions getMinimum(CryptoPermissions cryptoPermissions) {
        return null;
    }

    PermissionCollection getPermissionCollection(String str) {
        return null;
    }
}
