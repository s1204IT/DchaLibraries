package org.tukaani.xz.check;

import android.support.v4.app.DialogFragment;
import java.security.NoSuchAlgorithmException;
import org.tukaani.xz.UnsupportedOptionsException;

public abstract class Check {
    String name;
    int size;

    public abstract byte[] finish();

    public abstract void update(byte[] bArr, int i, int i2);

    public void update(byte[] bArr) {
        update(bArr, 0, bArr.length);
    }

    public int getSize() {
        return this.size;
    }

    public String getName() {
        return this.name;
    }

    public static Check getInstance(int i) throws UnsupportedOptionsException {
        if (i == 4) {
            return new CRC64();
        }
        if (i != 10) {
            switch (i) {
                case DialogFragment.STYLE_NORMAL:
                    return new None();
                case DialogFragment.STYLE_NO_TITLE:
                    return new CRC32();
            }
        }
        try {
            return new SHA256();
        } catch (NoSuchAlgorithmException e) {
        }
        throw new UnsupportedOptionsException("Unsupported Check ID " + i);
    }
}
