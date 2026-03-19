package android.content.pm;

import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiScanner;
import android.os.Parcel;
import android.os.Parcelable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class EphemeralResolveInfo implements Parcelable {
    public static final Parcelable.Creator<EphemeralResolveInfo> CREATOR = new Parcelable.Creator<EphemeralResolveInfo>() {
        @Override
        public EphemeralResolveInfo createFromParcel(Parcel in) {
            return new EphemeralResolveInfo(in);
        }

        @Override
        public EphemeralResolveInfo[] newArray(int size) {
            return new EphemeralResolveInfo[size];
        }
    };
    public static final String SHA_ALGORITHM = "SHA-256";
    private final byte[] mDigestBytes;
    private final int mDigestPrefix;
    private final List<IntentFilter> mFilters = new ArrayList();
    private final String mPackageName;

    public EphemeralResolveInfo(Uri uri, String packageName, List<IntentFilter> filters) {
        if (uri == null || packageName == null || filters == null || filters.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.mDigestBytes = generateDigest(uri);
        this.mDigestPrefix = (this.mDigestBytes[0] << 24) | (this.mDigestBytes[1] << WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK) | (this.mDigestBytes[2] << 8) | (this.mDigestBytes[3] << 0);
        this.mFilters.addAll(filters);
        this.mPackageName = packageName;
    }

    EphemeralResolveInfo(Parcel in) {
        this.mDigestBytes = in.createByteArray();
        this.mDigestPrefix = in.readInt();
        this.mPackageName = in.readString();
        in.readList(this.mFilters, null);
    }

    public byte[] getDigestBytes() {
        return this.mDigestBytes;
    }

    public int getDigestPrefix() {
        return this.mDigestPrefix;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public List<IntentFilter> getFilters() {
        return this.mFilters;
    }

    private static byte[] generateDigest(Uri uri) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hostBytes = uri.getHost().getBytes();
            return digest.digest(hostBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("could not find digest algorithm");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByteArray(this.mDigestBytes);
        out.writeInt(this.mDigestPrefix);
        out.writeString(this.mPackageName);
        out.writeList(this.mFilters);
    }

    public static final class EphemeralResolveIntentInfo extends IntentFilter {
        private final EphemeralResolveInfo mResolveInfo;

        public EphemeralResolveIntentInfo(IntentFilter orig, EphemeralResolveInfo resolveInfo) {
            super(orig);
            this.mResolveInfo = resolveInfo;
        }

        public EphemeralResolveInfo getEphemeralResolveInfo() {
            return this.mResolveInfo;
        }
    }
}
