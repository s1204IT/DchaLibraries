package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Slog;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import libcore.io.IoUtils;

public class ManifestDigest implements Parcelable {
    public static final Parcelable.Creator<ManifestDigest> CREATOR = new Parcelable.Creator<ManifestDigest>() {
        @Override
        public ManifestDigest createFromParcel(Parcel source) {
            return new ManifestDigest(source);
        }

        @Override
        public ManifestDigest[] newArray(int size) {
            return new ManifestDigest[size];
        }
    };
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String TAG = "ManifestDigest";
    private static final String TO_STRING_PREFIX = "ManifestDigest {mDigest=";
    private final byte[] mDigest;

    ManifestDigest(byte[] digest) {
        this.mDigest = digest;
    }

    private ManifestDigest(Parcel source) {
        this.mDigest = source.createByteArray();
    }

    static ManifestDigest fromInputStream(InputStream fileIs) {
        if (fileIs == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            DigestInputStream dis = new DigestInputStream(new BufferedInputStream(fileIs), md);
            try {
                try {
                    byte[] readBuffer = new byte[8192];
                    do {
                    } while (dis.read(readBuffer, 0, readBuffer.length) != -1);
                    IoUtils.closeQuietly(dis);
                    byte[] digest = md.digest();
                    return new ManifestDigest(digest);
                } catch (IOException e) {
                    Slog.w(TAG, "Could not read manifest");
                    IoUtils.closeQuietly(dis);
                    return null;
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(dis);
                throw th;
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException("SHA-256 must be available", e2);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ManifestDigest)) {
            return false;
        }
        ManifestDigest other = (ManifestDigest) o;
        return this == other || Arrays.equals(this.mDigest, other.mDigest);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mDigest);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(TO_STRING_PREFIX.length() + (this.mDigest.length * 3) + 1);
        sb.append(TO_STRING_PREFIX);
        int N = this.mDigest.length;
        for (int i = 0; i < N; i++) {
            byte b = this.mDigest[i];
            IntegralToString.appendByteAsHex(sb, b, false);
            sb.append(',');
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.mDigest);
    }
}
