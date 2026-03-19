package android.content.pm;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@SystemApi
public final class InstantAppResolveInfo implements Parcelable {
    private static final String SHA_ALGORITHM = "SHA-256";
    private final InstantAppDigest mDigest;
    private final Bundle mExtras;
    private final List<InstantAppIntentFilter> mFilters;
    private final String mPackageName;
    private final boolean mShouldLetInstallerDecide;
    private final long mVersionCode;
    private static final byte[] EMPTY_DIGEST = new byte[0];
    public static final Parcelable.Creator<InstantAppResolveInfo> CREATOR = new Parcelable.Creator<InstantAppResolveInfo>() {
        @Override
        public InstantAppResolveInfo createFromParcel(Parcel parcel) {
            return new InstantAppResolveInfo(parcel);
        }

        @Override
        public InstantAppResolveInfo[] newArray(int i) {
            return new InstantAppResolveInfo[i];
        }
    };

    public InstantAppResolveInfo(InstantAppDigest instantAppDigest, String str, List<InstantAppIntentFilter> list, int i) {
        this(instantAppDigest, str, list, i, null);
    }

    public InstantAppResolveInfo(InstantAppDigest instantAppDigest, String str, List<InstantAppIntentFilter> list, long j, Bundle bundle) {
        this(instantAppDigest, str, list, j, bundle, false);
    }

    public InstantAppResolveInfo(String str, String str2, List<InstantAppIntentFilter> list) {
        this(new InstantAppDigest(str), str2, list, -1L, null);
    }

    public InstantAppResolveInfo(Bundle bundle) {
        this(InstantAppDigest.UNDEFINED, null, null, -1L, bundle, true);
    }

    private InstantAppResolveInfo(InstantAppDigest instantAppDigest, String str, List<InstantAppIntentFilter> list, long j, Bundle bundle, boolean z) {
        if ((str == null && list != null && list.size() != 0) || (str != null && (list == null || list.size() == 0))) {
            throw new IllegalArgumentException();
        }
        this.mDigest = instantAppDigest;
        if (list != null) {
            this.mFilters = new ArrayList(list.size());
            this.mFilters.addAll(list);
        } else {
            this.mFilters = null;
        }
        this.mPackageName = str;
        this.mVersionCode = j;
        this.mExtras = bundle;
        this.mShouldLetInstallerDecide = z;
    }

    InstantAppResolveInfo(Parcel parcel) {
        this.mShouldLetInstallerDecide = parcel.readBoolean();
        this.mExtras = parcel.readBundle();
        if (this.mShouldLetInstallerDecide) {
            this.mDigest = InstantAppDigest.UNDEFINED;
            this.mPackageName = null;
            this.mFilters = Collections.emptyList();
            this.mVersionCode = -1L;
            return;
        }
        this.mDigest = (InstantAppDigest) parcel.readParcelable(null);
        this.mPackageName = parcel.readString();
        this.mFilters = new ArrayList();
        parcel.readList(this.mFilters, null);
        this.mVersionCode = parcel.readLong();
    }

    public boolean shouldLetInstallerDecide() {
        return this.mShouldLetInstallerDecide;
    }

    public byte[] getDigestBytes() {
        return this.mDigest.mDigestBytes.length > 0 ? this.mDigest.getDigestBytes()[0] : EMPTY_DIGEST;
    }

    public int getDigestPrefix() {
        return this.mDigest.getDigestPrefix()[0];
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public List<InstantAppIntentFilter> getIntentFilters() {
        return this.mFilters;
    }

    @Deprecated
    public int getVersionCode() {
        return (int) (this.mVersionCode & (-1));
    }

    public long getLongVersionCode() {
        return this.mVersionCode;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBoolean(this.mShouldLetInstallerDecide);
        parcel.writeBundle(this.mExtras);
        if (this.mShouldLetInstallerDecide) {
            return;
        }
        parcel.writeParcelable(this.mDigest, i);
        parcel.writeString(this.mPackageName);
        parcel.writeList(this.mFilters);
        parcel.writeLong(this.mVersionCode);
    }

    @SystemApi
    public static final class InstantAppDigest implements Parcelable {
        public static final Parcelable.Creator<InstantAppDigest> CREATOR;
        static final int DIGEST_MASK = -4096;
        public static final InstantAppDigest UNDEFINED = new InstantAppDigest(new byte[0][], new int[0]);
        private static Random sRandom;
        private final byte[][] mDigestBytes;
        private final int[] mDigestPrefix;
        private int[] mDigestPrefixSecure;

        static {
            sRandom = null;
            try {
                sRandom = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                sRandom = new Random();
            }
            CREATOR = new Parcelable.Creator<InstantAppDigest>() {
                @Override
                public InstantAppDigest createFromParcel(Parcel parcel) {
                    if (parcel.readBoolean()) {
                        return InstantAppDigest.UNDEFINED;
                    }
                    return new InstantAppDigest(parcel);
                }

                @Override
                public InstantAppDigest[] newArray(int i) {
                    return new InstantAppDigest[i];
                }
            };
        }

        public InstantAppDigest(String str) {
            this(str, -1);
        }

        public InstantAppDigest(String str, int i) {
            if (str == null) {
                throw new IllegalArgumentException();
            }
            this.mDigestBytes = generateDigest(str.toLowerCase(Locale.ENGLISH), i);
            this.mDigestPrefix = new int[this.mDigestBytes.length];
            for (int i2 = 0; i2 < this.mDigestBytes.length; i2++) {
                this.mDigestPrefix[i2] = (((this.mDigestBytes[i2][0] & 255) << 24) | ((this.mDigestBytes[i2][1] & 255) << 16) | ((this.mDigestBytes[i2][2] & 255) << 8) | ((this.mDigestBytes[i2][3] & 255) << 0)) & DIGEST_MASK;
            }
        }

        private InstantAppDigest(byte[][] bArr, int[] iArr) {
            this.mDigestPrefix = iArr;
            this.mDigestBytes = bArr;
        }

        private static byte[][] generateDigest(String str, int i) {
            ArrayList arrayList = new ArrayList();
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                if (i <= 0) {
                    arrayList.add(messageDigest.digest(str.getBytes()));
                } else {
                    int iLastIndexOf = str.lastIndexOf(46, str.lastIndexOf(46) - 1);
                    if (iLastIndexOf < 0) {
                        arrayList.add(messageDigest.digest(str.getBytes()));
                    } else {
                        arrayList.add(messageDigest.digest(str.substring(iLastIndexOf + 1, str.length()).getBytes()));
                        for (int i2 = 1; iLastIndexOf >= 0 && i2 < i; i2++) {
                            iLastIndexOf = str.lastIndexOf(46, iLastIndexOf - 1);
                            arrayList.add(messageDigest.digest(str.substring(iLastIndexOf + 1, str.length()).getBytes()));
                        }
                    }
                }
                return (byte[][]) arrayList.toArray(new byte[arrayList.size()][]);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("could not find digest algorithm");
            }
        }

        InstantAppDigest(Parcel parcel) {
            int i = parcel.readInt();
            if (i == -1) {
                this.mDigestBytes = null;
            } else {
                this.mDigestBytes = new byte[i][];
                for (int i2 = 0; i2 < i; i2++) {
                    this.mDigestBytes[i2] = parcel.createByteArray();
                }
            }
            this.mDigestPrefix = parcel.createIntArray();
            this.mDigestPrefixSecure = parcel.createIntArray();
        }

        public byte[][] getDigestBytes() {
            return this.mDigestBytes;
        }

        public int[] getDigestPrefix() {
            return this.mDigestPrefix;
        }

        public int[] getDigestPrefixSecure() {
            if (this == UNDEFINED) {
                return getDigestPrefix();
            }
            if (this.mDigestPrefixSecure == null) {
                int length = getDigestPrefix().length;
                int iNextInt = length + 10 + sRandom.nextInt(10);
                this.mDigestPrefixSecure = Arrays.copyOf(getDigestPrefix(), iNextInt);
                while (length < iNextInt) {
                    this.mDigestPrefixSecure[length] = sRandom.nextInt() & DIGEST_MASK;
                    length++;
                }
                Arrays.sort(this.mDigestPrefixSecure);
            }
            return this.mDigestPrefixSecure;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            boolean z = this == UNDEFINED;
            parcel.writeBoolean(z);
            if (z) {
                return;
            }
            if (this.mDigestBytes == null) {
                parcel.writeInt(-1);
            } else {
                parcel.writeInt(this.mDigestBytes.length);
                for (int i2 = 0; i2 < this.mDigestBytes.length; i2++) {
                    parcel.writeByteArray(this.mDigestBytes[i2]);
                }
            }
            parcel.writeIntArray(this.mDigestPrefix);
            parcel.writeIntArray(this.mDigestPrefixSecure);
        }
    }
}
