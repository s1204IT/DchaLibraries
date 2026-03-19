package sun.security.util;

import java.security.Key;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.security.spec.ECParameterSpec;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHKey;

public final class KeyUtil {
    public static final int getKeySize(Key key) {
        int length;
        if (key instanceof Length) {
            try {
                length = ((Length) key).length();
            } catch (UnsupportedOperationException e) {
                length = -1;
            }
            if (length >= 0) {
                return length;
            }
        } else {
            length = -1;
        }
        if (key instanceof SecretKey) {
            SecretKey secretKey = (SecretKey) key;
            if ("RAW".equals(secretKey.getFormat()) && secretKey.getEncoded() != null) {
                return secretKey.getEncoded().length * 8;
            }
            return length;
        }
        if (key instanceof RSAKey) {
            return ((RSAKey) key).getModulus().bitLength();
        }
        if (key instanceof ECKey) {
            ECParameterSpec params = ((ECKey) key).getParams();
            if (params != null) {
                return params.getOrder().bitLength();
            }
            return length;
        }
        if (key instanceof DSAKey) {
            DSAParams params2 = ((DSAKey) key).getParams();
            return params2 != null ? params2.getP().bitLength() : -1;
        }
        if (key instanceof DHKey) {
            return ((DHKey) key).getParams().getP().bitLength();
        }
        return length;
    }
}
